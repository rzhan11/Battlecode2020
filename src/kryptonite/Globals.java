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
	public static int mySensorRadiusSquared;

	public static int[][] sensableDirections = null; // stores (dx, dy) of locations that can be sensed

	public static Direction[] directions = {Direction.NORTH, Direction.NORTHEAST, Direction.EAST, Direction.SOUTHEAST, Direction.SOUTH, Direction.SOUTHWEST, Direction.WEST, Direction.NORTHWEST};
	public static Direction[] diagonalDirections = {Direction.NORTHEAST, Direction.SOUTHEAST, Direction.SOUTHWEST, Direction.NORTHWEST};

	/*
	Values that might change each turn
	*/

	public static MapLocation here;
	public static int roundNum;
	public static int teamSoup;

	public static RobotInfo[] visibleAllies = null;

	public static void init(RobotController theRC) {
		rc = theRC;
		here = rc.getLocation();

		us = rc.getTeam();
		them = us.opponent();

		myID = rc.getID();
		myType = rc.getType();
		mySensorRadiusSquared = myType.sensorRadiusSquared;
		sensableDirections = calculateSensableDirections(mySensorRadiusSquared);
	}

	public static void update() {
		roundNum = rc.getRoundNum();
		teamSoup = rc.getTeamSoup();
		visibleAllies = rc.senseNearbyRobots(mySensorRadiusSquared, us);
	}

	public static void updateRobot() {
		here = rc.getLocation();
	}

	public static int[][] calculateSensableDirections(int sensorRadiusSquared) {
		// Temporary debug
		return new int[0][2];
		// int maxWidth = 7; // 6 + 1 to be safe (for HQ)
		//
		// int size = 0;
		// for (int dx = -maxWidth; dx <= maxWidth; dx++) {
		// 	for (int dy = -maxWidth; dy <= maxWidth; dy++) {
		// 		if (sensorRadiusSquared >= dx * dx + dy * dy) {
		// 			size++;
		// 		}
		// 	}
		// }
		// int[][] temp = new int[size][2];
		//
		// int index = 0;
		// for (int dx = -maxWidth; dx <= maxWidth; dx++) {
		// 	for (int dy = -maxWidth; dy <= maxWidth; dy++) {
		// 		if (sensorRadiusSquared >= dx * dx + dy * dy) {
		// 			temp[index][0] = dx;
		// 			temp[index][1] = dy;
		// 			System.out.println("dx, dy " + dx + " " + dy);
		// 			index++;
		// 		}
		// 	}
		// }
		// return temp;
	}
}
