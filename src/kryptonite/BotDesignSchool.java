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
				MapLocation loc = rc.adjacentLocation(d);
				if (teamSoup >= RobotType.LANDSCAPER.cost + RobotType.REFINERY.cost + Communication.REFINERY_BUILT_COST
						&& Nav.checkElevation(loc) && !rc.senseFlooding(loc)
						&& landscapersMade < landscaperCheckpoints[0]) {
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
