package turtle;

import battlecode.common.*;

import static turtle.Debug.*;
import static turtle.Map.*;
import static turtle.Nav.*;
import static turtle.Wall.*;

public class BotLandscaper extends Globals {

	private static int role, currentStep = 0;
	private static final int TERRA_ROLE = 1, DEFENSE_ROLE = 2, ATTACK_ROLE = 3, TERRA_TARGET_ROLE = 4, TERRA_FILLER_ROLE = 5;

	// TERRA Variables
	private static MapLocation terraFillerLocation;
	private static MapLocation terraRotateLocation;
	private static boolean foundWallTerraLocation;
	private static boolean shouldUpdateFillerLocation = true;
	private static final int MAX_ELE_DIFF = 25;

	// Defense Variables
	private static MapLocation buildingLocation;

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

		if(isDigLoc(here) || maxXYDistance(here, HQLoc) <= 2) {
			bugNavigate(getSymmetryLoc());
			return;
		}

		ttlog("MY ROLE IS: " + role);


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
						if(rc.canDepositDirt(dropLoc)) Actions.doDepositDirt(dropLoc);
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
					for(Direction d : Direction.allDirections()) {
						MapLocation loc = rc.adjacentLocation(d);
						if (!rc.onTheMap(loc)) {
							continue;
						}
						if(rc.senseElevation(loc) > terraDepth && Math.abs(terraDepth-rc.senseElevation(loc)) < MAX_ELE_DIFF) {
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
						if(rc.senseElevation(loc) < terraDepth && !isDigLoc(loc) && !isLocBuilding(loc) && maxXYDistance(rc.adjacentLocation(d), HQLoc) > 2 && Math.abs(terraDepth-rc.senseElevation(loc)) < MAX_ELE_DIFF) {
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
		if(shouldUpdateFillerLocation || terraFillerLocation == null || !rc.canSenseLocation(terraFillerLocation) || rc.senseElevation(terraFillerLocation) == terraDepth) {
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
					for (Direction d : Direction.allDirections()) {
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
					for (Direction d : Direction.allDirections()) {
						MapLocation loc = rc.adjacentLocation(d);
						if (!rc.onTheMap(loc)) {
							continue;
						}
						if (rc.senseElevation(loc) < terraDepth && !isDigLoc(loc) && !isLocBuilding(loc) && maxXYDistance(rc.adjacentLocation(d), HQLoc) > 2 && Math.abs(terraDepth - rc.senseElevation(loc)) < MAX_ELE_DIFF) {
							if (rc.canDepositDirt(d)) Actions.doDepositDirt(d);
							return;
						}
					}
				}
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
