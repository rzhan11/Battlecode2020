package kryptonite;

import battlecode.common.*;
import wall.Actions;

import static kryptonite.Communication.*;
import static kryptonite.Constants.*;
import static kryptonite.Debug.*;
import static kryptonite.Map.*;
import static wall.Map.maxXYDistance;

public class BotLandscaper extends Globals {

	private static int role, currentStep = 0;
	private static final int WALL_ROLE = 1, DEFENSE_ROLE = 2, TERRA_ROLE = 3, ATTACK_ROLE = 4;


	// WALL_ROLE Variables
	private static boolean wallFull = false;
	private static MapLocation wallBuildLocation;

	// DEFENSE_ROLE Variables
	private static MapLocation buildingLocation;


	public static void loop() throws GameActionException {
		while (true) {
			try {
				Globals.update();
				if (firstTurn) {
					loadWallInformation();
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
						role = largeWallFull ? WALL_ROLE : TERRA_ROLE;
						if(role == WALL_ROLE) {
                            for(Direction d : Globals.allDirections) {
                                if(rc.senseRobotAtLocation(here.add(d)) != null) {
                                    buildingLocation = here.add(d);
                                    break;
                                }
                            }
                        }
					}
					Debug.ttlog("INITIAL ASSIGNED ROLE: " + role);



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
		switch(role) {
			case WALL_ROLE:
				// @todo: Implement Support Wall Roles
				if(here.equals(buildingLocation)) {
					if(wallFull) {
						if(currentStep == 0) {
							if(rc.getDirtCarrying() < 10) {
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
								if(rc.canDepositDirt(minDir)) Actions.doDepositDirt(minDir);
							}
							else {
								currentStep = 0;
							}
						}
					}
					else {
						if(rc.canDepositDirt(Direction.CENTER)) Actions.doDepositDirt(Direction.CENTER);
					}
				}
				else {
                    if(rc.canSenseLocation(buildingLocation)) {
                        if(rc.senseRobotAtLocation(buildingLocation) != null) {
                            rerollRole();
                        }
                        else {
                            Nav.bugNavigate(buildingLocation);
                        }
                    }
                    else {
                        Nav.bugNavigate(buildingLocation);
                    }
				}
				break;
			case DEFENSE_ROLE:
				// @todo: Move to an Active Role and not a Passive Defense
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
				if(rc.canDigDirt(d)) Actions.doDigDirt(d);
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
            role = largeWallFull ? WALL_ROLE : TERRA_ROLE;
        }
        if(role == WALL_ROLE) {
            for(Direction d : Globals.allDirections) {
                if(rc.senseRobotAtLocation(here.add(d)) != null) {
                    buildingLocation = here.add(d);
                    break;
                }
            }
        }
    }
}
