package kryptonite;

import battlecode.common.*;

import static kryptonite.Communication.*;
import static kryptonite.Constants.*;
import static kryptonite.Debug.*;
import static kryptonite.Map.*;

public class BotDeliveryDrone extends Globals {

	// roundNum where drones go onto offense
	final public static int OFFENSE_DRONE_TRIGGER_ROUND = 2000;

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

	public static MapLocation[] campLocations = {HQLocation.translate(2, 0), HQLocation.translate(-2, 0), HQLocation.translate(0, 2), HQLocation.translate(0,-2)};
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
					campLocationsOccupiedMemory = new boolean[campLocations.length];
					campLocationsLength = 4;
					if(rc.getRoundNum() % 2 == 0) {
						isOffenseDrone = true;
					}
					Globals.endTurn(true);
					Globals.update();
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
			if (movingRobotToWall) { // moves landscaper to wall
				// drop robot onto a wall tile that isn't occupied/flooded
				for (Direction dir : directions) {
					MapLocation loc = rc.adjacentLocation(dir);
					if (rc.onTheMap(loc) && maxXYDistance(HQLocation, loc) == 2 && !rc.senseFlooding(loc) && rc.senseRobotAtLocation(loc) == null) {
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
						if (maxXYDistance(HQLocation, loc) == 2) {
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

			// checks if we are already in an campLocation

			if (inArray(campLocations, here, campLocationsLength)) {
				log("Already in an campLocation, just chilling here.");
				return;
			}

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

			// if miner is on wall that has high elevation, move him outwards
			if (curRing == 2 && ri.type == RobotType.MINER) {
				MapLocation outerLoc = ri.location.add(dirFromHQ);
				if (!rc.canSenseLocation(outerLoc) || rc.senseElevation(ri.location) > rc.senseElevation(HQLocation) + 3) {
					log("Picking up miner on wall at " + ri.location);
					Actions.doPickUpUnit(ri.ID);
					if (isBuilderMiner(ri.ID)) {
						movingRobotInwards = true;
						tlog("Moving builder miner inwards from wall");
					} else {
						movingRobotOutwards = true;
						movingOutwardsLocation = ri.location.add(dirFromHQ).add(dirFromHQ).add(dirFromHQ);
						tlog("Moving robot outwards to " + movingOutwardsLocation);
						if (!rc.onTheMap(movingOutwardsLocation)) {
							tlog("Initial movingOutwardsLocation not in map, reverting to symmetry");
							movingOutwardsLocation = getSymmetryLocation();
						}
					}
					return true;
				}
			}

			// if miner is on outer transport tile and is blocked by high elevation wall, move him inwards
			// if landscaper is on outer transport tile and is blocked by high elevation wall, move onto wall
			if (curRing == 3) {
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
					if (rc.onTheMap(loc) && rc.senseFlooding(loc) && rc.senseRobotAtLocation(loc) == null) {
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
				if (rc.onTheMap(loc)) {
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
