package kryptonite;

import battlecode.common.*;

import static kryptonite.Debug.*;
import static kryptonite.Map.*;
import static kryptonite.Nav.*;
import static kryptonite.Wall.*;
import static kryptonite.Constants.*;
import static kryptonite.Globals.*;

public class BotLandscaper extends Globals {

	private static int role, currentStep = 0;
	private static final int TERRA_ROLE = 1, DEFENSE_ROLE = 2, ATTACK_ROLE = 3, TERRA_TARGET_ROLE = 4, TERRA_FILLER_ROLE = 5, WALL_ROLE = 6, SUPPORT_ROLE = 7;

	// TERRA Variables
	private static MapLocation terraFillerLocation;
	private static MapLocation terraRotateLocation;
	private static boolean foundWallTerraLocation;
	private static boolean shouldUpdateFillerLocation = true;
	private static final int MAX_ELE_DIFF = 25;

	// Defense Variables
	private static MapLocation buildingLocation;

    // WALL_ROLE Variables
    private static MapLocation wallBuildLocation;

    // SUPPORT_WALL_ROLE Variables
    private static MapLocation supportWallBuildLocation;

	public static void loop() throws GameActionException {
		while (true) {
			try {
				Globals.update();

				if (!initializedLandscaper) {
					initLandscaper();
				}

				turn();
			} catch (Exception e) {
				e.printStackTrace();
			}
			Globals.endTurn();
		}
	}

	public static boolean initializedLandscaper = false;

	public static void initLandscaper() throws GameActionException {

		rerollRole();

		initializedLandscaper = true;

		Globals.endTurn();
		Globals.update();
	}

	public static void turn() throws GameActionException {
		if(!rc.isReady()) {
			return;
		}
		for(int i = 0; i < directions.length; i++) {
			if(isDigLoc(rc.adjacentLocation(directions[i]))) {
				isDirMoveable[i] = false;
			}
		}

		for (RobotInfo ri : visibleEnemies) {
			if (ri.type.isBuilding()) {
				role = DEFENSE_ROLE;
				buildingLocation = ri.location;
			}
		}

		if(isDigLoc(here) || maxXYDistance(here, HQLoc) < wallRingRadius) {
			bugNavigate(getSymmetryLoc());
			return;
		}

		ttlog("MY ROLE IS: " + role);

		int avoidDangerResult = Nav.avoidDanger();
		if (avoidDangerResult == 1) {
			return;
		}

		switch(role) {
			case DEFENSE_ROLE:
				doDefenseRole();
				break;
			case TERRA_ROLE:
				if(rc.getID() % 2 == 0) role = TERRA_TARGET_ROLE; else role = TERRA_FILLER_ROLE;
				break;
			case TERRA_TARGET_ROLE:
				doTerraTargetRole();
				break;
			case TERRA_FILLER_ROLE:
				doTerraFillRole();
				break;
            case WALL_ROLE:
                doWallRole();
                break;
            case SUPPORT_ROLE:
                doSupportRole();
                break;
		}
	}

