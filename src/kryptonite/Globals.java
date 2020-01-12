package kryptonite;

import battlecode.common.*;

public class Globals {
	/*
	Constants for general use
	*/
	final public static int P_INF = 1000000000;
	final public static int N_INF = -1000000000;
	final public static int BIG_ARRAY_SIZE = 500;
	final public static int MAX_MAP_SIZE = 64;


	/*
	Constants that will never change
	*/
	public static RobotController rc;
	public static int spawnRound; // the first round this robot was called through RobotPlayer.java
	public static Team us;
	public static Team them;
	public static Team cowTeam;
	public static int myID;
	public static RobotType myType;
	public static int baseSensorRadiusSquared;

	public static int[][] senseDirections = null; // stores (dx, dy, magnitude) of locations that can be sensed

	public static int mapWidth;
	public static int mapHeight;
	public static Direction[] directions = {Direction.NORTH, Direction.NORTHEAST, Direction.EAST, Direction.SOUTHEAST, Direction.SOUTH, Direction.SOUTHWEST, Direction.WEST, Direction.NORTHWEST}; // 8 directions
	public static Direction[] allDirections = {Direction.NORTH, Direction.NORTHEAST, Direction.EAST, Direction.SOUTHEAST, Direction.SOUTH, Direction.SOUTHWEST, Direction.WEST, Direction.NORTHWEST, Direction.CENTER}; // includes center
	public static Direction[] cardinalDirections = {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST}; // four cardinal directions
	public static Direction[] diagonalDirections = {Direction.NORTHEAST, Direction.SOUTHEAST, Direction.SOUTHWEST, Direction.NORTHWEST}; // four diagonals

	public static MapLocation HQLocation = null;
	// MapLocations of enemy HQ if map has horizontal, vertical, or rotationally symmetry
	public static MapLocation[] symmetryHQLocations = new MapLocation[3];

	/*
	Values that might change each turn
	*/

	public static boolean firstTurn = true;

	public static boolean smallWallComplete = false;
	public static int roundNum;
	public static int teamSoup;

	public static MapLocation here;
	public static int myElevation;
	public static int waterLevel;

	public static int myPollution;
	public static int actualSensorRadiusSquared;
	public static boolean extremePollution; // when

	public static RobotInfo[] visibleAllies = null;
	public static RobotInfo[] visibleEnemies = null;
	public static RobotInfo[] visibleCows = null;

	public static RobotInfo[] adjacentAllies = null;
	public static RobotInfo[] adjacentEnemies = null;
	public static RobotInfo[] adjacentCows = null;

	// checks if drones picked up and dropped this unit
	public static boolean droppedLastTurn = false;
	public static int lastActiveTurn = 0;

	public static int oldTransactionsIndex = 1;

	public static int builderMinerID = -1;

	public static void init(RobotController theRC) throws GameActionException {
		rc = theRC;

		us = rc.getTeam();
		them = us.opponent();
		cowTeam = Team.NEUTRAL;

		myID = rc.getID();
		myType = rc.getType();
		baseSensorRadiusSquared = myType.sensorRadiusSquared;
		senseDirections = HardCode.getSenseDirections(myType); //calculateSenseDirections(baseSensorRadiusSquared);

		mapWidth = rc.getMapWidth();
		mapHeight = rc.getMapHeight();

		if (us == Team.A) {
			Communication.secretKey = 1337;
		} else {
			Communication.secretKey = 7331;
		}

		here = rc.getLocation();
		roundNum = rc.getRoundNum();
		spawnRound = roundNum;

		lastActiveTurn = spawnRound - 1;
	}

