package kryptonite;

import battlecode.common.*;

public class BotLandscaper extends Globals {


	private static MapLocation[] allDigLocations, smallWallCompleted;
	private static int allDigLocationsLength, smallWallCompletedLength, currentStep, role;
	private static int currentLargeWallIndex = -1;

	private static final int WALL_ROLE = 1, DEFENSE_ROLE = 2, SUPPORT_ROLE = 3;

	private static boolean moveClockwise = true;

	// @todo: Landscapers should attack enemy buildings if in sight and not on large wall
	// @todo: Landscapers should "heal" ally buildings if they are damaged
	public static void loop() throws GameActionException {
		while (true) {
			try {
				Globals.update();
				if (firstTurn) {

					loadWallInformation();

					role = WALL_ROLE;
					for (RobotInfo ri : visibleEnemies) {
						if (ri.type.isBuilding()) {
							role = DEFENSE_ROLE;
							break;
						}
					}
					// finds spots that can be used for digging
					allDigLocations = new MapLocation[50];
					MapLocation templ = HQLocation.translate(5,5);
					int index = 0;
					for(int i = 0; i < 6; i++) for(int j = 0; j < 6; j++) {
						MapLocation newl = templ.translate(-2*i, -2*j);
						if(inMap(newl) && !HQLocation.equals(newl)) {
							if (maxXYDistance(HQLocation, newl) > 2) { // excludes holes inside the 5x5 plot
								allDigLocations[index] = newl;
								index++;
							}
						}
					}
					allDigLocationsLength = index;

					Globals.endTurn(true);
					Globals.update();

					smallWallCompleted = new MapLocation[49];
					smallWallCompletedLength = 0;
				}

				turn();
			} catch (Exception e) {
				e.printStackTrace();
			}
			Globals.endTurn(false);
		}
	}

