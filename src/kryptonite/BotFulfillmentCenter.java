package kryptonite;

import battlecode.common.*;


import static kryptonite.Communication.*;
import static kryptonite.Debug.*;
import static kryptonite.Map.*;
import static kryptonite.Nav.*;
import static kryptonite.Utils.*;
import static kryptonite.Wall.*;
import static kryptonite.Zones.*;

public class BotFulfillmentCenter extends Globals {

	final public static int NUM_EARLY_DRONE = 3;

	public static int dronesBuilt = 0;
	public static int lastBuildRound = N_INF;

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

		if (isEarly && dronesBuilt < NUM_EARLY_DRONE) {
			buildDrone(getCloseDirections(here.directionTo(getSymmetryLoc())), RobotType.DELIVERY_DRONE.cost);
			return;
		}

		int incomePerRound = 1 + totalVaporators * RobotType.VAPORATOR.maxSoupProduced;
		int spawnDelay = 4 * RobotType.LANDSCAPER.cost / incomePerRound;
		if (roundNum < 400) {
			spawnDelay = 3 * spawnDelay / 2;
		}
		if (roundNum - lastBuildRound > spawnDelay ||
			roundNum >= 1000) {
			buildDrone(getCloseDirections(here.directionTo(getSymmetryLoc())), RobotType.DELIVERY_DRONE.cost);
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
