package kryptonite;

import battlecode.common.*;

public class BotMiner extends Globals {

	final private static int REFINERY_DISTANCE_LIMIT = 64; // distance at which we try to use refineries
	final private static int MIN_SOUP_WRITE_SOUP_CLUSTER = 500;
	final private static int MIN_SOUP_BUILD_REFINERY = 1000;

	private static MapLocation HQLocation;

	// Miner moves in the direction that it was spawned in, until it sees a soup deposit or hits a map edge
	// If it hits a map edge, it will rotate left the currentExploringDirection

	private static Direction currentExploringDirection;

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
				Globals.updateRobot();

				// Do first turn code here
				if (firstTurn) {
					// identify HQ MapLocation
					for (RobotInfo ri: visibleAllies) {
						if (ri.team == us && ri.type == RobotType.HQ) {
							HQLocation = ri.location;
						}
					}
					if (HQLocation == null) {
						Debug.tlogi("ERROR: Failed sanity check - Cannot find HQLocation");
					} else {
						Debug.tlog("HQLocation: " + HQLocation);
					}
					currentExploringDirection = HQLocation.directionTo(here);

					// store HQ as a refinery
					addToRefineries(HQLocation);

					//If this is the first robot made, designate that robot as the one to build the design school
					if(currentExploringDirection.equals(directions[0])){
						designSchoolMaker = true;
						designSchoolLocation = new MapLocation(HQLocation.x-1,HQLocation.y);
					}
				}

				turn();
			} catch (Exception e) {
				e.printStackTrace();
			}
			Globals.endTurn();
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
		If we are going to a refinery
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
				// checks newly added refineries
				int closestIndex = -1;
				int closestDist = P_INF;
				for (int i = refineriesChecked; i < refineriesSize; i++) {
					int dist = here.distanceSquaredTo(refineries[i]);
					if (dist < closestDist) {
						closestIndex = i;
						closestDist = dist;
					}
				}
				if (closestDist < here.distanceSquaredTo(refineries[refineriesIndex])) {
					refineriesIndex = closestIndex;
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
				buildRefineryVisibleSoup = buildRefineryVisibleSoup;
				refineriesIndex = findClosestRefinery();
				Debug.tlog("Refinery build location at " + buildRefineryLocation + " is not flooded/occupied.");
				Debug.tlog("Reverting to known refinery at " + refineries[refineriesIndex]);
			} else {
				// checks newly added refineries
				int closestIndex = -1;
				int closestDist = P_INF;
				for (int i = refineriesChecked; i < refineriesSize; i++) {
					int dist = here.distanceSquaredTo(refineries[i]);
					if (dist < closestDist) {
						closestIndex = i;
						closestDist = dist;
					}
				}
				if (closestDist <= REFINERY_DISTANCE_LIMIT) { // if close enough to use a newly found refinery
					refineriesIndex = closestIndex;
					Debug.tlog("Retargetting to newly found refinery at " + refineries[refineriesIndex]);
				} else {
					// move to/build refinery
					if (here.isAdjacentTo(buildRefineryLocation)) {
						Direction dir = here.directionTo(buildRefineryLocation);
						if (rc.isReady() && rc.canBuildRobot(RobotType.REFINERY, dir)) {
							rc.buildRobot(RobotType.REFINERY, dir);
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
				if (closestDistance < 32 * 32) {
					soupClusterIndex = closestIndex;
					Debug.tlog("Targetting soupCluster at " + soupClusters[soupClusterIndex]);
				}
			}
		}

		/*
		OLD INFORMATION
		if I have already found a soup deposit or I see a soup deposit
			flag this event and bug-nav towards it
		else
			move in the currentExploringDirection if possible
			if I encounter a map edge, rotateLeft the currentExploringDirection
			if I encounter a water/elevation obstacle, bug-nav around it
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

			// explore
			if (rc.isReady()) {
				MapLocation dest = rc.adjacentLocation(currentExploringDirection);
				// finds a Direction that points to a valid MapLocation
				int count = 0; // technically not needed in non 1x1 maps
				while (!rc.onTheMap(dest) && count < 4) {
					currentExploringDirection = currentExploringDirection.rotateLeft();
					currentExploringDirection = currentExploringDirection.rotateLeft();
					dest = rc.adjacentLocation(currentExploringDirection);
					count++;
				}
				// currentExploringDirection = currentExploringDirection.rotateLeft();
				// while (!rc.onTheMap(dest) && count < 4) {
				// 	currentExploringDirection = currentExploringDirection.rotateLeft();
				// 	currentExploringDirection = currentExploringDirection.rotateLeft();
				// 	dest = rc.adjacentLocation(currentExploringDirection);
				// 	count++;
				// }

				Debug.tlog("Exploring " + currentExploringDirection);

				count = 0;
				Direction curDir = currentExploringDirection;
				while ((!rc.canMove(curDir) || rc.senseFlooding(dest)) && count < 8) {
					curDir = curDir.rotateLeft();
					dest = rc.adjacentLocation(curDir);
					count++;
				}
				if (count < 8) {
					rc.move(curDir);
				}
			}
			myElevation = rc.senseElevation(here);
		}
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
		If we have not found a soupDeposit yet, we try to find the closest soupDeposit
		If we find a group of soupDeposits that satisfy the conditions for the soup cluster, submit a Transaction that signals this
	*/
	public static void locateSoup() throws GameActionException {
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
		if (teamSoup >= RobotType.REFINERY.cost && rc.isReady()) {
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
}
