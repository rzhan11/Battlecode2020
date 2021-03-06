package rush_bot;

import battlecode.common.*;

import static rush_bot.Communication.*;
import static rush_bot.Debug.*;
import static rush_bot.Map.*;
import static rush_bot.Utils.*;
import static rush_bot.Wall.*;
import static rush_bot.Zones.*;

public class BotHQ extends Globals {

	final public static int REASSIGN_ROUND_NUM = 25;
	final public static int MAX_BUILDER_MINER_COOLDOWN = 5;

	final public static int MINER_CHECKPOINT_1 = 4;
	final public static int MINER_CHECKPOINT_2 = 8;

//	public static int maxMinerMadeCount = 8;

	public static int minerBuiltCount = 0;
	public static int lastMinerBuiltRound = N_INF;

	public static int lastAssignmentRound = N_INF;
	public static RobotInfo closeFulfillmentCenterInfo = null;
	public static RobotInfo closeVaporatorInfo = null;
	public static RobotInfo closeDesignSchoolInfo = null;
	public static RobotInfo closeRefineryInfo = null;

	final public static int NUM_CLOSE_VAPORATOR = 3;
	public static int closeVaporatorCount = 0;

	public static void loop() throws GameActionException {
		while (true) {
			try {
				Globals.update();

				if (!initializedHQ) {
					initHQ();
				}
				if (roundNum == 2) {
					loadPlatformInfo();
				}

//				if(roundNum > 1500 && roundNum%5 == 0){
//					writeTrollMessage();
//				}

				turn();
			} catch (Exception e) {
				e.printStackTrace();
			}
			Globals.endTurn();
		}
	}

	public static boolean initializedHQ = false;

	public static void initHQ() throws GameActionException {

		log("Possible enemy HQ locations");
		tlog("" + symmetryHQLocs[0]);
		tlog("" + symmetryHQLocs[1]);
		tlog("" + symmetryHQLocs[2]);

		// finds visible soup locations
		closestVisibleSoupLoc = findClosestVisibleSoupLoc(true);
		log("closestVisibleSoupLocation: " + closestVisibleSoupLoc);
		// supposed to be 24 not 35, to give some room for special cases
		if (HQLoc.distanceSquaredTo(closestVisibleSoupLoc) > 24) {
			writeTransactionSoupCluster(closestVisibleSoupLoc);
		}

		initializedHQ = true;
	}

