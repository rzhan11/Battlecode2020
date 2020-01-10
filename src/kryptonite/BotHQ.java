package kryptonite;

import battlecode.common.*;

public class BotHQ extends Globals {

	private static boolean[] exploredDirections = new boolean[8]; // whether or not a Miner has been sent in this direction
	private static int explorerMinerCount = 0;

	// has miner been built to check horizontal, vertical, rotational symmetries
	private static boolean[] builtSymmetryMiner = new boolean[3];
	// MapLocations of enemy HQ if map has horizontal, vertical, or rotationally symmetry
	private static MapLocation[] symmetryHQLocations = new MapLocation[3];
	private static int symmetryMinerCount = 0;


	private static boolean madeBuilderMiner = false;

	public static void loop() throws GameActionException {
		while (true) {
			try {
				Globals.update();
				if (firstTurn) {
					// calculates possible enemy HQ locations
					symmetryHQLocations[0] = new MapLocation(mapWidth - 1 - here.x, here.y);
					symmetryHQLocations[1] = new MapLocation(here.x, mapHeight - 1 - here.y);
					symmetryHQLocations[2] = new MapLocation(mapWidth - 1 - here.x, mapHeight - 1 - here.y);
					Debug.tlog("Possible enemy HQ locations");
					Debug.ttlog("" + symmetryHQLocations[0]);
					Debug.ttlog("" + symmetryHQLocations[1]);
					Debug.ttlog("" + symmetryHQLocations[2]);

					Communication.writeTransactionHQFirstTurn(here);

					// finds visible soup locations
					locateNearbySoup();

				}
			    turn();
			} catch (Exception e) {
				e.printStackTrace();
			}
			Globals.endTurn();
		}
	}

	public static void turn() throws GameActionException {

		//if explorer miners have been build and have enough money, build a BuilderMiner
		if(true) {
			if (explorerMinerCount >= 8 && !madeBuilderMiner && teamSoup >= (RobotType.MINER.cost + 1) && rc.isReady()) {
				//try building
				for (int k = 0; k < 8; k++) {
					if (rc.canBuildRobot(RobotType.MINER, directions[k])) {
						rc.buildRobot(RobotType.MINER, directions[k]);
						teamSoup = rc.getTeamSoup();
						madeBuilderMiner = true;

						//make transaction
						RobotInfo ri = rc.senseRobotAtLocation(here.add(directions[k]));
						//SEND TRANSACTION
						Communication.writeTransactionBuilderMinerBuilt(ri.ID);
						return;
					}
				}
			}
		}
		/*
		build explorer miners
		build three Miners to explore symmetries
		EDIT: DRONES SHOULD EXPLORE SYMMETRIES
		*/
		if (teamSoup >= RobotType.MINER.cost && rc.isReady()) {
			buildMiner();
			// if (symmetryMinerCount < 3) {
			// 	buildSymmetryMiner();
			// } else {
			//	buildMiner();
			// }
		}

		// @todo: Shoot Enemy Bots

		// @todo: Create Attack Miners
	}

	/*
	Does not affect HQ yet - Remind Richard
	*/
	public static void locateNearbySoup () throws GameActionException {
		MapLocation[] soups = new MapLocation[senseDirections.length];
		int size = 0;

		int totalX = 0;
		int totalY = 0;
		int visibleSoup = 0;
		for (int[] dir: senseDirections) {
			MapLocation loc = here.translate(dir[0], dir[1]);
			if (rc.canSenseLocation(loc) && rc.senseSoup(loc) > 0) {
				totalX += loc.x;
				totalY += loc.y;
				visibleSoup += rc.senseSoup(loc);

				soups[size] = loc;
				size++;
			}
		}
		if (size == 0) {
			return;
		}
	}

	/*
	Tries to build a miner in an unexplored direction
	Returns true if built a miner
	Returns false if did not build a miner
	*/
	public static boolean buildMiner() throws GameActionException {
		for (int i = 0; i < directions.length; i++) {
			Direction dir = directions[i];
			MapLocation loc = rc.adjacentLocation(dir);
	        if (!exploredDirections[i] && rc.canBuildRobot(RobotType.MINER, dir)) {
	            rc.buildRobot(RobotType.MINER, dir);
				exploredDirections[i] = true;
				explorerMinerCount++;
				return true;
			}
		}
		return false;
	}

	/*
	Tries to build a symmetry miner if not all three symmetry miners have been built
	Returns true if built a symmetry miner
	Returns false if did not build a symmetry miner
	*/
	public static boolean buildSymmetryMiner() throws GameActionException {
		for (int i = 0; i < symmetryHQLocations.length; i++) {
			Direction dir = here.directionTo(symmetryHQLocations[i]);
			MapLocation loc = rc.adjacentLocation(dir);
			if (!builtSymmetryMiner[i] && rc.canBuildRobot(RobotType.MINER, dir)) {
				rc.buildRobot(RobotType.MINER, dir);
				teamSoup = rc.getTeamSoup();
				builtSymmetryMiner[i] = true;
				symmetryMinerCount++;

				RobotInfo ri = rc.senseRobotAtLocation(loc);
				Communication.writeTransactionSymmetryMinerBuilt(ri.ID, symmetryHQLocations[i]);
				return true;
			}
		}
		return false;
	}
}
