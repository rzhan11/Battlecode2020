package kryptonite;

import battlecode.common.*;

public class BotMiner extends Globals {

	// the explorer's target location will be this far from the map edge
	final private static int EXPLORER_EDGE_DISTANCE = 4;

	// distance at which we try to use refineries
	final private static int REFINERY_DISTANCE_LIMIT = 64;
	final private static int MIN_SOUP_WRITE_SOUP_CLUSTER = 500;
	final private static int MIN_SOUP_BUILD_REFINERY = 1000;

	// spawnDirection is the direction that this Miner was spawned by the HQ
	private static Direction spawnDirection;
	// the target location that this Miner wants to explore, based on spawnDirection
	private static MapLocation exploreLocation;

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

	private static boolean designSchoolMaker = false;
	private static boolean designSchoolMade = false;
	private static MapLocation designSchoolLocation;

	public static void loop() throws GameActionException {

		while (true) {
			try {
				Globals.update();

				// Do first turn code here
				if (firstTurn) {

					int distanceToEastEdge = mapWidth - 1 - HQLocation.x;
					int distanceToWestEdge = HQLocation.x;
					int distanceToNorthEdge = mapHeight - 1 - HQLocation.y;
					int distanceToSouthEdge = HQLocation.y;

					spawnDirection = HQLocation.directionTo(here);

					int change;
					switch (spawnDirection) {
						case NORTH:
							exploreLocation = new MapLocation(HQLocation.x, mapHeight - 1 - EXPLORER_EDGE_DISTANCE);
							break;
						case EAST:
							exploreLocation = new MapLocation(mapWidth - 1 - EXPLORER_EDGE_DISTANCE, HQLocation.y);
							break;
						case SOUTH:
							exploreLocation = new MapLocation(HQLocation.x, EXPLORER_EDGE_DISTANCE);
							break;
						case WEST:
							exploreLocation = new MapLocation(EXPLORER_EDGE_DISTANCE, HQLocation.y);
							break;
						case NORTHEAST:
							change = Math.min(distanceToEastEdge, distanceToNorthEdge) - EXPLORER_EDGE_DISTANCE;
							exploreLocation = new MapLocation(HQLocation.x + change, HQLocation.y + change);
							break;
						case SOUTHEAST:
							change = Math.min(distanceToEastEdge, distanceToSouthEdge) - EXPLORER_EDGE_DISTANCE;
							exploreLocation = new MapLocation(HQLocation.x + change, HQLocation.y - change);
							break;
						case SOUTHWEST:
							change = Math.min(distanceToWestEdge, distanceToSouthEdge) - EXPLORER_EDGE_DISTANCE;
							exploreLocation = new MapLocation(HQLocation.x - change, HQLocation.y - change);
							break;
						case NORTHWEST:
							change = Math.min(distanceToWestEdge, distanceToNorthEdge) - EXPLORER_EDGE_DISTANCE;
							exploreLocation = new MapLocation(HQLocation.x - change, HQLocation.y + change);
							break;
					}
					Debug.log("exploreLocation: " + exploreLocation);


					// store HQ as a refinery
					addToRefineries(HQLocation);

					//If this is the first robot made, designate that robot as the one to build the design school
					if(spawnDirection.equals(directions[0])){
						designSchoolMaker = true;
						designSchoolLocation = new MapLocation(HQLocation.x-1,HQLocation.y);
					}
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

		locateSoup();
		// updates known refineries based on what we can sense this turn
		locateRefineries();

		/*
		Builds design school if certain conditions are met
		*/
		if(teamSoup >= RobotType.DESIGN_SCHOOL.cost && designSchoolMaker && !designSchoolMade) {
			// potential bug - what if we are already on the designSchoolLocation?
			if(here.isAdjacentTo(designSchoolLocation)){
				//build design school
				if(rc.isReady() && rc.canBuildRobot(RobotType.DESIGN_SCHOOL,here.directionTo(designSchoolLocation))){
					rc.buildRobot(RobotType.DESIGN_SCHOOL,here.directionTo(designSchoolLocation));
					teamSoup = rc.getTeamSoup();
					designSchoolMade = true;
				}

			} else {
				Nav.bugNavigate(designSchoolLocation);
				Debug.tlog("Going to Design School Location");
			}
			return;
		}

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
				if (soupDeposit != null && rc.canSenseLocation(soupDeposit)) {

					Debug.tlog(soupDeposit + " " + rc.canSenseLocation(soupDeposit));
				}
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
		if (buildRefineryLocation != null) {
			// if centerOfVisibleSoup is better than buildRefineryLocation, replace it
			// makes sure that centerOfVisibleSoup isn't flooded or occupied
			if (visibleSoup > buildRefineryVisibleSoup && rc.senseFlooding(buildRefineryLocation) && rc.senseRobotAtLocation(buildRefineryLocation) == null) {
				buildRefineryLocation = centerOfVisibleSoup;
				buildRefineryVisibleSoup = visibleSoup;
				Debug.tlog("Retargetting refinery build location at " + buildRefineryLocation + " with " + buildRefineryVisibleSoup + " soup");
			}

			// if buildRefineryLocation is occupied or is flooded, revert to closest refinery
			if (rc.canSenseLocation(buildRefineryLocation) && (rc.senseRobotAtLocation(buildRefineryLocation) != null || rc.senseFlooding(buildRefineryLocation))) {
				buildRefineryLocation = null;
				buildRefineryVisibleSoup = 0;
				refineriesIndex = findClosestRefinery();
				Debug.tlog("Refinery build location at " + buildRefineryLocation + " is flooded/occupied.");
				Debug.tlog("Reverting to known refinery at " + refineries[refineriesIndex]);
			} else {
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
					buildRefineryVisibleSoup = 0;
					Debug.tlog("Retargetting from 'build' to newly found refinery at " + refineries[refineriesIndex]);
				} else {
					// move to/build refinery
					if (here.isAdjacentTo(buildRefineryLocation)) {
						Direction dir = here.directionTo(buildRefineryLocation);
						if (rc.isReady() && rc.canBuildRobot(RobotType.REFINERY, dir)) {
							rc.buildRobot(RobotType.REFINERY, dir);
							teamSoup = rc.getTeamSoup();
							Debug.tlog("Built refinery at " + buildRefineryLocation);
							Communication.writeTransactionRefineryBuilt(buildRefineryLocation);
							addToRefineries(buildRefineryLocation);

							buildRefineryLocation = null;
							buildRefineryVisibleSoup = -1;

						} else {
							buildRefineryLocation = null;
							buildRefineryVisibleSoup = -1;
							Debug.tlog("Refinery build location at " + buildRefineryLocation + " is too high/low. Resetting it.");
						}
					} else {
						Nav.bugNavigate(buildRefineryLocation);
						Debug.tlog("Moving to buildRefineryLocation at " + buildRefineryLocation);
					}
					return;
				}
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
				if (rc.isReady()) {
					rc.depositSoup(here.directionTo(loc), soupCarrying);
					refineriesIndex = -1;

					Debug.tlog("Deposited " + soupCarrying + " soup at refinery at " + loc);
				}
				return;
			}
			Nav.bugNavigate(loc);
			Debug.tlog("Moving to refinery at " + loc);
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
		PLEASE REMIND RICHARD TO FILL THIS OUT
		*/

		if (soupDeposit != null) {
			if (rc.isReady()) {
				Nav.bugNavigate(soupDeposit);
			}
			Debug.tlog("Moving to soupDeposit at " + soupDeposit);

		} else {
			// head to soupClusterIndex
			if (soupClusterIndex != -1) {
				MapLocation loc = soupClusters[soupClusterIndex];
				if (here.isAdjacentTo(loc) || here.equals(loc)) { // flag emptySoupClusters if no soup deposits are found
					emptySoupClusters[soupClusterIndex] = true;
					soupClusterIndex = -1;
				} else if (rc.isReady()) {
					Debug.tlog("Moving to soupCluster at " + soupClusters[soupClusterIndex]);
					Nav.bugNavigate(loc);
				}
			}

			// go to explore location
			if (rc.isReady()) {

				Direction dir = Nav.bugNavigate(exploreLocation);
				if (dir == null) {
					Debug.tlog("Exploring " + exploreLocation + ", but could not move");
				} else {
					Debug.tlog("Exploring " + exploreLocation + ", moved " + dir);
				}
			} else {
				Debug.tlog("Exploring " + exploreLocation + ", but is not ready");
			}
		}
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
			if (rc.canSenseLocation(loc) && rc.senseSoup(loc) > 0) {
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
