package rush_bot;

import battlecode.common.*;

import static rush_bot.Communication.*;
import static rush_bot.Debug.*;
import static rush_bot.Map.*;
import static rush_bot.Nav.*;
import static rush_bot.Utils.*;
import static rush_bot.Wall.*;

public class BotLandscaper extends Globals {

	private static int myRole, currentStep = 0;
	final private static int RUSH_ROLE = 1, WALL_ROLE = 2, DEFENSE_ROLE = 3, PLATFORM_ROLE = 4;

	private static final int MAX_ELE_DIFF = 25;

	private static int platformIndex;
	private static boolean initPlatformLandscaper = false;
	private static boolean[] elevationChecker = {false, false, false, false};

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

	public static void initLandscaperPlatform() {
		myRole = PLATFORM_ROLE;

		platformIndex = 0;

		initPlatformLandscaper = true;
	}

	public static void turn() throws GameActionException {
		if(!rc.isReady()) {
			log("Not ready");
			return;
		}

		if (myRole == RUSH_ROLE) {
			BotLandscaperRush.turn();
			return;
		}

//		if (!inArray(wallLocs, here, wallLocsLength) && !inArray(supportWallLocs, here, supportWallLocsLength)) {
//			for (RobotInfo ri : visibleEnemies) {
//				if (ri.type.isBuilding()) {
//					myRole = DEFENSE_ROLE;
//					enemyBuildingLoc = ri.location;
//				}
//			}
//		}

		if (myID == platformLandscaperID && !initPlatformLandscaper) {
			initLandscaperPlatform();
		}

		ttlog("MY ROLE IS: " + myRole);
		log("initialWallSetup " + initialWallSetup);
		log("wallFull " + wallFull);
		log("supportFull " + supportFull);

		switch(myRole) {
			case WALL_ROLE:
				doWallRole();
				break;
			case DEFENSE_ROLE:
				doDefenseRole();
				break;
			case PLATFORM_ROLE:
				doPlatformRole();
				break;
//			case DEFENSE_ROLE:
//				doDefenseRole();
//				break;
		}
	}

	private static void doInitialSetupWall() throws GameActionException {
		boolean shouldMove = false;
		if (here.distanceSquaredTo(HQLoc) == 1) {
			// STATE = CARDINAL from HQ
			int count = 0;
			int id = -1;
			RobotInfo[] cardHQAllies = rc.senseNearbyRobots(HQLoc, 1, us);
			for (RobotInfo ri : cardHQAllies) {
				if (ri.type == RobotType.LANDSCAPER) {
					count++;
					id = ri.ID;
				}
			}
			if (count > 0) {
				if (myID < id) {
					shouldMove = true;
				}
			}
		} else {
			shouldMove = true;
		}

		log("shouldMove " + shouldMove);
		if (shouldMove) {
			// directly move onto cardinal direction
			for (Direction dir: cardinalDirections) {
				MapLocation loc = HQLoc.add(dir);
				Direction dirFromHere = here.directionTo(loc);
				if (!rc.onTheMap(loc)) {
					continue;
				}
				if (here.isAdjacentTo(loc) && isLocDryFlatEmpty(loc)) {
					log("Moving to satisfy initial wall setup");
					Actions.doMove(dirFromHere);
					return;
				}
			}
			// use dig/deposit to reach cardinal direction
			for (Direction dir: cardinalDirections) {
				MapLocation loc = HQLoc.add(dir);
				Direction dirFromHere = here.directionTo(loc);
				if (!rc.onTheMap(loc)) {
					continue;
				}
				if (here.isAdjacentTo(loc) && isLocEmpty(loc)) {
					if (isLocWet(loc)) {
						log("Water to cardinal");
						if (rc.getDirtCarrying() > 0) {
							Actions.doDepositDirt(dirFromHere);
							return;
						} else {
							wallDig();
							return;
						}
					} else if (!isLocFlat(loc)) {
						log("Not flat to cardinal");
						if (rc.getDirtCarrying() > 0) {
							if (myElevation < rc.senseElevation(loc)) {
								Actions.doDepositDirt(Direction.CENTER);
								return;
							} else {
								Actions.doDepositDirt(dirFromHere);
								return;
							}
						} else {
							wallDig();
							return;
						}
					}
				}
			}
		}
	}