	private static void doDefenseRole() throws GameActionException {
		if(here.isAdjacentTo(buildingLocation)) {
			if(rc.senseRobotAtLocation(buildingLocation) == null) {
				rerollRole();
			}
			else {
				if(currentStep == 0) {
					if(rc.getDirtCarrying() == 0) {
						landscaperDig();
						return;
					}
					else {
						currentStep = 1;
					}
				}
				if(currentStep == 1) {
					if(rc.getDirtCarrying() > 0) {
						Direction dropLoc = here.directionTo(buildingLocation);
						if(rc.canDepositDirt(dropLoc)) {
							Actions.doDepositDirt(dropLoc);
						}
						return;
					}
					else {
						currentStep = 0;
					}
				}
			}
		}
		else if(rc.canSenseLocation(buildingLocation)) {
			if(rc.senseRobotAtLocation(buildingLocation) == null) {
				rerollRole();
			}
			else {
				bugNavigate(buildingLocation);
				return;
			}
		}
		else {
			bugNavigate(buildingLocation);
			return;
		}
	}
	private static void doTerraTargetRole() throws GameActionException {
		updateTerraDepth();
		if(terraTargetCheck()) {
			bugNavigate(getSymmetryLoc());
			return;
		}
		else {
			if(currentStep == 0) {
				if(rc.getDirtCarrying() == 0) {
					boolean flag = false;
					for(Direction d : allDirections) {
						MapLocation loc = rc.adjacentLocation(d);
						if (!rc.onTheMap(loc)) {
							continue;
						}
						if(rc.senseElevation(loc) > terraDepth && Math.abs(terraDepth - rc.senseElevation(loc)) < MAX_ELE_DIFF) {
							flag = true;
							if(rc.canDigDirt(d)) {
								Actions.doDigDirt(d);
								return;
							}
						}
					}
					if(!flag) landscaperDig();
				}
				else {
					currentStep = 1;
				}
			}
			if(currentStep == 1) {
				if(rc.getDirtCarrying() == 0) {
					currentStep = 0;
				}
				else {
					for(Direction d : Direction.allDirections()) {
						MapLocation loc = rc.adjacentLocation(d);
						if (!rc.onTheMap(loc)) {
							continue;
						}
						if(rc.senseElevation(loc) < terraDepth && !isDigLoc(loc) && !isLocBuilding(loc) && maxXYDistance(rc.adjacentLocation(d), HQLoc) >= wallRingRadius && Math.abs(terraDepth-rc.senseElevation(loc)) < MAX_ELE_DIFF) {
							if(rc.canDepositDirt(d)) Actions.doDepositDirt(d);
							return;
						}
					}
				}
			}
		}
	}

	private static void doTerraFillRole() throws GameActionException {
		updateTerraDepth();
		if(!wallCompleted) {
			MapLocation wallLoc = needsTerra(wallRingRadius);
			if(wallLoc != null) {
				if(wallLoc.isAdjacentTo(here)) {
					if(currentStep == 0) {
						if(rc.getDirtCarrying() == 0) {
							ttlog("DIGGING DIRT");
							landscaperDig();
						}
						else {
							currentStep = 1;
						}
					}
					if(currentStep == 1) {
						if(rc.getDirtCarrying() > 0) {
							ttlog("DEPOSITING DIRT IN DIRECTION: " + here.directionTo(wallLoc));
							if(rc.canDepositDirt(here.directionTo(wallLoc))) Actions.doDepositDirt(here.directionTo(wallLoc));
						}
						else {
							currentStep = 0;
						}
					}
				}
				else {
					Nav.bugNavigate(wallLoc);
					return;
				}
			}
			else {
				int rad = maxXYDistance(HQLoc, here);
				if (rad > 4) {
					int minRing = 100;
					Direction minD = null;
					for(Direction d : directions) {
						if(rc.canMove(d) && maxXYDistance(here.add(d), HQLoc) < minRing) {
							minD = d;
							minRing = maxXYDistance(here.add(d), HQLoc);
						}
					}
					if(minD == null) return;
					Actions.doMove(minD);
					return;
				} else if (rad < 4) {
					int maxRing = 0;
					Direction maxD = null;
					for(Direction d : directions) {
						if(rc.canMove(d) && maxXYDistance(here.add(d), HQLoc) > maxRing) {
							maxD = d;
							maxRing = maxXYDistance(here.add(d), HQLoc);
						}
					}
					if(maxD == null) return;
					Actions.doMove(maxD);
					return;
				} else {
					for(Direction d : directions) {
						if(rc.canMove(d) && maxXYDistance(here.add(d), HQLoc) == 4) {
							Actions.doMove(d);
							return;
						}
					}
				}
			}
		}
		else {
			if (shouldUpdateFillerLocation || terraFillerLocation == null || !rc.canSenseLocation(terraFillerLocation) || rc.senseElevation(terraFillerLocation) == terraDepth) {
				shouldUpdateFillerLocation = findNewFillerLocation();
			}
			ttlog("My Terra Fill Location is: " + terraFillerLocation);
			if (!here.isAdjacentTo(terraFillerLocation)) {
				bugNavigate(terraFillerLocation);
				return;
			} else {
				if (currentStep == 0) {
					if (rc.getDirtCarrying() == 0) {
						boolean flag = false;
						for (Direction d : allDirections) {
							MapLocation loc = rc.adjacentLocation(d);
							if (!rc.onTheMap(loc)) {
								continue;
							}
							if (rc.senseElevation(loc) > terraDepth && Math.abs(terraDepth - rc.senseElevation(loc)) < MAX_ELE_DIFF) {
								flag = true;
								if (rc.canDigDirt(d)) {
									Actions.doDigDirt(d);
									return;
								}
							}
						}
						if (!flag) landscaperDig();
						return;
					} else {
						currentStep = 1;
					}
				}
				if (currentStep == 1) {
					if (rc.getDirtCarrying() == 0) {
						currentStep = 0;
					} else {
						for (Direction d : allDirections) {
							MapLocation loc = rc.adjacentLocation(d);
							if (!rc.onTheMap(loc)) {
								continue;
							}
							if (rc.senseElevation(loc) < terraDepth && !isDigLoc(loc) && !isLocBuilding(loc) && maxXYDistance(rc.adjacentLocation(d), HQLoc) >= wallRingRadius && Math.abs(terraDepth - rc.senseElevation(loc)) < MAX_ELE_DIFF) {
								if (rc.canDepositDirt(d)) Actions.doDepositDirt(d);
								return;
							}
						}
					}
				}
			}
		}
	}

