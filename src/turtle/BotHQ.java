package turtle;

import battlecode.common.*;

import static turtle.Communication.*;
import static turtle.Debug.*;
import static turtle.Map.*;
import static turtle.Wall.*;
import static turtle.Utils.*;
import static turtle.Zones.*;

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

	final public static int NUM_CLOSE_VAPORATOR = 3;
	public static int closeVaporatorCount = 0;

	public static void loop() throws GameActionException {
		while (true) {
			try {
				Globals.update();

				if (!initializedHQ) {
					initHQ();
				}

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

		writeTransactionHQFirstTurn(here);

		// finds visible soup locations

		closestVisibleSoupLoc = findClosestVisibleSoupLoc(true);
		log("closestVisibleSoupLocation: " + closestVisibleSoupLoc);

		initializedHQ = true;

//		for (int i = 0; i < wallLocsLength; i++) {
//			log("wall loc " + wallLocs[i]);
//			drawDot(wallLocs[i], BLACK);
//		}
	}

	public static void turn() throws GameActionException {
		if (us == Team.A) {
			if (enemyHQLoc != null) {
				drawDot(enemyHQLoc, PINK);
			}
			for (int i = 0; i < symmetryHQLocs.length; i++) {
				int[] color = BLACK;
				if (isSymmetryHQLoc[i] == 0) {
					drawLine(here, symmetryHQLocs[i], YELLOW);
				} else if (isSymmetryHQLoc[i] == 1) {
					drawLine(here, symmetryHQLocs[i], GREEN);
				} else if (isSymmetryHQLoc[i] == 2) {
					drawLine(here, symmetryHQLocs[i], RED);
				}
			}
		}

		log("num v " + totalVaporators);

		// checks if wall is completed
		if (!wallCompleted) {
			wallCompleted = true;
			for (MapLocation loc: wallLocs) {
				if (rc.canSenseLocation(loc) && rc.senseElevation(loc) < terraDepth) {
					wallCompleted = false;
					break;
				}
			}
			if (wallCompleted) {
				writeTransactionWallCompleted();
			}
		}

		Communication.resubmitImportantTransactions();


		if (!rc.isReady()) {
			log("Not ready");
			return;
		}

		// assign construction of fulfillment center
		if (closeFulfillmentCenterInfo == null) {
			log("Trying to assign fulfillment center");
			if (minerBuiltCount >= MINER_CHECKPOINT_1) {
				for (RobotInfo ri : visibleAllies) {
					if (ri.type == RobotType.FULFILLMENT_CENTER) {
						closeFulfillmentCenterInfo = ri;
						lastAssignmentRound = N_INF;
					}
				}
				if (closeFulfillmentCenterInfo == null && roundNum - lastAssignmentRound > REASSIGN_ROUND_NUM &&
						rc.getTeamSoup() >= RobotType.FULFILLMENT_CENTER.cost) {
					int id = assignTask(BUILD_CLOSE_FULFILLMENT_CENTER);
					if (id >= 100000) {
						// we built this turn
						return;
					}
				}
			}

			// assign construction of close vaporators
		} else if (closeVaporatorInfo == null) {
			log("Trying to assign vaporator");
			for (RobotInfo ri: visibleAllies) {
				if (ri.type == RobotType.VAPORATOR) {
					closeVaporatorInfo = ri;
					lastAssignmentRound = N_INF;
				}
			}
			if (closeVaporatorInfo == null && roundNum - lastAssignmentRound > REASSIGN_ROUND_NUM &&
					rc.getTeamSoup() >= RobotType.VAPORATOR.cost) {
				int id = assignTask(BUILD_CLOSE_VAPORATOR, 1);
				if (id >= 100000) {
					// we built this turn
					return;
				}
			}

			// assign construction of close design school
		} else if (closeDesignSchoolInfo == null) {
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

			// assign construction of two more vaporators (three total)
		} else {
			int closeVaporatorCount = 0;
			for (RobotInfo ri: visibleAllies) {
				if (ri.type == RobotType.VAPORATOR) {
					closeVaporatorCount++;
				}
			}
			if (closeVaporatorCount >= NUM_CLOSE_VAPORATOR) {
				lastAssignmentRound = N_INF;
			} else {
				if (roundNum - lastAssignmentRound > REASSIGN_ROUND_NUM &&
						rc.getTeamSoup() >= RobotType.VAPORATOR.cost) {
					log("hi " + roundNum + " " + lastAssignmentRound);
					int id = assignTask(BUILD_CLOSE_VAPORATOR, NUM_CLOSE_VAPORATOR - closeVaporatorCount);
					if (id >= 100000) {
						// we built this turn
						return;
					}
				}
			}
		}

		int shotID = BotNetGun.tryShoot();
		if (shotID != -1) {
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

		// build up to second checkpoint after initial 3 buildings are done
		if (closeFulfillmentCenterInfo != null && minerBuiltCount < MINER_CHECKPOINT_2) {
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

		// enter mid-game
		if (wallCompleted) {

			int incomePerRound = 1 + totalVaporators * RobotType.VAPORATOR.maxSoupProduced;
			// intentionally uses landscaper cost, not miner cost
			int spawnDelay = 4 * RobotType.LANDSCAPER.cost / incomePerRound;
			if (roundNum - lastMinerBuiltRound > spawnDelay) {
				if (rc.getTeamSoup() >= RobotType.MINER.cost) {
					buildMiner(getSymmetryLoc());
					return;
				}
			}
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
		Direction[] orderedDirections = getCloseDirections(here.directionTo(target));

		log("Building miner towards " + target);
		Direction buildDir = tryBuild(RobotType.MINER, orderedDirections);
		if (buildDir != null) {
			minerBuiltCount++;
			lastMinerBuiltRound = roundNum;
			return buildDir;
		}

		return null;
	}
}
