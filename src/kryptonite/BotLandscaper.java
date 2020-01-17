package kryptonite;

import battlecode.common.*;

import static kryptonite.Communication.*;
import static kryptonite.Constants.*;
import static kryptonite.Debug.*;
import static kryptonite.Map.*;
import static kryptonite.Actions.*;

public class BotLandscaper extends Globals {

	private static int role, currentStep = 0;
	private static final int WALL_ROLE = 1, DEFENSE_ROLE = 2, TERRA_ROLE = 3, ATTACK_ROLE = 4;


	// WALL_ROLE Variables
	private static boolean wallFull = false;
	private static MapLocation wallBuildLocation;

	// DEFENSE_ROLE Variables
	private static MapLocation buildingLocation;

	// @todo: Fix Indicator Lines (Richard)
	public static void loop() throws GameActionException {
		while (true) {
			try {
				Globals.update();
				if (firstTurn) {
					role = ATTACK_ROLE;
					if(rc.canSenseLocation(HQLocation)) {
						role = largeWallFull ? TERRA_ROLE : WALL_ROLE;
						if(role == WALL_ROLE) {
                            for(Direction d : Globals.allDirections) {
                                if(rc.senseRobotAtLocation(HQLocation.add(d)) != null) {
                                    wallBuildLocation = HQLocation.add(d);
                                    break;
                                }
                            }
                        }
					}
					Debug.ttlog("INITIAL ASSIGNED ROLE: " + role);
					if(role == DEFENSE_ROLE) {
						Debug.ttlog("DEFENDING FROM BUILDING IN LOCATION: " + buildingLocation);
					}
					else if(role == WALL_ROLE) {
						Debug.ttlog("BUILDING WANN IN LOCATION: " + wallBuildLocation);
					}
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

	public static void turn() throws GameActionException {
		if(!rc.isReady()) return;
	    if(role != DEFENSE_ROLE || (role == WALL_ROLE && here.equals(wallBuildLocation))) {
            for (RobotInfo ri : visibleEnemies) {
                if (ri.type.isBuilding()) {
                    role = DEFENSE_ROLE;
                    buildingLocation = ri.location;
                }
            }
        }
		Debug.ttlog("MY ROLE IS: " + role);
		switch(role) {
			case WALL_ROLE:
				// @todo: Implement Support Wall Roles
				if(here.equals(wallBuildLocation)) {
					if(wallFull) {
						if(currentStep == 0) {
							if(rc.getDirtCarrying() < 10) {
								Debug.ttlog("DIGGING DIRT");
								landscaperDig();
							}
							else {
								currentStep = 1;
							}
						}
						if(currentStep == 1) {
							if(rc.getDirtCarrying() > 0) {
								int minDirt = 100000;
								Direction minDir = null;
								for(Direction d : Globals.allDirections) {
									if(maxXYDistance(HQLocation, here.add(d)) == 1) {
										if(minDirt > rc.senseElevation(here.add(d))) {
											minDirt = rc.senseElevation(here.add(d));
											minDir = d;
										}
									}
								}
								Debug.ttlog("DEPOSITING DIRT IN DIRECTION: " + minDir);
								if(rc.canDepositDirt(minDir)) rc.depositDirt(minDir);
							}
							else {
								currentStep = 0;
							}
						}
					}
					else {
						if(currentStep == 0) {
							if(rc.getDirtCarrying() < 10) {
								Debug.ttlog("DIGGING DIRT");
								landscaperDig();
							}
							else {
								currentStep = 1;
							}
						}
						if(currentStep == 1) {
							if(rc.getDirtCarrying() > 0) {
								Debug.ttlog("DEPOSITING DIRT IN DIRECTION: " + Direction.CENTER);
								if(rc.canDepositDirt(Direction.CENTER)) rc.depositDirt(Direction.CENTER);
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
				break;
			case DEFENSE_ROLE:
				if(here.isAdjacentTo(buildingLocation)) {
					if(rc.senseRobotAtLocation(buildingLocation) == null) {
						rerollRole();
					}
					else {
						if(currentStep == 0) {
							if(rc.getDirtCarrying() < 10) {
								Debug.ttlog("DIGGING DIRT");
								landscaperDig();
							}
							else {
								currentStep = 1;
							}
						}
						if(currentStep == 1) {
							if(rc.getDirtCarrying() > 0) {
								Direction dropLoc = here.directionTo(buildingLocation);
								Debug.ttlog("DEPOSITING DIRT IN DIRECTION: " + dropLoc);
								if(rc.canDepositDirt(dropLoc)) rc.depositDirt(dropLoc);
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
						Nav.bugNavigate(buildingLocation);
					}
				}
				else {
					Nav.bugNavigate(buildingLocation);
				}
				break;

			case TERRA_ROLE:
				// @todo: Explore the Terraforming of Enemy HQ Locations
				break;

			case ATTACK_ROLE:

				break;
		}
	}

	private static void landscaperDig() throws GameActionException {
		for(Direction d : Globals.allDirections) {
			if(isDigLocation(here.add(d))) {
				Debug.ttlog("DIGGING IN DIRECTION: " + d);
				if(rc.canDigDirt(d)) rc.digDirt(d);
			}
		}
	}

	private static void rerollRole() throws GameActionException{
        role = ATTACK_ROLE;
        // Selecting Role
        for (RobotInfo ri : visibleEnemies) {
            if (ri.type.isBuilding()) {
                role = DEFENSE_ROLE;
                buildingLocation = ri.location;
                break;
            }
        }
        if(rc.canSenseLocation(HQLocation)) {
            role = largeWallFull ? TERRA_ROLE : WALL_ROLE;
        }
        if(role == WALL_ROLE) {
            for(Direction d : Globals.allDirections) {
                if(rc.senseRobotAtLocation(HQLocation.add(d)) != null) {
                    wallBuildLocation = HQLocation.add(d);
                    break;
                }
            }
        }
    }
}
