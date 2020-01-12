package kryptonite;

import battlecode.common.*;

public class BotDeliveryDrone extends Globals {

	// 5x5 plot information
	public static int smallWallDepth;

	// wall information
	public static int wallRingDistance = 4;
	public static boolean insideWall;
	public static boolean onWall;
	public static boolean outsideWall;

	// information about digLocations
	public static MapLocation[] innerDigLocations;
	public static boolean[] innerDigLocationsOccupiedMemory; // the last time this digLocation was visible, was it occupied?
	public static int innerDigLocationsLength;

	public static MapLocation[] outerDigLocations;
	public static boolean[] outerDigLocationsOccupiedMemory; // the last time this digLocation was visible, was it occupied?
	public static int outerDigLocationsLength;

	public static MapLocation[] largeWall;
	public static int largeWallLength;

	// nearby pick-up-able robots
	public static RobotInfo[] allyMoveableRobots;
	public static int allyMoveableRobotsLength;

	// flooding memory
	// public static MapLocation[][] floodingMemoryZone;
	public static MapLocation floodingMemory;

	// what type of robot transport are we doing
	public static boolean movingRobotToWater; // used to kill enemies

	public static boolean movingRobotToPlot;
	public static boolean movingRobotToWall;
	public static MapLocation movingToWallLocation = null;
	public static boolean movingRobotInwards;
	public static boolean movingRobotOutwards;
	public static MapLocation movingOutwardsLocation = null;

	public static void loop() throws GameActionException {
		while (true) {
			try {
				Globals.update();
				if (firstTurn) {

					Nav.isDrone = true;

					// floodingMemory = new boolean[MAX_MAP_SIZE][MAX_MAP_SIZE];
					//
					// Globals.endTurn(true);
					// Globals.update();

					smallWallDepth = rc.senseElevation(HQLocation) + 3;

					// determine innerDigLocations
					int innnerRingDistance = 3;
					innerDigLocations = new MapLocation[12];
					innerDigLocationsOccupiedMemory = new boolean[innerDigLocations.length];
					MapLocation templ = HQLocation.translate(innnerRingDistance, innnerRingDistance);
					int index = 0;
					for(int i = 0; i < innnerRingDistance + 1; i++) for(int j = 0; j < innnerRingDistance + 1; j++) {
						MapLocation newl = templ.translate(-2 * i, -2 * j);
						if(inMap(newl) && !HQLocation.equals(newl)) {
							if (maxXYDistance(HQLocation, newl) >= innnerRingDistance) { // excludes holes inside the 5x5 plot
								innerDigLocations[index] = newl;
								index++;
							}
						}
					}
					innerDigLocationsLength = index;

					Globals.endTurn(true);
					Globals.update();

					// determine outerDigLocations
					/* @todo - remind Richard if he still wants this
					*/
					int outerRingDistance = 5;
					outerDigLocations = new MapLocation[20];
					outerDigLocationsOccupiedMemory = new boolean[outerDigLocations.length];
					templ = HQLocation.translate(outerRingDistance, outerRingDistance);
					index = 0;
					for(int i = 0; i < outerRingDistance + 1; i++) for(int j = 0; j < outerRingDistance + 1; j++) {
						MapLocation newl = templ.translate(-2 * i, -2 * j);
						if(inMap(newl) && !HQLocation.equals(newl)) {
							if (maxXYDistance(HQLocation, newl) >= outerRingDistance) { // excludes holes inside the 9x9 plot
								outerDigLocations[index] = newl;
								index++;
							}
						}
					}
					outerDigLocationsLength = index;

					Globals.endTurn(true);
					Globals.update();

					int largeWallRingSize = 9; // must be odd
					int cornerDist = largeWallRingSize / 2;

					largeWall = new MapLocation[36];
					index = 0;

					// move down along right wall
					templ = HQLocation.translate(cornerDist, cornerDist);
					for(int i = 0; i < largeWallRingSize - 1; i++) {
						MapLocation newl = templ.translate(0, -i);
						if(inMap(newl) && !inArray(innerDigLocations, newl, innerDigLocationsLength)) {
							largeWall[index] = newl;
							index++;
						}
					}
					// move left along bottom wall
					templ = HQLocation.translate(cornerDist, -cornerDist);
					for(int i = 0; i < largeWallRingSize - 1; i++) {
						MapLocation newl = templ.translate(-i, 0);
						if(inMap(newl) && !inArray(innerDigLocations, newl, innerDigLocationsLength)) {
							largeWall[index] = newl;
							index++;
						}
					}
					// move up along left wall
					templ = HQLocation.translate(-cornerDist, -cornerDist);
					for(int i = 0; i < largeWallRingSize - 1; i++) {
						MapLocation newl = templ.translate(0, i);
						if(inMap(newl) && !inArray(innerDigLocations, newl, innerDigLocationsLength)) {
							largeWall[index] = newl;
							index++;
						}
					}
					// move right along top wall
					templ = HQLocation.translate(-cornerDist, cornerDist);
					for(int i = 0; i < largeWallRingSize - 1; i++) {
						MapLocation newl = templ.translate(i, 0);
						if(inMap(newl) && !inArray(innerDigLocations, newl, innerDigLocationsLength)) {
							largeWall[index] = newl;
							index++;
						}
					}
					largeWallLength = index;
					Debug.ttlog("LARGE WALL LENGTH: " + largeWallLength);

					Globals.endTurn(true);
					Globals.update();
				}
			    turn();
			} catch (Exception e) {
				e.printStackTrace();
			}
			Globals.endTurn(false);
		}
	}

