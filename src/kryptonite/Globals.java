package kryptonite;

import battlecode.common.*;

public class Globals {

	/*
	Constants that will never change
	*/
	public static RobotController rc;
	public static Team us;
	public static Team them;
	public static int myID;
	public static RobotType myType;
	public static int baseSensorRadiusSquared;

	public static int[][] sensableDirections = null; // stores (dx, dy) of locations that can be sensed

	public static int mapWidth;
	public static int mapHeight;
	public static Direction[] directions = {Direction.NORTH, Direction.NORTHEAST, Direction.EAST, Direction.SOUTHEAST, Direction.SOUTH, Direction.SOUTHWEST, Direction.WEST, Direction.NORTHWEST}; // 8 directions
	public static Direction[] allDirections = {Direction.NORTH, Direction.NORTHEAST, Direction.EAST, Direction.SOUTHEAST, Direction.SOUTH, Direction.SOUTHWEST, Direction.WEST, Direction.NORTHWEST, Direction.CENTER}; // includes center
	public static Direction[] cardinalDirections = {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST}; // four cardinal directions
	public static Direction[] diagonalDirections = {Direction.NORTHEAST, Direction.SOUTHEAST, Direction.SOUTHWEST, Direction.NORTHWEST}; // four diagonals

	/*
	Values that might change each turn
	*/

	public static int roundNum;
	public static int teamSoup;

	public static MapLocation here;
	public static int myElevation;
	public static int waterLevel;


	public static int myPollution;
	public static int actualSensorRadiusSquared;
	public static boolean extremePollution; // when

	public static RobotInfo[] visibleAllies = null;

	public static void init(RobotController theRC) throws GameActionException {
		boolean b = (directions[0] == Direction.NORTH);
		System.out.println("ss " + b);
		rc = theRC;
		here = rc.getLocation();

		us = rc.getTeam();
		them = us.opponent();

		myID = rc.getID();
		myType = rc.getType();
		baseSensorRadiusSquared = myType.sensorRadiusSquared;
		sensableDirections = calculateSensableDirections(baseSensorRadiusSquared);

		mapWidth = rc.getMapWidth();
		mapHeight = rc.getMapHeight();
	}

	public static void update() throws GameActionException {
		roundNum = rc.getRoundNum();
		teamSoup = rc.getTeamSoup();

		myElevation = rc.senseElevation(here);
		waterLevel = (int) GameConstants.getWaterLevel(roundNum);

		myPollution = rc.sensePollution(here);
		actualSensorRadiusSquared = (int) (baseSensorRadiusSquared * GameConstants.getSensorRadiusPollutionCoefficient(myPollution));
		extremePollution = actualSensorRadiusSquared < 2;
		if (extremePollution) {
			System.out.println("WARNING: Extreme pollution has made actualSensorRadiusSquared < 2, so errors may occur. Ask Richard.");
		}

		visibleAllies = rc.senseNearbyRobots(actualSensorRadiusSquared, us);
	}

	public static void updateRobot() throws GameActionException {
		here = rc.getLocation();
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
					temp[index++][1] = dy;
					temp[index][0] = -dx;
					temp[index++][1] = dy;
					temp[index][0] = dx;
					temp[index++][1] = -dy;
					temp[index][0] = -dx;
					temp[index++][1] = -dy;
				}
			}
		}
		return temp;
	}
}
