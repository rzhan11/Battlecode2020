package landscaper_rush_bot;

import battlecode.common.*;

import static landscaper_rush_bot.Communication.*;
import static landscaper_rush_bot.Constants.*;
import static landscaper_rush_bot.Debug.*;
import static landscaper_rush_bot.Map.*;

public class BotDesignSchool extends Globals {

	public static int landscapersMade = 0;
	public static int[] landscaperCheckpoints = {16, 32, 48};
	public static boolean[] checkpointReached = {false, false};

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

		Direction[] dirToEnemyHQ = getCloseDirections(here.directionTo(getSymmetryLocation()));

		for (Direction dir : dirToEnemyHQ) {
			if (isDirDryFlatEmpty(dir)) {
				Actions.doBuildRobot(RobotType.LANDSCAPER, dir);
				landscapersMade++;
				if (landscapersMade >= landscaperCheckpoints[0]) {
					writeTransactionLandscaperCheckpoint(0);
				}
			}
		}
		return;
	}
}
