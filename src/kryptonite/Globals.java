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

	public static Direction[] directions = {Direction.NORTH, Direction.NORTHEAST, Direction.EAST, Direction.SOUTHEAST, Direction.SOUTH, Direction.SOUTHWEST, Direction.WEST, Direction.NORTHWEST};

	public static Direction[] diagonalDirections = {Direction.NORTHEAST, Direction.SOUTHEAST, Direction.SOUTHWEST, Direction.NORTHWEST};

	public static void init(RobotController theRC) {
		rc = theRC;

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
	public static int teamSoup;

	public static void update() {
		here = rc.getLocation();
		roundNum = rc.getRoundNum();
		teamSoup = rc.getTeamSoup();
	}
}
