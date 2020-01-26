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


	public static int dronesBuilt = 0;
	public static int lastBuildRound = N_INF;
	final public static int MAX_DRONES = 24;

	public static boolean isEarly = false;


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
		if (maxXYDistance(HQLoc, here) > wallRingRadius) {
			isEarly = false;
		} else {
			isEarly = true;
			RobotInfo[] localRobots = rc.senseNearbyRobots(HQLoc, 8, us);
			for (RobotInfo ri: localRobots) {
				if (ri.type == myType && ri.ID != myID) {
					isEarly = false;
					break;
				}
			}
		}

	}

	public static void turn() throws GameActionException {
		if (!rc.isReady()) {
			log("Not ready");
			return;
		}

		if (!isEarly) {
			BotFulfillmentCenterRush.turn();
		}

		if (dronesBuilt < MAX_DRONES) {
			dronesBuilt++;
			buildDrone(getCloseDirections(here.directionTo(getSymmetryLoc())), RobotType.DELIVERY_DRONE.cost);
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
