package turtle;

import battlecode.common.*;

import static turtle.Actions.*;
import static turtle.Communication.*;
import static turtle.Debug.*;
import static turtle.Globals.*;
import static turtle.Map.*;
import static turtle.Nav.*;
import static turtle.Utils.*;
import static turtle.Wall.*;
import static turtle.Zones.*;

public class Actions extends Globals {

	public static void doMove (Direction dir) throws GameActionException {
		log("MOVING " + dir);
		drawLine(here, rc.adjacentLocation(dir), YELLOW);
		rc.move(dir);
	}

	public static void doPickUpUnit (int id) throws GameActionException {
		RobotInfo ri = rc.senseRobot(id);
		log("PICKING UP " + ri.type + " " + here.directionTo(ri.location));
		drawLine(here, ri.location, PINK);
		rc.pickUpUnit(id);
	}

	public static void doDropUnit (Direction dir) throws GameActionException {
		log("DROPPING UNIT " + dir);
		drawLine(here, rc.adjacentLocation(dir), PURPLE);
		rc.dropUnit(dir);
	}

	public static void doShootUnit (int id) throws GameActionException {
		RobotInfo ri = rc.senseRobot(id);
		log("SHOOTING UNIT AT " + ri.location);
		drawLine(here, rc.senseRobot(id).location, ORANGE);
		rc.shootUnit(id);
	}

	public static void doDigDirt (Direction dir) throws GameActionException {
		if (dir == Direction.CENTER) {
			drawDot(here, WHITE);
		} else {
			drawLine(here, rc.adjacentLocation(dir), WHITE);
		}
		log("DIGGING DIRT " + dir);
		rc.digDirt(dir);
	}

	public static void doDepositDirt (Direction dir) throws GameActionException {
		if (dir == Direction.CENTER) {
			drawDot(here, BLACK);
		} else {
			drawLine(here, rc.adjacentLocation(dir), BLACK);
		}
		log("DEPOSITING DIRT " + dir);
		rc.depositDirt(dir);
	}

	public static void doBuildRobot (RobotType type, Direction dir) throws GameActionException {
		drawLine(here, rc.adjacentLocation(dir), CYAN);
		log("BUILDING " + type + " " + dir);
		rc.buildRobot(type, dir);
	}

	public static void doMineSoup (Direction dir) throws GameActionException {
		if (dir == Direction.CENTER) {
			drawDot(here, MAGENTA);
		} else {
			drawLine(here, rc.adjacentLocation(dir), MAGENTA);
		}
		log("MINING " + dir);
		rc.mineSoup(dir);
	}

	public static void doDepositSoup (Direction dir, int amount) throws GameActionException {
		log("DEPOSITING SOUP " + dir);
		drawLine(here, rc.adjacentLocation(dir), GRAY);
		rc.depositSoup(dir, amount);
	}
}
