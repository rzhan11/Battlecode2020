package kryptonite;

import battlecode.common.*;


import static kryptonite.Communication.*;
import static kryptonite.Debug.*;
import static kryptonite.Map.*;
import static kryptonite.Nav.*;
import static kryptonite.Utils.*;
import static kryptonite.Zones.*;

public class BotDesignSchool extends Globals {

	public static int landscapersBuilt = 0;

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

		if (roundNum % 25 == spawnRound % 25) {
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
				return;
			}
		}
		return;
	}
}
