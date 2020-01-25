package rush;

import battlecode.common.*;

import static rush.Communication.*;
import static rush.Constants.*;
import static rush.Debug.*;
import static rush.Map.*;

public class Actions extends Globals {

	public static void doMove (Direction dir) throws GameActionException {
		drawLine(here, rc.adjacentLocation(dir), YELLOW);
		rc.move(dir);
	}

	public static void doPickUpUnit (int id) throws GameActionException {
		drawLine(here, rc.senseRobot(id).location, MAGENTA);
		rc.pickUpUnit(id);
	}

	public static void doDropUnit (Direction dir) throws GameActionException {
		drawLine(here, rc.adjacentLocation(dir), PURPLE);
		rc.dropUnit(dir);
	}

	public static void doShootUnit (int id) throws GameActionException {
		drawLine(here, rc.senseRobot(id).location, ORANGE);
		rc.shootUnit(id);
	}

	public static void doDigDirt (Direction dir) throws GameActionException {
		if (dir == Direction.CENTER) {
			drawDot(here, WHITE);
		} else {
			drawLine(here, rc.adjacentLocation(dir), WHITE);
		}
		rc.digDirt(dir);
	}

	public static void doDepositDirt (Direction dir) throws GameActionException {
		if (dir == Direction.CENTER) {
			drawDot(here, BLACK);
		} else {
			drawLine(here, rc.adjacentLocation(dir), BLACK);
		}
		rc.depositDirt(dir);
	}

	public static void doBuildRobot (RobotType type, Direction dir) throws GameActionException {
		drawLine(here, rc.adjacentLocation(dir), CYAN);
		rc.buildRobot(type, dir);
	}
}
