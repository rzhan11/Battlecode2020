package kryptonite;

import battlecode.common.*;

public class BotDesignSchool extends Globals {

	public static int landscapersMade = 0;
	public static int[] landscaperCheckpoints = {8};

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
		if (landscapersMade < landscaperCheckpoints[0]) {
			for (Direction d : Direction.allDirections()) {
				if (teamSoup >= RobotType.LANDSCAPER.cost && rc.canBuildRobot(RobotType.LANDSCAPER, d) && landscapersMade < landscaperCheckpoints[0]) {
					Debug.tlog("Building landscapers at " + rc.adjacentLocation(d));
					if (rc.isReady()) {
						rc.buildRobot(RobotType.LANDSCAPER, d);
						teamSoup = rc.getTeamSoup();
						landscapersMade++;
						if (landscapersMade >= landscaperCheckpoints[0]) {
							Communication.writeTransactionLandscaperCheckpoint(1);
						}
						Debug.ttlog("Success");
					} else {
						Debug.ttlog("But not ready");
					}
					return;
				}
			}
		}
	}
}
