package rush_bot;

import battlecode.common.*;

import static rush_bot.Actions.*;
import static rush_bot.Communication.*;
import static rush_bot.Debug.*;
import static rush_bot.Globals.*;
import static rush_bot.Map.*;
import static rush_bot.Nav.*;
import static rush_bot.Utils.*;
import static rush_bot.Wall.*;
import static rush_bot.Zones.*;

public class BotLandscaper extends Globals {

	private static int role, currentStep = 0;
	private static final int TERRA_ROLE = 1, DEFENSE_ROLE = 2, ATTACK_ROLE = 3, TERRA_TARGET_ROLE = 4, TERRA_FILLER_ROLE = 5, RUSH_ROLE = 6, WALL_ROLE = 7;

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

//		rerollRole();

		role = WALL_ROLE;
		if (rc.canSenseLocation(getSymmetryLoc())) {
			RobotInfo ri = rc.senseRobotAtLocation(getSymmetryLoc());
			if (ri != null && ri.type == RobotType.HQ && ri.team == them) {
				role = RUSH_ROLE;
			}
		}

		initializedLandscaper = true;

		Globals.endTurn();
		Globals.update();
	}

	public static void turn() throws GameActionException {
		if(!rc.isReady()) {
			log("Not ready");
			return;
		}

		// rushers
		if (role == RUSH_ROLE) {
			doRushRole();
			return;
		}

//		for(int i = 0; i < directions.length; i++) {
//			if(isDigLoc(rc.adjacentLocation(directions[i]))) {
//				isDirMoveable[i] = false;
//			}
//		}

//		for (RobotInfo ri : visibleEnemies) {
//			if (ri.type.isBuilding()) {
//				role = DEFENSE_ROLE;
//				buildingLocation = ri.location;
//			}
//		}

//		if(isDigLoc(here) || maxXYDistance(here, HQLoc) <= 2) {
//			bugNavigate(getSymmetryLoc());
//			return;
//		}

		ttlog("MY ROLE IS: " + role);


		switch(role) {
			case WALL_ROLE:
				doWallRole();
				break;
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

	private static void doWallRole() throws GameActionException {
		// in the 3x3
		if (inArray(smallWallLocs, here, smallWallLocsLength)) {
			int maxDist = N_INF;
			Direction maxDir = null;
			for (Direction dir: directions) {
				MapLocation loc = rc.adjacentLocation(dir);
				if (isLocEmpty(loc) && inArray(smallWallLocs, loc, smallWallLocsLength)) {
					int dist = loc.distanceSquaredTo(getSymmetryLoc());
					if (dist > maxDist) {
						maxDist = dist;
						maxDir = dir;
					}
				}
			}
			int curDist = here.distanceSquaredTo(getSymmetryLoc());
			if (maxDist > curDist) {
				log("Moving to farther 3x3 loc");
				Actions.doMove(maxDir);
				return;
			}

			if (rc.getDirtCarrying() > 0) {
				if (startBuildInnerWall) {

				} else {
					Actions.doDepositDirt(Direction.CENTER);
					return;
				}
			} else {
				wallDig();
				return;
			}
		}

		moveLog(HQLoc);
	}

	private static void wallDig () throws GameActionException {
		Direction bestDir = null;
		int bestScore = N_INF;
		MapLocation[] digLocs2x2 = new MapLocation[4];
		for (int i = 0; i < cardinalDirections.length; i++) {
			digLocs2x2[i] = HQLoc.add(cardinalDirections[i]).add(cardinalDirections[i]);
		}
		for (Direction dir: directions) {
			MapLocation loc = rc.adjacentLocation(dir);
			if (!rc.onTheMap(loc)) {
				continue;
			}
			if (maxXYDistance(HQLoc, loc) <= 2 && !inArray(digLocs2x2, loc, digLocs2x2.length)) {
				continue;
			}
			int score = 0;
			if (isLocEmpty(loc)) {
				// prioritize highest tiles
				score = rc.senseElevation(loc);
			} else {
				RobotInfo ri = rc.senseRobotAtLocation(loc);
				if (ri.team == us) {
					if (ri.type.isBuilding()) {
						if (ri.dirtCarrying > 0) {
							score = P_INF;
						} else {
							continue;
						}
					} else {
						// prioritize allies farther from HQLoc
						score = loc.distanceSquaredTo(HQLoc);
					}
				} else {
					if (ri.type.isBuilding()) {
						continue;
					} else {
						score = P_INF / 2;
					}
				}
			}
			if (score > bestScore) {
				bestDir = dir;
				bestScore = score;
			}
		}
		Actions.doDigDirt(bestDir);
		return;
	}

	private static void doRushRole() throws GameActionException {
		if (here.isAdjacentTo(enemyHQLoc)) {
			if (rc.getDirtCarrying() > 0) {
				Actions.doDepositDirt(here.directionTo(enemyHQLoc));
			} else {
				rushDig();
			}
		} else {
			moveLog(enemyHQLoc);
		}
	}
	private static void rushDig () throws GameActionException {
		Direction bestDir = null;
		int bestScore = N_INF;
		for (Direction dir: allDirections) {
			MapLocation loc = rc.adjacentLocation(dir);
			if (!rc.onTheMap(loc)) {
				continue;
			}
			int score = 0;
			if (isLocEmpty(loc)) {
				// prioritize empty tiles farther from enemy HQ
				score = loc.distanceSquaredTo(enemyHQLoc);
			} else {
				RobotInfo ri = rc.senseRobotAtLocation(loc);
				if (ri.team == us) {
					if (ri.type.isBuilding()) {
						if (ri.dirtCarrying > 0) {
							score = P_INF;
						} else {
							continue;
						}
					} else {
						// prioritize allies closer to enemy HQ
						score = -loc.distanceSquaredTo(enemyHQLoc);
					}
				} else {
					if (ri.type.isBuilding()) {
						continue;
					} else {
						score = P_INF / 2;
					}
				}
			}
			if (score > bestScore) {
				bestDir = dir;
				bestScore = score;
			}
		}
		Actions.doDigDirt(bestDir);
		return;
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