	public static void update() throws GameActionException {
		here = rc.getLocation();
		roundNum = rc.getRoundNum();
		teamSoup = rc.getTeamSoup();

		if (firstTurn) {
			Debug.log();
			Debug.tlog("---------------");
			Debug.tlog("--FIRST TURN---");
			Debug.tlog("---------------");
		}

		myElevation = rc.senseElevation(here);
		waterLevel = (int) GameConstants.getWaterLevel(roundNum);

		myPollution = rc.sensePollution(here);
		actualSensorRadiusSquared = rc.getCurrentSensorRadiusSquared();
		extremePollution = actualSensorRadiusSquared < 2;
		if (extremePollution) {
			Debug.tlog("WARNING: Extreme pollution has made actualSensorRadiusSquared < 2, so errors may occur. Ask Richard.");
		}

		printMyInfo();

		visibleAllies = rc.senseNearbyRobots(-1, us); // -1 uses all robots within sense radius
		visibleEnemies = rc.senseNearbyRobots(-1, them);
		visibleCows = rc.senseNearbyRobots(-1, cowTeam);

		adjacentAllies = rc.senseNearbyRobots(2, us);
		adjacentEnemies = rc.senseNearbyRobots(2, them);
		adjacentCows = rc.senseNearbyRobots(2, cowTeam);

		if (roundNum == lastActiveTurn + 1) {
			droppedLastTurn = false;
		} else {
			droppedLastTurn = true;
			Debug.tlog("Was dropped last turn");
			Nav.bugTracing = false;
			Nav.bugLastWall = null;
			Nav.bugClosestDistanceToTarget = P_INF;
		}

		Communication.readTransactions(roundNum - 1);

		// tries to find our HQLocation by reading messages
		if (myType == RobotType.HQ) {
			HQLocation = here;
		} else {
			while (HQLocation == null) {
				if (oldTransactionsIndex == spawnRound - 1) {
					Debug.tlogi("Cannot find HQLocation");
					Globals.endTurn(true);
					Globals.update();
				}
				Communication.readTransactions(oldTransactionsIndex);
				oldTransactionsIndex++;
				if (HQLocation == null && isLowBytecodeLimit(myType)) {
					Debug.tlog("Did not find HQLocation, low bytecode limit");
					Globals.endTurn(true);
					Globals.update();
				}
			}
		}

		// calculates possible enemy HQ locations
		if (firstTurn) {
			symmetryHQLocations[0] = new MapLocation(mapWidth - 1 - HQLocation.x, HQLocation.y);
			symmetryHQLocations[1] = new MapLocation(HQLocation.x, mapHeight - 1 - HQLocation.y);
			symmetryHQLocations[2] = new MapLocation(mapWidth - 1 - HQLocation.x, mapHeight - 1 - HQLocation.y);
		}

		// tries to submit unsent messages from previous turns
		Communication.submitUnsentTransactions();
	}

	/*
	Prints various useful debuggin information
	*/
	final public static boolean noTurnLog = false;

	public static void printMyInfo () {
		if(noTurnLog) return;
		Debug.log();
		Debug.tlog("Robot: " + myType);
		Debug.tlog("roundNum: " + roundNum);
		Debug.tlog("ID: " + myID);
		Debug.tlog("Location: " + here);
		Debug.tlog("actualSensorRadiusSquared: " + actualSensorRadiusSquared);
		Debug.tlog("Cooldown: " + rc.getCooldownTurns());
		if (myID == builderMinerID) {
			Debug.tlog("I am the builder miner");
		}
	}

	/*
	earlyEnd should be true, unless this method was called at the end of the loop() method
	*/
	public static void endTurn (boolean earlyEnd) throws GameActionException {
		try {
			firstTurn &= earlyEnd; // if early end, do not count as full turn
			lastActiveTurn = roundNum;

			Communication.readOldTransactions();
			// check if we went over the bytecode limit
			int endTurn = rc.getRoundNum();
			if (roundNum != endTurn) {
				printMyInfo();
				Debug.tlogi("BYTECODE LIMIT EXCEEDED");
				int bytecodeOver = Clock.getBytecodeNum();
				int turns = endTurn - roundNum;
				Debug.ttlogi("Overused bytecode: " + (bytecodeOver + (turns - 1) * myType.bytecodeLimit));
				Debug.ttlogi("Skipped turns: " + turns);

				// catch up on missed Transactions
				for (int i = roundNum; i < endTurn; i++) {
					Communication.readTransactions(i);
				}
			}
			if(!noTurnLog) {
				Debug.tlog("Remaining bytecode: " + Clock.getBytecodesLeft());
				Debug.tlog("---------------");
				if (earlyEnd) {
					Debug.tlog("-----EARLY-----");
				}
				Debug.tlog("---END TURN----");
				Debug.tlog("---------------");
				Debug.log();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		Clock.yield();
	}

	/*
	Returns true if the location is within the map boundaries
	Returns false if not
	*/
	public static boolean inMap(MapLocation ml) {
		return ml.x >= 0 && ml.x < mapWidth && ml.y >= 0 && ml.y < mapHeight;
	}

	/*
	Useful for ring structures
	*/
	public static int maxXYDistance(MapLocation ml1, MapLocation ml2) {
		return Math.max(Math.abs(ml1.x - ml2.x), Math.abs(ml1.y - ml2.y));
	}

	public static boolean inArray(Object[] arr, Object item, int length) {
		for(int i = 0; i < length; i++) if(arr[i].equals(item)) return true;
		return false;
	}

	/*
	Returns true if this robot ID is the builder miner
	*/
	public static boolean isBuilderMiner (int id) {
		return id == builderMinerID;
	}

	/*
	Returns true if this RobotType has a low limit <= 7000
	*/
	public static boolean isLowBytecodeLimit (RobotType rt) {
		switch (rt) {
			case DESIGN_SCHOOL:
			case FULFILLMENT_CENTER:
			case NET_GUN:
			case REFINERY:
			case VAPORATOR:
				return true;
			default:
				return false;

		}
	}

}
