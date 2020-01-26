package rush_bot;

import battlecode.common.*;

import static rush_bot.Debug.*;
import static rush_bot.Map.*;
import static rush_bot.Nav.*;
import static rush_bot.Utils.*;
import static rush_bot.Wall.*;

public class BotLandscaper extends Globals {

	private static int myRole, currentStep = 0;
	final private static int RUSH_ROLE = 1, WALL_ROLE = 2, DEFENSE_ROLE = 3;

	private static final int MAX_ELE_DIFF = 25;

	private static MapLocation enemyBuildingLoc;

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

		myRole = WALL_ROLE;
		if (rc.canSenseLocation(getSymmetryLoc())) {
			RobotInfo ri = rc.senseRobotAtLocation(getSymmetryLoc());
			if (ri != null && ri.type == RobotType.HQ && ri.team == them) {
				myRole = RUSH_ROLE;
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
		if (myRole == RUSH_ROLE) {
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

		ttlog("MY ROLE IS: " + myRole);
		log("wallFull " + wallFull);
		log("supportFull " + supportFull);

		switch(myRole) {
			case WALL_ROLE:
				doWallRole();
				break;
			case DEFENSE_ROLE:
				doDefenseRole();
				break;
		}
	}

	private static void doWallRole() throws GameActionException {
		// in the 3x3
		if (inArray(wallLocs, here, wallLocsLength)) {
			int maxDist = N_INF;
			Direction maxDir = null;
			for (Direction dir: directions) {
				MapLocation loc = rc.adjacentLocation(dir);
				if (!rc.onTheMap(loc)) {
					continue;
				}
				if (isLocEmpty(loc) && inArray(wallLocs, loc, wallLocsLength)) {
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
				// keep other tiles unflooded
				if (unfloodWall()) {
					return;
				}
				if (wallFull) {
					// build wall
					wallDeposit();
					return;
				} else {
					// build only my tile
					Actions.doDepositDirt(Direction.CENTER);
					return;
				}
			} else {
				wallDig();
				return;
			}
		}

		if (wallFull && inArray(supportWallLocs, here, supportWallLocsLength)) {
			int maxDist = N_INF;
			Direction maxDir = null;
			for (Direction dir: directions) {
				MapLocation loc = rc.adjacentLocation(dir);
				if (!rc.onTheMap(loc)) {
					continue;
				}
				if (isLocEmpty(loc) && inArray(supportWallLocs, loc, supportWallLocsLength)) {
					int dist = loc.distanceSquaredTo(getSymmetryLoc());
					if (dist > maxDist) {
						maxDist = dist;
						maxDir = dir;
					}
				}
			}
			int curDist = here.distanceSquaredTo(getSymmetryLoc());
			if (maxDist > curDist) {
				log("Moving to farther support loc");
				Actions.doMove(maxDir);
				return;
			}

			if (rc.getDirtCarrying() > 0) {
				// save myself from flooding
				if(rc.senseElevation(here) - waterLevel < 3) {
					Actions.doDepositDirt(Direction.CENTER);
					return;
				}
				// keep other tiles above water
				if (unfloodWall()) {
					return;
				}
				// build the wall
				wallDeposit();
				return;
			} else {
				wallDig();
				return;
			}
		}

		moveLog(HQLoc);
	}

	private static boolean unfloodWall() throws GameActionException {
		if (waterLevel > 5) {
			return false;
		}
		// adds dirt to flooded support tiles
		for (Direction dir: directions) {
			MapLocation loc = rc.adjacentLocation(dir);
			if (!rc.onTheMap(loc)) {
				continue;
			}
			if (inArray(wallLocs, loc, wallLocsLength) || inArray(supportWallLocs, loc, supportWallLocsLength)) {
				if (rc.senseElevation(loc) - waterLevel < 2 ||
						(isLocWet(loc) && waterLevel - rc.senseElevation(loc) < MAX_ELE_DIFF)) {
					Actions.doDepositDirt(dir);
					return true;
				}
			}
		}
		return false;
	}

	private static void wallDeposit() throws GameActionException {
		// adds dirt to 3x3 wall
		int minElevation = P_INF;
		Direction minDir = null;
		for (Direction dir: directions) {
			MapLocation loc = rc.adjacentLocation(dir);
			if (!rc.onTheMap(loc)) {
				continue;
			}
			if (inArray(wallLocs, loc, wallLocsLength)) {
				int elevation = rc.senseElevation(loc);
				if (elevation < minElevation) {
					minDir = dir;
					minElevation = elevation;
				}
			}
		}
		if (minDir != null) {
			Actions.doDepositDirt(minDir);
			return;
		}
	}

	private static void wallDig () throws GameActionException {
		Direction bestDir = null;
		int bestScore = N_INF;
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
		if(here.isAdjacentTo(enemyBuildingLoc)) {
			if(rc.senseRobotAtLocation(enemyBuildingLoc) == null) {
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
						Direction dropLoc = here.directionTo(enemyBuildingLoc);
						if(rc.canDepositDirt(dropLoc)) Actions.doDepositDirt(dropLoc);
						return;
					}
					else {
						currentStep = 0;
					}
				}
			}
		}
		else if(rc.canSenseLocation(enemyBuildingLoc)) {
			if(rc.senseRobotAtLocation(enemyBuildingLoc) == null) {
				rerollRole();
			}
			else {
				bugNavigate(enemyBuildingLoc);
				return;
			}
		}
		else {
			bugNavigate(enemyBuildingLoc);
			return;
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

	private static void rerollRole() {
		myRole = WALL_ROLE;
	}
}
