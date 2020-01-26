package turtle;

import battlecode.common.*;

import static turtle.Constants.*;
import static turtle.Debug.*;
import static turtle.Map.*;
import static turtle.Utils.*;
import static turtle.Wall.*;

public class BotLandscaper extends Globals {

	private static int role, currentStep = 0;
	private static final int WALL_ROLE = 1;
	private static Direction[] landscaperDirections = {Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST, Direction.NORTHEAST, Direction.SOUTHWEST, Direction.SOUTHEAST, Direction.NORTHWEST}; // 8 directions


	// WALL_ROLE Variables
	private static MapLocation wallBuildLocation;

	public static void loop() throws GameActionException {
		while (true) {
				Globals.update();

				turn();

			Globals.endTurn();
		}
	}

	public static void turn() throws GameActionException {
		if(!rc.isReady()) {
			return;
		}


		landscaperDirections = getCloseDirections(HQLoc.directionTo(getSymmetryLoc()).opposite());
		role = WALL_ROLE;

		if(maxXYDistance(HQLoc, here) == 1) {
			wallBuildLocation = here;
		}



		ttlog("MY ROLE IS: " + role);

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
			if(wallCompleted) {
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

	private static void landscaperDig() throws GameActionException {
		for(Direction d : allDirections) {
			MapLocation loc = rc.adjacentLocation(d);
			if (!rc.onTheMap(loc)) {
				continue;
			}
			if(isDigLoc(loc)) {
				ttlog("DIGGING IN DIRECTION: " + d);
				if(rc.canDigDirt(d)) Actions.doDigDirt(d);
			}
		}
	}

	private static void rerollRole() throws GameActionException {

		if (role == WALL_ROLE) {
			wallBuildLocation = null;
			for(Direction d : landscaperDirections) {
				MapLocation loc = HQLoc.add(d);
				if (!rc.onTheMap(loc)) {
					continue;
				}
				if(!rc.canSenseLocation(HQLoc.add(d)) || rc.senseRobotAtLocation(HQLoc.add(d)) == null) {
					wallBuildLocation = HQLoc.add(d);
					break;
				}
			}
			ttlog("BUILDING WALL IN LOCATION: " + wallBuildLocation);
		}

	}


}