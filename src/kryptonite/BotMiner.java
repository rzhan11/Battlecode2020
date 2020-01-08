package kryptonite;

import battlecode.common.*;

public class BotMiner extends Globals {

	final private static int REFINERY_DISTANCE_LIMIT = 64; // distance at which we try to use refineries

	private static MapLocation HQLocation;

	// Miner moves in the direction that it was spawned in, until it sees a soup deposit or hits a map edge
	// If it hits a map edge, it will rotate left the currentExploringDirection

	private static Direction currentExploringDirection;

	// a soup deposit is a single soup location
	private static MapLocation soupDeposit = null;
	private static int soupCarrying;
	private static boolean adjacentToSoup = false;
	// private static boolean returningToHQ = false;

	public static MapLocation[] soupClusters = new MapLocation[BIG_ARRAY_SIZE];
	public static boolean[] emptySoupClusters = new boolean[BIG_ARRAY_SIZE];
	public static int soupClustersSize = 0;
	private static int soupClusterIndex = -1;

	public static MapLocation[] refineries = new MapLocation[BIG_ARRAY_SIZE];
	public static boolean[] deadRefineries = new boolean[BIG_ARRAY_SIZE];
	public static int refineriesSize = 0;
	private static int refineriesIndex = -1;

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
						Debug.tlog("HQ is located at " + HQLocation);
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

	public static void addToSoupClusters (MapLocation loc) {
		soupClusters[soupClustersSize] = loc;
		soupClustersSize++;
	}

	public static void turn() throws GameActionException {

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
		adjacentToSoup = soupDeposit != null && here.isAdjacentTo(soupDeposit);

		Debug.tlog("soupCarrying: " + soupCarrying);

		// moves away from immediate water danger
		Nav.avoidWater();

		/*
		Check if soupDeposit is depleted or if we are carrying maximum soup
		*/
		if (soupDeposit != null && adjacentToSoup && rc.senseSoup(soupDeposit) == 0) {
			soupDeposit = null;
			adjacentToSoup = false;
			if (soupCarrying > 0) {
				pickRefinery();
			}
		}
		if (soupCarrying == RobotType.MINER.soupLimit) {
			pickRefinery();
		}

		/*
		Bug navigate to refinery/HQ and deposit soup
		*/
		if (refineriesIndex != -1) {
			MapLocation loc = refineries[refineriesIndex];
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
		Bug navigate to HQ and deposit soup
		*/
		// if (returningToHQ) {
		// 	if (here.isAdjacentTo(HQLocation)) {
		// 		if (rc.isReady()) {
		// 			rc.depositSoup(here.directionTo(HQLocation), soupCarrying);
		// 			returningToHQ = false;
		// 			Debug.tlog("Deposited " + soupCarrying + " soup at HQ");
		// 		}
		// 		return;
		// 	}
		// 	Nav.bugNavigate(HQLocation);
		// 	Debug.tlog("Moving to HQ");
		// 	return;
		// }

		/*
		mine dat soup
		*/
		if (adjacentToSoup) {
			if (rc.isReady()) {
				Debug.tlog("Mining soup at ");
				rc.mineSoup(here.directionTo(soupDeposit));
			}
			return;
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

		if (soupDeposit == null) {
			if (locateSoup()) {
				Debug.tlog("Found soupDeposit at " + soupDeposit);
			} else {
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
		}

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

	public static void addToRefineries (MapLocation loc) {
		refineries[refineriesSize] = loc;
		refineriesSize++;
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
				boolean isNew = true;
				for (int i = 0; i < refineriesSize; i++) {
					if (ri.location.equals(refineries[i])) {
						isNew = false;
						break;
					}
				}
				if (isNew) {
					addToRefineries(ri.location);
					foundNewRefineries = true;
					Debug.tlog("Found a refinery at " + ri.location);
				}
			}
		}
		return foundNewRefineries;
	}

	/*
	Returns false if no soup deposits were found
	Returns true if soup deposits were found
		Also saves the MapLocation in variable 'soupDeposit'
		Also tries to submit a Transaction for the soup cluster
	*/
	public static boolean locateSoup() throws GameActionException {
		MapLocation[] soups = new MapLocation[sensableDirections.length];
		int size = 0;

		int totalX = 0;
		int totalY = 0;
		int totalSoup = 0;
		for (int[] pair: sensableDirections) {
			MapLocation loc = here.translate(pair[0], pair[1]);
			if (rc.canSenseLocation(loc) && rc.senseSoup(loc) > 0) {
				totalX += loc.x;
				totalY += loc.y;
				totalSoup += rc.senseSoup(loc);

				soups[size] = loc;
				size++;
			}
		}
		if (size == 0) {
			return false;
		}

		soupDeposit = soups[0];

		// if this cluster is too close (distance) to another cluster that has already been submitted in a Transaction, we will not submit a new Transaction
		boolean worthSubmitting = true;
		MapLocation centerLoc = new MapLocation(totalX / size, totalY / size);

		for (int i = 0; i < soupClustersSize; i++) {
			if (centerLoc.distanceSquaredTo(soupClusters[i]) < 16) {
				worthSubmitting = false;
				break;
			}
		}

		if (worthSubmitting) {
			Communication.writeTransactionSoupCluster(centerLoc);
		}

		return true;
	}

	/*
	This method tries to make the Miner use/build a refinery if we are too far away from other refineries
	Returns true if we chose to use/build refinery
	Returns false if we chose otherwise
	*/

	public static void pickRefinery () throws GameActionException {
		// identifies closest refinery
		int closestDistance = P_INF;
		int closestIndex = -1;
		for (int i = 0; i < refineriesSize; i++) {
			int dist = here.distanceSquaredTo(refineries[i]);
			if (dist < closestDistance) {
				closestDistance = dist;
				closestIndex = i;
			}
		}

		// in the worst case, HQ should appear as the closest refinery
		if (closestIndex == -1) {
			Debug.tlogi("ERROR: Failed sanity check - Cannot find any refineries");
		}

		// if there is a close enough refinery, target it
		if (closestDistance <= REFINERY_DISTANCE_LIMIT) {
			refineriesIndex = closestIndex;
			Debug.tlog("Targetting close refinery at " + refineries[refineriesIndex]);
			return;
		}

		// try to build a refinery
		if (teamSoup >= RobotType.REFINERY.cost && rc.isReady()) {
			for (Direction dir: directions) {
				if (rc.canBuildRobot(RobotType.REFINERY, dir)) {
					rc.buildRobot(RobotType.REFINERY, dir);

					addToRefineries(rc.adjacentLocation(dir));
					Debug.tlog("Built/targetting refinery at " + refineries[refineriesSize - 1]);

					Communication.writeTransactionRefineryBuilt(refineries[refineriesSize - 1]);

					return;
				}
			}
		}

		// target the closest refinery since we cannot build one
		refineriesIndex = closestIndex;
		Debug.tlog("Targetting far refinery at " + refineries[refineriesIndex]);
	}

}
