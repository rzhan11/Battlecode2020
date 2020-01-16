package rush;

import battlecode.common.*;

import static rush.Communication.*;
import static rush.Constants.*;
import static rush.Debug.*;
import static rush.Map.*;

public class BotLandscaper extends Globals {


	private static MapLocation[] allDigLocations, smallWallCompleted;
	private static int allDigLocationsLength, smallWallCompletedLength, currentStep, role;
	private static int currentLargeWallIndex = -1;

	private static final int WALL_ROLE = 1, DEFENSE_ROLE = 2, SUPPORT_ROLE = 3;

	private static boolean moveClockwise = true;

	// only used when on wall
	final private static int DEPOSITS_WITHOUT_MOVE_LIMIT = 3;
	private static int depositsWithoutMove = 0;
	public static boolean checkSelfDestructed = false;

	// @todo: Landscapers should attack enemy buildings if in sight and not on large wall
	// @todo: Landscapers should "heal" ally buildings if they are damaged
	public static void loop() throws GameActionException {
		while (true) {
			if (largeWallFull && !checkSelfDestructed) { // self destruct
				checkSelfDestructed = true;
				Direction dir = here.directionTo(getSymmetryLocation());
				if (maxXYDistance(HQLocation, here) == 4 && manhattanDistance(HQLocation, here) == 4) {
					int[] array = new int[1];
					log("exception " + array[1]);
				}
			}
			try {
				Globals.update();
				if (firstTurn) {

					loadWallInformation();

					role = WALL_ROLE;
					for (RobotInfo ri : visibleEnemies) {
						if (ri.type.isBuilding()) {
							role = DEFENSE_ROLE;
							break;
						}
					}
					// finds spots that can be used for digging
					allDigLocations = new MapLocation[50];
					MapLocation templ = HQLocation.translate(5,5);
					int index = 0;
					for(int i = 0; i < 6; i++) for(int j = 0; j < 6; j++) {
						MapLocation newl = templ.translate(-2*i, -2*j);
						if(inMap(newl) && !HQLocation.equals(newl)) {
							if (maxXYDistance(HQLocation, newl) > 2) { // excludes holes inside the 5x5 plot
								allDigLocations[index] = newl;
								index++;
							}
						}
					}
					allDigLocationsLength = index;

					Globals.endTurn(true);
					Globals.update();

					smallWallCompleted = new MapLocation[49];
					smallWallCompletedLength = 0;
				}

				turn();
			} catch (Exception e) {
				e.printStackTrace();
			}
			Globals.endTurn(false);
		}
	}

	public static void turn() throws GameActionException {

		if(rc.getRoundNum() >= 500) smallWallFinished = true;

		log("smallWallFinished " + smallWallFinished);
		log("largeWallFull " + largeWallFull);

		if (!rc.isReady()) {
			log("Not ready");
			return;
		}

		Nav.bugNavigate(getSymmetryLocation());

	}

	private static Direction getClockwiseLargeWallDirection (int curIndex) {
		int newIndex = (curIndex + 1 + largeWallLength) % largeWallLength;
		return largeWall[curIndex].directionTo(largeWall[newIndex]);
	}

	private static Direction getCounterClockwiseLargeWallDirection (int curIndex) {
		int newIndex = (curIndex - 1 + largeWallLength) % largeWallLength;
		return largeWall[curIndex].directionTo(largeWall[newIndex]);
	}

	private static Direction getClockwiseDir(MapLocation ml) {
		int delx = HQLocation.x - ml.x;
		int dely = HQLocation.y - ml.y;
		int absx = Math.abs(delx);
		int absy = Math.abs(dely);
		boolean xPos = absx == delx;
		boolean yPos = absy == dely;

		if(absx == absy) {
			if(xPos && yPos) return Direction.NORTH;
			if(xPos) return Direction.EAST;
			if(yPos) return Direction.WEST;
			return Direction.SOUTH;
		}
		else if(absx > absy) {
			if(xPos) return Direction.NORTH;
			return Direction.SOUTH;
		}
		else {
			if(yPos) return Direction.WEST;
			return Direction.EAST;
		}
	}

