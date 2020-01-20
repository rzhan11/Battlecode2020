package kryptonite;

import battlecode.common.*;

import static kryptonite.Debug.*;
import static kryptonite.Map.*;

public class BotDesignSchool extends Globals {

	public static int landscapersBuilt = 0;
	public static int roundParity = -1;

	public static void loop() throws GameActionException {
		while (true) {
			try {
				Globals.update();
				if (firstTurn) {
					roundParity = spawnRound % 2;
				}
				turn();
			} catch (Exception e) {
				e.printStackTrace();
			}
			Globals.endTurn(false);
		}
	}

	public static void turn() throws GameActionException {

		if (!rc.isReady()) {
			log("Not ready");
			return;
		}

		if (landscapersBuilt < 5) {
			designBuild(getCloseDirections(here.directionTo(HQLoc)), RobotType.LANDSCAPER.cost);
			return;
		} else {
			if (roundNum % 10 == spawnRound % 10) {
				designBuild(getCloseDirections(here.directionTo(getSymmetryLoc())), RobotType.LANDSCAPER.cost + RobotType.VAPORATOR.cost);
				return;
			}
		}

//		// close to hq one
//		if (roundParity < 0) {
//
//		} else {
//			// other type of design school
//			if (roundNum % 20 == roundNumBuilt % 20) {
//				designBuild(directions);
//				return;
//			}
//		}
	}

	private static void designBuild(Direction[] dirs, int soupLimit) throws GameActionException{
		for (Direction dir : dirs) {
			if (rc.getTeamSoup() >= soupLimit && isDirDryFlatEmpty(dir)) {
				Debug.tlog("We are building");
				Actions.doBuildRobot(RobotType.LANDSCAPER, dir);
				landscapersBuilt++;
				return;
			}
		}
		return;
	}
}
