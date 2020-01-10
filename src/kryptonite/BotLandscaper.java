package kryptonite;

import battlecode.common.*;

public class BotLandscaper extends Globals {

	private static MapLocation[] digSpots, smallWall, largeWall, smallWallCompleted;
	private static int digSpotsLength, smallWallLength, largeWallLength, smallWallDepth,
            smallWallCompletedLength, currentRing = -1, currentStep;
	private static boolean moveClockwise = true;

	public static void loop() throws GameActionException {
		while (true) {
			try {
				Globals.update();
				if (firstTurn) {
					// finds spots that can be used for digging
					digSpots = new MapLocation[12];
					MapLocation templ = HQLocation.translate(3,3);
					int index = 0;
					for(int i = 0; i < 4; i++) for(int j = 0; j < 4; j++) {
						MapLocation newl = templ.translate(-2*i, -2*j);
						if(inMap(newl) && !HQLocation.equals(newl)) {
							if (HQLocation.distanceSquaredTo(newl) >= 9) { // excludes holes inside the 5x5 plot
								digSpots[index] = newl;
								index++;
							}
						}
					}
					digSpotsLength = index;

					Globals.endTurn();
					Globals.update();

					// finds tiles that are on the 5x5 plot
					smallWall = new MapLocation[24];
					smallWallCompleted = new MapLocation[24];
					index = 0;
					templ = HQLocation.translate(2, 2);
					for(int i = 0; i < 5; i++) for(int j = 0; j < 5; j++) {
						MapLocation newl = templ.translate(-i, -j);
						if (inMap(newl) && !HQLocation.equals(newl) && !inArray(digSpots, newl, digSpotsLength)) {
                            smallWall[index] = newl;
                            index++;
						}
					}

					smallWallLength = index;
					smallWallDepth = rc.senseElevation(HQLocation) + 3;
					Debug.ttlog("SMALL WALL LENGTH: " + smallWallLength);
					Debug.ttlog("SMALL WALL DEPTH: " + smallWallDepth);

					Globals.endTurn();
					Globals.update();

					smallWallCompletedLength = 0;

					int largeWallRingSize = 9; // must be odd
					int cornerDist = largeWallRingSize / 2;

					largeWall = new MapLocation[36];
					index = 0;

					// move down along right wall
					templ = HQLocation.translate(cornerDist, cornerDist);
					for(int i = 0; i < largeWallRingSize - 1; i++) {
						MapLocation newl = templ.translate(0, -i);
						if(inMap(newl) && !inArray(digSpots, newl, digSpotsLength)) {
							largeWall[index] = newl;
							index++;
						}
					}
					// move left along bottom wall
					templ = HQLocation.translate(cornerDist, -cornerDist);
					for(int i = 0; i < largeWallRingSize - 1; i++) {
						MapLocation newl = templ.translate(-i, 0);
						if(inMap(newl) && !inArray(digSpots, newl, digSpotsLength)) {
							largeWall[index] = newl;
							index++;
						}
					}
					// move up along left wall
					templ = HQLocation.translate(-cornerDist, -cornerDist);
					for(int i = 0; i < largeWallRingSize - 1; i++) {
						MapLocation newl = templ.translate(0, i);
						if(inMap(newl) && !inArray(digSpots, newl, digSpotsLength)) {
							largeWall[index] = newl;
							index++;
						}
					}
					// move right along top wall
					templ = HQLocation.translate(-cornerDist, cornerDist);
					for(int i = 0; i < largeWallRingSize - 1; i++) {
						MapLocation newl = templ.translate(i, 0);
						if(inMap(newl) && !inArray(digSpots, newl, digSpotsLength)) {
							largeWall[index] = newl;
							index++;
						}
					}
					largeWallLength = index;
					Debug.ttlog("LARGE WALL LENGTH: " + largeWallLength);
				}

				if (!firstTurn) {
					turn();
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			Globals.endTurn();
		}
	}

	public static void turn() throws GameActionException {
		if(!rc.isReady()) return;
		// If in a Dig Spot Place, move to the Outward Radius
		if(inArray(digSpots, here, digSpotsLength)) {
			Direction move = HQLocation.directionTo(here);
            Debug.ttlog("GOING TO OUTER RING");
			landscaperMove(move);
		}

		// From Richard - added a check for if we are in the inner 3x3 ring
		if(inArray(digSpots, here, digSpotsLength) || HQLocation.distanceSquaredTo(here) <= 3) {
			Debug.ttlog("Trying to move out of 3x3 ring");
			for (Direction dir: directions) {
				MapLocation loc = rc.adjacentLocation(dir);
				// if the target location is in the 5x5 ring and is not occupied/flooded
				if (HQLocation.distanceSquaredTo(loc) > 3 && rc.senseRobotAtLocation(loc) == null) {
					landscaperMove(dir);
				}
			}
		}

		// If the First Wall isn't Complete and you can do stuff in your current position, do it
		if(!wallComplete()) {
			// Update smallWallCompleted Array
			for(Direction d: Direction.allDirections()) {
				MapLocation tempLoc = here.add(d);
				if(inArray(smallWall, tempLoc, smallWallLength) && !inArray(smallWallCompleted, tempLoc, smallWallCompletedLength)) {
					if (rc.canSenseLocation(tempLoc)) {
						RobotInfo ri = rc.senseRobotAtLocation(tempLoc);
						if (rc.senseElevation(tempLoc) == smallWallDepth || (ri != null && ri.type.isBuilding())) {
							smallWallCompleted[smallWallCompletedLength] = tempLoc;
							smallWallCompletedLength++;
						}
					}
				}
			}
			// Find an action to do
			boolean actionComplete = false;
			for(Direction d: allDirections) {
				MapLocation tempLoc = here.add(d);
				if(inArray(smallWall, tempLoc, smallWallLength) && !inArray(smallWallCompleted, tempLoc, smallWallCompletedLength)) {
					if(rc.senseElevation(tempLoc) < smallWallDepth) {
						actionComplete = true;
						if(rc.getDirtCarrying() == 0) {
							landscaperDig();
						}
						else {
							if(rc.canDepositDirt(d)) rc.depositDirt(d);
						}
					}
					else if (rc.senseElevation(tempLoc) > smallWallDepth) {
						actionComplete = true;
						if(rc.canDigDirt(d)) rc.digDirt(d);
					}
				}
			}
			// The Wall here is Done, Rotate Clockwise
			if(!actionComplete) {
				if(moveClockwise) {
					Direction d = getClockwiseDir();
					MapLocation moveLoc = here.add(d);
					if (inArray(digSpots, moveLoc, digSpotsLength)) {
						// Go to Outer Ring
						Debug.ttlog("GOING TO OUTER RING");
						Direction move = HQLocation.directionTo(here);
						landscaperMove(move);
					} else {
						Debug.ttlog("ROTATING CLOCKWISE");
						if (rc.canMove(d)) {
							Actions.doMove(d);
						}
						else if(rc.senseRobotAtLocation(moveLoc) != null) {
							moveClockwise = false;
							Debug.ttlog("COLLIDING, SWAPPING DIRECTION");
						}
					}
				}
				else {
					Direction d = getCounterClockwiseDir();
					MapLocation moveLoc = here.add(d);
					if (inArray(digSpots, moveLoc, digSpotsLength)) {
						// Go to Outer Ring
						Debug.ttlog("GOING TO OUTER RING");
						Direction move = HQLocation.directionTo(here);
						landscaperMove(move);
					} else {
						Debug.ttlog("ROTATING COUNTERCLOCKWISE");
						if (rc.canMove(d)) {
							Actions.doMove(d);
						}
						else if(rc.senseRobotAtLocation(moveLoc) != null) {
							moveClockwise = true;
							Debug.ttlog("COLLIDING, SWAPPING DIRECTION");
						}
					}
				}
			}
		}
		// Inner Wall Complete, Just Start Building Outer Wall
		else {
            // If this is the first time here, you are still in the 5x5
            if(currentRing == -1) {
                Debug.ttlog("5x5 RING COMPLETE");
                currentRing = 0;
            }

            // If you are in the 5x5, get to the 6x6. Acceptable distances from the HQ are 9 and 13 (10, 18 are holes)
            if(currentRing == 0) {
                for(Direction d: Direction.allDirections()) {
                    MapLocation newloc = here.add(d);
                    int dis = HQLocation.distanceSquaredTo(newloc);
                    if(dis == 9 || dis == 13) {
                        if(rc.canMove(d)) {
                            currentRing = 1;
                            Debug.ttlog("MOVING TO 6x6 RING");
                            Actions.doMove(d);
                        }
                    }
                }
            }
            // If you are in the 6x6, get to the 7x7. All distances are acceptable
            if(currentRing == 1) {
                for(Direction d: Direction.allDirections()) {
                    MapLocation newloc = here.add(d);
                    int dis = HQLocation.distanceSquaredTo(newloc);
                    if(dis >= 16 && dis != 18) {
                        if(rc.canMove(d)) {
                            currentRing = 2;
                            Debug.ttlog("MOVING TO 7x7 RING");
                            currentStep = 0;
                            Actions.doMove(d);
                        }
                    }
                }
            }
            if(currentRing == 2) {
                if(currentStep == 0) {
                    Debug.ttlog("DIGGING");
                    if(rc.getDirtCarrying() <= 2) {
                        landscaperDig();
                    }
                    else {
                        currentStep = 1;
                    }
                }
                if(currentStep == 1) {
                    Debug.ttlog("DEPOSITING");
                    if(rc.getDirtCarrying() != 0) {
                        int minDirt = 1000;
                        Direction minDir = null;
                        for(Direction d: Direction.allDirections()) {
                            MapLocation newloc = here.add(d);
                            if(inArray(largeWall, newloc, largeWallLength)) {
                                if(minDirt > rc.senseElevation(newloc)) {
                                    minDir = d;
                                    minDirt = rc.senseElevation(newloc);
                                }
                            }
                        }
                        if(rc.canDepositDirt(minDir)) rc.depositDirt(minDir);
                    }
                    else {
                        currentStep = 2;
                    }
                }
                if(currentStep == 2) {
                    Debug.ttlog("MOVING");
                    if(moveClockwise) {
						Direction d = getClockwiseDir();
						Debug.ttlog("MOVING IN " + d);
						if (rc.canMove(d)) {
							currentStep = 0;
							Actions.doMove(d);
						}
						else if (rc.senseElevation(here.add(d)) + 3 < rc.senseElevation(here)) {
							currentStep = 0;
						}
						else if(rc.senseRobotAtLocation(here.add(d)) != null) {
							Debug.ttlog("Colliding with: " + rc.senseRobotAtLocation(here.add(d)));
							moveClockwise = false;
						}
					}
                    else {
						Direction d = getCounterClockwiseDir();
						Debug.ttlog("MOVING IN " + d);
						if (rc.canMove(d)) {
							currentStep = 0;
							Actions.doMove(d);
						}
						else if (rc.senseElevation(here.add(d)) + 3 < rc.senseElevation(here)) {
							currentStep = 0;
						}
						else if(rc.senseRobotAtLocation(here.add(d)) != null) {
							Debug.ttlog("Colliding with: " + rc.senseRobotAtLocation(here.add(d)));
							moveClockwise = true;
						}
					}
                }
            }
		}
	}

	private static boolean inArray(Object[] arr, Object item, int length) {
		for(int i = 0; i < length; i++) if(arr[i].equals(item)) return true;
		return false;
	}

	private static Direction getClockwiseDir() {
		int delx = HQLocation.x - here.x;
		int dely = HQLocation.y - here.y;
		int absx = Math.abs(delx);
		int absy = Math.abs(dely);
		boolean xPos = absx == delx;
		boolean yPos = absy == dely;

		if(absx == absy) {
			if(xPos && yPos) return Direction.NORTH;
			if(xPos) return Direction.EAST;
			if(yPos) return Direction.WEST;
			return Direction.SOUTH;
		}
		else if(absx > absy) {
			if(xPos) return Direction.NORTH;
			return Direction.SOUTH;
		}
		else {
			if(yPos) return Direction.WEST;
			return Direction.EAST;
		}
	}

	private static Direction getCounterClockwiseDir() {
		int delx = HQLocation.x - here.x;
		int dely = HQLocation.y - here.y;
		int absx = Math.abs(delx);
		int absy = Math.abs(dely);
		boolean xPos = absx == delx;
		boolean yPos = absy == dely;

		if(absx == absy) {
			if(xPos && yPos) return Direction.EAST;
			if(xPos) return Direction.SOUTH;
			if(yPos) return Direction.NORTH;
			return Direction.WEST;
		}
		else if(absx > absy) {
			if(xPos) return Direction.SOUTH;
			return Direction.NORTH;
		}
		else {
			if(yPos) return Direction.EAST;
			return Direction.WEST;
		}
	}

	private static boolean wallComplete() {
		return smallWallLength == smallWallCompletedLength;
	}

	private static void landscaperMove(Direction d) throws GameActionException {
		if(rc.canMove(d)) Actions.doMove(d);
		else if(rc.senseElevation(here.add(d)) > 3 + rc.senseElevation(here))
			if(rc.canDigDirt(d)) rc.digDirt(d);
		else
			if(rc.getDirtCarrying() >= 0)
				if(rc.canDepositDirt(d)) rc.depositDirt(d);
	}

	private static void landscaperDig() throws GameActionException {
		for(Direction d: Direction.allDirections()) {
			if(inArray(digSpots, here.add(d), digSpotsLength)) if(rc.canDigDirt(d)) rc.digDirt(d);
		}
	}
}
