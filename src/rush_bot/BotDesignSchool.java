package rush_bot;

import battlecode.common.*;

import static rush_bot.Debug.*;
import static rush_bot.Map.*;
import static rush_bot.Utils.*;
import static rush_bot.Wall.*;
import static rush_bot.Communication.*;

public class BotDesignSchool extends Globals {

	public static int landscapersBuilt = 0;
	public static int lastBuildRound = N_INF;
	public static boolean isRushDS = false;
	public static boolean assignedPlatform = false;


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

		loadWallInfo();

		initializedDesignSchool = true;

	}

	public static void turn() throws GameActionException {

		if (!rc.isReady()) {
			log("Not ready");
			return;
		}

		if (landscapersBuilt < wallLocsLength + supportWallLocsLength) {
			int aID = buildLandscaper(getCloseDirections(here.directionTo(HQLoc)), RobotType.LANDSCAPER.cost);
			if(aID != -1 && landscapersBuilt == 6) {
					writeTransactionAssignPlatform(aID);
			}
			return;
		}
	}

	public static int buildLandscaper(Direction[] orderedDirs, int soupLimit) throws GameActionException{
		for (Direction dir : orderedDirs) {
			MapLocation loc = rc.adjacentLocation(dir);
			if (!rc.onTheMap(loc)) {
				continue;
			}
			if (rc.getTeamSoup() >= soupLimit && isLocDryFlatEmpty(loc)) {
				tlog("BUILDING LANDSCAPER " + dir);
				Actions.doBuildRobot(RobotType.LANDSCAPER, dir);
				landscapersBuilt++;
				lastBuildRound = roundNum;
				return rc.senseRobotAtLocation(here.add(dir)).ID;
			}
		}
		return -1;
	}
}
