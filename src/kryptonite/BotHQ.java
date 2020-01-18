package kryptonite;

import battlecode.common.*;

import static kryptonite.Communication.*;
import static kryptonite.Constants.*;
import static kryptonite.Debug.*;
import static kryptonite.Map.*;

public class BotHQ extends Globals {

	final public static int RELAX_LARGE_WALL_FULL_ROUND_NUM = 1000;
	final public static int RELAX_LARGE_WALL_FULL_AMOUNT = 4;

	final private static int BLOCK_LANDSCAPER_DIST = 13;

	private static int explorerMinerCount = 0;

	private static boolean madeBuilderMiner = false;

	public static MapLocation closestHQVisibleSoup;


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
					locateClosestSoup();

				}
			    turn();
			} catch (Exception e) {
				e.printStackTrace();
			}
			Globals.endTurn(false);
		}
	}

	public static void turn() throws GameActionException {
		if (!rc.isReady()) {
			return;
		}

		if(!wallFull) {
			boolean flag = true;
			for(Direction d : Globals.directions) {
				if(rc.senseRobotAtLocation(here.add(d)) == null) {
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
				if(!isDigLocation(here.translate(i, j)) && rc.senseRobotAtLocation(here.translate(i, j)) == null) {
					flag = false;
					break;
				}
			}
			if(flag) {
				Communication.writeTransactionSupportWallComplete();
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
		if (explorerMinerCount < 8) {
			if (rc.getTeamSoup() >= RobotType.MINER.cost) {
				buildMiner();
			} else {
				log("Not enough soup to build explorer miner");
			}
			return;
		}

		//if explorer miners have been build and have enough money, build a BuilderMiner
		if (!madeBuilderMiner && rc.getTeamSoup() >= RobotType.MINER.cost) {
			//try building
			for (Direction dir: directions) {
				if (isDirDryFlatEmpty(dir)) {
					log("Building builder miner");
					Actions.doBuildRobot(RobotType.MINER, dir);
					madeBuilderMiner = true;

					RobotInfo ri = rc.senseRobotAtLocation(rc.adjacentLocation(dir));
					writeTransactionBuilderMinerBuilt(ri.ID);

					return;
				}
			}
		}

		// @todo: Create Attack Miners
	}

	/*
	Finds closest soup location out of visible soup
	*/
	public static void locateClosestSoup() throws GameActionException {
		int minDist = P_INF;
		closestHQVisibleSoup = null;
		for (MapLocation loc: visibleSoupLocations) {
			int dist = here.distanceSquaredTo(loc);
			if (rc.senseSoup(loc) > 0 && dist < minDist) {
				minDist = dist;
				closestHQVisibleSoup = loc;
			}
		}
	}

	/*
	Tries to build a miner in an unexplored direction
	Returns true if built a miner
	Returns false if did not build a miner
	*/
	public static boolean buildMiner() throws GameActionException {
		Direction[] orderedDirections;
		if (closestHQVisibleSoup == null) {
			orderedDirections = getCloseDirections(here.directionTo(getSymmetryLocation()));
		} else {
			orderedDirections = getCloseDirections(here.directionTo(closestHQVisibleSoup));
		}

		for (Direction dir: orderedDirections) {
			MapLocation loc = rc.adjacentLocation(dir);
			if (isDirDryFlatEmpty(dir)) {
				log("Building explorer miner in " + dir);
				Actions.doBuildRobot(RobotType.MINER, dir);

				explorerMinerCount++;
				return true;
			}
		}

		return false;
	}

}
