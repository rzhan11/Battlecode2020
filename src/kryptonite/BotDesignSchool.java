package kryptonite;

import battlecode.common.*;

public class BotDesignSchool extends Globals {

	public static boolean firstTurn = true;
	public static int landscapersMade = 0;

	public static void loop() throws GameActionException {
		while (true) {
			int startTurn = rc.getRoundNum();
			try {
				Globals.update();
			    turn();
			} catch (Exception e) {
				e.printStackTrace();
			}
			int endTurn = rc.getRoundNum();
			if (startTurn != endTurn) {
				System.out.println("OVER BYTECODE LIMIT");
			}
			Clock.yield();
		}
	}

	public static void turn() throws GameActionException {
		if (rc.isReady()) {
			for (Direction d : Direction.allDirections()) {
				if (teamSoup >= RobotType.LANDSCAPER.cost && rc.canBuildRobot(RobotType.LANDSCAPER, d)) {
					rc.buildRobot(RobotType.LANDSCAPER, d);
					landscapersMade++;
				}
			}
		}
		return;
	}
}
