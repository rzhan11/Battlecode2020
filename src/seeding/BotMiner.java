package seeding;

import battlecode.common.*;

import static seeding.Communication.*;
import static seeding.Constants.*;
import static seeding.Debug.*;
import static seeding.HardCode.*;
import static seeding.Map.*;
import static seeding.Zones.*;

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

				// Do first turn code here
				if (firstTurn) {

					spawnDirection = HQLoc.directionTo(here);


					// store HQ as a refinery
					log("Saving HQ as a refinery");
					addToRefineries(HQLoc);

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
		int startByte = Clock.getBytecodesLeft();
		log("Bytecode at turn start " + startByte);
		if (startByte < MIN_MINER_BYTECODE_TURN) {
			log("Not enough bytecode for turn, returning");
			return;
		}

		locateCenterOfVisibleSoup();
		// updates known refineries based on what we can sense this turn
		locateRefineries();

		log("soupCarrying " + rc.getSoupCarrying());

		if (!rc.isReady()) {
			log("Not ready");
			return;
		}

		boolean seesAllyShoot = false;
		for (RobotInfo ri: visibleAllies) {
			if (ri.type.canShoot()) {
				seesAllyShoot = true;
			}
		}

		if (!seesAllyShoot && rc.getTeamSoup() >= RobotType.NET_GUN.cost) {
			for (RobotInfo ri: visibleEnemies) {
				if (ri.type == RobotType.DELIVERY_DRONE) {
					Direction dir = tryBuild(RobotType.NET_GUN, directions);
					if (dir != null) {
						log("Built net gun");
						return;
					}
				}
			}
		}

		// create vaporators
		while (rc.getTeamSoup() >= RobotType.VAPORATOR.cost) {
			//checks to see if there are too many adjacent allies/vaporators
			if (visibleAllies.length >= 25) {
				break;
			}
			int count = 0;
			for (RobotInfo ri: visibleAllies) {
				if (ri.type == RobotType.VAPORATOR) {
					count++;
				}
			}
			if (count >= 3) {
				break;
			}

			// find the highest valid adjacent tile
			Direction highestDir = null;
			int highestElevation = N_INF;
			for (Direction dir: directions) {
				MapLocation loc = rc.adjacentLocation(dir);
				if (!rc.onTheMap(loc)) {
					continue;
				}
				if (isDirDryFlatEmpty(dir) && isBuildLocation(loc) && maxXYDistance(HQLoc, loc) >= 2) {
					int elevation = rc.senseElevation(loc);
					if (elevation > highestElevation) {
						highestDir = dir;
						highestElevation = elevation;
					}
				}
			}
			// checks to make sure it is worth it
			if (highestDir != null) {
				int minRevenue = (getRoundFlooded(highestElevation) - roundNum) * RobotType.VAPORATOR.maxSoupProduced;
				if (minRevenue >= 2 * RobotType.VAPORATOR.cost) {
					log("Built vaporator with revenue " + minRevenue);
					Actions.doBuildRobot(RobotType.VAPORATOR, highestDir);
					return;
				}
			}
			break;
		}

//		log("targetNavLoc: " + targetNavLoc);
//		log("targetVisibleSoupLoc: " + targetVisibleSoupLoc);
//		log("targetSoupZone: " + targetSoupZone);
//		log("targetUnexploredZone: " + targetUnexploredZone);

		closestVisibleSoupLoc = findClosestVisibleSoupLoc(false);
//		log("closestVisibleSoupLocation: " + closestVisibleSoupLoc);

		closestSoupZone = findClosestSoupZone();
//		log("closestSoupZone: " + closestSoupZone);

//		log("closestUnexploredZone: " + closestUnexploredZone);
		closestUnexploredZone = findClosestUnexploredZone();

		int avoidDangerResult = Nav.avoidDanger();
		if (avoidDangerResult == 1) {
			return;
		}
		// moves away from immediate water danger
//		Nav.avoidWater();

		/*
		If no more soup at target location and cannot sense another soup location, try to deposit at a refinery
		 */
		if (targetVisibleSoupLoc != null && rc.canSenseLocation(targetVisibleSoupLoc) && rc.senseSoup(targetVisibleSoupLoc) == 0) {
			targetVisibleSoupLoc = null;
			targetNavLoc = null;
			// if we have soup and there is no more visible soup, then just try to return to a refinery
			log("No soup at targetVisibleSoupLoc");
			if (rc.getSoupCarrying() > 0 && visibleSoupLocs.length == 0) {
				tlog("Carrying some soup and no more visible soup, looking for refineries");
				pickRefinery();
			}
		}

		/*
		If we are full of soup, try to deposit at a refinery
		 */
		if (buildRefineryLocation == null && refineriesIndex == -1 && rc.getSoupCarrying() == RobotType.MINER.soupLimit) {
			// does not reset targetVisibleSoupLoc since the soup location is not depleted
			log("Full of soup, looking for refineries");
			pickRefinery();
			targetNavLoc = null;
		}

		/*
		If targetSoupZone has been denied of soup, reset it
		 */
		if (targetSoupZone != null && getHasSoupZonesAtLoc(targetSoupZone) == 2) {
			log("Resetting targetSoupZone");
			targetSoupZone = null;
			targetNavLoc = null;
		}

		/*
		If targetUnexploredZone has been explored, reset it
		 */
		if (targetUnexploredZone != null && getExploredZoneStatusAtLoc(targetUnexploredZone) == 1) {
			log("Resetting targetUnexploredZone");
			targetUnexploredZone = null;
			targetNavLoc = null;
		}

		/*
		Checks the targeted refinery still exists
		If not, picks a new refinery
		 */
		if (refineriesIndex != -1) {
			if (rc.canSenseLocation(refineries[refineriesIndex])) {
				RobotInfo ri = rc.senseRobotAtLocation(refineries[refineriesIndex]);
				if (ri == null || !ri.type.canRefine() || ri.team != us) {
					deadRefineries[refineriesIndex] = true;
					pickRefinery();
				}
			}
			// if HQ is walled off, force a new refinery to be built
			if (wallFull && refineries[refineriesIndex].equals(HQLoc)) {
				deadRefineries[refineriesIndex] = true;
				pickRefinery();
			}
		}

		/*
		If we have found a new refinery (through sensing or communication)
			If we are moving to a known refinery
				Check if there is a new refinery that is closer than the targeted one
			If we are trying to building a refinery
				Check if there is a new refinery that is within the min target distance
		*/
		if (refineriesChecked < refineriesSize) {
			int closestRefineryIndex = -1;
			int closestRefineryDist = P_INF;
			for (int i = refineriesChecked; i < refineriesSize; i++) {
				int dist = here.distanceSquaredTo(refineries[i]);
				if (dist < closestRefineryDist) {
					closestRefineryIndex = i;
					closestRefineryDist = dist;
				}
			}
			refineriesChecked = refineriesSize;

			// if moving
			if (refineriesIndex != -1) {
				if (closestRefineryDist < here.distanceSquaredTo(refineries[refineriesIndex])) {
					refineriesIndex = closestRefineryIndex;
					buildRefineryLocation = null;
					buildRefineryVisibleSoup = -1;
					mustBuild = false;
					log("Retargeting from 'move' to new refinery at " + refineries[refineriesIndex]);
				}
			}

			// if building
			if (buildRefineryLocation != null) {
				if (closestRefineryDist <= REFINERY_DISTANCE_LIMIT) {
					refineriesIndex = closestRefineryIndex;
					buildRefineryLocation = null;
					buildRefineryVisibleSoup = -1;
					mustBuild = false;
					log("Retargeting from 'build' to new refinery at " + refineries[refineriesIndex]);
				}
			}
		}

		/*
		If we are building a refinery, try to build the refinery or move towards the buildRefineryLocation
		*/
		while (buildRefineryLocation != null) { // this only ever runs one time, it is a while look to take advantage of break;
			if (rc.getTeamSoup() < RobotType.REFINERY.cost) {
				if (!mustBuild) {
					buildRefineryLocation = null;
					buildRefineryVisibleSoup = -1;
					log("Cannot afford to build refinery");

					refineriesIndex = findClosestRefinery();
					log("Reverting to known refinery at " + refineries[refineriesIndex]);
					break;
				} else {
					return;
				}
			}

			// if centerOfVisibleSoup is better than buildRefineryLocation, replace it
			// makes sure that centerOfVisibleSoup isn't flooded or occupied
			if (visibleSoup > buildRefineryVisibleSoup && isLocDry(centerOfVisibleSoup)) {
				buildRefineryLocation = centerOfVisibleSoup;
				buildRefineryVisibleSoup = visibleSoup;
				log("Retargeting buildRefineryLocation to " + buildRefineryLocation + " with " + buildRefineryVisibleSoup + " soup");
			}

			// if buildRefineryLocation is flooded
			// build buildRefineryLocation to any adjacent tile
			// otherwise revert to closest refinery
			if (rc.canSenseLocation(buildRefineryLocation) && !isLocDry(buildRefineryLocation)) {
				log("Refinery build location at " + buildRefineryLocation + " is flooded.");

				log("Trying to build refinery in adjacent tile.");
				Direction buildDir = tryBuild(RobotType.REFINERY, directions);
				if (buildDir != null) {
					buildRefineryLocation = null;
					buildRefineryVisibleSoup = -1;
					mustBuild = false;

					MapLocation newLoc = rc.adjacentLocation(buildDir);
					writeTransactionRefineryBuilt(newLoc);
					addToRefineries(newLoc);
					return;
				}

				// STATE == did not build refinery
				if (!mustBuild) {
					buildRefineryLocation = null;
					buildRefineryVisibleSoup = -1;

					refineriesIndex = findClosestRefinery();
					log("Reverting to known refinery at " + refineries[refineriesIndex]);
				} else {
					return;
				}
				break;
			}

			// if adjacent to/on top of the build location
			// build refinery in any direction
			if (here.isAdjacentTo(buildRefineryLocation)) {
				if (isLocDryFlatEmpty(buildRefineryLocation)) {
					// all conditions for building refinery have been met
					log("Building refinery at " + buildRefineryLocation);
					Actions.doBuildRobot(RobotType.REFINERY, here.directionTo(buildRefineryLocation));
					writeTransactionRefineryBuilt(buildRefineryLocation);
					addToRefineries(buildRefineryLocation);
					return;
				}

				Direction newDir = tryBuild(RobotType.REFINERY, directions);
				if (newDir != null) {
					buildRefineryLocation = null;
					buildRefineryVisibleSoup = -1;
					mustBuild = false;

					MapLocation newLoc = rc.adjacentLocation(newDir);
					tlog("Built close sub-optimal refinery at " + newLoc);
					writeTransactionRefineryBuilt(newLoc);
					addToRefineries(newLoc);
					return;
				}

				if (!mustBuild) {
					log("Refinery build location at " + buildRefineryLocation + " is not dry & flat & empty.");
					buildRefineryLocation = null;
					buildRefineryVisibleSoup = -1;

					// resorting to known refineries
					refineriesIndex = findClosestRefinery();
					tlog("Reverting to known refinery at " + refineries[refineriesIndex]);
					break;
				} else {
					return;
				}
			}

			/*
			 STATE == not adjacent to refinery
			 if the direction to the refinery is occupied/wet/not flat
				then try to build in any direction
			 */
			Direction dirToRef = here.directionTo(buildRefineryLocation);
			if (!isDirDryFlatEmpty(dirToRef)) {
				log("Path to buildRefineryLocation is blocked");
				Direction newDir = tryBuild(RobotType.REFINERY, directions);
				if (newDir != null) {
					buildRefineryLocation = null;
					buildRefineryVisibleSoup = -1;
					mustBuild = false;

					MapLocation newLoc = rc.adjacentLocation(newDir);
					tlog("Built far sub-optimal refinery at " + newLoc);
					writeTransactionRefineryBuilt(newLoc);
					addToRefineries(newLoc);
					return;
				}
			}

			log("Moving to buildRefineryLocation at " + buildRefineryLocation);
			moveLog(buildRefineryLocation);
			return;
		}

		/*
		Bug navigate to refinery/HQ and deposit soup
		*/
		if (refineriesIndex != -1) {
			MapLocation loc = refineries[refineriesIndex];
			if (here.isAdjacentTo(loc)) {
				log("Depositing " + rc.getSoupCarrying() + " soup at refinery at " + loc);
				Actions.doDepositSoup(here.directionTo(loc), rc.getSoupCarrying());
				refineriesIndex = -1;
				return;
			}

			log("Moving to refinery at " + refineries[refineriesIndex]);
			moveLog(refineries[refineriesIndex]);
			return;
		}

		/*
		Tries to give miner tasks in order of:
		1. Visible soup location
		2. Known soup zones
		3. Unexplored zones
		Does not retarget to task of similar priority
		 */
		if (targetVisibleSoupLoc == null) {
			if (closestVisibleSoupLoc != null) {
				targetVisibleSoupLoc = closestVisibleSoupLoc;
				targetSoupZone = null;
				targetUnexploredZone = null;
				targetNavLoc = targetVisibleSoupLoc;
				log("Targeting visible soup at " + targetVisibleSoupLoc);
			} else if (targetSoupZone == null) {
				if (closestSoupZone != null) {
					targetVisibleSoupLoc = null;
					targetSoupZone = closestSoupZone;
					targetUnexploredZone = null;
					targetNavLoc = targetSoupZone;
					log("Targeting soup zone at " + targetSoupZone);
				} else if (targetUnexploredZone == null) {
					if (closestUnexploredZone != null) {
						targetVisibleSoupLoc = null;
						targetSoupZone = null;
						targetUnexploredZone = closestUnexploredZone;
						targetNavLoc = targetUnexploredZone;
						log("Targeting unexplored zone at " + targetUnexploredZone);
					} else {
						logi("WARNING - Everything has been done. Ask Richard to check if this is actually possible");
						return;
					}
				}
			}
		}

		// if HQ is surrounded by allies, try to move away from HQ to give it space
		if (here.isAdjacentTo(HQLoc) && rc.senseNearbyRobots(HQLoc, 2, us).length == 8) {
			log("Trying to move away from HQ to unclog it");
			for (Direction dir: directions) {
				MapLocation loc = rc.adjacentLocation(dir);
				if (isDirDryFlatEmpty(dir) && !loc.isAdjacentTo(HQLoc)) {
					Actions.doMove(dir);
					tlog("Moved " + dir);
					return;
				}
			}
			tlog("Failed");
		}

		/*
		Mines the target soup location (not the closest one)
		*/
		if (targetVisibleSoupLoc != null && here.isAdjacentTo(targetVisibleSoupLoc)) {
			log("Mining soup at " + targetVisibleSoupLoc);
			Actions.doMineSoup(here.directionTo(targetVisibleSoupLoc));
			return;
		}

		if (targetNavLoc == null) {
			if (targetVisibleSoupLoc != null) {
				targetNavLoc = targetVisibleSoupLoc;
			} else if (targetSoupZone != null) {
				targetNavLoc = targetSoupZone;
			} else if (targetUnexploredZone != null) {
				targetNavLoc = targetUnexploredZone;
			}
		}

		/*
		Miners do one of the following:
		1. Try to go to the closest soup location
		2. Explore an unknown symmetry
		*/

		if (targetNavLoc != null) {
			log("Moving to target at " + targetNavLoc);

			moveLog(targetNavLoc);
			return;
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
	Finds the center of soup, relevant for determining refinery build location
	If there are a lot of visible soup locations, approximates the current location as center of soup
	 */
	public static void locateCenterOfVisibleSoup() throws GameActionException {
		if (visibleSoupLocs.length > VISIBLE_SOUP_LOCS_LIMIT) {
			log("Approximating center of soup");
			centerOfVisibleSoup = here;
			return;
		}

		centerOfVisibleSoup = null;
		visibleSoup = 0;
		int totalX = 0;
		int totalY = 0;

		for (MapLocation loc: visibleSoupLocs) {
			int soup = rc.senseSoup(loc);
			if (!rc.senseFlooding(loc)) {
				visibleSoup += soup;
				totalX += soup * loc.x;
				totalY += soup * loc.y;

				updateKnownSoupLocs(loc, 1);
			}
		}

		if (visibleSoup > 0) {
			centerOfVisibleSoup = new MapLocation(totalX / visibleSoup, totalY / visibleSoup);
		} else {
			centerOfVisibleSoup = here;
		}
	}

	/*
	Based on hasSoupZones, find the closest soup zone that has been confirmed to contain soup
	 */
	public static MapLocation findClosestSoupZone () throws GameActionException {
//		int startByte = Clock.getBytecodesLeft();
		int range = 4;
		int x_lower = Math.max(0, myZone[0] - range);
		int x_upper = Math.min(numXZones - 1, myZone[0] + range);
		int y_lower = Math.max(0, myZone[1] - range);
		int y_upper = Math.min(numYZones - 1, myZone[1] + range);

		int closestDist = P_INF;
		MapLocation closestLoc = null;
		for (int x = x_lower; x <= x_upper; x++) {
			for (int y = y_lower; y <= y_upper; y++) {
				if (hasSoupZones[x][y] == 1) {
					MapLocation targetLoc = new MapLocation(x * zoneSize + zoneSize / 2, y * zoneSize + zoneSize / 2);
					int dist = here.distanceSquaredTo(targetLoc);
					if (dist < closestDist) {
						closestDist = dist;
						closestLoc = targetLoc;
					}
				}
			}
		}
//		tlog("FIND CLOSEST SOUP ZONE BYTES: " + (startByte - Clock.getBytecodesLeft()));
		return closestLoc;
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
//				if (refineries[i].equals(HQLocation)) {
//					continue;
//				}
				int dist = here.distanceSquaredTo(refineries[i]);
				if (dist < closestDistance) {
					closestDistance = dist;
					closestIndex = i;
				}
			}
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
		if (closestIndex != -1 && here.distanceSquaredTo(refineries[closestIndex]) <= REFINERY_DISTANCE_LIMIT) {
			refineriesIndex = closestIndex;
			log("Targeting close refinery at " + refineries[refineriesIndex]);
			return;
		}

		// try to build a refinery
		if (closestIndex == -1) {
			mustBuild = true;
			log("Must build refinery, HQ is walled off");

			buildRefineryLocation = centerOfVisibleSoup;
			buildRefineryVisibleSoup = visibleSoup;
			log("Targeting refinery build location at " + buildRefineryLocation + " with " + buildRefineryVisibleSoup + " soup");
			return;
		}

		if (rc.getTeamSoup() >= RobotType.REFINERY.cost) {
			if (visibleSoup >= MIN_SOUP_BUILD_REFINERY) { // enough soup to warrant a refinery
				if (isLocDry(centerOfVisibleSoup)) { // centerOfVisibleSoup is not flooded
					buildRefineryLocation = centerOfVisibleSoup;
					buildRefineryVisibleSoup = visibleSoup;
					log("Targeting refinery build location at " + buildRefineryLocation + " with " + buildRefineryVisibleSoup + " soup");
					return;
				}
			}
		}

		// target the closest refinery since we cannot build one
		refineriesIndex = closestIndex;
		log("Targeting far refinery at " + refineries[refineriesIndex]);
	}
}
