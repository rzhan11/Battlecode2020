package kryptonite;

import battlecode.common.*;

import static kryptonite.Constants.*;
import static kryptonite.Debug.*;

public class BotMiner extends Globals {

	final private static int MIN_MINER_BYTECODE_TURN = 3000;

	// distance at which we try to use refineries
	final private static int REFINERY_DISTANCE_LIMIT = 32;
	final private static int MIN_SOUP_BUILD_REFINERY = 1000;
	final private static int MIN_SOUP_RETURN_REFINERY = 20;

	// spawnDirection is the direction that this Miner was spawned by the HQ
	private static Direction spawnDirection;

	public static boolean isSymmetryMiner = false;
	public static MapLocation symmetryLocation;

	// a soup deposit is a single soup location
	private static int visibleSoup;
	private static MapLocation centerOfVisibleSoup = null;

	public static boolean mustBuild = false;
	public static MapLocation[] refineries = new MapLocation[BIG_ARRAY_SIZE];
	public static boolean[] deadRefineries = new boolean[BIG_ARRAY_SIZE];
	public static int refineriesSize = 0;
	private static int refineriesChecked = 0;
	private static int refineriesIndex = -1;

	public static MapLocation buildRefineryLocation = null;
	public static int buildRefineryVisibleSoup;

	public static void loop() throws GameActionException {
		while (true) {
			try {
				Globals.update();

				turn();
			} catch (Exception e) {
				e.printStackTrace();
			}
			Globals.endTurn(false);
		}
	}

	public static void turn() throws GameActionException {

	}

	public static boolean addToRefineries (MapLocation loc) {
		boolean isNew = true;
		for (int i = 0; i < refineriesSize; i++) {
			if (loc.equals(refineries[i])) {
				isNew = false;
				break;
			}
		}
		if (isNew) {
			if (refineriesSize == BIG_ARRAY_SIZE) {
				logi("ERROR: refineriesSize reached BIG_ARRAY_SIZE limit");
				return false;
			}
			refineries[refineriesSize] = loc;
			refineriesSize++;
			log("Added a refinery at " + loc);
		}
		return isNew;
	}
}
