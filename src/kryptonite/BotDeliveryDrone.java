package kryptonite;

import battlecode.common.*;

public class BotDeliveryDrone extends Globals {

	public static int smallWallDepth;

	public static MapLocation[] digLocations;
	public static int digLocationsLength;

	public static MapLocation[] largeWall;
	public static int largeWallLength;

	public static void loop() throws GameActionException {
		while (true) {
			try {
				Globals.update();
				if (firstTurn) {

					Nav.isDrone = true;

					smallWallDepth = rc.senseElevation(HQLocation) + 3;

					digLocations = new MapLocation[12];
					MapLocation templ = HQLocation.translate(3,3);
					int index = 0;
					for(int i = 0; i < 4; i++) for(int j = 0; j < 4; j++) {
						MapLocation newl = templ.translate(-2*i, -2*j);
						if(inMap(newl) && !HQLocation.equals(newl)) {
							if (maxXYDistance(HQLocation, newl) > 2) { // excludes holes inside the 5x5 plot
								digLocations[index] = newl;
								index++;
							}
						}
					}
					digLocationsLength = index;

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
						if(inMap(newl) && !inArray(digLocations, newl, digLocationsLength)) {
							largeWall[index] = newl;
							index++;
						}
					}
					// move left along bottom wall
					templ = HQLocation.translate(cornerDist, -cornerDist);
					for(int i = 0; i < largeWallRingSize - 1; i++) {
						MapLocation newl = templ.translate(-i, 0);
						if(inMap(newl) && !inArray(digLocations, newl, digLocationsLength)) {
							largeWall[index] = newl;
							index++;
						}
					}
					// move up along left wall
					templ = HQLocation.translate(-cornerDist, -cornerDist);
					for(int i = 0; i < largeWallRingSize - 1; i++) {
						MapLocation newl = templ.translate(0, i);
						if(inMap(newl) && !inArray(digLocations, newl, digLocationsLength)) {
							largeWall[index] = newl;
							index++;
						}
					}
					// move right along top wall
					templ = HQLocation.translate(-cornerDist, cornerDist);
					for(int i = 0; i < largeWallRingSize - 1; i++) {
						MapLocation newl = templ.translate(i, 0);
						if(inMap(newl) && !inArray(digLocations, newl, digLocationsLength)) {
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

    // TODO: Identify if we are one a low elevation, pick up robots here, and move to clean space
	public static void turn() throws GameActionException {
		if (rc.isCurrentlyHoldingUnit()) {
			// drop unit onto a 5x5 plot tile that is not occupied/flooded
			for (Direction dir : directions) {
				MapLocation loc = rc.adjacentLocation(dir);
				if (inMap(loc) && maxXYDistance(HQLocation, loc) <= 2 && !rc.senseFlooding(loc) && rc.senseRobotAtLocation(loc) == null) {
					Debug.tlog("Dropped robot on the wall at " +  loc);
					if (rc.isReady()) {
						Debug.ttlog("Dropped " +  dir);
						rc.dropUnit(dir);
					} else {
						Debug.ttlog("But not ready");
					}
					return;
				}
			}

			Debug.tlog("Cannot drop held unit to safe location");
		} else { // STATE == not holding a unit
			// find closest ally robot that is pick-up-able and is stuck in a digLocation
			int closestAllyDist = P_INF;
			RobotInfo closestAllyInfo = null;
			for (RobotInfo ri: visibleAllies) {
				if (canPickUpType(ri.type) && inArray(digLocations, ri.location, digLocationsLength)) {
					int dist = here.distanceSquaredTo(ri.location);
					if (dist < closestAllyDist) {
						closestAllyDist = dist;
						closestAllyInfo = ri;
					}
				}
			}

			if (closestAllyInfo != null) { // STATE == ally is stuck in a dig location
				// if we are adjacent to ally, pull him out
				if (here.distanceSquaredTo(closestAllyInfo.location) <= 2) {
					Debug.tlog("Picking up ally at " + closestAllyInfo.location);
					if (rc.isReady()) {
						rc.pickUpUnit(closestAllyInfo.ID);
						Debug.ttlog("Success");
					} else {
						Debug.ttlog("But not ready");
					}
				} else {
					// go to ally that is stuck
					Debug.tlog("Moving to stuck ally at " + closestAllyInfo.location);
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
			} else { // STATE == no ally robots are stuck in a dig location
				// checks if we are already in the dig location
				if (inArray(digLocations, here, digLocationsLength)) {
					Debug.tlog("Already in a dig location, just gonna chill here");
					return;
				}

				// find the closest empty dig location
				int closestDigDist = P_INF;
				MapLocation closestDigLocation = null;
				for (int i = 0; i < digLocationsLength; i++) {
					if (rc.canSenseLocation(digLocations[i]) && rc.senseRobotAtLocation(digLocations[i]) == null) {
						int dist = here.distanceSquaredTo(digLocations[i]);
						if (dist < closestDigDist) {
							closestDigDist = dist;
							closestDigLocation = digLocations[i];
						}
					}
				}

				if (closestDigLocation != null) {
					// go to dig location
					Debug.tlog("Moving to closestDigLocation at " + closestDigLocation);
					if (rc.isReady()) {
						Direction move = Nav.bugNavigate(closestDigLocation);
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
			// checks for adjacent enemies that can be picked upup
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
