package kryptonite;

import battlecode.common.*;

public class BotDesignSchool extends Globals {

	public static int landscapersMade = 0;

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
			Globals.endTurn();
		}
	}

	public static void turn() throws GameActionException {
		if (rc.isReady()) {
			for (Direction d : Direction.allDirections()) {
				if(d.equals(Direction.NORTH) || d.equals(Direction.SOUTH)) continue;
				if (teamSoup >= RobotType.LANDSCAPER.cost && rc.canBuildRobot(RobotType.LANDSCAPER, d) && landscapersMade < 8) {
					rc.buildRobot(RobotType.LANDSCAPER, d);
					teamSoup = rc.getTeamSoup();
					landscapersMade++;
				}
			}
		}
		return;
	}
}