	private static void doWallRole() throws GameActionException {
        if(wallBuildLocation == null) rerollRole();
        ttlog("My Building Location is: " + wallBuildLocation);
        for (Direction dir: directions) {
            MapLocation loc = rc.adjacentLocation(dir);
            if (!rc.onTheMap(loc)) {
                continue;
            }
            if (isLocEnemyBuilding(loc)) {
                if(rc.getDirtCarrying() == 0) {
                    ttlog("DIGGING DIRT");
                    landscaperDig();
                }
                else {
                    ttlog("Attacking enemy buildings");
                    Actions.doDepositDirt(dir);
                }
                return;
            }
        }

        if(here.equals(wallBuildLocation)) {
            if(wallFull) {
                if(currentStep == 0) {
                    if(rc.getDirtCarrying() == 0) {
                        ttlog("DIGGING DIRT");
                        landscaperDig();
                    }
                    else {
                        currentStep = 1;
                    }
                }
                if(currentStep == 1) {
                    if(rc.getDirtCarrying() > 0) {
                        int minDirt = P_INF;
                        Direction minDir = null;
                        for(Direction d : allDirections) {
                            MapLocation loc = rc.adjacentLocation(d);
                            if (!rc.onTheMap(loc)) {
                                continue;
                            }
                            if(maxXYDistance(HQLoc, loc) == 1) {
                                if(minDirt > rc.senseElevation(loc)) {
                                    minDirt = rc.senseElevation(loc);
                                    minDir = d;
                                }
                            }
                        }
                        ttlog("DEPOSITING DIRT IN DIRECTION: " + minDir);
                        if(rc.canDepositDirt(minDir)) Actions.doDepositDirt(minDir);
                    }
                    else {
                        currentStep = 0;
                    }
                }
            }
            else {
                if(currentStep == 0) {
                    if(rc.getDirtCarrying() == 0) {
                        ttlog("DIGGING DIRT");
                        landscaperDig();
                    }
                    else {
                        currentStep = 1;
                    }
                }
                if(currentStep == 1) {
                    if(rc.getDirtCarrying() > 0) {
                        ttlog("DEPOSITING DIRT IN DIRECTION: " + Direction.CENTER);
                        if(rc.canDepositDirt(Direction.CENTER)) Actions.doDepositDirt(Direction.CENTER);
                    }
                    else {
                        currentStep = 0;
                    }
                }
            }
        }
        else {
            if(rc.canSenseLocation(wallBuildLocation)) {
                if(rc.senseRobotAtLocation(wallBuildLocation) != null) {
                    rerollRole();
                }
                else {
                    Nav.bugNavigate(wallBuildLocation);
                }
            }
            else {
                Nav.bugNavigate(wallBuildLocation);
            }
        }
    }

