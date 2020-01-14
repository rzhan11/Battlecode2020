package kryptonite;

import battlecode.common.*;

public class BotHQ extends Globals {

	final public static int RELAX_LARGE_WALL_FULL_ROUND_NUM = 1000;
	final public static int RELAX_LARGE_WALL_FULL_AMOUNT = 3;

	private static boolean[] exploredDirections = new boolean[8]; // whether or not a Miner has been sent in this direction
	private static int explorerMinerCount = 0;

	// has miner been built to check horizontal, vertical, rotational symmetries
	private static boolean[] builtSymmetryMiner = new boolean[3];
	private static int symmetryMinerCount = 0;

	private static boolean madeBuilderMiner = false;

	public static MapLocation soupLocation = null;

	final private static int BLOCK_LANDSCAPER_DIST = 13;

	public static void loop() throws GameActionException {
		while (true) {
			try {
				Globals.update();
				if (firstTurn) {
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
			Globals.endTurn(false);
		}
	}

	public static void turn() throws GameActionException {
		if (!hasLoadedWallInformation && madeBuilderMiner) {
			loadWallInformation();
		}

		if (visibleEnemies.length > 0) {
			for (RobotInfo ri : visibleEnemies) {
				if (ri.type == RobotType.LANDSCAPER && here.distanceSquaredTo(ri.location) < RobotType.MINER.sensorRadiusSquared) {
					Debug.tlog("Enemy Landscaper detected");
					if (teamSoup >= RobotType.MINER.cost) {
						Debug.tlog("Trying to build protection miner");
						boolean didBuild = tryBuild(RobotType.MINER, directions);
						if (didBuild) {
							explorerMinerCount++;
						}
					}
				}
			}
		}
		if (hasLoadedWallInformation) {
			if(!smallWallFinished) {
				boolean canSeeAll = true;
				smallWallFinished = true; // temporary flag, will be set to false if not valid

				for (int i = 0; i < smallWallLength; i++) {
					if (rc.canSenseLocation(smallWall[i])) {
						RobotInfo unit = rc.senseRobotAtLocation(smallWall[i]);
						if(unit == null || !unit.type.isBuilding()) {
							if(rc.senseElevation(smallWall[i]) != smallWallDepth) {
								smallWallFinished = false;
								Debug.tlog("smallWall not complete at " + smallWall[i]);
								break;
							}
						}
					} else { // if cannot see one of the walls, do not mark as complete
						canSeeAll = false;
						break;
					}
				}

				if (!canSeeAll) {
					Debug.tlog("Cannot see all of small wall");
				} else {
					if(smallWallFinished) {
						Debug.ttlog("SMALL WALL IS DONE");
						Communication.writeTransactionSmallWallComplete();
					}
				}

			}

			if (!largeWallFull) {
				boolean canSeeAll = true;
				int count = 0;
				for (int i = 0; i < largeWallLength; i++) {
					if (rc.canSenseLocation(largeWall[i])) {
						RobotInfo unit = rc.senseRobotAtLocation(largeWall[i]);
						if (unit != null && unit.type == RobotType.LANDSCAPER) {
							count++;
						}
					} else {
						canSeeAll = false;
						break;
					}
				}

				Debug.tlog("Large wall full: " + count + " / " + largeWallLength);
				if (!canSeeAll) {
					Debug.tlog("Cannot see all of large wall");
				} else {
					if (count == largeWallLength) {
						Debug.ttlog("LARGE WALL IS FULL");
						largeWallFull = true;
						Communication.writeTransactionLargeWallFull();
					} else if (count >= largeWallLength - RELAX_LARGE_WALL_FULL_AMOUNT && roundNum >= RELAX_LARGE_WALL_FULL_ROUND_NUM) {
						Debug.ttlog("LARGE WALL IS ABOUT FULL");
						largeWallFull = true;
						Communication.writeTransactionLargeWallFull();
					}
				}
			}
		}

		// try to shoot the closest visible enemy units
		if (rc.isReady()) {
			int closestDist = P_INF;
			int id = -1;
			//MapLocation here = rc.getLocation();

			for (RobotInfo ri: visibleEnemies) {
				if (ri.type == RobotType.DELIVERY_DRONE) {
					int dist = here.distanceSquaredTo(ri.location);
					if(dist < closestDist){
						closestDist = dist;
						id = ri.ID;
					}
				}
			}

			// shoot radius less than sensor radius
			if(id != -1 && closestDist <= GameConstants.NET_GUN_SHOOT_RADIUS_SQUARED) {
				Actions.doShootUnit(id);
			}
		}

		//if explorer miners have been build and have enough money, build a BuilderMiner
		if(true) {
			if (explorerMinerCount >= 8 && !madeBuilderMiner && teamSoup >= (RobotType.MINER.cost + 1) && rc.isReady()) {
				//try building
				for (int k = 0; k < 8; k++) {
					if (rc.canBuildRobot(RobotType.MINER, directions[k])) {
						Actions.doBuildRobot(RobotType.MINER, directions[k]);
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

		// @todo: Create Attack Miners
	}

	/*
	Does not affect HQ yet - Remind Richard
	*/
	public static void locateNearbySoup () throws GameActionException {
		int minDistance = Integer.MAX_VALUE;

		for (int[] dir: senseDirections) {
			if (actualSensorRadiusSquared < dir[2]) {
				break;
			}
			MapLocation loc = here.translate(dir[0], dir[1]);
			if (rc.canSenseLocation(loc) && rc.senseSoup(loc) > 0) {
				soupLocation = loc;
				break;
			}
		}
		return;
	}

	/*
	Tries to build a miner in an unexplored direction
	Returns true if built a miner
	Returns false if did not build a miner
	*/
	public static boolean buildMiner() throws GameActionException {
		Direction[] orderedDirections;
		if (soupLocation == null) {
			orderedDirections = getCloseDirections(here.directionTo(symmetryHQLocations[2]));
		} else
		orderedDirections = getCloseDirections(here.directionTo(soupLocation));

		for (int i = 0; i < orderedDirections.length; i++) {
			Direction dir = orderedDirections[i];
			MapLocation loc = rc.adjacentLocation(dir);
			if (!exploredDirections[i] && rc.canBuildRobot(RobotType.MINER, dir)) {
				Actions.doBuildRobot(RobotType.MINER, dir);
				teamSoup = rc.getTeamSoup();
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
				Actions.doBuildRobot(RobotType.MINER, dir);
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