	public static boolean canPickUpType (RobotType rt) {
		return rt == RobotType.MINER || rt == RobotType.LANDSCAPER || rt == RobotType.COW;
	}

	public static void turn() throws GameActionException {

		/*
		update drone specific parameters
		*/

		locateFlooding();

		insideWall = wallRingDistance > maxXYDistance(HQLocation, here);
		onWall = wallRingDistance == maxXYDistance(HQLocation, here);
		outsideWall = wallRingDistance < maxXYDistance(HQLocation, here);

		allyMoveableRobots = new RobotInfo[69]; // for sensorRadiusSquared = 24
		int index = 0;
		for (RobotInfo ri: visibleAllies) {
			if (canPickUpType(ri.type)) {
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
						Debug.tlog("Dropped robot inside the wall at " +  loc);
						if (rc.isReady()) {
							Debug.ttlog("Dropped " +  dir);
							Actions.doDropUnit(dir);
							movingRobotInwards = false;
						} else {
							Debug.ttlog("But not ready");
						}
						return;
					}
				}

				// go towards HQLocation
				Debug.tlog("Moving to drop inwards");
				if (rc.isReady()) {
					Direction move = Nav.bugNavigate(HQLocation);
					if (move != null) {
						Debug.ttlog("Moved " + move);
					} else {
						Debug.ttlog("But no move found");
					}
				} else {
					Debug.ttlog("But not ready");
				}

				return;
			}

			if (movingRobotOutwards) { // moves miner to outside wall

				// check adjacent tiles for a tile outside the wall that is not occupied/flooded
				for (Direction dir : directions) {
					MapLocation loc = rc.adjacentLocation(dir);
					if (inMap(loc) && maxXYDistance(HQLocation, loc) > (wallRingDistance + 1) && !rc.senseFlooding(loc) && rc.senseRobotAtLocation(loc) == null) {
						Debug.tlog("Dropped robot outside the wall at " +  loc);
						if (rc.isReady()) {
							Debug.ttlog("Dropped " +  dir);
							Actions.doDropUnit(dir);
							movingRobotOutwards = false;
							movingOutwardsLocation = null;
						} else {
							Debug.ttlog("But not ready");
						}
						return;
					}
				}

				// if movingOutwardsLocation is flooded, revert to symmetry
				if (rc.canSenseLocation(movingOutwardsLocation) && rc.senseFlooding(movingOutwardsLocation)) {
					Debug.tlog("movingOutwardsLocation is flooded, reverting to symmetry");
					movingOutwardsLocation = symmetryHQLocations[0];
				}

				// go towards movingOutwardsLocation
				Debug.tlog("Moving to drop outwards at " + movingOutwardsLocation);
				if (rc.isReady()) {
					Direction move = Nav.bugNavigate(movingOutwardsLocation);
					if (move != null) {
						Debug.ttlog("Moved " + move);
					} else {
						Debug.ttlog("But no move found");
					}
				} else {
					Debug.ttlog("But not ready");
				}

				return;
			}

