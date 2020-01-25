package rush_bot;

import battlecode.common.*;

import static rush_bot.Actions.*;
import static rush_bot.Communication.*;
import static rush_bot.Debug.*;
import static rush_bot.Globals.*;
import static rush_bot.Map.*;
import static rush_bot.Nav.*;
import static rush_bot.Utils.*;
import static rush_bot.Wall.*;
import static rush_bot.Zones.*;

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

		loadInnerWallInfo();

		initializedDesignSchool = true;

	}

	public static void turn() throws GameActionException {

		if (!rc.isReady()) {
			log("Not ready");
			return;
		}

		if (landscapersBuilt < smallWallLocsLength + supportWallLocsLength) {
			buildLandscaper(getCloseDirections(here.directionTo(HQLoc)), RobotType.LANDSCAPER.cost);
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
	}
}
