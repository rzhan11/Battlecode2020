package kryptonite;

import battlecode.common.*;

import static kryptonite.Communication.*;
import static kryptonite.Constants.*;
import static kryptonite.Debug.*;
import static kryptonite.Map.*;

public class BotDesignSchool extends Globals {

	public static int landscapersBuilt = 0;
	public static int roundParity = -1;
	public static int roundNumBuilt = 0;

	public static void loop() throws GameActionException {
		while (true) {
			try {
				Globals.update();
				if (firstTurn) {

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
		if (roundNumBuilt == 0) {
			roundNumBuilt = roundNum;
			roundParity = roundNum % 2;
		}
		// close to hq one
		if (roundParity < 0) {
			if (landscapersBuilt < 5) {
				designBuild(getCloseDirections(here.directionTo(HQLocation)));
				return;
			}
		}

		// other type of design school
		else {
			if (roundNum % 20 == roundNumBuilt % 20) {
				designBuild(directions);
				return;
			}
		}
	}

	private static void designBuild(Direction[] dirs) throws GameActionException{
		for (Direction dir : dirs) {
			if (rc.getTeamSoup() >= RobotType.LANDSCAPER.cost + RobotType.REFINERY.cost && isDirDryFlatEmpty(dir)) {
				Debug.tlog("We are building");
				Actions.doBuildRobot(RobotType.LANDSCAPER, dir);
				landscapersBuilt++;
				return;
			}
		}
		return;
	}
}
