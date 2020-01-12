package kryptonite;

import battlecode.common.*;

public class BotDesignSchool extends Globals {

	public static int landscapersMade = 0;
	public static int maxLandscapersMade = 12;

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
		if (rc.isReady()) {
			for (Direction d : Direction.allDirections()) {
				int actualMax = maxLandscapersMade * (1 + roundNum / 500);
				if (teamSoup >= RobotType.LANDSCAPER.cost && rc.canBuildRobot(RobotType.LANDSCAPER, d) && landscapersMade < actualMax) {
					rc.buildRobot(RobotType.LANDSCAPER, d);
					teamSoup = rc.getTeamSoup();
					landscapersMade++;
				}
			}
		}
		return;
	}
}
