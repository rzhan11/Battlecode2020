package rush;

import battlecode.common.*;

import static rush.Communication.*;
import static rush.Constants.*;
import static rush.Debug.*;
import static rush.Map.*;

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
