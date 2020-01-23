package kryptonite;

import battlecode.common.*;

import static kryptonite.Actions.*;
import static kryptonite.Debug.*;
import static kryptonite.Map.*;
import static kryptonite.Nav.*;
import static kryptonite.Globals.*;

public class BotLandscaper extends Globals {

	private static int role, currentStep = 0;
	private static final int TERRA_ROLE = 1, DEFENSE_ROLE = 2, ATTACK_ROLE = 3, TERRA_TARGET_ROLE = 4, TERRA_FILLER_ROLE = 5;

	// TERRA Variables
	private static int terraDepth;
	private static MapLocation terraTargetLocation;
	private static MapLocation terraFillerLocation;
	private static boolean shouldUpdateFillerLocation = true;
	private static final int MAX_ELE_DIFF = 25;

	// Defense Variables
	private static MapLocation buildingLocation;

	public static void loop() throws GameActionException {
		while (true) {
			try {
				Globals.update();
				if (firstTurn) {
					rerollRole();
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

		if(isDigLoc(here) || maxXYDistance(here, HQLoc) <= 2) {
			bugNavigate(getSymmetryLoc());
		}

		ttlog("MY ROLE IS: " + role);
		switch(role) {
			case DEFENSE_ROLE:
				if(here.isAdjacentTo(buildingLocation)) {
					if(rc.senseRobotAtLocation(buildingLocation) == null) {
						rerollRole();
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
								Direction dropLoc = here.directionTo(buildingLocation);
								ttlog("DEPOSITING DIRT IN DIRECTION: " + dropLoc);
								if(rc.canDepositDirt(dropLoc)) Actions.doDepositDirt(dropLoc);
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
					}
				}
				else {
					bugNavigate(buildingLocation);
				}
				break;

			case TERRA_ROLE:
				if(rc.getID() % 2 == 0) role = TERRA_TARGET_ROLE; else role = TERRA_FILLER_ROLE;
				break;
			case TERRA_TARGET_ROLE:
				updateTerraDepth();
				if(terraTargetCheck()) {
					bugNavigate(terraTargetLocation);
				}
				else {
					if(currentStep == 0) {
						if(rc.getDirtCarrying() == 0) {
							boolean flag = false;
							for(Direction d : Direction.allDirections()) {
								MapLocation loc = rc.adjacentLocation(d);
								if (!rc.onTheMap(loc)) {
									continue;
								}
								if(rc.senseElevation(loc) > terraDepth && Math.abs(terraDepth-rc.senseElevation(loc)) < MAX_ELE_DIFF) {
									flag = true;
									if(rc.canDigDirt(d)) doDigDirt(d);
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
								if(rc.senseElevation(loc) < terraDepth && !isDigLoc(loc) && !isLocBuilding(loc) && maxXYDistance(rc.adjacentLocation(d), HQLoc) > 2 && Math.abs(terraDepth-rc.senseElevation(loc)) < MAX_ELE_DIFF) {
									if(rc.canDepositDirt(d)) Actions.doDepositDirt(d);
								}
							}
						}
					}
				}
				break;
			case TERRA_FILLER_ROLE:
				updateTerraDepth();
				Debug.ttlog("My Terra Fill Location is: " + terraFillerLocation);
				if(shouldUpdateFillerLocation || terraFillerLocation == null || !rc.canSenseLocation(terraFillerLocation) || rc.senseElevation(terraFillerLocation) == terraDepth) {
					shouldUpdateFillerLocation = findNewFillerLocation();
				}
				if(!here.isAdjacentTo(terraFillerLocation)) {
					bugNavigate(terraFillerLocation);
				}
				else {
					if(currentStep == 0) {
						if(rc.getDirtCarrying() == 0) {
							boolean flag = false;
							for(Direction d : Direction.allDirections()) {
								MapLocation loc = rc.adjacentLocation(d);
								if (!rc.onTheMap(loc)) {
									continue;
								}
								if(rc.senseElevation(loc) > terraDepth && Math.abs(terraDepth-rc.senseElevation(loc)) < MAX_ELE_DIFF) {
									flag = true;
									if(rc.canDigDirt(d)) doDigDirt(d);
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
								if(rc.senseElevation(loc) < terraDepth && !isDigLoc(loc) && !isLocBuilding(loc) && maxXYDistance(rc.adjacentLocation(d), HQLoc) > 2 && Math.abs(terraDepth-rc.senseElevation(loc)) < MAX_ELE_DIFF) {
									if(rc.canDepositDirt(d)) Actions.doDepositDirt(d);
								}
							}
						}
					}
				}
				break;
		}
	}

	private static void landscaperDig() throws GameActionException {
		for (Direction d : allDirections) {
			MapLocation loc = rc.adjacentLocation(d);
			if (!rc.onTheMap(loc)) {
				continue;
			}
			if (isDigLoc(loc)) {
				ttlog("DIGGING IN DIRECTION: " + d);
				if (rc.canDigDirt(d)) doDigDirt(d);
			}
		}
	}

	private static boolean terraTargetCheck() throws GameActionException {
		for(Direction d : allDirections) {
			MapLocation loc = rc.adjacentLocation(d);
			if (!rc.onTheMap(loc))  continue;
			if(maxXYDistance(HQLoc, loc) <= 2) continue;
			if(rc.senseElevation(loc) < terraDepth && !isDigLoc(loc) && !isLocBuilding(loc) && Math.abs(terraDepth-rc.senseElevation(loc)) < MAX_ELE_DIFF) {
				ttlog("TERRA CHECK: FAILED CHECK IN DIRECTION " + d);
				return false;
			}
		}
		return true;
	}

	private static boolean terraFillerCheck() throws GameActionException {
		for(int i = -5; i <= 5; i++) for(int j = -5; j <= 5; j++) {
			MapLocation loc = here.translate(i,j);
			if(!rc.canSenseLocation(loc)) continue;
			if (!rc.onTheMap(loc))  continue;
			if(maxXYDistance(HQLoc, loc) <= 2) continue;
			if(rc.senseElevation(loc) < terraDepth && !isDigLoc(loc) && !isLocBuilding(loc) && Math.abs(terraDepth-rc.senseElevation(loc)) < MAX_ELE_DIFF) {
				ttlog("TERRA CHECK: FAILED CHECK IN MAP LOCATION: " + loc);
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
		terraTargetLocation = getSymmetryLoc();
	}

	private static boolean findNewFillerLocation() throws GameActionException {
		MapLocation ml = null;
		int maxCirc = 10;
		for(int i = -5; i <= 5; i++) for(int j = -5; j <= 5; j++) {
			MapLocation loc = here.translate(i,j);
			if(!rc.canSenseLocation(loc)) continue;
			if (!rc.onTheMap(loc))  continue;
			if(maxXYDistance(HQLoc, loc) <= 2) continue;
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
}
