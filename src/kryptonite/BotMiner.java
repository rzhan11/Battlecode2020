package kryptonite;

import battlecode.common.*;

public class BotMiner extends Globals {

	// the explorer's target location will be this far from the map edge
	final private static int EXPLORER_EDGE_DISTANCE = 4;

	// distance at which we try to use refineries
	final private static int REFINERY_DISTANCE_LIMIT = 25;
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
	private static MapLocation soupDeposit = null;
	private static int soupCarrying;
	private static int visibleSoup;
	private static MapLocation centerOfVisibleSoup = null;
	// private static boolean returningToHQ = false;

	public static MapLocation[] soupClusters = new MapLocation[BIG_ARRAY_SIZE];
	public static boolean[] emptySoupClusters = new boolean[BIG_ARRAY_SIZE];
	public static int soupClustersSize = 0;
	private static int soupClusterIndex = -1;

	public static MapLocation[] refineries = new MapLocation[BIG_ARRAY_SIZE];
	public static boolean[] deadRefineries = new boolean[BIG_ARRAY_SIZE];
	public static int refineriesSize = 0;
	private static int refineriesChecked = 0;
	private static int refineriesIndex = -1;

	public static MapLocation buildRefineryLocation = null;
	public static int buildRefineryVisibleSoup;

	//obsolete with builder miner
	private static boolean designSchoolMaker = false;
	private static boolean designSchoolMade = false;

	private static MapLocation designSchoolLocation;
	private static MapLocation fulfillmentCenterLocation;

	//builder miner

	public static int builderMinerID = -1;
	private static boolean designSchoolBuilt = false;
	private static boolean fulfillmentCenterBuilt = false;



