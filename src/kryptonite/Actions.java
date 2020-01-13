package kryptonite;

import battlecode.common.*;

public class Actions extends Globals {

	public static int[] YELLOW = {255, 255, 0}; // moving
	public static int[] MAGENTA = {255, 0, 255}; // picking up robot
	public static int[] PURPLE = {128, 0, 128}; // dropping robot
	public static int[] ORANGE = {256, 128, 0}; // shooting drone
	public static int[] WHITE = {255, 255, 255}; // drone explore symmetry
	public static int[] BLACK = {0, 0, 0}; // drone targetting HQLocation

	private static int[] color;

	public static void doMove (Direction dir) throws GameActionException {
		color = YELLOW;
		rc.setIndicatorLine(here, rc.adjacentLocation(dir), color[0], color[1], color[2]);
		rc.move(dir);
	}

	public static void doPickUpUnit (int id) throws GameActionException {
		color = MAGENTA;
		rc.setIndicatorLine(here, rc.senseRobot(id).location, color[0], color[1], color[2]);
		rc.pickUpUnit(id);
	}

	public static void doDropUnit (Direction dir) throws GameActionException {
		color = PURPLE;
		rc.setIndicatorLine(here, rc.adjacentLocation(dir), color[0], color[1], color[2]);
		rc.dropUnit(dir);
	}

	public static void doShootUnit (int id) throws GameActionException {
		color = ORANGE;
		rc.setIndicatorLine(here, rc.senseRobot(id).location, color[0], color[1], color[2]);
		rc.shootUnit(id);
	}
}