	private static void doWallRole() throws GameActionException {

		if (here.isAdjacentTo(HQLoc)) {
			RobotInfo HQInfo = rc.senseRobotAtLocation(HQLoc);
			if (HQInfo.dirtCarrying > 0) {
				Direction dir = here.directionTo(HQInfo.location);
				if (rc.getDirtCarrying() < myType.dirtLimit && rc.canDigDirt(dir)) {
					Actions.doDigDirt(dir);
					return;
				}
			}
		}

		// unflood tiles adjacent to HQ
		for (Direction dir: directions) {
			MapLocation loc = HQLoc.add(dir);
			if (!rc.onTheMap(loc)) {
				continue;
			}
			if (here.isAdjacentTo(loc) && isLocWet(loc)) {
				if (rc.getDirtCarrying() > 0) {
					Actions.doDepositDirt(here.directionTo(loc));
					return;
				} else {
					wallDig();
					return;
				}
			}
		}

		// in the 3x3
		if (inArray(wallLocs, here, wallLocsLength)) {
			if (!initialWallSetup) {
				initialWallSetup = checkInitialWallSetup();
			}
			if (!initialWallSetup) {
				doInitialSetupWall();
				return;
			}

			if (rc.getDirtCarrying() > 0) {
				// save myself from flooding
				if(myElevation - waterLevel < 3) {
					Actions.doDepositDirt(Direction.CENTER);
					return;
				}
				// keep other tiles unflooded
				if (unfloodWall()) {
					return;
				}
				wallDeposit();
				return;
			} else {
				wallDig();
				return;
			}
		}

		if (wallFull && inArray(supportWallLocs, here, supportWallLocsLength)) {

			// move to tile farthest from enemyhq
			int maxDist = N_INF;
			Direction maxDir = null;
			for (Direction dir: directions) {
				MapLocation loc = rc.adjacentLocation(dir);
				if (!rc.onTheMap(loc)) {
					continue;
				}
				// tiles that are (2, 2) away from hq have least priority since they cannot be defended by hq's net gun
				if (loc.distanceSquaredTo(HQLoc) == 8) {
					continue;
				}
				if (isLocDryFlatEmpty(loc) && inArray(supportWallLocs, loc, supportWallLocsLength)) {
					int dist = loc.distanceSquaredTo(getSymmetryLoc());
					if (dist > maxDist) {
						maxDist = dist;
						maxDir = dir;
					}
				}
			}
			int curDist = here.distanceSquaredTo(getSymmetryLoc());
			if (curDist == 8 || maxDist > curDist) {
				log("Moving to better/farther support loc");
				Actions.doMove(maxDir);
				return;
			}

			if (rc.getDirtCarrying() > 0) {
				// save myself from flooding
				if(myElevation - waterLevel < 3) {
					boolean trySaveSelf = true;
					int diff = waterLevel - myElevation;
					// if large difference between my height and water level
					if (diff >= MAX_ELE_DIFF) {
						int roundsLeft = HardCode.getRoundFlooded(2) - roundNum;
						if (roundsLeft / 2 < diff) {
							trySaveSelf = false;
						}
					}
					if (trySaveSelf) {
						Actions.doDepositDirt(Direction.CENTER);
						return;
					} else {
						log("Cannot save self");
					}
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
		if (waterLevel > UNFLOOD_WALL_LIMIT) {
			return false;
		}
		// adds dirt to flooded support tiles
		for (Direction dir: directions) {
			MapLocation loc = rc.adjacentLocation(dir);
			if (!rc.onTheMap(loc)) {
				continue;
			}
			if (inArray(wallLocs, loc, wallLocsLength) || inArray(supportWallLocs, loc, supportWallLocsLength)) {
				if (waterLevel - rc.senseElevation(loc) < MAX_ELE_DIFF &&
						(rc.senseElevation(loc) - waterLevel < 2 || isLocWet(loc))) {
					Actions.doDepositDirt(dir);
					return true;
				}
			}
		}
		return false;
	}

	private static void wallDeposit() throws GameActionException {

		// raise elevation of tiles adjacent to HQ
		if (roundNum >= HardCode.getRoundFlooded(2) - 100) {
			for (Direction dir: directions) {
				MapLocation loc = HQLoc.add(dir);
				if (!rc.onTheMap(loc)) {
					continue;
				}
				if (here.isAdjacentTo(loc) && rc.senseElevation(loc) - waterLevel < 2) {
					if (rc.getDirtCarrying() > 0) {
						Actions.doDepositDirt(here.directionTo(loc));
						return;
					} else {
						wallDig();
						return;
					}
				}
			}
		}

		// build only my tile in early rounds
		if (roundNum < HardCode.getRoundFlooded(2) && !wallFull && here.isAdjacentTo(HQLoc)) {
			Actions.doDepositDirt(Direction.CENTER);
			return;
		}

		// adds dirt to 3x3 wall
		int minElevation = P_INF;
		Direction minDir = null;
		for (Direction dir: allDirections) {
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

	private static void wallDig() throws GameActionException {
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
				// prioritize lowest tiles
				score = -rc.senseElevation(loc);
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
						score = -P_INF / 3 + loc.distanceSquaredTo(HQLoc);
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
		for (Direction dir: directions) {
			MapLocation loc = rc.adjacentLocation(dir);
			if (!rc.onTheMap(loc)) {
				continue;
			}
			if (isLocEnemyBuilding(loc)) {
				if (rc.getDirtCarrying() > 0) {
					Actions.doDepositDirt(dir);
					return;
				} else {
					landscaperDig();
					return;
				}
			}
		}
		if(here.isAdjacentTo(enemyBuildingLoc)) {
			if(rc.senseRobotAtLocation(enemyBuildingLoc) == null) {
				rerollRole();
			} else {
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

	private static void doPlatformRole() throws GameActionException {
		if (inArray(platformLocs, here, platformLocs.length)) {
			// checking if finished
			for (MapLocation loc : platformLocs) {
				int elevation = rc.senseElevation(loc);
				if (elevation < PLATFORM_ELEVATION) {
					if (rc.getDirtCarrying() > 0) {
						Actions.doDepositDirt(here.directionTo(loc));
						return;
					} else {
						platformDig(Direction.CENTER);
						return;
					}
				}
			}
			rerollRole();
			writeTransactionPlatformCompleted();
			return;
		}

		// STATE == not on platform
		Direction dirToPlatform = here.directionTo(platformCornerLoc);
		MapLocation locToPlatform = rc.adjacentLocation(dirToPlatform);
		if (isLocEmpty(locToPlatform)) {
			if (isLocWet(locToPlatform)) {
				log("Water to platform");
				if (rc.getDirtCarrying() > 0) {
					Actions.doDepositDirt(dirToPlatform);
					return;
				} else {
					platformDig(dirToPlatform);
					return;
				}
			} else if (!isLocFlat(locToPlatform)) {
				log("Not flat to platform");
				if (rc.getDirtCarrying() > 0) {
					if (myElevation < rc.senseElevation(locToPlatform)) {
						Actions.doDepositDirt(Direction.CENTER);
						return;
					} else {
						Actions.doDepositDirt(dirToPlatform);
						return;
					}
				} else {
					platformDig(dirToPlatform);
					return;
				}
			} else {
				// loc is empty, wet, flat
				moveLog(locToPlatform);
				return;
			}
		} else {
			moveLog(locToPlatform);
			return;
		}

	}


	private static void platformDig(Direction noDigDir) throws GameActionException {
		Direction bestDir = null;
		int bestScore = N_INF;
		for (Direction dir: directions) {
			if (dir.equals(noDigDir)) {
				continue;
			}
			MapLocation loc = rc.adjacentLocation(dir);
			if (!rc.onTheMap(loc)) {
				continue;
			}
			if (maxXYDistance(HQLoc, loc) <= 2 && !inArray(digLocs2x2, loc, digLocs2x2.length)) {
				continue;
			}
			if (inArray(platformLocs, loc, platformLocs.length)) {
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

	private static void rerollRole() {
		myRole = WALL_ROLE;
	}
}
