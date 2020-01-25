package turtle;

import battlecode.common.*;


import static turtle.Communication.*;
import static turtle.Debug.*;
import static turtle.Map.*;
import static turtle.Nav.*;
import static turtle.Utils.*;
import static turtle.Wall.*;
import static turtle.Zones.*;

public class BotFulfillmentCenter extends Globals {

	final public static int NUM_EARLY_DRONE = 3;

	public static int dronesBuilt = 0;
	public static int lastBuildRound = N_INF;

	public static boolean helpRush = false;


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

		// determines if I am the first building of my type


		for (RobotInfo ri: visibleAllies) {
			if (ri.ID == rushMinerID && rc.senseRobotAtLocation(ri.location).ID == rushMinerID) {
				helpRush = true;
			}
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

	}

	public static void buildDrone(Direction[] dirs, int soupLimit) throws GameActionException{
		for (Direction dir : dirs) {
			MapLocation loc = rc.adjacentLocation(dir);
			if (!rc.onTheMap(loc)) {
				continue;
			}
			if (rc.getTeamSoup() >= soupLimit && isDirDryFlatEmpty(dir)) {
				tlog("BUILDING DRONE " + dir);
				Actions.doBuildRobot(RobotType.DELIVERY_DRONE, dir);
				dronesBuilt++;
				lastBuildRound = roundNum;
				return;
			}
		}
		return;
	}
}