    private static void doSupportRole() throws GameActionException {
		if (supportWallBuildLocation == null) rerollRole();
		if (here.equals(supportWallBuildLocation)) {
			if (currentStep == 0) {
				if (rc.getDirtCarrying() == 0) {
					ttlog("DIGGING DIRT");
					landscaperDig();
				} else {
					currentStep = 1;
				}
			}
			if (currentStep == 1) {
				if (rc.getDirtCarrying() > 0) {
					if (rc.senseElevation(here) - GameConstants.getWaterLevel(rc.getRoundNum()) < 1) {
						ttlog("DEPOSITING DIRT IN DIRECTION: " + Direction.CENTER);
						if (rc.canDepositDirt(Direction.CENTER)) Actions.doDepositDirt(Direction.CENTER);
					}
					int minDirt = 10000;
					Direction minDir = null;
					for (Direction d : directions) {
						MapLocation loc = rc.adjacentLocation(d);
						if (!rc.onTheMap(loc)) {
							continue;
						}
						if (maxXYDistance(HQLoc, loc) == 1 && rc.senseElevation(loc) < minDirt) {
							minDirt = rc.senseElevation(loc);
							minDir = d;
						}
					}
					ttlog("DEPOSITING DIRT IN DIRECTION: " + minDir);
					if (rc.canDepositDirt(minDir)) Actions.doDepositDirt(minDir);
				} else {
					currentStep = 0;
				}
			}
		} else {
			if (rc.canSenseLocation(supportWallBuildLocation)) {
				if (rc.senseRobotAtLocation(supportWallBuildLocation) != null) {
					rerollRole();
				} else {
					Nav.bugNavigate(supportWallBuildLocation);
				}
			} else {
				Nav.bugNavigate(supportWallBuildLocation);
			}
		}
    }

	private static void landscaperDig() throws GameActionException {
		for (Direction d : allDirections) {
			MapLocation loc = rc.adjacentLocation(d);
			if (!rc.onTheMap(loc)) {
				continue;
			}
			if (isDigLoc(loc)) {
				if (rc.canDigDirt(d)) {
					Actions.doDigDirt(d);
				}
			}
		}
	}

	private static boolean terraTargetCheck() throws GameActionException {
		for(Direction d : allDirections) {
			MapLocation loc = rc.adjacentLocation(d);
			if (!rc.onTheMap(loc)) {
				continue;
			}
			if(maxXYDistance(HQLoc, loc) < wallRingRadius) {
				continue;
			}
			if(rc.senseElevation(loc) < terraDepth && !isDigLoc(loc) && !isLocBuilding(loc) && Math.abs(terraDepth-rc.senseElevation(loc)) < MAX_ELE_DIFF) {
				ttlog("TERRA CHECK: FAILED CHECK IN DIRECTION " + d);
				return false;
			}
		}
		return true;
	}

	private static void updateTerraDepth() {
		terraDepth = 8;
	}

	private static void rerollRole() {
		role = TERRA_ROLE;
	}

	private static boolean findNewFillerLocation() throws GameActionException {
		MapLocation ml = null;
		int maxCirc = 10;
		for(int i = -5; i <= 5; i++) for(int j = -5; j <= 5; j++) {
			MapLocation loc = here.translate(i,j);
			if(!rc.canSenseLocation(loc)) continue;
			if (!rc.onTheMap(loc))  continue;
			if(maxXYDistance(HQLoc, loc) <= wallRingRadius) continue;
			if(rc.senseElevation(loc) < terraDepth && !isDigLoc(loc) && !isLocBuilding(loc) && Math.abs(terraDepth-rc.senseElevation(loc)) < MAX_ELE_DIFF) {
				if(maxCirc > maxXYDistance(here, loc)) {
					ml = loc;
					maxCirc = maxXYDistance(here, loc);
				}
			}
		}
		if(ml == null) {
			terraFillerLocation = getSymmetryLoc();
			return false;
		}
		else {
			terraFillerLocation = ml;
			return true;
		}
	}

	public static MapLocation needsTerra(int ring) throws GameActionException {
		int minRad = 100;
		MapLocation ml = null;
		for(int i = 0; i < Globals.senseDirections.length; i++) {
			MapLocation templ = here.translate(Globals.senseDirections[i][0], Globals.senseDirections[i][1]);
			if(!rc.canSenseLocation(templ)) continue;
			if(maxXYDistance(templ, HQLoc) == ring && minRad > Globals.senseDirections[i][2] && rc.senseElevation(templ) != terraDepth) {
				ml = templ;
				minRad = Globals.senseDirections[i][2];
			}
		}
		return ml;
	}
}
