package kryptonite;

import battlecode.common.*;

import static kryptonite.Constants.*;
import static kryptonite.Debug.*;
import static kryptonite.Map.*;
import static kryptonite.Zones.*;

public class BotLandscaper extends Globals {

	private static int role, currentStep = 0;
	private static final int WALL_ROLE = 1, DEFENSE_ROLE = 2, TERRA_ROLE = 3, ATTACK_ROLE = 4, SUPPORT_WALL_ROLE = 5,
							TERRA_ATTACK_ROLE = 6, TERRA_BOUNCE_ROLE = 7;
	private static Direction[] landscaperDirections = {Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST, Direction.NORTHEAST, Direction.SOUTHWEST, Direction.SOUTHEAST, Direction.NORTHWEST}; // 8 directions


	// WALL_ROLE Variables
	private static MapLocation wallBuildLocation;

	// DEFENSE_ROLE Variables
	private static MapLocation buildingLocation;

	// TERRA Variable
	private static int terraDepth = 4;

	// TERRA_ATTACK_ROLE Variables
	private static MapLocation terraTargetLocation;

	// TERRA_BOUNCE_ROLE Variables
	private static Direction currentDirection;

	// SUPPORT_WALL_ROLE Variables
	private static MapLocation supportWallBuildLocation;

