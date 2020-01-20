package kryptonite;

import battlecode.common.*;

import static kryptonite.Communication.*;
import static kryptonite.Constants.*;
import static kryptonite.Debug.*;
import static kryptonite.Map.*;
import static kryptonite.Zones.*;

public class BotDeliveryDrone extends Globals {

	// roundNum where drones go onto offense
	final public static int OFFENSE_DRONE_TRIGGER_ROUND = 2000;

	// flooding memory
	// public static MapLocation[][] floodingMemoryZone;
	public static MapLocation floodingMemory;

	public static boolean[] isWallLocFull = new boolean[8];

	// what type of robot transport are we doing
	public static boolean transportRobotToWater; // used to kill enemies

	public static boolean transportRobotToWall;
	public static MapLocation transportWalLoc = null;
	public static boolean foundWall = false;

	public static boolean transportRobotToSupport;
	public static MapLocation transportSupportLoc = null;
	public static boolean foundSupport = false;

	public static boolean transportRobotToOutwards;
	public static MapLocation transportOutwardsLoc = null;

	public static MapLocation[] campLocs;
	public static boolean[] campLocsOccupied; // the last time this campLocation was visible, was it occupied?
	public static int campLocsLength;

	public static boolean isOffenseDrone = false;
	public static boolean isDroneSwarming = false;

