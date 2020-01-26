package landscaper_rush_bot;

import battlecode.common.*;

import static landscaper_rush_bot.Communication.*;
import static landscaper_rush_bot.Constants.*;
import static landscaper_rush_bot.Debug.*;
import static landscaper_rush_bot.Map.*;

public class BotDeliveryDrone extends Globals {

	// roundNum where drones go onto offense
	final public static int OFFENSE_DRONE_TRIGGER_ROUND = 2000;

	public static boolean insideWall;
	public static boolean onWall;
	public static boolean outsideWall;

	// nearby pick-up-able robots
	public static RobotInfo[] allyMoveableRobots;
	public static int allyMoveableRobotsLength;

	// flooding memory
	// public static MapLocation[][] floodingMemoryZone;
	public static MapLocation floodingMemory;

	// what type of robot transport are we doing
	public static boolean movingRobotToWater; // used to kill enemies

	public static boolean movingRobotToWall;
	public static MapLocation movingToWallLocation = null;
	public static boolean foundWall = false;

	public static boolean movingRobotInwards;
	public static boolean movingRobotOutwards;
	public static MapLocation movingOutwardsLocation = null;

	public static MapLocation[] campLocations;
	public static boolean[] campLocationsOccupiedMemory; // the last time this campLocation was visible, was it occupied?
	public static int campLocationsLength;

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
					int innerRingRadius = smallWallRingRadius;
					campLocations = new MapLocation[8];
					campLocationsOccupiedMemory = new boolean[campLocations.length];
					MapLocation templ = HQLocation.translate(innerRingRadius, innerRingRadius);
					int index = 0;
					for(int i = 0; i < innerRingRadius + 1; i++) for(int j = 0; j < innerRingRadius + 1; j++) {
						MapLocation newl = templ.translate(-2 * i, -2 * j);
						if(inMap(newl) && !HQLocation.equals(newl)) {
							if (maxXYDistance(HQLocation, newl) >= innerRingRadius) { // excludes holes inside the 5x5 plot
								// excludes corners
								if (HQLocation.distanceSquaredTo(newl) == 18) {
									continue;
								}
								campLocations[index] = newl;
								index++;
							}
						}
					}
					campLocationsLength = index;

					Globals.endTurn(true);
					Globals.update();

					// floodingMemory = new boolean[MAX_MAP_SIZE][MAX_MAP_SIZE];
					//
					// Globals.endTurn(true);
					// Globals.update();

