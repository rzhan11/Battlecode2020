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
			for (int i = 0; i < wallLocsLength; i++) {
				MapLocation loc = wallLocs[i];
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

//		if (closeDesignSchoolInfo == null) {
//			log("Trying to assign design school");
//			for (RobotInfo ri: visibleAllies) {
//				if (ri.type == RobotType.DESIGN_SCHOOL) {
//					closeDesignSchoolInfo = ri;
//					lastAssignmentRound = N_INF;
//				}
//			}
//			if (closeDesignSchoolInfo == null && roundNum - lastAssignmentRound > REASSIGN_ROUND_NUM &&
//					rc.getTeamSoup() >= RobotType.DESIGN_SCHOOL.cost) {
//				int id = assignTask(BUILD_CLOSE_DESIGN_SCHOOL);
//				if (id >= 100000) {
//					// we built this turn
//					return;
//				}
//			}
//		}

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
