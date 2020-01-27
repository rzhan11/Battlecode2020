package rush_bot;

import battlecode.common.*;

import static rush_bot.Debug.*;
import static rush_bot.Map.*;
import static rush_bot.Utils.*;
import static rush_bot.Zones.*;

public class BotMiner extends Globals {

	final public static int
			MINER_RESOURCE_ROLE = 1,
			MINER_BUILDER_ROLE = 2,
			MINER_RUSH_ROLE = 3;

	public static int myRole = -1;

	// disintegrates if stuck on platform
	public static int numRoundsOnPlatform = 0;

	final private static int MIN_SOUP_WRITE_SOUP_CLUSTER = 500;

	public static void loop() throws GameActionException {
		while (true) {
			try {
				Globals.update();

				if (!initializedMiner) {
					initMiner();
				}

				turn();

			} catch (Exception e) {
				e.printStackTrace();
			}
			Globals.endTurn();
		}
	}


	public static boolean initializedMiner = false;

	public static void initMiner() throws GameActionException {

		if (spawnRound == 2) {
			myRole = MINER_RUSH_ROLE;
		} else {
			myRole = MINER_RESOURCE_ROLE;
		}

		log("EARLY END");
		Clock.yield();
		Globals.updateBasic();

		initializedMiner = true;

		Globals.endTurn();
		Globals.update();
	}

	public static void turn() throws GameActionException {
		if (platformCompleted && myID != platformMinerID) {
			if (inArray(platformLocs, here, platformLocs.length)) {
				numRoundsOnPlatform++;
			} else {
				numRoundsOnPlatform = 0;
			}
			if (numRoundsOnPlatform > 10) {
				rc.disintegrate();
			}
		}

		locateSoup();

		if (myRole == MINER_RUSH_ROLE) {
			BotMinerRush.turn();
			return;
		}

		if (!rc.isReady()) {
			log("Not ready");
			return;
		}

		int avoidDangerResult = Nav.avoidDanger();
		if (avoidDangerResult == 1) {
			Nav.bugTracing = false;
			return;
		}
		if (avoidDangerResult == -1) {
			// when danger is unavoidable, reset isDirMoveable to ignore danger tiles
			updateIsDirMoveable();
		}

		switch (myRole) {
			case MINER_RESOURCE_ROLE:
				BotMinerResource.turn();
				break;
			case MINER_BUILDER_ROLE:
				BotMinerBuilder.turn();
				break;
		}

	}

	public static void locateSoup () throws GameActionException {
		MapLocation[] soups = new MapLocation[senseDirections.length];
		int size = 0;

		int totalX = 0;
		int totalY = 0;
		int visibleSoup = 0;
		if (visibleSoupLocs.length <= VISIBLE_SOUP_LOCS_LIMIT) {
			for (MapLocation loc: visibleSoupLocs) {
				if (isLocDry(loc) || isAdjLocDry(loc)) {
					totalX += loc.x;
					totalY += loc.y;
					visibleSoup += rc.senseSoup(loc);

					soups[size] = loc;
					size++;
				}
			}
		} else {
			int startByte = Clock.getBytecodesLeft();
			for (int i = 0; i < visibleSoupLocs.length; i++) {
				if (startByte - Clock.getBytecodesLeft() > 2000) {
					log("Checked " + i + " random soup locations");
					break;
				}
				MapLocation loc = visibleSoupLocs[i];
				totalX += loc.x;
				totalY += loc.y;
				visibleSoup += rc.senseSoup(loc);

				soups[size] = loc;
				size++;
			}
		}

		if (size == 0) {
			return;
		}

		if (visibleSoup >= MIN_SOUP_WRITE_SOUP_CLUSTER) { // enough soup to warrant a Transaction
			// if this cluster is too close (distance) to another cluster that has already been submitted in a Transaction, we will not submit a new Transaction
			boolean worthSubmitting = true;
			centerOfVisibleSoup = new MapLocation(totalX / size, totalY / size);

			for (int i = 0; i < soupClustersLength; i++) {
				if (centerOfVisibleSoup.distanceSquaredTo(soupClusters[i]) < BotMinerResource.REFINERY_DISTANCE_LIMIT) {
					worthSubmitting = false;
					break;
				}
			}
			if (centerOfVisibleSoup.distanceSquaredTo(HQLoc) <= 24) {
				worthSubmitting = false;
			}

			if (worthSubmitting) {
				Communication.writeTransactionSoupCluster(centerOfVisibleSoup);
			}
		}
	}
}