	public static void loop() throws GameActionException {
		while (true) {
			try {
				Globals.update();
				if (firstTurn) {

					Nav.isDrone = true;
					log("LOADING CAMP LOCATIONS");

					// determine campLocations
					campLocs = new MapLocation[] {HQLoc.translate(2, 0), HQLoc.translate(-2, 0), HQLoc.translate(0, 2), HQLoc.translate(0,-2)};

					campLocsOccupied = new boolean[campLocs.length];
					campLocsLength = 4;
					Globals.endTurn(true);
					Globals.update();
				}

				if (wallFull && supportFull) {
					isOffenseDrone = true;
				}
				if (roundNum % 500 == 0) {
					isDroneSwarming = true;
				}
				if (roundNum % 500 == 50) {
					isDroneSwarming = false;
				}

				if (isOffenseDrone) {
					BotDeliveryDroneOffense.turn();
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

		// tries to visually check whether wall is full or not
		if (!wallFull) {
			for (int i = 0; i < directions.length; i++) {
				MapLocation loc = HQLoc.add(directions[i]);
				if (!rc.onTheMap(loc)) {
					isWallLocFull[i] = true;
					continue;
				}
				if (rc.canSenseLocation(loc)) {
					RobotInfo ri = rc.senseRobotAtLocation(loc);
					if (ri != null && ri.team == us && ri.type == RobotType.LANDSCAPER) {
						isWallLocFull[i] = true;
					} else {
						isWallLocFull[i] = false;
					}
				}
			}

			wallFull = true;
			for (int i = 0; i < isWallLocFull.length; i++) {
				if (!isWallLocFull[i]) {
					wallFull = false;
					break;
				}
			}
		}

		locateFlooding();
		log("floodingMemory: " + floodingMemory);

		if (!rc.isReady()) {
			log("Not ready");
			return;
		}

		int avoidDangerResult = Nav.avoidDanger();
		if (avoidDangerResult == 1) {
			return;
		}

		// chase enemies and drop them into water
		boolean sawEnemy = tryKillRobots(visibleEnemies, them);
		if (sawEnemy) {
			return;
		}
		boolean sawCow = tryKillRobots(visibleCows, cowTeam);
		if (sawCow) {
			return;
		}

		if (wallFull && supportFull) {
			log("Wall and support full, swarming ally HQ");
			moveLog(HQLoc);
			return;
		}

		if (rc.isCurrentlyHoldingUnit()) {

			if (transportRobotToOutwards) { // moves miner to outside of wall/support

				// check adjacent tiles for a tile outside the wall that is not occupied/flooded
				for (Direction dir : directions) {
					MapLocation loc = rc.adjacentLocation(dir);
					if (!rc.onTheMap(loc)) {
						continue;
					}
					if (maxXYDistance(HQLoc, loc) >= 3 && isLocDryEmpty(loc)) {
						log("Dropped robot outside the wall at " +  loc);
						tlog("Dropped " +  dir);
						Actions.doDropUnit(dir);
						transportRobotToOutwards = false;
						transportOutwardsLoc = null;
						return;
					}
				}

				// if movingOutwardsLoc is flooded, revert to symmetry
				if (rc.canSenseLocation(transportOutwardsLoc) && !isLocDry(transportOutwardsLoc)) {
					log("movingOutwardsLoc is flooded, reverting to symmetry");
					transportOutwardsLoc = getSymmetryLoc();
				}

				// go towards movingOutwardsLoc
				log("Moving to drop outwards at " + transportOutwardsLoc);
				moveLog(transportOutwardsLoc);
				return;
			}

			while (transportRobotToWall) { // moves landscaper to wall
				if (wallFull) {
					log("Wall is full, reverting to support role");
					transportRobotToWall = false;
					transportWalLoc = null;
					foundWall = false;
					transportRobotToSupport = true;
					break;
				}

				// drop robot onto a wall tile that isn't occupied/flooded
				for (Direction dir : directions) {
					MapLocation loc = rc.adjacentLocation(dir);
					if (rc.onTheMap(loc) && maxXYDistance(HQLoc, loc) == 1 && isLocDryEmpty(loc)) {
						log("Dropped robot on the wall at " +  loc);
						tlog("Dropped " +  dir);
						Actions.doDropUnit(dir);
						transportRobotToWall = false;
						transportWalLoc = null;
						foundWall = false;
						return;
					}
				}

				// reset movingToWallLoc if flooded/occupied
				if (transportWalLoc != null && rc.canSenseLocation(transportWalLoc)) {
					if (!isLocDryEmpty(transportWalLoc)) {
						transportWalLoc = null;
						foundWall = false;
						log("movingToWallLoc is flooded/occupied, resetting it");
					}
				}

				// looks for an wall location that is not flooded or occupied
				if (!foundWall) {
					for (int[] dir: senseDirections) {
						if (actualSensorRadiusSquared < dir[2]) {
							break;
						}
						MapLocation loc = here.translate(dir[0], dir[1]);
						if (maxXYDistance(HQLoc, loc) == 1) {
							if (rc.canSenseLocation(loc) && isLocDryEmpty(loc)) {
								transportWalLoc = loc;
								foundWall = true;
								log("Setting movingToWallLoc to " + transportWalLoc);
								break;
							}
						}
					}
				}

				if (transportWalLoc == null || here.equals(transportWalLoc)) {
					// if no visible open/nonflooded wall locations, try to move to the reflected position across the HQLocation
					int dx = HQLoc.x - here.x;
					int dy = HQLoc.y - here.y;
					transportWalLoc = new MapLocation(HQLoc.x + dx, HQLoc.y + dy);
					foundWall = false;
					log("Reflecting movingToWallLoc across HQ to " + transportWalLoc);
				}

				// moves towards movingToWallLoc
				log("Moving to movingToWallLoc at " + transportWalLoc);
				moveLog(transportWalLoc);
				return;
			}

			while (transportRobotToSupport) { // moves landscaper to support
				if (supportFull) {
					log("Support role is full, reverting to movingRobotOutwards");

					transportRobotToSupport = false;
					transportSupportLoc = null;
					foundSupport = false;

					Direction dirFromHQ = HQLoc.directionTo(here);
					transportRobotToOutwards = true;
					transportOutwardsLoc = here.add(dirFromHQ).add(dirFromHQ);
					if (!rc.onTheMap(transportOutwardsLoc)) {
						tlog("Initial movingOutwardsLoc not in map, reverting to symmetry");
						transportOutwardsLoc = getSymmetryLoc();
					}
					break;
				}

				// drop robot onto a support tile that isn't occupied/flooded
				for (Direction dir : directions) {
					MapLocation loc = rc.adjacentLocation(dir);
					if (!rc.onTheMap(loc)) {
						continue;
					}
					if (maxXYDistance(HQLoc, loc) == 2 && !isDigLoc(loc) && isLocDryEmpty(loc)) {
						log("Dropped robot on the support wall at " +  loc);
						tlog("Dropped " +  dir);
						Actions.doDropUnit(dir);
						transportRobotToSupport = false;
						transportSupportLoc = null;
						foundSupport = false;
						return;
					}
				}

				// reset movingToSupportLoc if flooded/occupied
				if (transportSupportLoc != null && rc.canSenseLocation(transportSupportLoc)) {
					if (!isLocDryEmpty(transportSupportLoc)) {
						transportSupportLoc = null;
						foundSupport = false;
						log("movingToSupportLoc is flooded/occupied, resetting it");
					}
				}

				// looks for an support location that is not flooded or occupied
				if (!foundSupport) {
					for (int[] dir: senseDirections) {
						if (actualSensorRadiusSquared < dir[2]) {
							break;
						}
						MapLocation loc = here.translate(dir[0], dir[1]);
						if (maxXYDistance(HQLoc, loc) == 1) {
							if (rc.canSenseLocation(loc) && isLocDryEmpty(loc)) {
								transportSupportLoc = loc;
								foundSupport = true;
								log("Setting movingToSupportLoc to " + transportSupportLoc);
								break;
							}
						}
					}
				}

				if (transportSupportLoc == null || here.equals(transportSupportLoc)) {
					// if no visible open/nonflooded support positions, try to move to the reflected position across the HQLocation
					int dx = HQLoc.x - here.x;
					int dy = HQLoc.y - here.y;
					transportSupportLoc = new MapLocation(HQLoc.x + dx, HQLoc.y + dy);
					foundSupport = false;
					log("Reflecting movingToSupportLoc across HQ to " + transportSupportLoc);
				}

				// moves towards movingToSupportLoc
				log("Moving to movingToSupportLoc at " + transportSupportLoc);
				moveLog(transportSupportLoc);
				return;
			}

		} else { // STATE == not holding a unit

			for (RobotInfo ri: adjacentAllies) {
				boolean result = tryPickUpTransport(ri);
				if (result) {
					return;
				}
			}

			for (RobotInfo ri: visibleAllies) {
				if (ri.type.canBePickedUp()) {
					if (here.isAdjacentTo(ri.location)) {
						continue;
					}

					boolean shouldTransport = false;
					int curRing = maxXYDistance(HQLoc, ri.location);

					if (ri.type == RobotType.LANDSCAPER) {
						if (isDigLoc(ri.location)) {
							shouldTransport = true;
						}
						if (!wallFull) {
							if (2 <= curRing && curRing <= 3) {
								shouldTransport = true;
							}
						} else if (!supportFull) {
							if (3 <= curRing && curRing <= 4) {
								shouldTransport = true;
							}
						}
					} else {
						// STATE == 'ri' is a miner
						if (curRing == 1 && ri.soupCarrying == 0) {
							shouldTransport = true;
						}
						if (curRing == 2 && (wallFull || ri.soupCarrying == 0)) {
							shouldTransport = true;
						}
						if (curRing <= 2 && isDigLoc(ri.location)) {
							shouldTransport = true;
						}
					}
					if (shouldTransport) {
						log("Moving to transport ally at " + ri.location);
						moveLog(ri.location);
						return;
					}
				}
			}


			// STATE: no adjacent, pick-up-able ally robots are on inner/outer transport tiles or wall

			// checks if we are already in an campLocation

			Direction dirToHQ = HQLoc.directionTo(here);
			Direction targetDir = dirToHQ.rotateRight().rotateRight();
			MapLocation targetLoc = HQLoc.add(targetDir).add(targetDir);
			log("Rotating around ally HQ");
			moveLog(targetLoc);
		}
	}

	/*
	Given an array of MapLocations and memory of its prior occupation
	Return the closest available one
	*/
	public static MapLocation findClosestOpenLocation (MapLocation[] targetLocs, boolean[] targetLocsOccupied, int targetLocsLength) throws GameActionException {

		int closestTargetDist = P_INF;
		MapLocation closestTargetLoc = null;
		for (int i = 0; i < targetLocsLength; i++) {
			int dist = here.distanceSquaredTo(targetLocs[i]);
			if (dist < closestTargetDist) {
				if (rc.canSenseLocation(targetLocs[i])) { // prioritize visible targetLocs
					if (rc.senseRobotAtLocation(targetLocs[i]) == null) {
						targetLocsOccupied[i] = false;
						closestTargetDist = dist;
						closestTargetLoc = targetLocs[i];
					} else {
						targetLocsOccupied[i] = true;
					}
				} else if (closestTargetLoc == null && !targetLocsOccupied[i]) {
					// only check not visible targetLocs if we haven't found any visible ones yet
					// this targetLocation also must not have been occupied (in previous turns)
					closestTargetDist = dist;
					closestTargetLoc = targetLocs[i];
				}
			}
		}

		return closestTargetLoc;
	}

	/*
	Given a robot, tries to pick up the unit for transport
	Returns true if the unit is valid (even if drone is not ready)
	Returns false otherwise
	*/
	public static boolean tryPickUpTransport (RobotInfo ri) throws GameActionException {
		if (ri.type.canBePickedUp()) {
			int curRing = maxXYDistance(HQLoc, ri.location);
			Direction dirFromHQ = HQLoc.directionTo(ri.location);

			if (ri.type == RobotType.LANDSCAPER) {
				if (isDigLoc(ri.location)) {
					Actions.doPickUpUnit(ri.ID);

					if (!wallFull) {
						transportRobotToWall = true;
						tlog("Transporting landscaper from dig loc to wall");
					} else if (supportFull) {
						transportRobotToSupport = true;
						tlog("Transporting landscaper from dig loc to support");
					} else {
						transportRobotToOutwards = true;
						transportOutwardsLoc = ri.location.add(dirFromHQ).add(dirFromHQ);
						tlog("Transporting landscaper from dig loc to outwards " + transportOutwardsLoc);
						if (!rc.onTheMap(transportOutwardsLoc)) {
							tlog("Initial movingOutwardsLoc not in map, reverting to symmetry");
							transportOutwardsLoc = getSymmetryLoc();
						}
					}
					return true;
				}
				if (!wallFull) {
					if (2 <= curRing && curRing <= 3) {
						Actions.doPickUpUnit(ri.ID);

						transportRobotToWall = true;
						tlog("Transporting landscaper to wall");
						return true;
					}
				} else if (!supportFull) {
					if (3 <= curRing && curRing <= 4) {
						Actions.doPickUpUnit(ri.ID);

						transportRobotToSupport = true;
						tlog("Transporting landscaper to support wall");
						return true;
					}
				}
				return false;
			} else {
				// STATE == 'ri' is a miner
				if ((curRing == 1 && ri.soupCarrying == 0) ||
						curRing == 2 && (wallFull || ri.soupCarrying == 0) ||
						(curRing <= 2 && isDigLoc(ri.location))) {
					Actions.doPickUpUnit(ri.ID);

					transportRobotToOutwards = true;
					transportOutwardsLoc = ri.location.add(dirFromHQ).add(dirFromHQ);
					tlog("Transporting miner outwards to " + transportOutwardsLoc);
					if (!rc.onTheMap(transportOutwardsLoc)) {
						tlog("Initial movingOutwardsLoc not in map, reverting to symmetry");
						transportOutwardsLoc = getSymmetryLoc();
					}
					return true;
				}
				return false;
			}
		}
		return false;
	}

	/*
	targetRobots should contain the robots that the drone is checking for
	In most cases, it will be either visibleEnemies or visibleCows
	*/
	public static boolean tryKillRobots (RobotInfo[] targetRobots, Team killTeam) throws GameActionException {
		if (rc.isCurrentlyHoldingUnit()) {
			if (!transportRobotToWater) {
				return false;
			} else {
				// check for adjacent empty water
				for (Direction dir: directions) {
					MapLocation loc = rc.adjacentLocation(dir);
					if (rc.onTheMap(loc) && rc.senseFlooding(loc) && rc.senseRobotAtLocation(loc) == null) {
						log("Dropped unit into water at " + loc);
						Actions.doDropUnit(dir);
						transportRobotToWater = false;
						return true;
					}
				}

				// go towards water
				if (floodingMemory != null) {
					// if on the flooded tile, tries to move off of it
					if (here.equals(floodingMemory)) {
						log("Moving to get off of floodingMemory");
						for (Direction dir: directions) {
							Direction move = Nav.tryMoveInDirection(dir);
							if (move != null) {
								tlog("Moved " + move);
							} else {
								tlog("But no move found");
							}
						}
						return true;
					}

					log("Moving to drop into water " + floodingMemory);
					moveLog(floodingMemory);
					return true;
				}

				// explore
				MapLocation loc = findClosestUnexploredZone();
				log("Moving to unexplored zones to find water");
				moveLog(loc);
				return true;

			}
		} else {
			// checks for adjacent enemies that can be picked up
			for (Direction dir: directions) {
				MapLocation loc = rc.adjacentLocation(dir);
				if (rc.onTheMap(loc)) {
					RobotInfo ri = rc.senseRobotAtLocation(loc);
					if (ri != null && ri.team == killTeam && ri.type.canBePickedUp()) {
						Actions.doPickUpUnit(ri.ID);
						transportRobotToWater = true;
						log("Picked up unit at " + loc);
						return true;
					}
				}
			}

			// checks for nearby enemy that can be picked up
			int closestEnemyDist = P_INF;
			int closestEnemyIndex = -1;
			int index = 0;
			for (RobotInfo ri: targetRobots) {
				if (ri.type.canBePickedUp()) {
					int dist = here.distanceSquaredTo(ri.location);
					if (dist < closestEnemyDist) {
						closestEnemyDist = dist;
						closestEnemyIndex = index;
					}
				}
			}

			// if there is a nearby enemy that can be picked up, try to chase it
			if (closestEnemyIndex != -1) {
				MapLocation loc = targetRobots[closestEnemyIndex].location;
				log("Moving to enemy at " + loc);
				Direction move = Nav.bugNavigate(loc);
				if (move != null) {
					tlog("Moved " + move);
				} else {
					tlog("But no move found");
				}
				return true;
			}
		}
		return false;
	}

	/*
	If we do not already know a visible flooded tile
		Checks visible tiles for flooding
		Saves the flooded tile to memory
	 */
	public static void locateFlooding () throws GameActionException {
		// checks if floodingMemory still exists
		if (floodingMemory != null && rc.canSenseLocation(floodingMemory)) {
			if (rc.senseFlooding(floodingMemory)) {
				log("Confirmed that floodingMemory at " + floodingMemory + " is flooded");
				return;
			} else {
				log("Resetting floodingMemory at " + floodingMemory + " since it is dry");
				floodingMemory = null;
			}
		}

		// runs if floodingMemory is not visible or is null
		// searches for a flooded tile that is empty
		for (int[] dir: senseDirections) {
			if (actualSensorRadiusSquared < dir[2]) {
				break;
			}
			MapLocation loc = here.translate(dir[0], dir[1]);
			if (rc.onTheMap(loc) && rc.senseFlooding(loc) && rc.senseRobotAtLocation(loc) == null) {
				// floodingMemory[loc.x][loc.y] = rc.senseFlooding(loc);

				log("Found visible flooded tile at " + loc);

				// if floodingMemory is null, write a Transaction
				if (floodingMemory == null) {
					writeTransactionFloodingFound(loc);
				}

				floodingMemory = loc;
				return;
			}
		}
	}
}
