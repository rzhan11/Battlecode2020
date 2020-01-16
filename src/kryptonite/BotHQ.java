package kryptonite;

import battlecode.common.*;

import static kryptonite.Communication.*;
import static kryptonite.Constants.*;
import static kryptonite.Debug.*;
import static kryptonite.Map.*;

public class BotHQ extends Globals {

	final public static int RELAX_LARGE_WALL_FULL_ROUND_NUM = 1000;
	final public static int RELAX_LARGE_WALL_FULL_AMOUNT = 4;

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
					log("Possible enemy HQ locations");
					tlog("" + symmetryHQLocations[0]);
					tlog("" + symmetryHQLocations[1]);
					tlog("" + symmetryHQLocations[2]);

					writeTransactionHQFirstTurn(here);

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
								log("smallWall not complete at " + smallWall[i]);
								break;
							}
						}
					} else { // if cannot see one of the walls, do not mark as complete
						canSeeAll = false;
						break;
					}
				}

				if (!canSeeAll) {
					log("Cannot see all of small wall");
				} else {
					if(smallWallFinished) {
						tlog("SMALL WALL IS DONE");
						writeTransactionSmallWallComplete();
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

				log("Large wall full: " + count + " / " + largeWallLength);
				if (!canSeeAll) {
					log("Cannot see all of large wall");
				} else {
					if (count == largeWallLength) {
						tlog("LARGE WALL IS FULL");
						largeWallFull = true;
						writeTransactionLargeWallFull();
					} else if (count >= largeWallLength - RELAX_LARGE_WALL_FULL_AMOUNT && roundNum >= RELAX_LARGE_WALL_FULL_ROUND_NUM) {
						tlog("LARGE WALL IS ABOUT FULL");
						largeWallFull = true;
						writeTransactionLargeWallFull();
					}
				}
			}
		}

		if (!rc.isReady()) {
			log("Not ready");
			return;
		}

		// 1. Loop through visible enemies and identify if any are bad landscapers
		// 2. Find the closest landscaper and go that way
		// 3. Check if I can place a troop in that direction
		// 4. If I can't move onto the next one
		int minDist = P_INF;
		Direction closestEnemyDir = null;
		for (RobotInfo ri : visibleEnemies) {
			if (ri.type == RobotType.LANDSCAPER) {
				int dist = here.distanceSquaredTo(ri.location);
				if (dist < minDist) {
					Direction dir = here.directionTo(ri.location);
					if (isDirDryFlatEmpty(dir)) {
						minDist = dist;
						closestEnemyDir = dir;
					}
					if (isDirDryFlatEmpty(dir.rotateLeft())) {
						minDist = dist;
						closestEnemyDir = dir.rotateLeft();
					}
					if (isDirDryFlatEmpty(dir.rotateRight())) {
						minDist = dist;
						closestEnemyDir = dir.rotateRight();
					}
				}
			}
		}
		if (closestEnemyDir != null) {
			if (teamSoup >= RobotType.MINER.cost) {
				log("Building defensive miner");
				Actions.doBuildRobot(RobotType.MINER, closestEnemyDir);
				explorerMinerCount++;
				return;
			} else {
				log("Would build defensive miner but can't afford it");
			}
		}

		// try to shoot the closest visible enemy units
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
			log("Shooting unit");
			Actions.doShootUnit(id);
			return;
		}

		/*
		build explorer miners
		build three Miners to explore symmetries
		EDIT: DRONES SHOULD EXPLORE SYMMETRIES
		*/
		if (explorerMinerCount < 8 && teamSoup >= RobotType.MINER.cost) {
			buildMiner();
		} else {
			log("Not enough soup to build explorer miner");
		}

		//if explorer miners have been build and have enough money, build a BuilderMiner
		if (!madeBuilderMiner && teamSoup >= RobotType.MINER.cost) {
			//try building
			for (int k = 0; k < 8; k++) {
				if (rc.canBuildRobot(RobotType.MINER, directions[k])) {
					log("Building builder miner");
					Actions.doBuildRobot(RobotType.MINER, directions[k]);
					teamSoup = rc.getTeamSoup();
					madeBuilderMiner = true;

					//make transaction
					RobotInfo ri = rc.senseRobotAtLocation(here.add(directions[k]));
					//SEND TRANSACTION
					writeTransactionBuilderMinerBuilt(ri.ID);

					return;
				}
			}
		}

		// @todo: Create Attack Miners
	}

	/*
	Does not affect HQ yet - Remind Richard
	*/
	public static void locateNearbySoup () throws GameActionException {
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
			orderedDirections = getCloseDirections(here.directionTo(getSymmetryLocation()));
		} else {
			orderedDirections = getCloseDirections(here.directionTo(soupLocation));
		}

		for (int i = 0; i < orderedDirections.length; i++) {
			Direction dir = orderedDirections[i];
			MapLocation loc = rc.adjacentLocation(dir);
			if (rc.canBuildRobot(RobotType.MINER, dir)) {
				Actions.doBuildRobot(RobotType.MINER, dir);
				teamSoup = rc.getTeamSoup();
				explorerMinerCount++;
				return true;
			}
		}

		return false;
	}

}
