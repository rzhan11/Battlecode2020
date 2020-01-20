package kryptonite;

import battlecode.common.*;

import static kryptonite.Communication.*;
import static kryptonite.Constants.*;
import static kryptonite.Debug.*;
import static kryptonite.Map.*;
import static kryptonite.Zones.*;

public class BotHQ extends Globals {

	private static int explorerMinerCount = 0;

	private static boolean madeBuilderMiner = false;


	public static void loop() throws GameActionException {
		while (true) {
			try {
				Globals.update();
				log("Bytecode left " + Clock.getBytecodesLeft());
				if (firstTurn) {
					log("Possible enemy HQ locations");
					tlog("" + symmetryHQLocations[0]);
					tlog("" + symmetryHQLocations[1]);
					tlog("" + symmetryHQLocations[2]);

					writeTransactionHQFirstTurn(here);

					// finds visible soup locations

					closestVisibleSoupLoc = findClosestVisibleSoupLoc(true);
					log("closestVisibleSoupLocation: " + closestVisibleSoupLoc);

				}
			    turn();
			} catch (Exception e) {
				e.printStackTrace();
			}
			Globals.endTurn(false);
		}
	}

	public static void turn() throws GameActionException {
		Communication.resubmitImportantTransactions();

		if (!rc.isReady()) {
			return;
		}

		if(!wallFull) {
			boolean flag = true;
			for(Direction d : directions) {
				MapLocation loc = rc.adjacentLocation(d);
				if (!rc.onTheMap(loc)) {
					continue;
				}
				if(!isLocAllyLandscaper(loc)) {
					flag = false;
					break;
				}
			}
			if(flag) {
				Communication.writeTransactionWallFull();
			}
		}
		if (!supportFull) {
			boolean flag = true;
			for(int i = 2; i >= -2; i--) for(int j = 2; j >= -2; j--) {
				if(Math.abs(i) != 2 && Math.abs(j) != 2) continue;
				MapLocation loc = here.translate(i, j);
				if (!rc.onTheMap(loc)) {
					continue;
				}
				if (isDigLoc(loc)) {
					continue;
				}
				if(!isLocAllyLandscaper(loc)) {
					flag = false;
					break;
				}
			}
			if(flag) {
				Communication.writeTransactionSupportWallFull();
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
		build explorer miners that explore symmetries
		*/
		if (explorerMinerCount < 7) {
			if (rc.getTeamSoup() >= RobotType.MINER.cost) {
				buildMiner();
			} else {
				log("Not enough soup to build explorer miner");
			}
			return;
		}

		//if explorer miners have been build and have enough money, build a BuilderMiner
		if (!madeBuilderMiner) {
			if(rc.getTeamSoup() >= RobotType.MINER.cost) {
				//try building
				log("Building builder miner");
				Direction buildDir = tryBuild(RobotType.MINER, directions);
				if (buildDir != null) {
					madeBuilderMiner = true;
					RobotInfo ri = rc.senseRobotAtLocation(rc.adjacentLocation(buildDir));
					writeTransactionBuilderMinerBuilt(ri.ID);
				}
			} else {
				log("Not enough soup to build explorer miner");
			}
			return;
		}

//		if (explorerMinerCount < 8) {
//			if (rc.getTeamSoup() >= RobotType.MINER.cost) {
//				buildMiner();
//			} else {
//				log("Not enough soup to build 5-8 explorer miner");
//			}
//			return;
//		}

		// @todo: Create Attack Miners
	}

	/*
	Tries to build a miner in an unexplored direction
	Returns true if built a miner
	Returns false if did not build a miner
	*/
	public static boolean buildMiner() throws GameActionException {
		Direction[] orderedDirections;
		MapLocation target = null;
		if (closestVisibleSoupLoc == null) {
			target = getSymmetryLoc();
		} else {
			target = closestVisibleSoupLoc;
		}
		orderedDirections = getCloseDirections(here.directionTo(target));

		log("Building explorer miner towards " + target);
		Direction buildDir = tryBuild(RobotType.MINER, orderedDirections);
		if (buildDir != null) {
			log("Built in " + buildDir);

			explorerMinerCount++;
			return true;
		}

		return false;
	}

}
