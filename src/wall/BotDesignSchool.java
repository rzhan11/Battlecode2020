package wall;

import battlecode.common.*;

import static wall.Communication.*;
import static wall.Constants.*;
import static wall.Debug.*;
import static wall.Map.*;

public class BotDesignSchool extends Globals {

	public static int landscapersMade = 0;
	public static int[] landscaperCheckpoints = {4, 32, 48};
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

		if (visibleEnemies.length > 0) {
			log("Enemies detected");
			if (rc.getTeamSoup() >= RobotType.LANDSCAPER.cost) {
				log("Trying to build protection landscapers");
				boolean didBuild = tryBuild(RobotType.LANDSCAPER, directions);
				if (didBuild) {
					landscapersMade++;
					if (landscapersMade >= landscaperCheckpoints[0] && !checkpointReached[0]) {
						writeTransactionLandscaperCheckpoint(0);
						checkpointReached[0] = true;
					} else if (landscapersMade >= landscaperCheckpoints[1] && !checkpointReached[1]) {
						writeTransactionLandscaperCheckpoint(1);
						checkpointReached[1] = true;
					}
				}
			}
			return;
		}
		//initial 31 landscapers
		if (landscapersMade < landscaperCheckpoints[0]) {
			log("Landscaper checkpoint 0 not reached");
			log("soup " + rc.getTeamSoup());
			// leave enough to build a refinery
			if (rc.getTeamSoup() >= RobotType.LANDSCAPER.cost + RobotType.REFINERY.cost) {
				log("Trying to build landscaper");
				boolean didBuild = tryBuild(RobotType.LANDSCAPER, directions);
				if (didBuild) {
					landscapersMade++;
					if (landscapersMade >= landscaperCheckpoints[0] && !checkpointReached[0]) {
						writeTransactionLandscaperCheckpoint(0);
						checkpointReached[0] = true;
					}
				}
			}
			return;
		} else {
			log("Landscaper checkpoint 0 reached");
		}

		if (reachedNetgunCheckpoint) {
			log("Continuing: Netgun checkpoint reached");
		} else {
			log("Returning: Netgun checkpoint not reached");
			return;
		}

		// 32 landscapers built after the 8 net guns 12 drones and 4 vaporators built
		if (landscapersMade < landscaperCheckpoints[1]) {
			if (rc.getTeamSoup() >= RobotType.LANDSCAPER.cost + RobotType.REFINERY.cost) {
				log("Trying to build landscaper");
				boolean didBuild = tryBuild(RobotType.LANDSCAPER, directions);
				if (didBuild) {
					landscapersMade++;
				}
				if (landscapersMade >= landscaperCheckpoints[1] && !checkpointReached[1]) {
					writeTransactionLandscaperCheckpoint(1);
					checkpointReached[1] = true;
				}
			}
			log("Can't afford landscaper");
		} else {
			log("Landscaper checkpoint 1 reached");
		}

		if (largeWallFull) {
			log("Continuing: largeWallFull checkpoint reached");
		} else {
			log("Returning: largeWallFull checkpoint not reached");
			return;
		}

		// comment from here
		// next 16 landscapers built after largeWallFull
//		if (landscapersMade < landscaperCheckpoints[2]) {
//			if (rc.getTeamSoup() >= RobotType.LANDSCAPER.cost) {
//				log("Trying to build landscaper");
//				boolean didBuild = tryBuild(RobotType.LANDSCAPER, directions);
//				if (didBuild) {
//					landscapersMade++;
//				}
//				if (landscapersMade >= landscaperCheckpoints[2] && !checkpointReached[1]) {
//					writeTransactionLandscaperCheckpoint(2);
//					checkpointReached[2] = true;
//				}
//			}
//			log("Can't afford landscaper");
//		} else {
//			log("Landscaper checkpoint 2 reached");
//		}
		// to here

		return;
	}
}