	public static void loop() throws GameActionException {

		while (true) {
			try {
				Globals.update();

				// Do first turn code here
				if (firstTurn) {

					spawnDirection = HQLocation.directionTo(here);

					myExploreDirection = spawnDirection;
					myExploreLocation = findExploreLocation(myExploreDirection);
					Debug.log("myExploreLocation: " + myExploreLocation);


					// store HQ as a refinery
					addToRefineries(HQLocation);

					//If this is the first robot made, designate that robot as the one to build the design school
					//obsolete with builderminer
					if(spawnDirection.equals(directions[0])){
						designSchoolMaker = true;
						designSchoolLocation = new MapLocation(HQLocation.x-1,HQLocation.y);
					}
					designSchoolLocation = new MapLocation(HQLocation.x-1,HQLocation.y);
					fulfillmentCenterLocation = new MapLocation(HQLocation.x+1,HQLocation.y);
				}

				/*
				Skip the normal turn() actions if it is the firstTurn
				This prevents the miner from exceeding the bytecode limit on the first turn
				*/
				if (!firstTurn) {
					if (isSymmetryMiner) {
						symmtryMinerTurn();
					} else {
						turn();
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			Globals.endTurn();
		}
	}

	public static void turn() throws GameActionException {

		//builder miner code
		if (builderMinerID == rc.getID()) {
			Debug.tlog("I am the builder miner");
			if (teamSoup >= RobotType.DESIGN_SCHOOL.cost && !designSchoolBuilt) {
				// potential bug - what if we are already on the designSchoolLocation?
				if(here.isAdjacentTo(designSchoolLocation)){
					//build design school
					Direction dir = here.directionTo(designSchoolLocation);
					if (rc.canBuildRobot(RobotType.DESIGN_SCHOOL, dir)) {
						rc.buildRobot(RobotType.DESIGN_SCHOOL, dir);
						teamSoup = rc.getTeamSoup();
						designSchoolBuilt = true;
					}

				} else {
					Nav.bugNavigate(designSchoolLocation);
					Debug.tlog("Going to designSchoolLocation");
				}
			} else if (teamSoup >= RobotType.FULFILLMENT_CENTER.cost && !fulfillmentCenterBuilt) {
				// potential bug - what if we are already on the fulfillmentCenterLocation?
				if (here.isAdjacentTo(fulfillmentCenterLocation)) {
					// build fulfillment center
					Direction dir = here.directionTo(fulfillmentCenterLocation);
					if(rc.isReady() && rc.canBuildRobot(RobotType.FULFILLMENT_CENTER, dir)){
						rc.buildRobot(RobotType.FULFILLMENT_CENTER, dir);
						teamSoup = rc.getTeamSoup();
						fulfillmentCenterBuilt = true;
					}

				} else {
					Nav.bugNavigate(fulfillmentCenterLocation);
					Debug.tlog("Going to fulfillmentCenterLocation");
				}
			}
			return;
		}






		locateSoup();
		// updates known refineries based on what we can sense this turn
		locateRefineries();

		/*
		Builds design school if certain conditions are met
		Obsolete with builderminer
		*/
		// if(teamSoup >= RobotType.DESIGN_SCHOOL.cost && designSchoolMaker && !designSchoolMade) {
		// 	// potential bug - what if we are already on the designSchoolLocation?
		// 	if(here.isAdjacentTo(designSchoolLocation)){
		// 		//build design school
		// 		if(rc.isReady() && rc.canBuildRobot(RobotType.DESIGN_SCHOOL,here.directionTo(designSchoolLocation))){
		// 			rc.buildRobot(RobotType.DESIGN_SCHOOL,here.directionTo(designSchoolLocation));
		// 			teamSoup = rc.getTeamSoup();
		// 			designSchoolMade = true;
		// 		}
		//
		// 	} else {
		// 		Nav.bugNavigate(designSchoolLocation);
		// 		Debug.tlog("Going to Design School Location");
		// 	}
		// 	return;
		// }

		soupCarrying = rc.getSoupCarrying();

		Debug.tlog("soupCarrying: " + soupCarrying);

		// moves away from immediate water danger
		Nav.avoidWater();

		/*
		If we are not moving to/building a refinery
			Check if soupDeposit is depleted or if we are carrying maximum soup
				If either is true, target a refinery
		If we are movging to a refinery
			Check if there is a better one
		*/
		if (buildRefineryLocation == null) {
			if (refineriesIndex == -1) {
				if (soupDeposit != null && rc.canSenseLocation(soupDeposit) && rc.senseSoup(soupDeposit) == 0) {
					soupDeposit = null;
					if (soupCarrying > 0) {
						pickRefinery();
					}
				}
				if (refineriesIndex == -1 && soupCarrying == RobotType.MINER.soupLimit) {
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
					Debug.tlog("Retargetting from 'move' to newly found refinery at " + refineries[refineriesIndex]);
				}
			}
		}

		/*
		If we are building a refinery, try to build the refinery or move towards the buildRefineryLocation
		*/
		while (buildRefineryLocation != null) { // this only ever runs one time, it is a while look to take advantage of break;
			if (teamSoup < RobotType.REFINERY.cost) {
				buildRefineryLocation = null;
				buildRefineryVisibleSoup = -1;
				Debug.tlog("Cannot afford to build refinery");

				refineriesIndex = findClosestRefinery();
				Debug.tlog("Reverting to known refinery at " + refineries[refineriesIndex]);
				break;
			}
			// if centerOfVisibleSoup is better than buildRefineryLocation, replace it
			// makes sure that centerOfVisibleSoup isn't flooded or occupied
			if (visibleSoup > buildRefineryVisibleSoup && rc.senseFlooding(buildRefineryLocation) && rc.senseRobotAtLocation(buildRefineryLocation) == null) {
				buildRefineryLocation = centerOfVisibleSoup;
				buildRefineryVisibleSoup = visibleSoup;
				Debug.tlog("Retargetting buildRefineryLocation to " + buildRefineryLocation + " with " + buildRefineryVisibleSoup + " soup");
			}

			// if buildRefineryLocation is occupied or is flooded, revert to closest refinery
			if (rc.canSenseLocation(buildRefineryLocation) && (rc.senseRobotAtLocation(buildRefineryLocation) != null || rc.senseFlooding(buildRefineryLocation))) {
				buildRefineryLocation = null;
				buildRefineryVisibleSoup = -1;
				Debug.tlog("Refinery build location at " + buildRefineryLocation + " is flooded/occupied.");

				refineriesIndex = findClosestRefinery();
				Debug.tlog("Reverting to known refinery at " + refineries[refineriesIndex]);
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
				Debug.tlog("Retargetting from 'build' to newly found refinery at " + refineries[refineriesIndex]);
				break;
			}

			// move to/build refinery
			if (here.isAdjacentTo(buildRefineryLocation)) {
				Direction dir = here.directionTo(buildRefineryLocation);
				MapLocation loc = rc.adjacentLocation(dir);
				if (!Nav.checkElevation(loc)) {
					// if I cannot build a refinery here because of elevation difference
					buildRefineryLocation = null;
					buildRefineryVisibleSoup = -1;
					Debug.tlog("Refinery build location at " + buildRefineryLocation + " is too high/low.");

					refineriesIndex = findClosestRefinery();
					Debug.tlog("Reverting to known refinery at " + refineries[refineriesIndex]);
					break;
				}

				// all conditions for building refinery have been met
				if (rc.isReady()) {
					rc.buildRobot(RobotType.REFINERY, dir);
					teamSoup = rc.getTeamSoup();
					Debug.tlog("Building refinery at " + buildRefineryLocation);
					Communication.writeTransactionRefineryBuilt(buildRefineryLocation);
					addToRefineries(buildRefineryLocation);

					buildRefineryLocation = null;
					buildRefineryVisibleSoup = -1;
					return;
				} else {
					Debug.tlog("Would build refinery at " + buildRefineryLocation + ", but not ready");
					return;
				}
			} else {
				if (rc.isReady()) {
					Direction dir = Nav.bugNavigate(buildRefineryLocation);
					if (dir != null) {
						Debug.tlog("Moving to buildRefineryLocation at " + buildRefineryLocation + ", moved " + dir);
					} else {
						Debug.tlog("Moving to buildRefineryLocation at " + buildRefineryLocation + ", but no move found");
					}
				} else {
					Debug.tlog("Moving to buildRefineryLocation at " + buildRefineryLocation + ", but not ready");
				}
			}

			return; // always returns since we only want the "while" loop to run once
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
				if (rc.isReady()) {
					rc.depositSoup(here.directionTo(loc), soupCarrying);
					refineriesIndex = -1;

					Debug.tlog("Deposited " + soupCarrying + " soup at refinery at " + loc);
				} else {
					Debug.tlog("Would deposit soup at refinery at " + loc + ", but not ready");
				}
				return;
			}

			if (rc.isReady()) {
				Direction dir = Nav.bugNavigate(refineries[refineriesIndex]);
				if (dir != null) {
					Debug.tlog("Moving to refinery at " + refineries[refineriesIndex] + ", moved " + dir);
				} else {
					Debug.tlog("Moving to refinery at " + refineries[refineriesIndex] + ", but no move found");
				}
			} else {
				Debug.tlog("Moving to refinery at " + refineries[refineriesIndex] + ", but not ready");
			}
			return;
		}

		/*
		mine dat soup
		*/
		if (soupDeposit != null && here.isAdjacentTo(soupDeposit)) {
			if (rc.isReady()) {
				Debug.tlog("Mining soup at " + soupDeposit);
				rc.mineSoup(here.directionTo(soupDeposit));
			}
			return;
		}

		/*
		Tries to target a soupCluster
		*/
		if (soupDeposit == null) {
			int closestDistance = P_INF;
			int closestIndex = -1;
			for (int i = 0; i < soupClustersSize; i++) {
				if (!emptySoupClusters[i]) {
					int dist = here.distanceSquaredTo(soupClusters[i]);
					if (dist < closestDistance) {
						closestDistance = dist;
						closestIndex = i;
					}

				}
			}
			if (closestIndex != -1) {
				soupClusterIndex = closestIndex;
				Debug.tlog("Targetting soupCluster at " + soupClusters[soupClusterIndex]);
			}
		}

		/*
		Miners do one of the following:
		1. Try to go to soupDeposits if they know of one
		2. Try to go to any soupClusters that they learned from Transactions
		3. Explore the direction they were spawned in
		*/

		if (soupDeposit != null) {
			if (rc.isReady()) {
				Direction dir = Nav.bugNavigate(soupDeposit);
				if (dir != null) {
					Debug.tlog("Moving to soupDeposit at " + soupDeposit + ", moved " + dir);
				} else {
					Debug.tlog("Moving to soupDeposit at " + soupDeposit + ", but no move found");
				}
			} else {
				Debug.tlog("Moving to soupDeposit at " + soupDeposit + ", but not ready");
			}
			return;
		} else {
			// head to soupClusterIndex
			if (soupClusterIndex != -1) {
				MapLocation loc = soupClusters[soupClusterIndex];
				if (here.isAdjacentTo(loc) || here.equals(loc)) { // flag emptySoupClusters if no soup deposits are found at this soup cluster
					Debug.tlog("Reached and removing soupCluster at " + soupClusters[soupClusterIndex]);
					emptySoupClusters[soupClusterIndex] = true;
					soupClusterIndex = -1;
					// do not return, instead try to explore
				} else {
					if (rc.isReady()) {
						Direction dir = Nav.bugNavigate(loc);
						if (dir != null) {
							Debug.tlog("Moving to soupCluster at " + soupClusters[soupClusterIndex] + ", moved " + dir);
						} else {
							Debug.tlog("Moving to soupCluster at " + soupClusters[soupClusterIndex] + ", but no move found");
						}
					} else {
						Debug.tlog("Moving to soupCluster at " + soupClusters[soupClusterIndex] + ", but not ready");
					}
					return;
				}
			}

			// go to explore location
			if (here.equals(myExploreLocation)) {
				myExploreDirection = myExploreDirection.rotateLeft();
				myExploreLocation = findExploreLocation(myExploreDirection);
				Debug.tlog("Finished exploring " + myExploreDirection);
			}
			if (rc.isReady()) {

				Direction dir = Nav.bugNavigate(myExploreLocation);
				if (dir != null) {
					Debug.tlog("Exploring " + myExploreLocation + ", moved " + dir);
				} else {
					Debug.tlog("Exploring " + myExploreLocation + ", but no move found");
				}
			} else {
				Debug.tlog("Exploring " + myExploreLocation + ", but not ready");
			}
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

	/*
	Adds a location to soupClusters if not already added
	*/
	public static boolean addToSoupClusters (MapLocation loc) {
		boolean isNew = true;
		for (int i = 0; i < soupClustersSize; i++) {
			if (loc.equals(soupClusters[i])) {
				isNew = false;
				break;
			}
		}
		if (isNew) {
			if (soupClustersSize == BIG_ARRAY_SIZE) {
				Debug.tlogi("ERROR: soupClustersSize reached BIG_ARRAY_SIZE limit");
				return false;
			}
			soupClusters[soupClustersSize] = loc;
			soupClustersSize++;
			Debug.tlog("Added a soupCluster at " + loc);
		}
		return isNew;
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
				Debug.tlogi("ERROR: refineriesSize reached BIG_ARRAY_SIZE limit");
				return false;
			}
			refineries[refineriesSize] = loc;
			refineriesSize++;
			Debug.tlog("Added a refinery at " + loc);
		}
		return isNew;
	}

	/*
		If we have not found a soupDeposit yet, we try to find the closest soupDeposit
		If we find a group of soupDeposits that satisfy the conditions for the soup cluster, submit a Transaction that signals this
	*/
	public static void locateSoup () throws GameActionException {
		MapLocation[] soups = new MapLocation[sensableDirections.length];
		int size = 0;

		int totalX = 0;
		int totalY = 0;
		visibleSoup = 0;
		for (int[] dir: sensableDirections) {
			MapLocation loc = here.translate(dir[0], dir[1]);
			if (inMap(loc) && rc.canSenseLocation(loc) && rc.senseSoup(loc) > 0) {
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

		if (soupDeposit == null) {
			soupDeposit = soups[0];
		}

		if (visibleSoup >= MIN_SOUP_WRITE_SOUP_CLUSTER) { // enough soup to warrant a Transaction
			// if this cluster is too close (distance) to another cluster that has already been submitted in a Transaction, we will not submit a new Transaction
			boolean worthSubmitting = true;
			centerOfVisibleSoup = new MapLocation(totalX / size, totalY / size);

			for (int i = 0; i < soupClustersSize; i++) {
				if (centerOfVisibleSoup.distanceSquaredTo(soupClusters[i]) < 16) {
					worthSubmitting = false;
					break;
				}
			}

			if (worthSubmitting) {
				Communication.writeTransactionSoupCluster(centerOfVisibleSoup);
			}
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
				int dist = here.distanceSquaredTo(refineries[i]);
				if (dist < closestDistance) {
					closestDistance = dist;
					closestIndex = i;
				}
			}
		}

		// in the worst case, HQ should appear as the closest refinery
		if (closestIndex == -1) {
			Debug.tlogi("ERROR: Failed sanity check - Cannot find any refineries");
		}

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

		// if there is a close enough refinery, target it
		if (here.distanceSquaredTo(refineries[closestIndex]) <= REFINERY_DISTANCE_LIMIT) {
			refineriesIndex = closestIndex;
			Debug.tlog("Targetting close refinery at " + refineries[refineriesIndex]);
			return;
		}

		// try to build a refinery
		if (teamSoup >= RobotType.REFINERY.cost) {
			if (visibleSoup >= MIN_SOUP_BUILD_REFINERY) { // enough soup to warrant a refinery
				if (!rc.senseFlooding(centerOfVisibleSoup) && rc.senseRobotAtLocation(centerOfVisibleSoup) == null) { // centerOfVisibleSoup is not flooded/occupied
					buildRefineryLocation = centerOfVisibleSoup;
					buildRefineryVisibleSoup = visibleSoup;
					Debug.tlog("Targetting refinery build location at " + buildRefineryLocation + " with " + buildRefineryVisibleSoup + " soup");
					return;
				}
			}
		}

		// target the closest refinery since we cannot build one
		refineriesIndex = closestIndex;
		Debug.tlog("Targetting far refinery at " + refineries[refineriesIndex]);
	}

	public static void symmtryMinerTurn () {
		Debug.tlog("Symmetry miner turn");
	}
}
