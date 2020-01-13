package kryptonite;

import battlecode.common.*;

public class BotDesignSchool extends Globals {

	public static int landscapersMade = 0;
	public static int[] landscaperCheckpoints = {8, 32};
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
		if (visibleEnemies != null) {
			Debug.tlog("Enemies detected");
			if (teamSoup >= RobotType.LANDSCAPER.cost) {
				Debug.tlog("Trying to build protection landscapers");
				boolean didBuild = tryBuild(RobotType.LANDSCAPER);
				if (didBuild) {
					landscapersMade++;
					if (landscapersMade >= landscaperCheckpoints[0] && !checkpointReached[0]) {
						Communication.writeTransactionLandscaperCheckpoint(0);
						checkpointReached[0] = true;
					} else if (landscapersMade >= landscaperCheckpoints[1] && !checkpointReached[1]) {
						Communication.writeTransactionLandscaperCheckpoint(1);
						checkpointReached[1] = true;
					}
				}
			}
			return;
		}
		//initial 8 landscapers
		if (landscapersMade < landscaperCheckpoints[0]) {
			Debug.tlog("Landscaper checkpoint 0 not reached");
			// leave enough to build a refinery
			if (teamSoup >= RobotType.LANDSCAPER.cost + RobotType.REFINERY.cost) {
				Debug.tlog("Trying to build landscaper");
				boolean didBuild = tryBuild(RobotType.LANDSCAPER);
				if (didBuild) {
					landscapersMade++;
					if (landscapersMade >= landscaperCheckpoints[0] && !checkpointReached[0]) {
						Communication.writeTransactionLandscaperCheckpoint(0);
						checkpointReached[0] = true;
					}
				}
			}
			return;
		} else {
			Debug.tlog("Landscaper checkpoint 0 reached");
		}

		if (reachedNetgunCheckpoint) {
			Debug.tlog("Continuing: Netgun checkpoint reached");
		} else {
			Debug.tlog("Returning: Netgun checkpoint not reached");
			return;
		}

		// next 24 landscapers built after the 8 net guns 12 drones and 4 vaporators built
		if (landscapersMade < landscaperCheckpoints[1]) {
			if (teamSoup >= RobotType.LANDSCAPER.cost + RobotType.REFINERY.cost) {
				Debug.tlog("Trying to build landscaper");
				boolean didBuild = tryBuild(RobotType.LANDSCAPER);
				if (didBuild) {
					landscapersMade++;
				}
				if (landscapersMade >= landscaperCheckpoints[1] && !checkpointReached[1]) {
					Communication.writeTransactionLandscaperCheckpoint(1);
					checkpointReached[1] = true;
				}
			}
			Debug.tlog("Can't afford landscaper");
		} else {
			Debug.tlog("Landscaper checkpoint 1 reached");
		}


		return;
	}
}