			if (movingRobotToWall) { // moves landscaper to wall
				// drop robot onto a wall tile that isn't occupied/flooded
				for (Direction dir : directions) {
					MapLocation loc = rc.adjacentLocation(dir);
					if (inMap(loc) && maxXYDistance(HQLocation, loc) == wallRingDistance && !rc.senseFlooding(loc) && rc.senseRobotAtLocation(loc) == null) {
						Debug.tlog("Dropped robot on the wall at " +  loc);
						if (rc.isReady()) {
							Debug.ttlog("Dropped " +  dir);
							Actions.doDropUnit(dir);
							movingRobotToWall = false;
						} else {
							Debug.ttlog("But not ready");
						}
						return;
					}
				}

				if (movingToWallLocation == null) {
					for (int[] dir: senseDirections) {
						if (actualSensorRadiusSquared < dir[2]) {
							break;
						}
						MapLocation loc = here.translate(dir[0], dir[1]);
						if (maxXYDistance(HQLocation, loc) == wallRingDistance) {
							if (rc.canSenseLocation(loc) && rc.senseFlooding(loc) && rc.senseRobotAtLocation(loc) == null) {
								movingToWallLocation = loc;
								break;
							}
						}
					}
				}

				if (movingToWallLocation == null) {
					// if no visible open/nonflooded wall locations, try to move to the reflected position across the HQLocation
					int dx = HQLocation.x - here.x;
					int dy = HQLocation.y - here.y;
					movingToWallLocation = new MapLocation(HQLocation.x + dx, HQLocation.y + dy);
				}

				Debug.tlog("Moving to movingToWallLocation at " + movingToWallLocation);
				if (rc.isReady()) {
					Direction move = Nav.bugNavigate(movingToWallLocation);
					if (move != null) {
						Debug.ttlog("Moved " + move);
					} else {
						Debug.ttlog("But no move found");
					}
				} else {
					Debug.ttlog("But not ready");
				}

				return;
			}

