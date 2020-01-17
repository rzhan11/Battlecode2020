package kryptonite;

import battlecode.common.*;

import static kryptonite.Communication.*;
import static kryptonite.Constants.*;
import static kryptonite.Debug.*;
import static kryptonite.Map.*;
import static kryptonite.Zones.*;

public class BotMiner extends Globals {

	// the explorer's target location will be this far from the map edge
	final private static int EXPLORER_EDGE_DISTANCE = 4;

	// distance at which we try to use refineries
	final private static int REFINERY_DISTANCE_LIMIT = 18;
	final private static int MIN_SOUP_WRITE_SOUP_CLUSTER = 500;
	final private static int MIN_SOUP_BUILD_REFINERY = 1000;

	// spawnDirection is the direction that this Miner was spawned by the HQ
	private static Direction spawnDirection;
	// the target location that this Miner wants to explore, based on spawnDirection
	private static Direction myExploreDirection;
	private static MapLocation myExploreLocation;

	public static boolean isSymmetryMiner = false;
	public static MapLocation symmetryLocation;

	// a soup deposit is a single soup location
	private static int visibleSoup;
	private static MapLocation centerOfVisibleSoup = null;

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

				// Do first turn code here
				if (firstTurn) {

					spawnDirection = HQLocation.directionTo(here);

					myExploreDirection = directions[myID % 8];
					myExploreLocation = findExploreLocation(myExploreDirection);
					log("myExploreLocation: " + myExploreLocation);


					// store HQ as a refinery
					log("Saving HQ as a refinery");
					addToRefineries(HQLocation);

					loadZoneInformation();
				}

