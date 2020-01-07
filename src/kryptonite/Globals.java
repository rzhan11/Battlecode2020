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

	public static Direction[] directions = {Direction.NORTH, Direction.NORTH_EAST, Direction.EAST, Direction.SOUTH_EAST, Direction.SOUTH, Direction.SOUTH_WEST, Direction.WEST, Direction.NORTH_WEST};

	public static void init(RobotController theRC) {
		rc = theRC;
		here = rc.getLocation();

		us = rc.getTeam();
		them = us.opponent();

		myID = rc.getID();
		myType = rc.getType();
		mySensorRadiusSquared = myType.sensorRadiusSquared;
	}

	/*
	Values that might change each turn
	*/

	public static MapLocation here;
	public static int roundNum;

	public static void update() {
		here = rc.getLocation();
		roundNum = rc.getRoundNum();
	}
}