	public static void turn() throws GameActionException {
		log("Turn start bytes " + Clock.getBytecodesLeft());

		// checks if we are being rushed
		if (!abortRush && !enemyRush) {
			RobotInfo[] closeEnemies = rc.senseNearbyRobots(8, them);
			for (RobotInfo ri: closeEnemies) {
				if (ri.type == RobotType.LANDSCAPER || ri.type == RobotType.MINER) {
					enemyRush = true;
					break;
				}
			}
			if (enemyRush) {
				writeTransactionEnemyRush();
			}
		}

		if (!initialWallSetup) {
			initialWallSetup = checkInitialWallSetup();
			if (initialWallSetup) {
				writeTransactionWallStatus(INITIAL_WALL_SETUP_FLAG);
			}
		}

		if (!wallFull) {
			wallFull = true;
			for (int i = 0; i < wallLocsLength; i++) {
				MapLocation loc = wallLocs[i];
				if (rc.canSenseLocation(loc) && !isLocAllyLandscaper(loc)) {
					wallFull = false;
					break;
				}
			}
			if (wallFull) {
				writeTransactionWallStatus(WALL_FULL_FLAG);
			}
		}

		if (!supportFull) {
			supportFull = true;
			for (int i = 0; i < supportWallLocsLength; i++) {
				MapLocation loc = supportWallLocs[i];
				if (rc.canSenseLocation(loc) && !isLocAllyLandscaper(loc)) {
					supportFull = false;
					break;
				}
			}
			if (supportFull) {
				writeTransactionWallStatus(SUPPORT_FULL_FLAG);
			}
		}

		log("initialWallSetup " + initialWallSetup);
		log("wallFull " + wallFull);
		log("supportFull " + supportFull);

		Communication.resubmitImportantTransactions();

		if (!rc.isReady()) {
			log("Not ready");
			return;
		}

		if (minerBuiltCount < MINER_CHECKPOINT_1) {
			if (rc.getTeamSoup() >= RobotType.MINER.cost) {
				if (closestVisibleSoupLoc == null) {
					buildMiner(getSymmetryLoc());
				} else {
					buildMiner(closestVisibleSoupLoc);
				}
			} else {
				log("Not enough soup to build miner");
			}
			return;
		}

		if (abortRush) {
			if (closeDesignSchoolInfo == null) {
				log("Trying to assign design school");
				for (RobotInfo ri: visibleAllies) {
					if (ri.type == RobotType.DESIGN_SCHOOL) {
						closeDesignSchoolInfo = ri;
						lastAssignmentRound = N_INF;
					}
				}
				if (closeDesignSchoolInfo == null && roundNum - lastAssignmentRound > REASSIGN_ROUND_NUM &&
						rc.getTeamSoup() >= RobotType.DESIGN_SCHOOL.cost) {
					int id = assignTask(BUILD_CLOSE_DESIGN_SCHOOL);
					if (id >= 100000) {
						// we built this turn
						return;
					}
				}
			} else if (closeRefineryInfo == null) {
				log("Trying to assign refinery");
				for (RobotInfo ri: visibleAllies) {
					if (ri.type == RobotType.REFINERY) {
						closeRefineryInfo = ri;
						lastAssignmentRound = N_INF;
					}
				}
				if (closeRefineryInfo == null && roundNum - lastAssignmentRound > REASSIGN_ROUND_NUM &&
						rc.getTeamSoup() >= RobotType.REFINERY.cost) {
					int id = assignTask(BUILD_CLOSE_REFINERY);
					if (id >= 100000) {
						// we built this turn
						return;
					}
				}
			} else if (closeFulfillmentCenterInfo == null) {
				log("Trying to assign fulfillment center");
				for (RobotInfo ri: visibleAllies) {
					if (ri.type == RobotType.FULFILLMENT_CENTER) {
						closeFulfillmentCenterInfo = ri;
						lastAssignmentRound = N_INF;
					}
				}
				if (closeFulfillmentCenterInfo == null && roundNum - lastAssignmentRound > REASSIGN_ROUND_NUM &&
						rc.getTeamSoup() >= RobotType.FULFILLMENT_CENTER.cost) {
					int id = assignTask(BUILD_CLOSE_FULFILLMENT_CENTER);
					if (id >= 100000) {
						// we built this tur
						return;
					}
				}
				// assign platform builder
			} else if (platformLandscaperID != -1 && !platformBuildingsCompleted && platformMinerID == -1) {
				log("Trying to assign platform miner");
				int id = assignTask(BUILD_PLATFORM);
				if (id >= 100000) {
					// we built this turn
					return;
				}
				if (id != -1) {
					platformMinerID = id;
				}
			}
		}


		int shotID = BotNetGun.tryShoot();
		if (shotID != -1) {
			return;
		}
	}

	/*
	Returns the id of the miner that was assigned this task
	Returns -1 if no miner found
	 */
	public static int assignTask (int instruction, int details) throws GameActionException {
		int id = findVisibleRobotType(RobotType.MINER);
		if (id == -1) {
			if (rc.getTeamSoup() >= RobotType.MINER.cost && roundNum - lastMinerBuiltRound >= GameConstants.INITIAL_COOLDOWN_TURNS - MAX_BUILDER_MINER_COOLDOWN) {
				Direction dir = buildMiner(getSymmetryLoc());
				if (dir != null) {
					return rc.senseRobotAtLocation(rc.adjacentLocation(dir)).ID * 100000 + 100000;
				}
			}
			return -1;
		} else {
			writeTransactionBuildInstruction(id, instruction, details);
			lastAssignmentRound = roundNum;
			return id;
		}
	}

	public static int assignTask (int instruction) throws GameActionException {
		return assignTask(instruction, 0);
	}

	/*
	Returns the index of a valid builder miner (lowish cooldown)
	 */
	public static int findVisibleRobotType (RobotType rt) {
		for (RobotInfo ri: visibleAllies) {
			if (ri.type == rt && ri.cooldownTurns <= MAX_BUILDER_MINER_COOLDOWN) {
				return ri.ID;
			}
		}
		return -1;
	}

	/*
	Tries to build a miner in an unexplored direction
	Returns the direction that the miner was built in
	Returns null if did not build a miner
	*/
	public static Direction buildMiner(MapLocation target) throws GameActionException {
		if (roundNum == 1) {
			target = symmetryHQLocs[getClosestSymmetryIndex()];
		}
		Direction[] orderedDirections = getCloseDirections(here.directionTo(target));

		log("Building miner towards " + target);
		Direction buildDir = tryBuild(RobotType.MINER, orderedDirections);
		if (buildDir != null) {
			if (roundNum == 1) {
				rushMinerID = rc.senseRobotAtLocation(rc.adjacentLocation(buildDir)).ID;
			}
			minerBuiltCount++;
			lastMinerBuiltRound = roundNum;
			return buildDir;
		}

		return null;
	}
}
