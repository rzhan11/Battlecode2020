package kryptonite;

import battlecode.common.*;

public class Globals {
	/*
	Constants for general use
	*/
	final public static int BIG_ARRAY_SIZE = 500;
	final public static int P_INF = 1000000000;
	final public static int N_INF = -1000000000;



	/*
	Constants that will never change
	*/
	public static RobotController rc;
	public static Team us;
	public static int spawnRound; // the first round this robot was called through RobotPlayer.java
	public static Team them;
	public static int myID;
	public static RobotType myType;
	public static int baseSensorRadiusSquared;

	public static int[][] sensableDirections = null; // stores (dx, dy, magnitude) of locations that can be sensed

	public static int mapWidth;
	public static int mapHeight;
	public static Direction[] directions = {Direction.NORTH, Direction.NORTHEAST, Direction.EAST, Direction.SOUTHEAST, Direction.SOUTH, Direction.SOUTHWEST, Direction.WEST, Direction.NORTHWEST}; // 8 directions
	public static Direction[] allDirections = {Direction.NORTH, Direction.NORTHEAST, Direction.EAST, Direction.SOUTHEAST, Direction.SOUTH, Direction.SOUTHWEST, Direction.WEST, Direction.NORTHWEST, Direction.CENTER}; // includes center
	public static Direction[] cardinalDirections = {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST}; // four cardinal directions
	public static Direction[] diagonalDirections = {Direction.NORTHEAST, Direction.SOUTHEAST, Direction.SOUTHWEST, Direction.NORTHWEST}; // four diagonals

	public static MapLocation HQLocation = null;

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

	public static int oldTransactionsIndex = 1;

	public static void init(RobotController theRC) throws GameActionException {
		rc = theRC;

		us = rc.getTeam();
		them = us.opponent();

		myID = rc.getID();
		myType = rc.getType();
		baseSensorRadiusSquared = myType.sensorRadiusSquared;
		sensableDirections = calculateSensableDirections(baseSensorRadiusSquared);

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
	}

	public static void update() throws GameActionException {
		here = rc.getLocation();
		roundNum = rc.getRoundNum();
		teamSoup = rc.getTeamSoup();

		Debug.log();
		if (firstTurn) {
			Debug.tlog("---------------");
			Debug.tlog("--FIRST TURN---");
			Debug.tlog("---------------");
		}
		Debug.tlog("Robot: " + myType);
		Debug.tlog("roundNum: " + roundNum);
		Debug.tlog("ID: " + myID);
		Debug.tlog("Location: " + here);
		Debug.tlog("Cooldown: " + rc.getCooldownTurns());

		myElevation = rc.senseElevation(here);
		waterLevel = (int) GameConstants.getWaterLevel(roundNum);

		myPollution = rc.sensePollution(here);
		actualSensorRadiusSquared = (int) (baseSensorRadiusSquared * GameConstants.getSensorRadiusPollutionCoefficient(myPollution));
		extremePollution = actualSensorRadiusSquared < 2;
		if (extremePollution) {
			Debug.tlog("WARNING: Extreme pollution has made actualSensorRadiusSquared < 2, so errors may occur. Ask Richard.");
		}

		visibleAllies = rc.senseNearbyRobots(-1, us); // -1 uses all robots within sense radius

		Communication.readTransactions(roundNum - 1);

		if (myType == RobotType.HQ) {
			HQLocation = here;
		} else {
			while (HQLocation == null) {
				if (oldTransactionsIndex == spawnRound - 1) {
					Debug.tlogi("ERROR: Failed sanity check - Cannot find HQLocation");
				}
				Communication.readTransactions(oldTransactionsIndex);
				oldTransactionsIndex++;
			}
		}
	}

	public static void endTurn() throws GameActionException {
		try {
			firstTurn = false;

			Communication.readOldTransactions();

			// check if we went over the bytecode limit
			int endTurn = rc.getRoundNum();
			if (roundNum != endTurn) {
				Debug.tlogi("ERROR: Exceeded the bytecode limit");
				int bytecodeOver = Clock.getBytecodeNum();
				int turns = endTurn - roundNum;
				Debug.ttlogi("Overused bytecode: " + (bytecodeOver + (turns - 1) * myType.bytecodeLimit));
				Debug.ttlogi("Skipped turns: " + turns);
			}

			Debug.tlog("Ending turn with " + Clock.getBytecodesLeft() + " bytecodes");
			Debug.log();
		} catch (Exception e) {
			e.printStackTrace();
		}
		Clock.yield();
	}

	public static int[][] calculateSensableDirections(int sensorRadiusSquared) {
		int maxRadius = 6; // 6 for HQ sensor radius of 48

		int[] maxdy = new int[maxRadius + 1];
		int size = 1;
		int dy = maxRadius;
		for (int dx = 0; dx <= maxRadius; dx++) {
			while (dy > 0) {
				if (sensorRadiusSquared >= dx * dx + dy * dy) {
					maxdy[dx] = dy;
					size += 4 * dy;
					break;
				} else {
					dy--;
				}
			}
		}

		int[][] temp = new int[size][2];
		temp[0][0] = temp[0][1] = 0;

		int index = 1;
		for (int dx = 0; dx <= maxRadius; dx++) {
			for (dy = 1; dy <= maxdy[dx]; dy++) {
				if (sensorRadiusSquared >= dx * dx + dy * dy) {
					temp[index][0] = dx;
					temp[index][1] = dy;
					index++;
					temp[index][0] = dy;
					temp[index][1] = -dx;
					index++;
					temp[index][0] = -dx;
					temp[index][1] = -dy;
					index++;
					temp[index][0] = -dy;
					temp[index][1] = dx;
					index++;
				}
			}
		}
		return temp;
	}

	public static boolean isInMap(MapLocation ml) {
		return ml.x >= 0 && ml.x < mapWidth && ml.y >= 0 && ml.y < mapHeight;
	}
}
