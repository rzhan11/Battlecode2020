package kryptonite;

import battlecode.common.*;


import static kryptonite.Communication.*;
import static kryptonite.Debug.*;
import static kryptonite.Map.*;
import static kryptonite.Nav.*;
import static kryptonite.Utils.*;
import static kryptonite.Zones.*;

public class BotFulfillmentCenter extends Globals {

	public static int dronesBuilt = 0;
	public static int lastBuildRound = N_INF;

	public static void loop() throws GameActionException {
		while (true) {
			try {
				Globals.update();

				turn();
			} catch (Exception e) {
				e.printStackTrace();
			}
			Globals.endTurn();
		}
	}

	public static void turn() throws GameActionException {
		if (!rc.isReady()) {
			log("Not ready");
			return;
		}

		int incomePerRound = 1 + totalVaporators * RobotType.VAPORATOR.maxSoupProduced;
		int spawnDelay = 4 * RobotType.LANDSCAPER.cost / incomePerRound;
		if (roundNum - lastBuildRound > spawnDelay ||
			roundNum >= 1000) {
			buildDrone(getCloseDirections(here.directionTo(getSymmetryLoc())), RobotType.DELIVERY_DRONE.cost);
			return;
		}

	}

	public static void buildDrone(Direction[] dirs, int soupLimit) throws GameActionException{
		for (Direction dir : dirs) {
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
