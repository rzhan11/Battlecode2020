package kryptonite;

import battlecode.common.*;

import static kryptonite.Actions.*;
import static kryptonite.Communication.*;
import static kryptonite.Debug.*;
import static kryptonite.Map.*;
import static kryptonite.Nav.*;
import static kryptonite.Utils.*;
import static kryptonite.Zones.*;

public class BotHQ extends Globals {

	public static int minerMadeCount = 0;

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

		int shotID = BotNetGun.tryShoot();
		if (shotID != -1) {
			return;
		}

		if (minerMadeCount < 8) {
			if (rc.getTeamSoup() >= RobotType.MINER.cost) {
				buildMiner();
			} else {
				log("Not enough soup to build miner");
			}
			return;
		}

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
