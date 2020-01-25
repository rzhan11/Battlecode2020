package turtle;

import battlecode.common.*;


import static turtle.Communication.*;
import static turtle.Debug.*;
import static turtle.Map.*;
import static turtle.Nav.*;
import static turtle.Utils.*;
import static turtle.Zones.*;

public class BotDesignSchool extends Globals {

	public static int landscapersBuilt = 0;
	public static int lastBuildRound = N_INF;
	public static boolean isRushDS = false;

	public static void loop() throws GameActionException {
		while (true) {
			try {
				Globals.update();

				if (!initializedDesignSchool) {
					initDesignSchool();
				}

				if (isRushDS) {
					BotDesignSchoolRush.turn();
				} else {
					turn();
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			Globals.endTurn();
		}
	}

	public static boolean initializedDesignSchool = false;

	public static void initDesignSchool() throws GameActionException {

		if (rc.canSenseLocation(getSymmetryLoc())) {
			RobotInfo ri = rc.senseRobotAtLocation(getSymmetryLoc());
			if (ri != null && ri.type == RobotType.HQ && ri.team == them) {
				isRushDS = true;
			}
		}

		initializedDesignSchool = true;

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
			buildLandscaper(getCloseDirections(here.directionTo(getSymmetryLoc())), RobotType.LANDSCAPER.cost);
			return;
		}
	}

	public static void buildLandscaper(Direction[] dirs, int soupLimit) throws GameActionException{
		for (Direction dir : dirs) {
			if (rc.getTeamSoup() >= soupLimit && isDirDryFlatEmpty(dir)) {
				tlog("BUILDING LANDSCAPER " + dir);
				Actions.doBuildRobot(RobotType.LANDSCAPER, dir);
				landscapersBuilt++;
				lastBuildRound = roundNum;
				return;
			}
		}
		return;
	}
}