			if (movingRobotToPlot) { // moving a robot from the dig spot to the plot

				// drop robot onto a 5x5 plot tile that is not occupied/flooded
				for (Direction dir : directions) {
					MapLocation loc = rc.adjacentLocation(dir);
					if (inMap(loc) && maxXYDistance(HQLocation, loc) <= 2 && !rc.senseFlooding(loc) && rc.senseRobotAtLocation(loc) == null) {
						Debug.tlog("Dropped robot on the 5x5 plot at " +  loc);
						if (rc.isReady()) {
							Debug.ttlog("Dropped " +  dir);
							Actions.doDropUnit(dir);
							movingRobotToPlot = false;
						} else {
							Debug.ttlog("But not ready");
						}
						return;
					}
				}

				// go towards HQLocation
				Debug.tlog("Moving to drop in 5x5 plot");
				if (rc.isReady()) {
					Direction move = Nav.bugNavigate(HQLocation);
					if (move != null) {
						Debug.ttlog("Moved " + move);
					} else {
						Debug.ttlog("But no move found");
					}
				} else {
					Debug.ttlog("But not ready");
				}
			}

		} else { // STATE == not holding a unit

			// find closest ally robot that is pick-up-able and is stuck in a digLocation
			int closestAllyDist = P_INF;
			RobotInfo closestAllyInfo = null;
			for (RobotInfo ri: visibleAllies) {
				if (isBuilderMiner(ri.ID)) continue;
				if (canPickUpType(ri.type) && inArray(innerDigLocations, ri.location, innerDigLocationsLength)) {
					int dist = here.distanceSquaredTo(ri.location);
					if (dist < closestAllyDist) {
						closestAllyDist = dist;
						closestAllyInfo = ri;
					}
				}
			}

			if (closestAllyInfo != null) { // STATE == ally is stuck in a dig location
				movingRobotToPlot = true;
				// if we are adjacent to ally, pull him out
				if (here.distanceSquaredTo(closestAllyInfo.location) <= 2) {
					Debug.tlog("Picking up ally at digLocation " + closestAllyInfo.location);
					if (rc.isReady()) {
						Actions.doPickUpUnit(closestAllyInfo.ID);
						Debug.ttlog("Success");
					} else {
						Debug.ttlog("But not ready");
					}
				} else {
					// go to ally that is stuck
					Debug.tlog("Moving to pick up ally at digLocation " + closestAllyInfo.location);
					if (rc.isReady()) {
						Direction move = Nav.bugNavigate(closestAllyInfo.location);
						if (move != null) {
							Debug.ttlog("Moved " + move);
						} else {
							Debug.ttlog("But no move found");
						}
					} else {
						Debug.ttlog("But not ready");
					}
				}

				return;
			}

			// check if adjacent robots are on transport tiles/wall
			for (RobotInfo ri: adjacentAllies) {
				if (isBuilderMiner(ri.ID)) continue;
				boolean result = tryPickUpTransport(ri);
				if (result) {
					return;
				}
			}

			// STATE: no adjacent, pick-up-able ally robots are on inner/outer transport tiles or wall

			// try to move towards the closest visible robot that is on transport tiles/wall
			for (RobotInfo ri: visibleAllies) {
				if (isBuilderMiner(ri.ID)) continue;
				if (canPickUpType(ri.type)) {
					boolean shouldTransport = false;

					int curRing = maxXYDistance(HQLocation, ri.location);
					Direction dirFromHQ = HQLocation.directionTo(ri.location);

					if (curRing == wallRingDistance - 1 && ri.type == RobotType.MINER) {
						// inner transport tile
						MapLocation wallLoc = ri.location.add(dirFromHQ);
						if (!rc.canSenseLocation(wallLoc) || !Nav.checkElevation(ri.location, wallLoc)) {
							shouldTransport = true;
						}
					} else if (curRing == wallRingDistance && ri.type == RobotType.MINER) {
						// wall tile
						MapLocation outerLoc = ri.location.add(dirFromHQ);
						if (!rc.canSenseLocation(outerLoc) || !Nav.checkElevation(ri.location, outerLoc)) {
							shouldTransport = true;
						}
					} else if (curRing == wallRingDistance + 1) {
						// outer transport tile
						MapLocation wallLoc = ri.location.subtract(dirFromHQ);
						if (!rc.canSenseLocation(wallLoc) || !Nav.checkElevation(ri.location, wallLoc)) {
							shouldTransport = true;
						}
					}

					if (shouldTransport) {
						Debug.tlog("Moving to pick up ally at " + ri.location);
						if (rc.isReady()) {
							Direction move = Nav.bugNavigate(ri.location);
							if (move != null) {
								Debug.ttlog("Moved " + move);
							} else {
								Debug.ttlog("But no move found");
							}
						} else {
							Debug.ttlog("But not ready");
						}
						return;
					}
				}
			}

			// STATE: no visible, pick-up-able ally robots are on inner/outer transport tiles or wall

			// checks if we are already in an innerDigLocation
			/*
			if (inArray(innerDigLocations, here, innerDigLocationsLength)) {
				Debug.tlog("Already in an innerDigLocation, just chilling here.");
				return;
			}
			*/

			// checks if we are already in an outerDigLocation
			if (inArray(outerDigLocations, here, outerDigLocationsLength)) {
				Debug.tlog("Already in an outerDigLocation, just chilling here.");
				return;
			}

			/*
			MapLocation closestInnerDigLocation = findClosestOpenLocation(innerDigLocations, innerDigLocationsOccupiedMemory, innerDigLocationsLength);
			if (closestInnerDigLocation != null) {
				// go to dig location
				Debug.tlog("Moving to closestInnerDigLocation at " + closestInnerDigLocation);
				if (rc.isReady()) {
					Direction move = Nav.bugNavigate(closestInnerDigLocation);
					if (move != null) {
						Debug.ttlog("Moved " + move);
					} else {
						Debug.ttlog("But no move found");
					}
				} else {
					Debug.ttlog("But not ready");
				}
				return;
			}
			*/

			// STATE == not in an outerDigLocation

			MapLocation closestOuterDigLocation = findClosestOpenLocation(outerDigLocations, outerDigLocationsOccupiedMemory, outerDigLocationsLength);
			if (closestOuterDigLocation != null) {
				// go to dig location
				Debug.tlog("Moving to closestOuterDigLocation at " + closestOuterDigLocation);
				if (rc.isReady()) {
					Direction move = Nav.bugNavigate(closestOuterDigLocation);
					if (move != null) {
						Debug.ttlog("Moved " + move);
					} else {
						Debug.ttlog("But no move found");
					}
				} else {
					Debug.ttlog("But not ready");
				}
				return;
			}

			Debug.tlog("Inner and outer dig locations are visibly occupied or occupied in memory");
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
		if (canPickUpType(ri.type)) {
			int curRing = maxXYDistance(HQLocation, ri.location);
			Direction dirFromHQ = HQLocation.directionTo(ri.location);

			// if miner is on inner transport tile and is blocked by high elevation wall, move him outwards
			// ignore landscapers for now
			if (curRing == wallRingDistance - 1 && ri.type == RobotType.MINER) {
				MapLocation wallLoc = ri.location.add(dirFromHQ);
				// if we cannot sense the wallLoc, assume it is high and pick up the miner
				if (!rc.canSenseLocation(wallLoc) || !Nav.checkElevation(ri.location, wallLoc)) {
					Debug.tlog("Picking up robot on inner transport tile at " + ri.location);
					if (rc.isReady()) {
						Actions.doPickUpUnit(ri.ID);
						movingRobotOutwards = true;
						movingOutwardsLocation = here.add(dirFromHQ).add(dirFromHQ).add(dirFromHQ);
						if (!inMap(movingOutwardsLocation)) {
							Debug.ttlog("Initial movingOutwardsLocation not in map, reverting to symmetry");
							movingOutwardsLocation = symmetryHQLocations[0];
						}
						Debug.ttlog("Moving robot outwards");
					} else {
						Debug.ttlog("But not ready");
					}
					return true;
				}
			}

			// if miner is on wall that has high elevation, move him outwards
			if (curRing == wallRingDistance && ri.type == RobotType.MINER) {
				MapLocation outerLoc = ri.location.add(dirFromHQ);
				// if we cannot sense the outerLoc, assume it is high and pick up the miner
				if (!rc.canSenseLocation(outerLoc) || !Nav.checkElevation(ri.location, outerLoc)) {
					Debug.tlog("Picking up miner on wall at " + ri.location);
					if (rc.isReady()) {
						Actions.doPickUpUnit(ri.ID);
						movingRobotOutwards = true;
						movingOutwardsLocation = here.add(dirFromHQ).add(dirFromHQ).add(dirFromHQ);
						if (!inMap(movingOutwardsLocation)) {
							Debug.ttlog("Initial movingOutwardsLocation not in map, reverting to symmetry");
							movingOutwardsLocation = symmetryHQLocations[0];
						}
						Debug.ttlog("Moving robot outwards");
					} else {
						Debug.ttlog("But not ready");
					}
					return true;
				}
			}

			// if miner is on inner transport tile and is blocked by high elevation wall, move him outwards
			// if landscaper is on inner transport tile and is blocked by high elevation wall, move onto wall
			if (curRing == wallRingDistance + 1) {
				MapLocation wallLoc = ri.location.subtract(dirFromHQ);
				// if we cannot sense the wallLoc, assume it is high and pick up the miner
				if (!rc.canSenseLocation(wallLoc) || !Nav.checkElevation(ri.location, wallLoc)) {
					Debug.tlog("Picking up robot on outer transport tile at " + ri.location);
					if (rc.isReady()) {
						Actions.doPickUpUnit(ri.ID);
						if (ri.type == RobotType.LANDSCAPER) {
							// move landscapers to wall
							movingRobotToWall = true;
							Debug.ttlog("Moving landscaper to wall from inner");
						} else {
							// move miners inside
							movingRobotInwards = true;
							Debug.ttlog("Moving miner inwards");
						}
					} else {
						Debug.ttlog("But not ready");
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
						Debug.tlog("Dropped unit into water at " + loc);
						if (rc.isReady()) {
							Actions.doDropUnit(dir);
							movingRobotToWater = false;
							Debug.ttlog("Success");
						} else {
							Debug.ttlog("But not ready");
						}
						return true;
					}
				}

				// go towards water
				if (floodingMemory != null) {
					Debug.tlog("Moving to drop into water " + floodingMemory);
					if (rc.isReady()) {
						Direction move = Nav.bugNavigate(floodingMemory);
						if (move != null) {
							Debug.ttlog("Moved " + move);
						} else {
							Debug.ttlog("But no move found");
						}
					} else {
						Debug.ttlog("But not ready");
					}
					return true;
				}

				// move away from hq
				Debug.tlog("Moving away from HQ to look for water");
				if (rc.isReady()) {
					Direction dirFromHQ = HQLocation.directionTo(here);
					Direction move = Nav.tryMoveInGeneralDirection(dirFromHQ);
					if (move != null) {
						Debug.ttlog("Moved " + move);
					} else {
						Debug.ttlog("But no move found");
					}
				} else {
					Debug.ttlog("But not ready");
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
						Debug.tlog("Picked up unit at " + loc);
						return true;
					}
				}
			}

			// checks for nearby enemy that can be picked up
			int closestEnemyDist = P_INF;
			int closestEnemyIndex = -1;
			int index = 0;
			for (RobotInfo ri: targetRobots) {
				if (canPickUpType(ri.type)) {
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
				Debug.tlog("Moving to enemy at " + loc);
				if (rc.isReady()) {
					Direction move = Nav.bugNavigate(loc);
					if (move != null) {
						Debug.ttlog("Moved " + move);
					} else {
						Debug.ttlog("But no move found");
					}
				} else {
					Debug.ttlog("But not ready");
				}
				return true;
			}
		}
		return false;
	}

	public static void locateFlooding () throws GameActionException {

		for (int[] dir: senseDirections) {
			if (actualSensorRadiusSquared < dir[2]) {
				break;
			}
			MapLocation loc = here.translate(dir[0], dir[1]);
			if (rc.canSenseLocation(loc) && rc.senseFlooding(loc) && rc.senseRobotAtLocation(loc) == null) {
				// floodingMemory[loc.x][loc.y] = rc.senseFlooding(loc);
				floodingMemory = loc;
				break;
			}
		}

	}
}
