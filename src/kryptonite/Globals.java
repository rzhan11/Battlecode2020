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
	public static int HQElevation;
	// MapLocations of enemy HQ if map has horizontal, vertical, or rotationally symmetry
	public static MapLocation[] symmetryHQLocations = new MapLocation[3];
	public static int[] isSymmetry = {-1, -1, -1}; // -1 is unknown, 0 is false, 1 is true

	/*
	Values that might change each turn
	*/

	public static boolean firstTurn = true;

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

	/*
	CHECKPOINTS
	 */
	public static boolean reachedVaporatorCheckpoint = false;
	public static boolean reachedNetgunCheckpoint = false;
	public static int reachedDroneCheckpoint = 0;
	public static int reachedLandscaperCheckpoint = 0;


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

		Debug.tlog("Reading the previous round's Transactions");
		Communication.readTransactions(roundNum - 1);

		// tries to find our HQLocation and HQElevation by reading messages
		// will skip turn if not found
		if (myType == RobotType.HQ) {
			HQLocation = here;
			HQElevation = myElevation;
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
		Debug.log("Robot: " + myType);
		Debug.log("roundNum: " + roundNum);
		Debug.log("ID: " + myID);
		Debug.log("Location: " + here);
		Debug.log("actualSensorRadiusSquared: " + actualSensorRadiusSquared);
		Debug.log("Cooldown: " + rc.getCooldownTurns());
		Debug.log();
		if (myID == builderMinerID) {
			Debug.log("I am the builder miner");
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

	public static boolean canPickUpType (RobotType rt) {
		return rt == RobotType.MINER || rt == RobotType.LANDSCAPER || rt == RobotType.COW;
	}

	public static Direction moveLog(MapLocation loc) throws GameActionException {
		Direction move = null;
		if (rc.isReady()) {
			move = Nav.bugNavigate(loc);
			if (move != null) {
				Debug.ttlog("Moved " + move);
			} else {
				Debug.ttlog("But no move found");
			}
		} else {
			Debug.ttlog("But not ready");
		}
		return move;
	}

	// information about digLocations
	public static boolean hasLoadedWallInformation = false;

	public static MapLocation[] innerDigLocations;
	public static boolean[] innerDigLocationsOccupiedMemory; // the last time this digLocation was visible, was it occupied?
	public static int innerDigLocationsLength;

	public static MapLocation[] outerDigLocations;
	public static boolean[] outerDigLocationsOccupiedMemory; // the last time this digLocation was visible, was it occupied?
	public static int outerDigLocationsLength;

	public static int smallWallRingRadius = 3;
	public static int smallWallRingSize = 2 * smallWallRingRadius + 1;
	public static int smallWallDepth; // HQElevation + 3, set in the readTransactionHQFirstTurn
	public static MapLocation[] smallWall;
	public static int smallWallLength;
	public static boolean smallWallComplete = false;

	public static int largeWallRingRadius = 4;
	public static int largeWallRingSize = 2 * largeWallRingRadius + 1;
	public static MapLocation[] largeWall;
	public static int largeWallLength;
	public static boolean largeWallFull = false;

	/*
	Loads wall information into variables
	Takes 4 turns
	*/

	public static void loadWallInformation () throws GameActionException {

		// determine innerDigLocations
		int innnerRingRadius = smallWallRingRadius;
		innerDigLocations = new MapLocation[12];
		innerDigLocationsOccupiedMemory = new boolean[innerDigLocations.length];
		MapLocation templ = HQLocation.translate(innnerRingRadius, innnerRingRadius);
		int index = 0;
		for(int i = 0; i < innnerRingRadius + 1; i++) for(int j = 0; j < innnerRingRadius + 1; j++) {
			MapLocation newl = templ.translate(-2 * i, -2 * j);
			if(inMap(newl) && !HQLocation.equals(newl)) {
				if (maxXYDistance(HQLocation, newl) >= innnerRingRadius) { // excludes holes inside the 5x5 plot
					innerDigLocations[index] = newl;
					index++;
				}
			}
		}
		innerDigLocationsLength = index;

		Globals.endTurn(true);
		Globals.update();

		// finds tiles that are on the 5x5 plot
		smallWall = new MapLocation[49];
		index = 0;
		templ = HQLocation.translate(smallWallRingRadius, smallWallRingRadius);
		for(int i = 0; i < smallWallRingSize; i++) for(int j = 0; j < smallWallRingSize; j++) {
			MapLocation newl = templ.translate(-i, -j);
			if (inMap(newl) && !HQLocation.equals(newl) && !inArray(innerDigLocations, newl, innerDigLocationsLength)) {
				smallWall[index] = newl;
				index++;
			}
		}
		smallWallLength = index;
		smallWallDepth = HQElevation + 3;

		Globals.endTurn(true);
		Globals.update();

		// determine outerDigLocations
		/* @todo - remind Richard if he still wants this
		*/
		int outerRingRadius = largeWallRingRadius + 1;
		outerDigLocations = new MapLocation[20];
		outerDigLocationsOccupiedMemory = new boolean[outerDigLocations.length];
		templ = HQLocation.translate(outerRingRadius, outerRingRadius);
		index = 0;
		for(int i = 0; i < outerRingRadius + 1; i++) for(int j = 0; j < outerRingRadius + 1; j++) {
			MapLocation newl = templ.translate(-2 * i, -2 * j);
			if(inMap(newl) && !HQLocation.equals(newl)) {
				if (maxXYDistance(HQLocation, newl) >= outerRingRadius) { // excludes holes inside the 9x9 plot
					outerDigLocations[index] = newl;
					index++;
				}
			}
		}
		outerDigLocationsLength = index;

		Globals.endTurn(true);
		Globals.update();

		largeWall = new MapLocation[36];
		index = 0;

		// move down along right wall
		templ = HQLocation.translate(largeWallRingRadius, largeWallRingRadius);
		for(int i = 0; i < largeWallRingSize - 1; i++) {
			MapLocation newl = templ.translate(0, -i);
			if(inMap(newl) && !inArray(innerDigLocations, newl, innerDigLocationsLength)) {
				largeWall[index] = newl;
				index++;
			}
		}
		// move left along bottom wall
		templ = HQLocation.translate(largeWallRingRadius, -largeWallRingRadius);
		for(int i = 0; i < largeWallRingSize - 1; i++) {
			MapLocation newl = templ.translate(-i, 0);
			if(inMap(newl) && !inArray(innerDigLocations, newl, innerDigLocationsLength)) {
				largeWall[index] = newl;
				index++;
			}
		}
		// move up along left wall
		templ = HQLocation.translate(-largeWallRingRadius, -largeWallRingRadius);
		for(int i = 0; i < largeWallRingSize - 1; i++) {
			MapLocation newl = templ.translate(0, i);
			if(inMap(newl) && !inArray(innerDigLocations, newl, innerDigLocationsLength)) {
				largeWall[index] = newl;
				index++;
			}
		}
		// move right along top wall
		templ = HQLocation.translate(-largeWallRingRadius, largeWallRingRadius);
		for(int i = 0; i < largeWallRingSize - 1; i++) {
			MapLocation newl = templ.translate(i, 0);
			if(inMap(newl) && !inArray(innerDigLocations, newl, innerDigLocationsLength)) {
				largeWall[index] = newl;
				index++;
			}
		}
		largeWallLength = index;
		Debug.ttlog("LARGE WALL LENGTH: " + largeWallLength);

		Globals.endTurn(true);
		Globals.update();

		hasLoadedWallInformation = true;
	}


	/*
	Assumes cost and isReady are already checked
	Tries to build a given robot type in any direction
	 */
	public static boolean tryBuild (RobotType rt) throws GameActionException {
		for (Direction d : directions) {
			MapLocation loc = rc.adjacentLocation(d);
			if (!rc.senseFlooding(loc) && Nav.checkElevation(loc) && rc.senseRobotAtLocation(loc) == null) {
				Debug.ttlog("Location: " + loc);
				if (rc.isReady()) {
					rc.buildRobot(rt, d);
					teamSoup = rc.getTeamSoup();
					Debug.ttlog("Success");
					return true;
				} else {
					Debug.ttlog("But not ready");
					return false;
				}
			}
		}
		Debug.ttlog("No open spots found");
		return false;
	}
}
