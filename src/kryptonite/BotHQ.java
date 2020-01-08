package kryptonite;

import battlecode.common.*;

public class BotHQ extends Globals {

	private static boolean[] exploredDirections = new boolean[8]; // whether or not a Miner has been sent in this direction

	public static void loop() throws GameActionException {
		while (true) {
			try {
				Globals.update();
			    turn();
			} catch (Exception e) {
				e.printStackTrace();
			}
			Globals.endTurn();
		}
	}

	public static void turn() throws GameActionException {
		/*
		build a Miner if we can afford it, are not on cooldown, and less than eight Miners have been built
		*/
		if (teamSoup >= RobotType.MINER.cost && rc.isReady()) {
			for (int i = 0; i < directions.length; i++) {
				Direction dir = directions[i];
				MapLocation loc = rc.adjacentLocation(dir);
		        if (!exploredDirections[i] && rc.canBuildRobot(RobotType.MINER, dir)) {
		            rc.buildRobot(RobotType.MINER, dir);
					exploredDirections[i] = true;
					return;
				}
			}
		}
	}
}
