package kryptonite;

import battlecode.common.*;

public class BotDeliveryDrone extends Globals {

	public static int wallRingDistance = 4;
	public static boolean insideWall;
	public static boolean onWall;
	public static boolean outsideWall;

	public static boolean movingRobotToPlot;
	public static boolean movingRobotToWall;
	public static boolean movingRobotInwards;

	public static MapLocation movingOutwardsLocation = null;
	public static boolean movingRobotOutwards;

	public static int smallWallDepth;

	public static MapLocation[] innerDigLocations;
	public static boolean[] innerDigLocationsOccupiedMemory; // the last time this digLocation was visible, was it occupied?
	public static int innerDigLocationsLength;

	public static MapLocation[] outerDigLocations;
	public static boolean[] outerDigLocationsOccupiedMemory; // the last time this digLocation was visible, was it occupied?
	public static int outerDigLocationsLength;

	public static MapLocation[] largeWall;
	public static int largeWallLength;

	public static void loop() throws GameActionException {
		while (true) {
			try {
				Globals.update();
				if (firstTurn) {

					Nav.isDrone = true;

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

					Globals.endTurn();
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


					Globals.endTurn();
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

					Globals.endTurn();
					Globals.update();
				}
			    turn();
			} catch (Exception e) {
				e.printStackTrace();
			}
			Globals.endTurn();
		}
	}

	public static boolean canPickUpType (RobotType rt) {
		return rt == RobotType.MINER || rt == RobotType.LANDSCAPER || rt == RobotType.COW;
	}

	public static void turn() throws GameActionException {

		insideWall = wallRingDistance > maxXYDistance(HQLocation, here);
		onWall = wallRingDistance == maxXYDistance(HQLocation, here);
		outsideWall = wallRingDistance < maxXYDistance(HQLocation, here);


		if (rc.isCurrentlyHoldingUnit()) {

			if (movingRobotInwards) {// moves miner to within the wall, on the 5x5 plot

				// check adjacent tiles for a 5x5 plot tile that is not occupied/flooded
				for (Direction dir : directions) {
					MapLocation loc = rc.adjacentLocation(dir);
					if (inMap(loc) && maxXYDistance(HQLocation, loc) <= 2 && !rc.senseFlooding(loc) && rc.senseRobotAtLocation(loc) == null) {
						Debug.tlog("Dropped robot inside the wall at " +  loc);
						if (rc.isReady()) {
							Debug.ttlog("Dropped " +  dir);
							rc.dropUnit(dir);
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
							rc.dropUnit(dir);
							movingRobotOutwards = false;
							movingOutwardsLocation = null;
						} else {
							Debug.ttlog("But not ready");
						}
						return;
					}
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
							rc.dropUnit(dir);
							movingRobotToWall = false;
						} else {
							Debug.ttlog("But not ready");
						}
						return;
					}
				}
				Debug.tlog("Cannot drop held unit to safe wall tile");

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
							rc.dropUnit(dir);
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
						rc.pickUpUnit(closestAllyInfo.ID);
						Debug.ttlog("Success");
					} else {
						Debug.ttlog("But not ready");
					}
				} else {
					// go to ally that is stuck
					Debug.tlog("Moving to ally at digLocation " + closestAllyInfo.location);
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
			
			// checks if we are already in an innerDigLocation
			if (inArray(innerDigLocations, here, innerDigLocationsLength)) {
				Debug.tlog("Already in an innerDigLocation");
				for (Direction dir: directions) {
					MapLocation loc = rc.adjacentLocation(dir);
					if (inMap(loc) && maxXYDistance(HQLocation, loc) == maxXYDistance(HQLocation, here)) { // if on the same ring as me
						RobotInfo ri = rc.senseRobotAtLocation(loc);
						if (ri != null && canPickUpType(ri.type)) {
							Debug.tlog("Picking up robot on inner transport tile at " +  loc);
							if (rc.isReady()) {
								Debug.ttlog("Picked up " +  dir);
								rc.pickUpUnit(ri.ID);
								if (ri.type == RobotType.LANDSCAPER) {
									movingRobotToWall = true;
									Debug.ttlog("Moving robot to wall from inner");
								} else {
									movingRobotOutwards = true;
									Direction dirFromHQ = HQLocation.directionTo(here);
									movingOutwardsLocation = here.add(dirFromHQ).add(dirFromHQ);
									Debug.ttlog("Moving robot outwards");
								}
							} else {
								Debug.ttlog("But not ready");
							}
							return;
						}
					}
				}
				Debug.tlog("Nobody on the inner transport tiles");
				return;
			}

			// checks if we are already in an outerDigLocation
			if (inArray(outerDigLocations, here, outerDigLocationsLength)) {
				Debug.tlog("Already in an outerDigLocation");
				for (Direction dir: directions) {
					MapLocation loc = rc.adjacentLocation(dir);
					if (inMap(loc) && maxXYDistance(HQLocation, loc) == maxXYDistance(HQLocation, here)) { // if on the same ring as me
						RobotInfo ri = rc.senseRobotAtLocation(loc);
						if (ri != null && canPickUpType(ri.type)) {
							Debug.tlog("Picking up robot on outer transport tile at " +  loc);
							if (rc.isReady()) {
								Debug.ttlog("Picked up " +  dir);
								rc.pickUpUnit(ri.ID);
								if (ri.type == RobotType.LANDSCAPER) {
									movingRobotToWall = true;
									Debug.ttlog("Moving robot to wall from outer");
								} else {
									movingRobotInwards = true;
									Debug.ttlog("Moving robot inwards");
								}
							} else {
								Debug.ttlog("But not ready");
							}
							return;
						}
					}
				}
				Debug.tlog("Nobody on the outer transport tiles");
				return;
			}

			// STATE == no ally robots are stuck in a dig location or on a transport tile

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

	public static void richardMethod() throws GameActionException {
		if (rc.isCurrentlyHoldingUnit()) {
			for (Direction dir: directions) {
				MapLocation loc = rc.adjacentLocation(dir);
				if (inMap(loc) && rc.senseFlooding(loc) && rc.canDropUnit(dir)) {
					rc.dropUnit(dir);
					Debug.tlog("Dropped unit into water at " + loc);
					return;
				}
			}
		} else {
			// checks for adjacent enemies that can be picked up
			for (Direction dir: directions) {
				MapLocation loc = rc.adjacentLocation(dir);
				if (inMap(loc)) {
					RobotInfo ri = rc.senseRobotAtLocation(loc);
					if (ri != null && ri.team == them && rc.canPickUpUnit(ri.ID)) {
						rc.pickUpUnit(ri.ID);
						Debug.tlog("Picked up unit at " + loc);
						return;
					}
				}
			}

			// checks for nearby enemy that can be picked up
			int closestEnemyDist = P_INF;
			int closestEnemyIndex = -1;
			int index = 0;
			for (RobotInfo ri: visibleEnemies) {
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
				MapLocation loc = visibleEnemies[closestEnemyIndex].location;
				Direction move = Nav.tryMoveInDirection(here.directionTo(loc));
				Debug.tlog("Chasing enemy at " + loc + ", moved " + move);
			}
		}

		// moves away from HQLocation
		int curHQDist = here.distanceSquaredTo(HQLocation);
		for (Direction dir: directions) {
			MapLocation loc = rc.adjacentLocation(dir);
			if (rc.canMove(dir) && HQLocation.distanceSquaredTo(loc) > curHQDist) {
				Actions.doMove(dir);
			}
		}
	}
}