				if (builderMinerID == rc.getID()) {
					BotMinerBuilder.turn();
				} else {
					turn();
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			Globals.endTurn(false);
		}
	}

	public static void turn() throws GameActionException {

		locateSoup();
		// updates known refineries based on what we can sense this turn
		locateRefineries();

		if (!rc.isReady()) {
			log("Not ready");
			return;
		}

		closestSoupLocation = findClosestSoup();
		log("closestSoupLocation: " + closestSoupLocation);

		log("soupCarrying: " + rc.getSoupCarrying());

		// defend
		if (here.isAdjacentTo(HQLocation)) {
			for (RobotInfo ri : visibleEnemies) {
				if (ri.type == RobotType.LANDSCAPER) {
					log("Staying still to protect HQ from landscaper");
					return;
				}
			}
		}

		int avoidDangerResult = Nav.avoidDanger();
		if (avoidDangerResult == 1) {
			return;
		}
		// moves away from immediate water danger
//		Nav.avoidWater();

		/*
		If we are not moving to/building a refinery
			Check if soupDeposit is depleted or if we are carrying maximum soup
				If either is true, target a refinery
		If we are moving to a refinery
			Check if there is a better one
		*/
		if (buildRefineryLocation == null) {
			if (refineriesIndex == -1) {
				if (closestSoupLocation != null && rc.canSenseLocation(closestSoupLocation) && rc.senseSoup(closestSoupLocation) == 0) {
					closestSoupLocation = null;
					if (rc.getSoupCarrying() > 0) {
						pickRefinery();
					}
				}
				if (refineriesIndex == -1 && rc.getSoupCarrying() == RobotType.MINER.soupLimit) {
					pickRefinery();
				}
			} else {
				// checks newly added refineries against the refinery that we are MOVING to
				int closestIndex = -1;
				int closestDist = P_INF;
				for (int i = refineriesChecked; i < refineriesSize; i++) {
					int dist = here.distanceSquaredTo(refineries[i]);
					if (dist < closestDist) {
						closestIndex = i;
						closestDist = dist;
					}
				}
				refineriesChecked = refineriesSize;
				if (closestDist < here.distanceSquaredTo(refineries[refineriesIndex])) {
					refineriesIndex = closestIndex;
					log("Retargetting from 'move' to newly found refinery at " + refineries[refineriesIndex]);
				}
			}
		}

		/*
		If we are building a refinery, try to build the refinery or move towards the buildRefineryLocation
		*/
		while (buildRefineryLocation != null) { // this only ever runs one time, it is a while look to take advantage of break;
			if (rc.getTeamSoup() < RobotType.REFINERY.cost) {
				buildRefineryLocation = null;
				buildRefineryVisibleSoup = -1;
				log("Cannot afford to build refinery");

				refineriesIndex = findClosestRefinery();
				log("Reverting to known refinery at " + refineries[refineriesIndex]);
				break;
			}

			// if centerOfVisibleSoup is better than buildRefineryLocation, replace it
			// makes sure that centerOfVisibleSoup isn't flooded or occupied
			if (visibleSoup > buildRefineryVisibleSoup && !rc.senseFlooding(centerOfVisibleSoup) && rc.senseRobotAtLocation(centerOfVisibleSoup) == null) {
				buildRefineryLocation = centerOfVisibleSoup;
				buildRefineryVisibleSoup = visibleSoup;
				log("Retargetting buildRefineryLocation to " + buildRefineryLocation + " with " + buildRefineryVisibleSoup + " soup");
			}

			// if buildRefineryLocation is occupied or is flooded
			// build buildRefineryLocation to any adjacent tile
			// otherwise revert to closest refinery
			if (rc.canSenseLocation(buildRefineryLocation) && (rc.senseRobotAtLocation(buildRefineryLocation) != null || rc.senseFlooding(buildRefineryLocation))) {
				buildRefineryLocation = null;
				buildRefineryVisibleSoup = -1;
				log("Refinery build location at " + buildRefineryLocation + " is flooded/occupied.");

				log("Trying to build refinery in adjacent tile.");
				boolean result = tryBuild(RobotType.REFINERY, directions);
				if (result) {
					tlog("Success");

					writeTransactionRefineryBuilt(buildRefineryLocation);
					addToRefineries(buildRefineryLocation);

					buildRefineryLocation = null;
					buildRefineryVisibleSoup = -1;
					return;
				}

				// STATE == did not build refinery in adjacent direction
				refineriesIndex = findClosestRefinery();
				log("Reverting to known refinery at " + refineries[refineriesIndex]);
				break;
			}

			// checks newly added refineries against refinery that we are BUILDING
			int closestIndex = -1;
			int closestDist = P_INF;
			for (int i = refineriesChecked; i < refineriesSize; i++) {
				int dist = here.distanceSquaredTo(refineries[i]);
				if (dist < closestDist) {
					closestIndex = i;
					closestDist = dist;
				}
			}
			refineriesChecked = refineriesSize;
			if (closestDist <= REFINERY_DISTANCE_LIMIT) { // if close enough to use a newly found refinery
				refineriesIndex = closestIndex;
				buildRefineryLocation = null;
				buildRefineryVisibleSoup = -1;
				log("Retargetting from 'build' to newly found refinery at " + refineries[refineriesIndex]);
				break;
			}

			// move to/build refinery
			if (here.isAdjacentTo(buildRefineryLocation)) {
				Direction dir = here.directionTo(buildRefineryLocation);
				MapLocation loc = rc.adjacentLocation(dir);
				if (!isLocFlat(loc)) {
					// if I cannot build a refinery here because of elevation difference
					buildRefineryLocation = null;
					buildRefineryVisibleSoup = -1;
					log("Refinery build location at " + buildRefineryLocation + " is too high/low.");

					refineriesIndex = findClosestRefinery();
					log("Reverting to known refinery at " + refineries[refineriesIndex]);
					break;
				}

				// all conditions for building refinery have been met
				log("Building refinery at " + buildRefineryLocation);
				Actions.doBuildRobot(RobotType.REFINERY, dir);


				writeTransactionRefineryBuilt(buildRefineryLocation);
				addToRefineries(buildRefineryLocation);

				buildRefineryLocation = null;
				buildRefineryVisibleSoup = -1;
				return;
			} else {
				// STATE == not adjacent to refinery

				// consider building in any direction
				// if blocked by elevation or flooding or building, then try to build in any direction
				// however excludes non-building occupiers
				Direction dirToRef = here.directionTo(buildRefineryLocation);
				MapLocation locToRef = rc.adjacentLocation(dirToRef);
				RobotInfo riToRef = rc.senseRobotAtLocation(locToRef);
				if (rc.senseFlooding(locToRef) || !isLocFlat(locToRef)
					|| (riToRef != null && riToRef.type.isBuilding())) {
					log("Trying to build refinery in adjacent tile due to blocked path.");
					boolean result = tryBuild(RobotType.REFINERY, directions);
					if (result) {
						writeTransactionRefineryBuilt(buildRefineryLocation);
						addToRefineries(buildRefineryLocation);

						buildRefineryLocation = null;
						buildRefineryVisibleSoup = -1;
						return;
					}
				}

				Direction move = Nav.bugNavigate(buildRefineryLocation);
				log("Moving to buildRefineryLocation at " + buildRefineryLocation);
				if (move != null) {
					tlog("Moved " + move);
				} else {
					tlog("But no move found");
				}
				return;
			}

		}

		/*
		Bug navigate to refinery/HQ and deposit soup
		*/
		if (refineriesIndex != -1) {
			MapLocation loc = refineries[refineriesIndex];
			if (rc.canSenseLocation(loc) && rc.senseRobotAtLocation(loc) == null) {
				deadRefineries[refineriesIndex] = true;
				pickRefinery();
				loc = refineries[refineriesIndex];
			}
			if (here.isAdjacentTo(loc)) {
				log("Depositing " + rc.getSoupCarrying() + " soup at refinery at " + loc);
				Actions.doDepositSoup(here.directionTo(loc), rc.getSoupCarrying());
				refineriesIndex = -1;
				return;
			}

			log("Moving to refinery at " + refineries[refineriesIndex]);
			Direction move = Nav.bugNavigate(refineries[refineriesIndex]);
			if (move != null) {
				tlog("Moved " + move);
			} else {
				tlog("But no move found");
			}
			return;
		}

		/*
		mine dat soup
		*/
		if (closestSoupLocation != null && here.distanceSquaredTo(closestSoupLocation) <= 2) {
			log("Mining soup at " + closestSoupLocation);
			Actions.doMineSoup(here.directionTo(closestSoupLocation));
			return;
		}

		/*
		Tries to target a soupCluster
		*/
//		if (closestSoupLocation == null) {
//			int closestDistance = P_INF;
//			int closestIndex = -1;
//			for (int i = 0; i < soupClustersSize; i++) {
//				if (!emptySoupClusters[i]) {
//					int dist = here.distanceSquaredTo(soupClusters[i]);
//					if (dist < closestDistance) {
//						closestDistance = dist;
//						closestIndex = i;
//					}
//
//				}
//			}
//			if (closestIndex != -1) {
//				soupClusterIndex = closestIndex;
//				log("Targetting soupCluster at " + soupClusters[soupClusterIndex]);
//			}
//		}

		/*
		Miners do one of the following:
		1. Try to go to soupDeposits if they know of one
		2. Try to go to any soupClusters that they learned from Transactions
		3. Explore the direction they were spawned in
		*/

		if (closestSoupLocation != null) {
			log("Moving to soupDeposit at " + closestSoupLocation);
			int[] zone = zoneIndex(closestSoupLocation);
			int zoneIndex = zoneLocIndex(closestSoupLocation);

			Direction move = Nav.bugNavigate(closestSoupLocation);
			if (move != null) {
				tlog("Moved " + move);
			} else {
				tlog("But no move found");
			}
			return;
		} else {
			// head to soupClusterIndex
//			if (soupClusterIndex != -1) {
//				MapLocation loc = soupClusters[soupClusterIndex];
//				if (here.distanceSquaredTo(loc) <= 2) { // flag emptySoupClusters if no soup deposits are found at this soup cluster
//					log("Reached and removing soupCluster at " + soupClusters[soupClusterIndex]);
//					emptySoupClusters[soupClusterIndex] = true;
//					soupClusterIndex = -1;
//					// do not return, instead try to explore
//				} else {
//					log("Moving to soupCluster at " + soupClusters[soupClusterIndex]);
//					Direction move = Nav.bugNavigate(loc);
//					if (move != null) {
//						tlog("Moved " + move);
//					} else {
//						tlog("But no move found");
//					}
//					return;
//				}
//			}

			// go to explore location
			if (here.equals(myExploreLocation)) {
				myExploreDirection = myExploreDirection.rotateLeft();
				myExploreLocation = findExploreLocation(myExploreDirection);
				log("Finished exploring " + myExploreDirection);
			}

			log("Exploring " + myExploreLocation);
			Direction move = Nav.bugNavigate(myExploreLocation);
			if (move != null) {
				tlog("Moved " + move);
			} else {
				tlog("But no move found");
			}
			return;
		}
	}

	/*
	Returns the exploreLocation for each direction
	*/
	public static MapLocation findExploreLocation (Direction dir) {
		int hLowerBound = EXPLORER_EDGE_DISTANCE;
		int hMiddleBound = mapWidth / 2;
		int hUpperBound = mapWidth - 1 - EXPLORER_EDGE_DISTANCE;
		int vLowerBound = EXPLORER_EDGE_DISTANCE;
		int vMiddleBound = mapHeight / 2;
		int vUpperBound = mapHeight - 1 - EXPLORER_EDGE_DISTANCE;

		switch (dir) {
			case NORTH:
				return new MapLocation(hMiddleBound, vUpperBound);
			case EAST:
				return new MapLocation(hUpperBound, vMiddleBound);
			case SOUTH:
				return new MapLocation(hMiddleBound, vLowerBound);
			case WEST:
				return new MapLocation(hLowerBound, vMiddleBound);
			case NORTHEAST:
				return new MapLocation(hUpperBound, vUpperBound);
			case SOUTHEAST:
				return new MapLocation(hUpperBound, vLowerBound);
			case SOUTHWEST:
				return new MapLocation(hLowerBound, vLowerBound);
			case NORTHWEST:
				return new MapLocation(hLowerBound, vUpperBound);
		}
		return null;
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

	/*
	Based on soupZonesLocs, find the closest soup location
	 */
	public static MapLocation findClosestSoup () throws GameActionException {
		int startByte = Clock.getBytecodesLeft();
		int[] zone = zoneIndex(here);
		int range = 1;
		int x_lower = Math.max(0, zone[0] - range);
		int x_upper = Math.min(numXZones - 1, zone[0] + range);
		int y_lower = Math.max(0, zone[1] - range);
		int y_upper = Math.min(numYZones - 1, zone[1] + range);

		int closestDist = P_INF;
		MapLocation closestLoc = null;
		for (int x = x_lower; x <= x_upper; x++) {
			for (int y = y_lower; y <= y_upper; y++) {
				for (int i = 0; i < soupZonesLocsLength[x][y]; i++) {
					int index = soupZonesLocs[x][y][i];
					if (soupZonesAmount[x][y][index] > 0) {
						MapLocation loc = indexToLoc(x, y, index);
						int dist = here.distanceSquaredTo(loc);
						if (dist < closestDist) {
							closestDist = dist;
							closestLoc = loc;
						}
					}
				}
			}
		}
		tlog("BYTES: " + (startByte - Clock.getBytecodesLeft()));
		return closestLoc;
	}

	public static void locateSoup () throws GameActionException {

		centerOfVisibleSoup = null;
		visibleSoup = 0;
		int totalX = 0;
		int totalY = 0;

		for (MapLocation loc: visibleSoupLocations) {
			int soup = rc.senseSoup(loc);
			int level = soupToIndex(soup);
			if (level > 0 && !rc.senseFlooding(loc)) {
				visibleSoup += soup;
				totalX += soup * loc.x;
				totalY += soup * loc.y;

				int[] zone = zoneIndex(loc);
				int zoneLoc = zoneLocIndex(loc);
				// use !inArray(soupZonesLocs[zone[0]][zone[1]], i, soupZonesLocsLength[zone[0]][zone[1]])
				if (soupZonesAmount[zone[0]][zone[1]][zoneLoc] != level) {
					newSoupLocs[newSoupLocsLength] = loc;
					newSoupLocsLength++;
					addToSoupZones(loc, level);
				}
			}
		}

		if (visibleSoup > 0) {
			centerOfVisibleSoup = new MapLocation(totalX / visibleSoup, totalY / visibleSoup);
		}

		if (newSoupLocsLength > 0) {
			writeTransactionSoupFound(newSoupLocs, 0, newSoupLocsLength);
		}
	}

	/*
	Updates our known refineries based on currently visible refineries
	Returns false if no refineries were found
	Returns true if refineries were found
		Also saves their MapLocations in variable 'refineries'
	*/
	public static boolean locateRefineries() throws GameActionException {
		boolean foundNewRefineries = false;

		for (RobotInfo ri: visibleAllies) {
			if (ri.type == RobotType.REFINERY) {
				foundNewRefineries |= addToRefineries(ri.location);
			}
		}
		return foundNewRefineries;
	}

	/*
	This method returns the index of the closest refinery to this Miner
	*/
	public static int findClosestRefinery () throws GameActionException {
		// identifies closest refinery
		int closestDistance = P_INF;
		int closestIndex = -1;
		for (int i = 0; i < refineriesSize; i++) {
			if (!deadRefineries[i]) {
				if (largeWallFull && refineries[i].equals(HQLocation)) {
					continue;
				}
				int dist = here.distanceSquaredTo(refineries[i]);
				if (dist < closestDistance) {
					closestDistance = dist;
					closestIndex = i;
				}
			}
		}

		// not true anymore since HQ is ignored after largeWallFull
		// in the worst case, HQ should appear as the closest refinery
//		if (closestIndex == -1) {
//			logi("ERROR: Failed sanity check - Cannot find any refineries");
//		}

		return closestIndex;
	}

	/*
	This method tries to make the Miner use/build a refinery if we are too far away from other refineries
	Returns true if we chose to use/build refinery
	Returns false if we chose otherwise
	*/
	public static void pickRefinery () throws GameActionException {
		refineriesChecked = refineriesSize;

		int closestIndex = findClosestRefinery();

		if (closestIndex == -1) {
			log("No refineries found, largeWall must be full");
		}

		// if there is a close enough refinery, target it
		if (closestIndex != -1 && here.distanceSquaredTo(refineries[closestIndex]) <= REFINERY_DISTANCE_LIMIT) {
			refineriesIndex = closestIndex;
			log("Targetting close refinery at " + refineries[refineriesIndex]);
			return;
		}

		// try to build a refinery
		if (rc.getTeamSoup() >= RobotType.REFINERY.cost) {
			if (visibleSoup >= MIN_SOUP_BUILD_REFINERY) { // enough soup to warrant a refinery
				if (!rc.senseFlooding(centerOfVisibleSoup) && rc.senseRobotAtLocation(centerOfVisibleSoup) == null) { // centerOfVisibleSoup is not flooded/occupied
					buildRefineryLocation = centerOfVisibleSoup;
					buildRefineryVisibleSoup = visibleSoup;
					log("Targetting refinery build location at " + buildRefineryLocation + " with " + buildRefineryVisibleSoup + " soup");
					return;
				}
			}
		}

		// target the closest refinery since we cannot build one
		refineriesIndex = closestIndex;
		log("Targetting far refinery at " + refineries[refineriesIndex]);
	}
}