					loadWallInformation();
				}

				if (largeWallFull) {
					isOffenseDrone = true;
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

		/*
		update drone specific parameters
		*/

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

		insideWall = largeWallRingRadius > maxXYDistance(HQLocation, here);
		onWall = largeWallRingRadius == maxXYDistance(HQLocation, here);
		outsideWall = largeWallRingRadius < maxXYDistance(HQLocation, here);

		allyMoveableRobots = new RobotInfo[69]; // for sensorRadiusSquared = 24
		int index = 0;
		for (RobotInfo ri: visibleAllies) {
			if (canBePickedUpType(ri.type)) {
				allyMoveableRobots[index] = ri;
				index++;
			}
		}
		allyMoveableRobotsLength = index;

		// chase enemies and drop them into water
		boolean sawEnemy = tryKillRobots(visibleEnemies, them);
		if (sawEnemy) {
			return;
		}
		boolean sawCow = tryKillRobots(visibleCows, cowTeam);
		if (sawCow) {
			return;
		}

		if (rc.isCurrentlyHoldingUnit()) {

			if (movingRobotInwards) {// moves miner to within the wall, on the 5x5 plot

				// check adjacent tiles for a 5x5 plot tile that is not occupied/flooded
				for (Direction dir : directions) {
					MapLocation loc = rc.adjacentLocation(dir);
					if (inMap(loc) && maxXYDistance(HQLocation, loc) <= 2 && !rc.senseFlooding(loc) && rc.senseRobotAtLocation(loc) == null) {
						log("Dropped robot inside the wall at " +  loc);
						tlog("Dropped " +  dir);
						Actions.doDropUnit(dir);
						movingRobotInwards = false;
						return;
					}
				}

				// go towards HQLocation
				log("Moving to drop inwards");
				Direction move = Nav.bugNavigate(HQLocation);
				if (move != null) {
					tlog("Moved " + move);
				} else {
					tlog("But no move found");
				}

				return;
			}

			if (movingRobotOutwards) { // moves miner to outside wall

				// check adjacent tiles for a tile outside the wall that is not occupied/flooded
				for (Direction dir : directions) {
					MapLocation loc = rc.adjacentLocation(dir);
					if (inMap(loc) && maxXYDistance(HQLocation, loc) > (largeWallRingRadius + 1) && !rc.senseFlooding(loc) && rc.senseRobotAtLocation(loc) == null) {
						log("Dropped robot outside the wall at " +  loc);
						tlog("Dropped " +  dir);
						Actions.doDropUnit(dir);
						movingRobotOutwards = false;
						movingOutwardsLocation = null;
						return;
					}
				}

				// if movingOutwardsLocation is flooded or not in map, revert to symmetry
				if (rc.canSenseLocation(movingOutwardsLocation) &&
						(rc.senseFlooding(movingOutwardsLocation))) {
					log("movingOutwardsLocation is flooded, reverting to symmetry");
					movingOutwardsLocation = getSymmetryLocation();
				}

				// go towards movingOutwardsLocation
				log("Moving to drop outwards at " + movingOutwardsLocation);
				Direction move = Nav.bugNavigate(movingOutwardsLocation);
				if (move != null) {
					tlog("Moved " + move);
				} else {
					tlog("But no move found");
				}

				return;
			}

			if (movingRobotToWall) { // moves landscaper to wall
				// drop robot onto a wall tile that isn't occupied/flooded
				for (Direction dir : directions) {
					MapLocation loc = rc.adjacentLocation(dir);
					if (inMap(loc) && maxXYDistance(HQLocation, loc) == largeWallRingRadius && !rc.senseFlooding(loc) && rc.senseRobotAtLocation(loc) == null) {
						log("Dropped robot on the wall at " +  loc);
							tlog("Dropped " +  dir);
							Actions.doDropUnit(dir);
							movingRobotToWall = false;
							movingToWallLocation = null;
							foundWall = false;
						return;
					}
				}

				// reset movingToWallLocation if flooded/occupied
				if (movingToWallLocation != null && rc.canSenseLocation(movingToWallLocation)) {
				 	if (rc.senseFlooding(movingToWallLocation) || rc.senseRobotAtLocation(movingToWallLocation) != null) {
						movingToWallLocation = null;
						foundWall = false;
						log("movingToWallLocation is flooded/occupied, resetting it");
					}
				}

				// looks for an wall location that is not flooded or occupied
				if (!foundWall) {
					for (int[] dir: senseDirections) {
						if (actualSensorRadiusSquared < dir[2]) {
							break;
						}
						MapLocation loc = here.translate(dir[0], dir[1]);
						if (maxXYDistance(HQLocation, loc) == largeWallRingRadius) {
							if (rc.canSenseLocation(loc) && !rc.senseFlooding(loc) && rc.senseRobotAtLocation(loc) == null) {
								movingToWallLocation = loc;
								foundWall = true;
								log("Setting movingToWallLocation to " + movingToWallLocation);
								break;
							}
						}
					}
				}

				if (movingToWallLocation == null || here.equals(movingToWallLocation)) {
					// if no visible open/nonflooded wall locations, try to move to the reflected position across the HQLocation
					int dx = HQLocation.x - here.x;
					int dy = HQLocation.y - here.y;
					movingToWallLocation = new MapLocation(HQLocation.x + dx, HQLocation.y + dy);
					foundWall = false;
					log("Reflecting movingToWallLocation across HQ to " + movingToWallLocation);
				}

				// moves towards movingToWallLocation
				log("Moving to movingToWallLocation at " + movingToWallLocation);
				Direction move = Nav.bugNavigate(movingToWallLocation);
				if (move != null) {
					tlog("Moved " + move);
				} else {
					tlog("But no move found");
				}

				return;
			}

		} else { // STATE == not holding a unit

			// check if adjacent robots are on transport tiles/wall
			for (RobotInfo ri: adjacentAllies) {
				boolean result = tryPickUpTransport(ri);
				if (result) {
					return;
				}
			}

			// STATE: no adjacent, pick-up-able ally robots are on inner/outer transport tiles or wall

			// try to move towards the closest visible robot that is on transport tiles/wall
			for (RobotInfo ri: visibleAllies) {
				if (canBePickedUpType(ri.type)) {
					boolean shouldTransport = false;

					int curRing = maxXYDistance(HQLocation, ri.location);
					Direction dirFromHQ = HQLocation.directionTo(ri.location);

					if (curRing == largeWallRingRadius - 1) {
						if (isBuilderMiner(ri.ID)) continue;
						// inner transport tile
						MapLocation wallLoc = ri.location.add(dirFromHQ);
						if (!rc.canSenseLocation(wallLoc) || !checkElevation(ri.location, wallLoc)) {
							shouldTransport = true;
						}
					} else if (curRing == largeWallRingRadius && ri.type == RobotType.MINER) {
						// wall tile
						MapLocation outerLoc = ri.location.add(dirFromHQ);
						if (!rc.canSenseLocation(outerLoc) || rc.senseElevation(ri.location) > smallWallDepth + 3) {
							shouldTransport = true;
						}
					} else if (curRing == largeWallRingRadius + 1) {
						// outer transport tile
						MapLocation wallLoc = ri.location.subtract(dirFromHQ);
						if (!rc.canSenseLocation(wallLoc) || !checkElevation(ri.location, wallLoc)) {
							shouldTransport = true;
						}
					}

					if (shouldTransport) {
						log("Moving to pick up ally at " + ri.location);
						Direction move = Nav.bugNavigate(ri.location);
						if (move != null) {
							tlog("Moved " + move);
						} else {
							tlog("But no move found");
						}
						return;
					}
				}
			}

			// STATE: no visible, pick-up-able ally robots are on inner/outer transport tiles or wall

			// checks if we are already in an campLocation

			if (inArray(campLocations, here, campLocationsLength)) {
				log("Already in an campLocation, just chilling here.");
				return;
			}


			// checks if we are already in an outerDigLocation
			/*
			if (inArray(outerDigLocations, here, outerDigLocationsLength)) {
				log("Already in an outerDigLocation, just chilling here.");
				return;
			}
			 */

			// STATE == not in an campLocation
			MapLocation closestCampLocation = findClosestOpenLocation(campLocations, campLocationsOccupiedMemory, campLocationsLength);
			if (closestCampLocation != null) {
				// go to dig location
				log("Moving to closestCampLocation at " + closestCampLocation);
				Direction move = Nav.bugNavigate(closestCampLocation);
				if (move != null) {
					tlog("Moved " + move);
				} else {
					tlog("But no move found");
				}
				return;
			}



			/*
			// STATE == not in an outerDigLocation
			MapLocation closestOuterDigLocation = findClosestOpenLocation(outerDigLocations, outerDigLocationsOccupiedMemory, outerDigLocationsLength);
			if (closestOuterDigLocation != null) {
				// go to dig location
				log("Moving to closestOuterDigLocation at " + closestOuterDigLocation);
				Direction move = Nav.bugNavigate(closestOuterDigLocation);
				if (move != null) {
					tlog("Moved " + move);
				} else {
					tlog("But no move found");
				}
				return;
			}
			 */

			log("Inner and outer dig locations are visibly occupied or occupied in memory");
		}
	}

	/*
	Given an array of MapLocations and memory of its prior occupation
	Return the closest available one
	*/
	public static MapLocation findClosestOpenLocation (MapLocation[] targetLocations, boolean[] targetLocationsOccupiedMemory, int targetLocationsLength) throws GameActionException {

		int closestTargetDist = P_INF;
		MapLocation closestTargetLocation = null;
		for (int i = 0; i < targetLocationsLength; i++) {
			int dist = here.distanceSquaredTo(targetLocations[i]);
			if (dist < closestTargetDist) {
				if (rc.canSenseLocation(targetLocations[i])) { // prioritize visible targetLocations
					if (rc.senseRobotAtLocation(targetLocations[i]) == null) {
						targetLocationsOccupiedMemory[i] = false;
						closestTargetDist = dist;
						closestTargetLocation = targetLocations[i];
					} else {
						targetLocationsOccupiedMemory[i] = true;
					}
				} else if (closestTargetLocation == null && !targetLocationsOccupiedMemory[i]) {
					// only check not visible targetLocations if we haven't found any visible ones yet
					// this targetLocation also must not have been occupied (in previous turns)
					closestTargetDist = dist;
					closestTargetLocation = targetLocations[i];
				}
			}
		}

		return closestTargetLocation;
	}

	/*
	Given a robot, tries to pick up the unit for transport
	Returns true if the unit is valid (even if drone is not ready)
	Returns false otherwise
	*/
	public static boolean tryPickUpTransport (RobotInfo ri) throws GameActionException {
		if (canBePickedUpType(ri.type)) {
			int curRing = maxXYDistance(HQLocation, ri.location);
			Direction dirFromHQ = HQLocation.directionTo(ri.location);

			// if miner is on inner transport tile and is blocked by high elevation wall, move him outwards
			// if landscaper is on inner transport tile and is blocked by high elevation wall, move onto wall
			if (curRing == largeWallRingRadius - 1) {
				if (isBuilderMiner(ri.ID)) return false;
				MapLocation wallLoc = ri.location.add(dirFromHQ);
				// if we cannot sense the wallLoc, assume it is high and pick up the robot
				if (!rc.canSenseLocation(wallLoc) || !checkElevation(ri.location, wallLoc)) {
					log("Picking up robot on inner transport tile at " + ri.location);
					Actions.doPickUpUnit(ri.ID);
					if (ri.type == RobotType.LANDSCAPER) {
						// move landscapers to wall
						movingRobotToWall = true;
						tlog("Moving landscaper from inner to wall");
					} else {
						movingRobotOutwards = true;
						movingOutwardsLocation = ri.location.add(dirFromHQ).add(dirFromHQ).add(dirFromHQ);
						tlog("Moving robot outwards to " + movingOutwardsLocation);
						if (!inMap(movingOutwardsLocation)) {
							tlog("Initial movingOutwardsLocation not in map, reverting to symmetry");
							movingOutwardsLocation = getSymmetryLocation();
						}
					}
					return true;
				}
			}

			// if miner is on wall that has high elevation, move him outwards
			if (curRing == largeWallRingRadius && ri.type == RobotType.MINER) {
				MapLocation outerLoc = ri.location.add(dirFromHQ);
				if (!rc.canSenseLocation(outerLoc) || rc.senseElevation(ri.location) > smallWallDepth + 3) {
					log("Picking up miner on wall at " + ri.location);
					Actions.doPickUpUnit(ri.ID);
					if (isBuilderMiner(ri.ID)) {
						movingRobotInwards = true;
						tlog("Moving builder miner inwards from wall");
					} else {
						movingRobotOutwards = true;
						movingOutwardsLocation = ri.location.add(dirFromHQ).add(dirFromHQ).add(dirFromHQ);
						tlog("Moving robot outwards to " + movingOutwardsLocation);
						if (!inMap(movingOutwardsLocation)) {
							tlog("Initial movingOutwardsLocation not in map, reverting to symmetry");
							movingOutwardsLocation = getSymmetryLocation();
						}
					}
					return true;
				}
			}

			// if miner is on outer transport tile and is blocked by high elevation wall, move him inwards
			// if landscaper is on outer transport tile and is blocked by high elevation wall, move onto wall
			if (curRing == largeWallRingRadius + 1) {
				MapLocation wallLoc = ri.location.subtract(dirFromHQ);
				// if we cannot sense the wallLoc, assume it is high and pick up the miner
				if (!rc.canSenseLocation(wallLoc) || !checkElevation(ri.location, wallLoc)) {
					log("Picking up robot on outer transport tile at " + ri.location);
					Actions.doPickUpUnit(ri.ID);
					if (ri.type == RobotType.LANDSCAPER) {
						// move landscapers to wall
						movingRobotToWall = true;
						tlog("Moving landscaper from outer to wall");
					} else {
						// move miners inside
						movingRobotInwards = true;
						tlog("Moving miner inwards");
					}
					return true;
				}
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
			if (!movingRobotToWater) {
				return false;
			} else {
				// check for adjacent empty water
				for (Direction dir: directions) {
					MapLocation loc = rc.adjacentLocation(dir);
					if (inMap(loc) && rc.senseFlooding(loc) && rc.senseRobotAtLocation(loc) == null) {
						log("Dropped unit into water at " + loc);
						Actions.doDropUnit(dir);
						movingRobotToWater = false;
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
					Direction move = Nav.bugNavigate(floodingMemory);
					if (move != null) {
						tlog("Moved " + move);
					} else {
						tlog("But no move found");
					}
					return true;
				}

				// move away from hq
				log("Moving away from HQ to look for water");
				Direction dirFromHQ = HQLocation.directionTo(here);
				Direction move = Nav.tryMoveInGeneralDirection(dirFromHQ);
				if (move != null) {
					tlog("Moved " + move);
				} else {
					tlog("But no move found");
				}
				return true;

			}
		} else {
			// checks for adjacent enemies that can be picked up
			for (Direction dir: directions) {
				MapLocation loc = rc.adjacentLocation(dir);
				if (inMap(loc)) {
					RobotInfo ri = rc.senseRobotAtLocation(loc);
					if (ri != null && ri.team == killTeam && rc.canPickUpUnit(ri.ID)) {
						Actions.doPickUpUnit(ri.ID);
						movingRobotToWater = true;
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
				if (canBePickedUpType(ri.type)) {
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
			if (inMap(loc) && rc.senseFlooding(loc) && rc.senseRobotAtLocation(loc) == null) {
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
