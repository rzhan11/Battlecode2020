package kryptonite;

import battlecode.common.*;

public class BotHQ extends Globals {

	private static int minerSpawnCount = 0;

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
		/*
		build a Miner if we can afford it, are not on cooldown, and less than four Miners have been built
		*/
		boolean builtMinerFlag = false;
		if (teamSoup >= RobotType.MINER.cost && rc.isReady()) {
			if (minerSpawnCount < 4) {
				Direction dir = diagonalDirections[minerSpawnCount];
		        if (rc.canBuildRobot(RobotType.MINER, dir)) {
		            rc.buildRobot(RobotType.MINER, dir);
					minerSpawnCount++;
					builtMinerFlag = true;
				}
			}
		}
	}
}