	// @todo: Fix Indicator Lines (Richard)
	public static void loop() throws GameActionException {
		while (true) {
			try {
				Globals.update();
				if (firstTurn) {
					rerollRole();
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
		if(!rc.isReady()) {
			return;
		}


		landscaperDirections = getCloseDirections(HQLoc.directionTo(getSymmetryLoc()).opposite());

		if(maxXYDistance(HQLoc, here) == 1) {
			role = WALL_ROLE;
			wallBuildLocation = here;
		}
		if(maxXYDistance(HQLoc, here) == 2 && !isDigLoc(here) && wallFull) {
			role = SUPPORT_WALL_ROLE;
			supportWallBuildLocation = here;
		}

		if(role != DEFENSE_ROLE && !(role == WALL_ROLE && maxXYDistance(HQLoc, here) == 1)) {
			for (RobotInfo ri : visibleEnemies) {
				if (ri.type.isBuilding()) {
					role = DEFENSE_ROLE;
					buildingLocation = ri.location;
				}
			}
		}


		ttlog("MY ROLE IS: " + role);
		switch(role) {
			case WALL_ROLE:
				if(wallBuildLocation == null) rerollRole();
				ttlog("My Building Location is: " + wallBuildLocation);
				for (Direction dir: directions) {
					if (isLocEnemyBuilding(rc.adjacentLocation(dir))) {
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
									if(maxXYDistance(HQLoc, here.add(d)) == 1) {
										if(minDirt > rc.senseElevation(here.add(d))) {
											minDirt = rc.senseElevation(here.add(d));
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
				break;

			case SUPPORT_WALL_ROLE:
				if(supportWallBuildLocation == null) rerollRole();
				if(here.equals(supportWallBuildLocation)) {
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
							if(rc.senseElevation(here) - GameConstants.getWaterLevel(rc.getRoundNum()) < 1 && rc.senseElevation(here) < 15) {
								ttlog("DEPOSITING DIRT IN DIRECTION: " + Direction.CENTER);
								if(rc.canDepositDirt(Direction.CENTER)) Actions.doDepositDirt(Direction.CENTER);
							}
							int minDirt = 10000;
							Direction minDir = null;
							for(Direction d : directions) {
								if(maxXYDistance(HQLoc, here.add(d)) == 1 && rc.senseElevation(here.add(d)) < minDirt) {
									minDirt = rc.senseElevation(here.add(d));
									minDir = d;
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
					if(rc.canSenseLocation(supportWallBuildLocation)) {
						if(rc.senseRobotAtLocation(supportWallBuildLocation) != null) {
							rerollRole();
						}
						else {
							Nav.bugNavigate(supportWallBuildLocation);
						}
					}
					else {
						Nav.bugNavigate(supportWallBuildLocation);
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
						Nav.bugNavigate(buildingLocation);
					}
				}
				else {
					Nav.bugNavigate(buildingLocation);
				}
				break;

			case TERRA_ROLE:
				// @todo: Explore the Terraforming of Enemy HQ Locations
				if(rc.getID() % 4 == 0) {
					role = TERRA_ATTACK_ROLE;
				}
				else {
					//role = TERRA_BOUNCE_ROLE;
					role = TERRA_ATTACK_ROLE;
				}
				if(role == TERRA_ATTACK_ROLE) {
					terraTargetLocation = getSymmetryLoc();
				}
				break;

			case TERRA_ATTACK_ROLE:
				updateTerraDepth();
				if(terraCheck()) {
					Nav.bugNavigate(terraTargetLocation);
				}
				else {
					if(currentStep == 0) {
						if(rc.getDirtCarrying() == 0) {
							boolean flag = false;
							for(Direction d : Direction.allDirections()) {
								if(rc.senseElevation(rc.adjacentLocation(d)) > terraDepth) {
									flag = true;
									if(rc.canDigDirt(d)) Actions.doDigDirt(d);
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
								MapLocation adjLoc = rc.adjacentLocation(d);
								if(rc.senseElevation(adjLoc) < terraDepth && !isDigLoc(adjLoc) && !isLocBuilding(adjLoc)) {
									if(rc.canDepositDirt(d)) Actions.doDepositDirt(d);
								}
							}
						}
					}
				}
				break;

			case TERRA_BOUNCE_ROLE:

				break;

			case ATTACK_ROLE:

				break;
		}
	}

	private static void landscaperDig() throws GameActionException {
		for(Direction d : allDirections) {
			if(isDigLoc(here.add(d))) {
				ttlog("DIGGING IN DIRECTION: " + d);
				if(rc.canDigDirt(d)) Actions.doDigDirt(d);
			}
		}
	}

	private static void rerollRole() throws GameActionException {
		role = ATTACK_ROLE;
		if(rc.canSenseLocation(HQLoc)) {
			if(wallFull) {
				if(supportFull) role = TERRA_ROLE;
				else {
					role = (rc.getID() % 2 == 0) ? TERRA_ROLE : SUPPORT_WALL_ROLE;
				}
			}
			else {
				role = WALL_ROLE;
			}
		}
		ttlog("REROLLED ROLE: " + role);
		if(role == DEFENSE_ROLE) {
			ttlog("DEFENDING FROM BUILDING IN LOCATION: " + buildingLocation);
		}
		else if (role == WALL_ROLE) {
			wallBuildLocation = null;
			for(Direction d : landscaperDirections) {
				if(!rc.canSenseLocation(HQLoc.add(d)) || rc.senseRobotAtLocation(HQLoc.add(d)) == null) {
					wallBuildLocation = HQLoc.add(d);
					break;
				}
			}
			ttlog("BUILDING WALL IN LOCATION: " + wallBuildLocation);
		}
		else if(role == SUPPORT_WALL_ROLE) {
			for(int i = 2; i >= -2; i--) for(int j = 2; j >= -2; j--) {
				if(Math.abs(i) != 2 && Math.abs(j) != 2) continue;
				if(!isDigLoc(HQLoc.translate(i, j))) {
					if (!rc.canSenseLocation(HQLoc.translate(i, j)) ||  rc.senseRobotAtLocation(HQLoc.translate(i, j)) == null) {
						supportWallBuildLocation = HQLoc.translate(i, j);
						break;
					}
				}
			}
			ttlog("BUILDING WALL IN LOCATION: " + supportWallBuildLocation);
		}
	}

	private static boolean terraCheck() throws GameActionException {
		for(Direction d : allDirections) {
			MapLocation adjLoc = rc.adjacentLocation(d);
			if(rc.senseElevation(adjLoc) < terraDepth && !isDigLoc(adjLoc) && !isLocBuilding(adjLoc)) {
				ttlog("TERRA CHECK: FAILED CHECK IN DIRECTION " + d);
				return false;
			}
		}
		return true;
	}

	private static void updateTerraDepth() {
		terraDepth = Math.max(4, (int) Math.ceil(GameConstants.getWaterLevel(rc.getRoundNum()) + 3));
	}
}