	private static Direction getCounterClockwiseDir(MapLocation ml) {
		int delx = HQLocation.x - ml.x;
		int dely = HQLocation.y - ml.y;
		int absx = Math.abs(delx);
		int absy = Math.abs(dely);
		boolean xPos = absx == delx;
		boolean yPos = absy == dely;

		if(absx == absy) {
			if(xPos && yPos) return Direction.EAST;
			if(xPos) return Direction.SOUTH;
			if(yPos) return Direction.NORTH;
			return Direction.WEST;
		}
		else if(absx > absy) {
			if(xPos) return Direction.SOUTH;
			return Direction.NORTH;
		}
		else {
			if(yPos) return Direction.EAST;
			return Direction.WEST;
		}
	}

	private static void landscaperMove(Direction d) throws GameActionException {
		if(rc.canMove(d)) Actions.doMove(d);
		else if(rc.senseElevation(here.add(d)) > 3 + rc.senseElevation(here))
			if(rc.canDigDirt(d)) Actions.doDigDirt(d);
			else
			if(rc.getDirtCarrying() >= 0)
				if(rc.canDepositDirt(d)) Actions.doDepositDirt(d);
	}

	private static void landscaperWallMove(Direction dir) throws GameActionException {
		MapLocation loc = rc.adjacentLocation(dir);
		RobotInfo ri = rc.senseRobotAtLocation(loc);
		if (ri != null) {
			return;
		}
		if (isLocFlat(loc) && !rc.senseFlooding(loc)) {
			Actions.doMove(dir);
			depositsWithoutMove = 0;
			return;
		} else if (myElevation + GameConstants.MAX_DIRT_DIFFERENCE < rc.senseElevation(loc)) {
			// STATE == we are lower than dest
			if (rc.getDirtCarrying() == 0) {
				landscaperDig(2);
				return;
			} else {
				Actions.doDepositDirt(Direction.CENTER);
				depositsWithoutMove++;
				return;
			}
		} else {
			// STATE == we are higher than dest or dest is flooded
			if (rc.getDirtCarrying() == 0) {
				landscaperDig(2);
				return;
			} else {
				Actions.doDepositDirt(dir);
				depositsWithoutMove++;
				return;
			}
		}

	}

	private static void landscaperDig(int where) throws GameActionException {
		if(where == 1) {
			for (Direction d : Direction.allDirections()) {
				if (inArray(allDigLocations, here.add(d), allDigLocationsLength)) if (rc.canDigDirt(d)) Actions.doDigDirt(d);
			}
		}
		else if(where == 2) {
			for (Direction d : Direction.allDirections()) {
				if (maxXYDistance(HQLocation, here.add(d)) == 5 && inArray(allDigLocations, here.add(d), allDigLocationsLength)) if (rc.canDigDirt(d)) Actions.doDigDirt(d);
			}
		}
	}

	private static void buildWall () throws GameActionException {
		if (rc.getDirtCarrying() == 0) {
			tlog("Digging dirt");
			landscaperDig(2);
		} else {
			int minDirt = P_INF;
			Direction minDir = null;
			for (Direction d : allDirections) {
				MapLocation newloc = here.add(d);
				if (!inMap(newloc)) {
					continue;
				}

				RobotInfo ri = rc.senseRobotAtLocation(newloc);
				if(ri != null && ri.type.isBuilding() && ri.team == rc.getTeam()) {
					continue;
				}

				// checks if inside tiles are below elevation
				// if largeWallFull of landscapers, ignore this check
				if(!largeWallFull) {
					if (maxXYDistance(HQLocation, newloc) == 3 && rc.senseElevation(newloc) < smallWallDepth) {
						minDir = d;
						minDirt = -1;
						break;
					}
				}

				if (maxXYDistance(HQLocation, newloc) == 4) {
					if (minDirt > rc.senseElevation(newloc)) {
						minDir = d;
						minDirt = rc.senseElevation(newloc);
					}
				}
			}
			tlog("Depositing dirt in direction " + minDir);
			Actions.doDepositDirt(minDir);
			depositsWithoutMove++;
		}
	}
}
