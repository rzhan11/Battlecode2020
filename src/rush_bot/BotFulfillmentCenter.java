package rush_bot;

import battlecode.common.*;

import static rush_bot.Debug.*;
import static rush_bot.Map.*;
import static rush_bot.Utils.*;
import static rush_bot.Wall.*;

public class BotFulfillmentCenter extends Globals {

	final public static int NUM_EARLY_DRONE = 3;

	public static int dronesBuilt = 0;
	public static int lastBuildRound = N_INF;

	public static boolean helpRush = false;
	public static boolean onPlatform = false;

	public static void loop() throws GameActionException {
		while (true) {
			try {
				Globals.update();

				if (!initializedFulfillmentCenter) {
					initFulfillmentCenter();
				}

				turn();
			} catch (Exception e) {
				e.printStackTrace();
			}
			Globals.endTurn();
		}
	}

	public static boolean initializedFulfillmentCenter = false;

	public static void initFulfillmentCenter() throws GameActionException {

		for (RobotInfo ri: visibleAllies) {
			if (ri.ID == rushMinerID && rc.senseRobotAtLocation(ri.location).ID == rushMinerID) {
				helpRush = true;
			}
		}

		if (platformCornerLoc != null && inArray(platformLocs, here, platformLocs.length)) {
			return;
		}
	}

	public static void turn() throws GameActionException {
		if (!rc.isReady()) {
			log("Not ready");
			return;
		}

		if (helpRush) {
			BotFulfillmentCenterRush.turn();
			return;
		}

		if (dronesBuilt < 1) {
			buildDrone(getCloseDirections(here.directionTo(HQLoc)), RobotType.DELIVERY_DRONE.cost);
			return;
		}

		if (wallFull) {
			// spam drones
			if (supportFull) {
				buildDrone(getCloseDirections(here.directionTo(HQLoc)), RobotType.DELIVERY_DRONE.cost);
			}

			boolean seesDrone = false;
			for (RobotInfo ri: visibleAllies) {
				if (ri.type == RobotType.DELIVERY_DRONE) {
					seesDrone = true;
					break;
				}
			}
			if (!seesDrone) {
				buildDrone(getCloseDirections(here.directionTo(HQLoc)), RobotType.DELIVERY_DRONE.cost);
			}
		}

	}

	public static void buildDrone(Direction[] dirs, int soupLimit) throws GameActionException{
		for (Direction dir : dirs) {
			MapLocation loc = rc.adjacentLocation(dir);
			if (!rc.onTheMap(loc)) {
				continue;
			}
			if (rc.getTeamSoup() >= soupLimit && isLocEmpty(loc)) {
				Actions.doBuildRobot(RobotType.DELIVERY_DRONE, dir);
				dronesBuilt++;
				lastBuildRound = roundNum;
				return;
			}
		}
		return;
	}
}