	public static void turn() throws GameActionException {

		if(rc.getRoundNum() >= 500) smallWallFinished = true;

		Debug.tlog("smallWallFinished " + smallWallFinished);
		Debug.tlog("largeWallFull " + largeWallFull);

		if(!rc.isReady()) return;


		// calculates the index of largeWall that we are on
		if (maxXYDistance(HQLocation, here) == 4) {
			for (int i = 0; i < largeWallLength; i++) {
				if (here.equals(largeWall[i])) {
					currentLargeWallIndex = i;
					break;
				}
			}
		} else {
			currentLargeWallIndex = -1;
			for(RobotInfo ri : visibleEnemies) {
				if(ri.type.isBuilding()) {
					role = DEFENSE_ROLE;
				}
			}
		}

		if(role == WALL_ROLE) {
			// If in a Dig Spot Place, move to the Outward Radius
			if (inArray(allDigLocations, here, allDigLocationsLength)) {
				Direction move = HQLocation.directionTo(here);
				Debug.ttlog("GOING TO OUTER RING");
				landscaperMove(move);
			}

			// From Richard - added a check for if we are in the inner 3x3 ring
			if (inArray(allDigLocations, here, allDigLocationsLength) || maxXYDistance(HQLocation, here) <= 1) {
				Debug.ttlog("Trying to move out of 3x3 ring");
				for (Direction dir : directions) {
					MapLocation loc = rc.adjacentLocation(dir);
					// if the target location is in the 5x5 ring and is not occupied/flooded
					if (maxXYDistance(HQLocation, loc) >= 2 && rc.senseRobotAtLocation(loc) == null) {
						landscaperMove(dir);
					}
				}
			}

			// If the First Wall isn't Complete and you can do stuff in your current position, do it
			if (!smallWallFinished) {
				// Update smallWallCompleted Array
				for (Direction d : Direction.allDirections()) {
					MapLocation tempLoc = here.add(d);
					if (inArray(smallWall, tempLoc, smallWallLength) && !inArray(smallWallCompleted, tempLoc, smallWallCompletedLength)) {
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
				for (Direction d : allDirections) {
					MapLocation tempLoc = here.add(d);
					if (inArray(smallWall, tempLoc, smallWallLength) && !inArray(smallWallCompleted, tempLoc, smallWallCompletedLength)) {
						if (rc.senseElevation(tempLoc) < smallWallDepth) {
							actionComplete = true;
							if (rc.getDirtCarrying() == 0) {
								landscaperDig(1);
							} else {
								if (rc.canDepositDirt(d)) Actions.doDepositDirt(d);
							}
						} else if (rc.senseElevation(tempLoc) > smallWallDepth) {
							actionComplete = true;
							if (rc.canDigDirt(d)) Actions.doDigDirt(d);
						}
					}
				}
				// The Wall here is Done, Rotate Clockwise
				if (!actionComplete) {
					if (moveClockwise) {
						Direction d = getClockwiseDir(here);
						MapLocation moveLoc = here.add(d);
						if (inArray(allDigLocations, moveLoc, allDigLocationsLength)) {
							// Go to Outer Ring
							Debug.ttlog("GOING TO OUTER RING");
							Direction move = HQLocation.directionTo(here);
							landscaperMove(move);
						} else {
							Debug.ttlog("ROTATING CLOCKWISE");
							if (rc.canMove(d)) {
								Actions.doMove(d);
							} else if (rc.senseRobotAtLocation(moveLoc) != null) {
								moveClockwise = false;
								Debug.ttlog("COLLIDING, SWAPPING DIRECTION");
							}
						}
					} else {
						Direction d = getCounterClockwiseDir(here);
						MapLocation moveLoc = here.add(d);
						if (inArray(allDigLocations, moveLoc, allDigLocationsLength)) {
							// Go to Outer Ring
							Debug.ttlog("GOING TO OUTER RING");
							Direction move = HQLocation.directionTo(here);
							landscaperMove(move);
						} else {
							Debug.ttlog("ROTATING COUNTERCLOCKWISE");
							if (rc.canMove(d)) {
								Actions.doMove(d);
							} else if (rc.senseRobotAtLocation(moveLoc) != null) {
								moveClockwise = true;
								Debug.ttlog("COLLIDING, SWAPPING DIRECTION");
							}
						}
					}
				}
			}
			// Inner Wall Complete, Just Start Building Outer Wall
			else {
				// If you are in the 5x5, get to the 7x7. Acceptable distances from the HQ are 9 and 13 (10, 18 are holes)
				if (maxXYDistance(HQLocation, here) == 2) {
					if(largeWallFull) {
						role = SUPPORT_ROLE;
					}
					if(role == WALL_ROLE || role == SUPPORT_ROLE) {
						for (Direction d : Direction.allDirections()) {
							MapLocation newloc = here.add(d);
							if (maxXYDistance(HQLocation, newloc) == 3) {
								if (rc.canMove(d)) {
									Debug.ttlog("MOVING TO 7x7 RING");
									Actions.doMove(d);
									return;
								}
							}
						}
					}
				}
				// If you are in the 6x6, get to the 7x7. All distances are acceptable
				if (maxXYDistance(HQLocation, here) == 3) {
					if(largeWallFull) {
						role = SUPPORT_ROLE;
					}
					if(role == WALL_ROLE) {
						for (Direction d : Direction.allDirections()) {
							MapLocation newloc = here.add(d);
							if (maxXYDistance(HQLocation, newloc) == 4) {
								if (rc.canMove(d)) {
									Debug.ttlog("MOVING TO 7x7 RING");
									currentStep = 0;
									Actions.doMove(d);
									return;
								}
							}
						}
					}
					if(role == SUPPORT_ROLE) {
						if(currentStep == 0) {
							if(rc.getDirtCarrying() < 25) {
								if(rc.canDigDirt(Direction.CENTER)) rc.digDirt(Direction.CENTER);
							}
							else currentStep = 1;
						}
						if(currentStep == 1) {
							if(rc.getDirtCarrying() == 0) {
								currentStep = 0;
							}
							else {
								int minDirt = 100000;
								Direction minDir = null;
								for (Direction d : Direction.allDirections()) {
									MapLocation newloc = here.add(d);
									if (maxXYDistance(HQLocation, newloc) == 4) {
										if (rc.senseElevation(newloc) < minDirt) {
											minDirt = rc.senseElevation(newloc);
											minDir = d;
										}
									}
								}
								if (rc.canDepositDirt(minDir)) rc.depositDirt(minDir);
							}
						}
					}
				}
				// STATE == on the large wall
				if (maxXYDistance(HQLocation, here) == 4) {
					Debug.ttlog("ON THE WALL");
					boolean isFlooded = false;
					Direction inFlood = null;
					for(Direction d : Direction.allDirections()) {
						if(rc.senseFlooding(here.add(d)) && maxXYDistance(HQLocation, here.add(d)) <= 4) {
							inFlood = d;
							isFlooded = true;
							return;
						}
					}

					while (true) { // fake while loop, only ever runs once, used while so i can break
						Debug.ttlog("MOVING");

						int clock_index = (currentLargeWallIndex + 1 + largeWallLength) % largeWallLength;
						int counterclock_index = (currentLargeWallIndex - 1 + largeWallLength) % largeWallLength;

						Direction d_clock = getClockwiseLargeWallDirection(currentLargeWallIndex);
						Direction d_clock2 = getClockwiseLargeWallDirection(clock_index);
						Direction d_counterclock = getCounterClockwiseLargeWallDirection(currentLargeWallIndex);
						Direction d_counterclock2 = getCounterClockwiseLargeWallDirection(counterclock_index);

						MapLocation loc_clock = here.add(d_clock);
						MapLocation loc_clock2 = loc_clock.add(d_clock2);
						MapLocation loc_counterclock = here.add(d_counterclock);
						MapLocation loc_counterclock2 = loc_counterclock.add(d_counterclock2);
						Debug.ttlog("loc_clock");
						Debug.ttlog("here: loc_clock");
						Debug.ttlog("dcw: " + d_clock);
						Debug.ttlog("dccw: " + d_counterclock);
						Debug.ttlog("cw1 "+loc_clock);
						Debug.ttlog("cw2 "+loc_clock2);
						Debug.ttlog("ccw1 "+loc_counterclock);
						Debug.ttlog("ccw2 "+loc_counterclock2);

						if (rc.canSenseLocation(loc_clock)
								&& rc.canSenseLocation(loc_clock2)
								&& rc.canSenseLocation(loc_counterclock)
								&& rc.canSenseLocation(loc_counterclock2)) {
							RobotInfo ri_clock = rc.senseRobotAtLocation(loc_clock);
							RobotInfo ri_clock2 = rc.senseRobotAtLocation(loc_clock2);
							RobotInfo ri_counterclock = rc.senseRobotAtLocation(loc_counterclock);
							RobotInfo ri_counterclock2 = rc.senseRobotAtLocation(loc_counterclock2);

							// is there a landscaper within the clockwise direction x 2
							boolean ls_in_clock = ri_clock != null && ri_clock.type == RobotType.LANDSCAPER;
							boolean ls_in_clock2 = ri_clock2 != null && ri_clock2.type == RobotType.LANDSCAPER;
							boolean ls_within_two_clock = ls_in_clock || ls_in_clock2;
							boolean ls_in_counterclock = ri_counterclock != null && ri_counterclock.type == RobotType.LANDSCAPER;
							boolean ls_in_counterclock2 = ri_counterclock2 != null && ri_counterclock2.type == RobotType.LANDSCAPER;
							boolean ls_within_two_counterclock = ls_in_counterclock || ls_in_counterclock2;

							Debug.tlog("ls " + ls_in_clock + " " + ls_in_clock2 + " v " + ls_in_counterclock + " " + ls_in_counterclock2);

							if (ls_in_counterclock && !ls_within_two_clock) {
								// surroundings look like __XL*
								Direction d = getClockwiseDir(here);
								Debug.tlog("MOVING CLOCKWISE IN " + d);
								if (rc.isReady()) {
									// if too steep, dig to fix heights
									if (!Nav.checkElevation(rc.adjacentLocation(d))) {
										Debug.ttlog("TOO STEEP: MINING MORE");
										currentStep = 0;
										break;
									}
									if (ri_clock != null) {
										Debug.ttlog("But is occupied");
										return;
									} else if (rc.senseFlooding(loc_clock)) {
										Debug.ttlog("But is flooded");
										break; // break to try to plug hole
									} else {
										Actions.doMove(d);
										Debug.ttlog("Success");
										return;
									}
								} else {
									Debug.ttlog("But not ready");
								}
								return;
							}

							if (ls_in_clock && !ls_within_two_counterclock) {
								// surroundings look like *LX__
								// move counterclockwise
								Direction d = getCounterClockwiseDir(here);
								Debug.tlog("MOVING COUNTERCLOCKWISE IN " + d);
								if (rc.isReady()) {
									// if too steep, dig to fix heights
									if (!Nav.checkElevation(rc.adjacentLocation(d))) {
										Debug.ttlog("TOO STEEP: MINING MORE");
										currentStep = 0;
										break;
									}
									if (ri_counterclock == null) {
										Actions.doMove(d);
										Debug.ttlog("Success");
										return;
									} else {
										Debug.ttlog("Failed");
										return;
									}
								}
								return;
							}

							// move to corner
							Debug.tlog("trying to move to corner ");
							int distToHQ = HQLocation.distanceSquaredTo(here);
							for (Direction dir: directions) {
								if (dir == d_clock && ls_within_two_clock) {
									continue;
								}
								if (dir == d_counterclock && ls_within_two_counterclock) {
									continue;
								}
								MapLocation loc = rc.adjacentLocation(dir);
								if (!inMap(loc)) {
									continue;
								}
								// on ring and gets farther from hq
								if (maxXYDistance(HQLocation, loc) == 4 && HQLocation.distanceSquaredTo(loc) > distToHQ) {
									if (!rc.senseFlooding(loc) && Nav.checkElevation(loc) && rc.senseRobotAtLocation(loc) == null) {
										Debug.tlog("moving to corner ");
										Actions.doMove(dir);
										return;
									}
								}
							}

							Debug.tlog("Unexpected case " + ls_in_clock + " " + ls_in_clock2 + " v " + ls_in_counterclock + " " + ls_in_counterclock);
							// do nothing
							// do not return
						}

						break;
					}


					// STATE == not trying to move
					// or needs to use dirt to move

					if (rc.getDirtCarrying() < 1) {
						Debug.ttlog("DIGGING");
						landscaperDig(2);
						return;
					}

					// Has enough dirt to deposit

					Debug.ttlog("DEPOSITING");
					if(isFlooded) {
						Debug.ttlog("DEPOSITING IN WATER IN DIRECTION: " + inFlood);
						if(rc.canDepositDirt(inFlood)) {
							Actions.doDepositDirt(inFlood);
							Debug.tlog("Success");
						} else {
							Debug.tlog("Failed");
						}
						return;
					}
					else {
						int minDirt = P_INF;
						Direction minDir = null;
						for (Direction d : Direction.allDirections()) {
							MapLocation newloc = here.add(d);
							RobotInfo ri = rc.senseRobotAtLocation(newloc);
							if(ri != null && ri.type.isBuilding() && ri.team == rc.getTeam()) continue;
							// checks if inside tiles are below elevation
							// if largeWallFull of landscapers, ignore this check
							if(!largeWallFull) {
								if (maxXYDistance(HQLocation, newloc) == 3 && rc.senseElevation(newloc) < smallWallDepth) {
									minDir = d;
									minDirt = -1;
									break;
								}
							}
							if (maxXYDistance(HQLocation, newloc) == 4) {
								if (minDirt > rc.senseElevation(newloc)) {
									minDir = d;
									minDirt = rc.senseElevation(newloc);
								}
							}
						}
						Debug.ttlog("DEPOSITING DIRT IN DIRECTION: " + minDir);
						if (rc.canDepositDirt(minDir)) {
							Actions.doDepositDirt(minDir);
						}
					}
					// end of landscaper
				}
			}
		}
		else if(role == DEFENSE_ROLE) {
			if(currentStep == 0) {
				if(maxXYDistance(here, HQLocation) != 2) {
					for(Direction d: Direction.allDirections()) {
						if(maxXYDistance(here.add(d), HQLocation) == 2) {
							if(rc.canMove(d)) {
								rc.move(d);
								return;
							}
						}
					}
				}
				if(rc.getRoundNum() < 1000) if(rc.getDirtCarrying() < 5) currentStep = 1;
				else if(rc.getDirtCarrying() < RobotType.LANDSCAPER.dirtLimit) currentStep = 1;
			}
			else if(currentStep == 1) {
				if(rc.getDirtCarrying() == 0) {
					currentStep = 0;
				}
				else {
					for (RobotInfo ri : visibleEnemies) {
						if (ri.type.isBuilding()) {
							if (ri.location.isAdjacentTo(here)) {
								Direction drop = here.directionTo(ri.location);
								if (rc.canDepositDirt(drop)) {
									Actions.doDepositDirt(drop);
									return;
								}
							} else {
								Nav.bugNavigate(ri.location);
							}
						}
					}
				}
			}
		}
	}

	private static Direction getClockwiseLargeWallDirection (int curIndex) {
		int newIndex = (curIndex + 1 + largeWallLength) % largeWallLength;
		return largeWall[curIndex].directionTo(largeWall[newIndex]);
	}

	private static Direction getCounterClockwiseLargeWallDirection (int curIndex) {
		int newIndex = (curIndex - 1 + largeWallLength) % largeWallLength;
		return largeWall[curIndex].directionTo(largeWall[newIndex]);
	}

	private static Direction getClockwiseDir(MapLocation ml) {
		int delx = HQLocation.x - ml.x;
		int dely = HQLocation.y - ml.y;
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

	private static Direction getCounterClockwiseDir(MapLocation ml) {
		int delx = HQLocation.x - ml.x;
		int dely = HQLocation.y - ml.y;
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

	private static void landscaperMove(Direction d) throws GameActionException {
		if(rc.canMove(d)) Actions.doMove(d);
		else if(rc.senseElevation(here.add(d)) > 3 + rc.senseElevation(here))
			if(rc.canDigDirt(d)) Actions.doDigDirt(d);
			else
			if(rc.getDirtCarrying() >= 0)
				if(rc.canDepositDirt(d)) Actions.doDepositDirt(d);
	}

	private static void landscaperDig(int where) throws GameActionException {
		if(where == 1) {
			for (Direction d : Direction.allDirections()) {
				if (inArray(allDigLocations, here.add(d), allDigLocationsLength)) if (rc.canDigDirt(d)) Actions.doDigDirt(d);
			}
		}
		else if(where == 2) {
			for (Direction d : Direction.allDirections()) {
				if (maxXYDistance(HQLocation, here.add(d)) == 5 && inArray(allDigLocations, here.add(d), allDigLocationsLength)) if (rc.canDigDirt(d)) Actions.doDigDirt(d);
			}
		}
	}
}
