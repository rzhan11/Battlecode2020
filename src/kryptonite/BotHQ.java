package kryptonite;

import battlecode.common.*;

import static kryptonite.Communication.*;
import static kryptonite.Debug.*;
import static kryptonite.Utils.*;
import static kryptonite.Zones.*;

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

	public static void loop() throws GameActionException {
		while (true) {
			try {
				Globals.update();

				if (!initialized) {
					initHQ();
				}

				turn();
			} catch (Exception e) {
				e.printStackTrace();
			}
			Globals.endTurn();
		}
	}

	public static boolean initialized = false;
	public static void initHQ() throws GameActionException {
		log("Possible enemy HQ locations");
		tlog("" + symmetryHQLocations[0]);
		tlog("" + symmetryHQLocations[1]);
		tlog("" + symmetryHQLocations[2]);

		writeTransactionHQFirstTurn(here);

		// finds visible soup locations

		closestVisibleSoupLoc = findClosestVisibleSoupLoc(true);
		log("closestVisibleSoupLocation: " + closestVisibleSoupLoc);

		initialized = true;
	}

	public static void turn() throws GameActionException {

		Communication.resubmitImportantTransactions();

		if (!rc.isReady()) {
			log("Not ready");
			return;
		}

		// assign construction of fulfillment center
		if (closeFulfillmentCenterInfo == null) {
			log("fulfillment");
			if (minerBuiltCount >= MINER_CHECKPOINT_1) {
				for (RobotInfo ri : visibleAllies) {
					if (ri.type == RobotType.FULFILLMENT_CENTER) {
						closeFulfillmentCenterInfo = ri;
						lastAssignmentRound = N_INF;
					}
				}
				if (closeFulfillmentCenterInfo == null && roundNum - lastAssignmentRound >= REASSIGN_ROUND_NUM &&
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
			log("vaporator");
			for (RobotInfo ri: visibleAllies) {
				if (ri.type == RobotType.VAPORATOR) {
					closeVaporatorInfo = ri;
					lastAssignmentRound = N_INF;
				}
			}
			if (closeVaporatorInfo == null && roundNum - lastAssignmentRound >= REASSIGN_ROUND_NUM &&
					rc.getTeamSoup() >= RobotType.VAPORATOR.cost) {
				int id = assignTask(BUILD_CLOSE_VAPORATOR);
				if (id >= 100000) {
					// we built this turn
					return;
				}
			}

			// assign construction of close design school
		} else if (closeDesignSchoolInfo == null) {
			log("design school");
			for (RobotInfo ri: visibleAllies) {
				if (ri.type == RobotType.DESIGN_SCHOOL) {
					closeDesignSchoolInfo = ri;
					lastAssignmentRound = N_INF;
				}
			}
			if (closeDesignSchoolInfo == null && roundNum - lastAssignmentRound >= REASSIGN_ROUND_NUM &&
					rc.getTeamSoup() >= RobotType.DESIGN_SCHOOL.cost) {
				int id = assignTask(BUILD_CLOSE_DESIGN_SCHOOL);
				if (id >= 100000) {
					// we built this turn
					return;
				}
			}
		}

		int shotID = BotNetGun.tryShoot();
		if (shotID != -1) {
			return;
		}

		if (minerBuiltCount < MINER_CHECKPOINT_1) {
			if (rc.getTeamSoup() >= RobotType.MINER.cost) {
				buildMiner();
			} else {
				log("Not enough soup to build miner");
			}
			return;
		}

		// build up to second checkpoint after initial 3 buildings are done
		if (closeFulfillmentCenterInfo != null && minerBuiltCount < MINER_CHECKPOINT_2) {
			if (rc.getTeamSoup() >= RobotType.MINER.cost) {
				buildMiner();
			} else {
				log("Not enough soup to build miner");
			}
			return;
		}

	}

	/*
	Returns the id of the miner that was assigned this task
	Returns -1 if no miner found
	 */
	public static int assignTask (int instruction) throws GameActionException {
		int id = findVisibleRobotType(RobotType.MINER);
		if (id == -1) {
			if (rc.getTeamSoup() >= RobotType.MINER.cost && roundNum - lastMinerBuiltRound >= GameConstants.INITIAL_COOLDOWN_TURNS - MAX_BUILDER_MINER_COOLDOWN) {
				Direction dir = buildMiner();
				if (dir != null) {
					return rc.senseRobotAtLocation(rc.adjacentLocation(dir)).ID * 100000 + 100000;
				}
			}
			return -1;
		} else {
			writeTransactionBuildInstruction(id, instruction);
			lastAssignmentRound = roundNum;
			return id;
		}
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
	public static Direction buildMiner() throws GameActionException {
		Direction[] orderedDirections;
		MapLocation target = null;
		if (closestVisibleSoupLoc == null) {
			target = getSymmetryLoc();
		} else {
			target = closestVisibleSoupLoc;
		}
		orderedDirections = getCloseDirections(here.directionTo(target));

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
