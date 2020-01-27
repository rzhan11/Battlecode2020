package decent_rush_bot;

import battlecode.common.*;

import java.awt.*;

import static decent_rush_bot.Communication.*;
import static decent_rush_bot.Debug.*;
import static decent_rush_bot.Map.*;
import static decent_rush_bot.Utils.*;
import static decent_rush_bot.Wall.*;
import static decent_rush_bot.Communication.*;

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

				turn();
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

		if (isRushDS) {
			BotDesignSchoolRush.turn();
			return;
		}

		// build 2 landscapers
		if (landscapersBuilt < 2) {
			buildLandscaper(getCloseDirections(here.directionTo(HQLoc)), RobotType.LANDSCAPER.cost);
			log("Created landscaper " + landscapersBuilt);
			return;
		}

		if (platformLandscaperID == -1) {
			int newID = buildLandscaper(getCloseDirections(here.directionTo(platformCornerLoc)), RobotType.LANDSCAPER.cost);
			if (newID != -1) {
				platformLandscaperID = newID;
				writeTransactionAssignPlatform(platformLandscaperID);
			}
			log("Created platform landscaper");
			return;
		}

		if (!initialWallSetup && roundNum - lastBuildRound > 25) {
			buildLandscaper(getCloseDirections(here.directionTo(HQLoc)), RobotType.LANDSCAPER.cost);
			log("Created extra init landscaper " + landscapersBuilt);
			return;
		}

		if (supportFull) {
			log("Returning: Support full");
			return;
		}

		// if platform buildings have taken too long to complete
//		if () {
//
//		}

		// create landscapers if platform buildings are completed
		if (platformBuildingsCompleted || rc.getTeamSoup() >= RobotType.VAPORATOR.cost + RobotType.LANDSCAPER.cost) {
			int id = buildLandscaper(getCloseDirections(here.directionTo(HQLoc)), RobotType.LANDSCAPER.cost);
			log("Created wall/support landscaper " + landscapersBuilt);
			return;
		}
	}

	public static int buildLandscaper(Direction[] orderedDirs, int soupLimit) throws GameActionException{
		if (rc.getTeamSoup() < soupLimit) {
			log("Team does not have " + soupLimit + " soup");
			return -1;
		}
		outer: for (Direction dir : orderedDirs) {
			MapLocation loc = rc.adjacentLocation(dir);
			if (!rc.onTheMap(loc)) {
				continue;
			}
			RobotInfo[] spawnLocEnemies = rc.senseNearbyRobots(loc, 2, them);
			for (RobotInfo ri: spawnLocEnemies) {
				if (ri.type == RobotType.DELIVERY_DRONE) {
					continue outer;
				}
			}
			log("loc " + loc);
			if (isLocDryFlatEmpty(loc)) {
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
