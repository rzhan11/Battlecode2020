package kryptonite;

import battlecode.common.*;

public class Actions extends Globals {

	public static int[] YELLOW = {255, 255, 0}; // moving
	public static int[] MAGENTA = {255, 0, 255}; // picking up robot
	public static int[] PURPLE = {128, 0, 128}; // dropping robot
	public static int[] ORANGE = {256, 128, 0}; // shooting drone
	public static int[] WHITE = {255, 255, 255}; // drone explore symmetry
	// white also used for dig dirt
	public static int[] BLACK = {0, 0, 0}; // drone targetting HQLocation
	// black also used for deposit dirt
	public static int[] CYAN = {0, 255, 255}; // drone explore symmetry

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

	public static void doDigDirt (Direction dir) throws GameActionException {
		color = WHITE;
		rc.setIndicatorLine(here, rc.adjacentLocation(dir), color[0], color[1], color[2]);
		rc.digDirt(dir);
	}

	public static void doDepositDirt (Direction dir) throws GameActionException {
		color = BLACK;
		rc.setIndicatorLine(here, rc.adjacentLocation(dir), color[0], color[1], color[2]);
		rc.depositDirt(dir);
	}

	public static void doBuildRobot (RobotType type, Direction dir) throws GameActionException {
		color = CYAN;
		rc.setIndicatorLine(here, rc.adjacentLocation(dir), color[0], color[1], color[2]);
		rc.buildRobot(type, dir);
	}
}
