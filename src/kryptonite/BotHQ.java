package kryptonite;

import battlecode.common.*;

public class BotHQ extends Globals {

	private static int minerSpawnCount = 0;

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
			if (minerSpawnCount < 8) {
				Direction dir = directions[minerSpawnCount];
		        if (rc.canBuildRobot(RobotType.MINER, dir)) {
		            rc.buildRobot(RobotType.MINER, dir);
					minerSpawnCount++;
					return;
				}
			}
		}
	}
}
