package kryptonite;

import battlecode.common.*;

import static kryptonite.Communication.*;
import static kryptonite.Debug.*;
import static kryptonite.Utils.*;
import static kryptonite.Zones.*;

public class BotHQ extends Globals {

	final public static int REASSIGN_ROUND_NUM = 25;

	public static int maxMinerMadeCount = 8;

	public static int minerMadeCount = 0;

	public static int lastAssignmentRound = N_INF;
	public static boolean builtCloseDesignSchool = false;
	public static boolean builtCloseFulfillmentCenter = false;

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

		// assign construction of close design school
		if (!builtCloseDesignSchool) {
			for (RobotInfo ri: visibleAllies) {
				if (ri.type == RobotType.DESIGN_SCHOOL) {
					builtCloseDesignSchool = true;
					lastAssignmentRound = N_INF;
					break;
				}
			}
			if (!builtCloseDesignSchool && roundNum - lastAssignmentRound >= REASSIGN_ROUND_NUM) {
				if (minerMadeCount >= 4) {
					int id = findBuilderMiner();
					if (id == -1) {
						buildMiner();
					} else {
						writeTransactionBuildInstruction(id, BUILD_CLOSE_DESIGN_SCHOOL);
						lastAssignmentRound = roundNum;
					}
				}
			}
		} else if (!builtCloseFulfillmentCenter) {
			int landscaperCount = 0;
			for (RobotInfo ri: visibleAllies) {
				switch(ri.type) {
					case FULFILLMENT_CENTER:
						builtCloseFulfillmentCenter = true;
						lastAssignmentRound = N_INF;
						break;
					case LANDSCAPER:
						landscaperCount++;
						break;

				}
			}
			if (landscaperCount >= 3 &&
					!builtCloseFulfillmentCenter && roundNum - lastAssignmentRound >= REASSIGN_ROUND_NUM) {
				if (minerMadeCount >= 4) {
					int id = findBuilderMiner();
					if (id == -1) {
						buildMiner();
					} else {
						writeTransactionBuildInstruction(id, BUILD_CLOSE_FULFILLMENT_CENTER);
						lastAssignmentRound = roundNum;
					}
				}
			}
		}

		if (!rc.isReady()) {
			log("Not ready");
			return;
		}

		int shotID = BotNetGun.tryShoot();
		if (shotID != -1) {
			return;
		}

		if (minerMadeCount < maxMinerMadeCount) {
			if (rc.getTeamSoup() >= RobotType.MINER.cost) {
				buildMiner();
			} else {
				log("Not enough soup to build miner");
			}
			return;
		}

	}

	/*
	Returns the index of a valid builder miner (lowish cooldown)
	 */
	public static int findBuilderMiner () {
		for (RobotInfo ri: visibleAllies) {
			if (ri.type == RobotType.MINER && ri.cooldownTurns < 5) {
				return ri.ID;
			}
		}
		return -1;
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

		log("Building miner towards " + target);
		Direction buildDir = tryBuild(RobotType.MINER, orderedDirections);
		if (buildDir != null) {
			minerMadeCount++;
			return true;
		}

		return false;
	}
}
