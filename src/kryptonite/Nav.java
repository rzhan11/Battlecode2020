package kryptonite;

import battlecode.common.*;

public class Nav extends Globals {

	/*
	NOTE: This is a temporary method that is in place of rc.onTheMap() since I believe there is an engine bug
	Please DO NOT USE this method if possible - if you see places that use it, please let Richard know
	*/
	public static boolean onMap (MapLocation loc) {
		return 0 <= loc.x && loc.x < mapWidth && 0 <= loc.y && loc.y < mapHeight;
	}

	/*
	Returns true if we can move in a direction to a tile that is not occupied and not flooded
	Returns false otherwise
	*/
	public static boolean checkDirectionMoveable (Direction dir) throws GameActionException {
		MapLocation loc = rc.adjacentLocation(dir);
		return Nav.onMap(loc) && rc.canMove(dir) && !rc.senseFlooding(loc);
	}


	/*
	Tries to move in the target direction, or rotateLeft/rotateRight of it
	Does not move into flooded tiles
	Returns the Direction that we moved in
	Returns null if did not move
	*/
	public static Direction tryMoveInDirection (Direction dir) throws GameActionException {
		if (checkDirectionMoveable(dir)) {
			rc.move(dir);
			return dir;
		}
		Direction leftDir = dir.rotateLeft();
		if (checkDirectionMoveable(leftDir)) {
			rc.move(leftDir);
			return leftDir;
		}
		Direction rightDir = dir.rotateRight();
		if (checkDirectionMoveable(rightDir)) {
			rc.move(rightDir);
			return rightDir;
		}
		return null;
	}

	/*
	---------------
	BUG PATHFINDING
	---------------
	Uses the bug pathfinding algorithm to navigate around obstacles towards a target MapLocation
	Details here: https://www.cs.cmu.edu/~motionplanning/lecture/Chap2-Bug-Alg_howie.pdf

	Taken/adapted from TheDuck314 Battlecode 2016

	Assumes that we are ready to move
	Returns the Direction we moved in
	Returns null if did not move
	*/


	private static MapLocation bugTarget = null;

	private static boolean bugTracing = false;
	private static MapLocation bugLastWall = null;
	private static int bugClosestDistanceOnWall = P_INF;
	private static int bugTurnsWithoutWall = 0;
	private static boolean bugRotateLeft = true; // whether we are rotating left or right
	private static boolean[][] bugVisitedLocations;

	public static Direction bugNavigate (MapLocation target) throws GameActionException {
		if (!target.equals(bugTarget)) {
			bugTarget = target;
			bugTracing = false;
		}

		if (here.equals(bugTarget)) {
			return null;
		}

		Direction destDir = here.directionTo(bugTarget);
		if (!bugTracing) { // try to go directly towards the target
			Direction tryMoveResult = tryMoveInDirection(destDir);
			if (tryMoveResult != null) {
				return tryMoveResult;
			} else {
				bugStartTracing();
			}
		} else { // we are on obstacle, trying to get off of it
			if (here.distanceSquaredTo(bugTarget) < bugClosestDistanceOnWall) {
				Direction tryMoveResult = tryMoveInDirection(destDir);
				if (tryMoveResult != null) { // we got off of the obstacle
					bugTracing = false;
					return tryMoveResult;
				}
			}
		}

		Direction moveDir = bugTraceMove(false);

	    if (bugTurnsWithoutWall >= 2) {
	    	bugTracing = false;
	    }

		return moveDir;
	}

	/*
	Runs if we just encoutnered an obstacle
	*/
	public static void bugStartTracing() throws GameActionException {
		bugTracing = true;
		bugVisitedLocations = new boolean[MAX_MAP_SIZE][MAX_MAP_SIZE];

		bugClosestDistanceOnWall = here.distanceSquaredTo(bugTarget);
		bugTurnsWithoutWall = 0;

		Direction destDir = here.directionTo(bugTarget);

		Direction leftDir = destDir;
		MapLocation leftDest = rc.adjacentLocation(leftDir);
		int leftDist = Integer.MAX_VALUE;
		for (int i = 0; i < 8; ++i) {
			leftDir = leftDir.rotateLeft();
			if (checkDirectionMoveable(leftDir)) {
				leftDist = leftDest.distanceSquaredTo(bugTarget);
				break;
			}
		}

		Direction rightDir = destDir;
		MapLocation rightDest = rc.adjacentLocation(rightDir);
		int rightDist = Integer.MAX_VALUE;
		for (int i = 0; i < 8; ++i) {
			rightDir = rightDir.rotateRight();
			if (checkDirectionMoveable(rightDir)) {
				rightDist = rightDest.distanceSquaredTo(bugTarget);
				break;
			}
		}

		if (leftDist < rightDist) {
			bugRotateLeft = true;
			bugLastWall = rc.adjacentLocation(leftDir.rotateRight());
		} else {
			bugRotateLeft = false;
			bugLastWall = rc.adjacentLocation(rightDir.rotateLeft());
		}
	}

	/*
	Returns the Direction that we moved in
	Returns null if we did not move
	*/
	public static Direction bugTraceMove(boolean recursed) throws GameActionException {
		Direction curDir = here.directionTo(bugLastWall);
		bugVisitedLocations[here.x % MAX_MAP_SIZE][here.y % MAX_MAP_SIZE] = true;
		if (rc.canMove(curDir)) {
			bugTurnsWithoutWall += 1;
		} else {
			bugTurnsWithoutWall = 0;
		}

		for (int i = 0; i < 8; ++i) {
			if (bugRotateLeft) {
				curDir = curDir.rotateLeft();
			} else {
				curDir = curDir.rotateRight();
			}
			MapLocation dirLoc = rc.adjacentLocation(curDir);
			if (!Nav.onMap(dirLoc) && !recursed) {
				// if we hit the edge of the map, reverse direction and recurse
				bugRotateLeft = !bugRotateLeft;
				return bugTraceMove(true);
			}
			if (checkDirectionMoveable(curDir)) {
				rc.move(curDir);
				if (bugVisitedLocations[here.x % MAX_MAP_SIZE][here.y % MAX_MAP_SIZE]) {
					bugTracing = false;
				}
				return curDir;
			} else {
				bugLastWall = rc.adjacentLocation(curDir);
			}
		}
		
		return null;
	}

	/*
	Checks if water will kill us next turn and moves away from it if capable
	Returns true if we were in danger and moved away from water
	Returns false if we did not move away from danger (or was unable to due to cooldown)
	*/
	public static boolean avoidWater() throws GameActionException {
		// check we are ready to move and if our current elevation in under the water elevation
		if (myElevation > waterLevel) {
			return false;
		}
		// check if there is a water tile next to us
		boolean danger = false;
		for (Direction dir: directions) {
			MapLocation loc = rc.adjacentLocation(dir);
			if (Nav.onMap(loc) && rc.senseFlooding(loc)) {
				danger = true;
				break;
			}
		}
		if (!danger) {
			return false;
		}

		Debug.tlog("This tile will be flooded next turn");
		if (!rc.isReady()) {
			Debug.tlog("Cooldown is not ready. I am dying to water! :(");
			return false;
		}

		int size = 8;
		int[] elevationDirection = new int[size];
		boolean[] dangerDirection = new boolean[size];
		if (false) { //(actualSensorRadiusSquared >= 8) { // 5x5 square centered at 'here'
			// remind Richard about this
			// this is a smarter (but more costly) way to avoid water
			/*
			int[][] elevations = new int[5][5];
			boolean[][] flooded = new boolean[5][5];
			for (int i = 0; i < 5; i++) {
				for (int j = 0; j < 5; j++) {
					elevations[i][j] = rc.senseElevation(new MapLocation(here.x + i - 2, here.y + j - 2));
					flooded[i][j] = rc.senseFlooding(new MapLocation(here.x + i - 2, here.y + j - 2));
				}
			}

			MapLocation loc;
			for (int dx = -2; dx <= 2; dx++) {
				for (int dy = -2; dy <= 2; dy++) {
					loc = here.translate(dx, dy);
					if (rc.senseFlooding(loc)) {
						if (elevations[dx + 2][dy + 2] > elevations[dx + 2][dy + 2]) {

						}
					}
				}
			}
			*/
		} else { // requires actualSensorRadiusSquared >= 2
			int index = 0;
			for (Direction dir: directions) {
				MapLocation loc = rc.adjacentLocation(dir);
				if (Nav.onMap(loc)) {
					elevationDirection[index] = rc.senseElevation(loc);
				} else {
					elevationDirection[index] = N_INF;
				}
				index++;
			}

			// sets dangerDirection to true if next turn, it will be flooded by an adjacent tile
			index = 0;
			for (Direction dir: directions) {
				MapLocation loc = rc.adjacentLocation(dir);
				if (checkDirectionMoveable(dir)) {
					int ii = index;
					dangerDirection[ii] |= elevationDirection[ii] <= waterLevel;
					ii = (index + size - 1) % size;
					dangerDirection[ii] |= elevationDirection[ii] <= waterLevel;
					ii = (index + size + 1) % size;
					dangerDirection[ii] |= elevationDirection[ii] <= waterLevel;
					if (index % 2 == 0) {
						ii = (index + size - 2) % size;
						dangerDirection[ii] |= elevationDirection[ii] <= waterLevel;
						ii = (index + size + 2) % size;
						dangerDirection[ii] |= elevationDirection[ii] <= waterLevel;
					}
				}
				index++;
			}

			// moves to a safe direction with the highest elevation
			index = 0;
			int bestIndex = -1;
			for (Direction dir: directions) {
				MapLocation loc = rc.adjacentLocation(dir);
				// Debug.tlogi("dir " + dir);
				if (rc.canMove(dir) && !dangerDirection[index]) {
					if (bestIndex == -1 || elevationDirection[index] > elevationDirection[bestIndex]) {
						bestIndex = index;
					}
				}
				index++;
			}
			if (bestIndex == -1) {
				Debug.tlog("No safe directions. I am dying to water! :(");
			} else {
				rc.move(directions[bestIndex]);
				return true;
			}
		}

		return false;
	}
}
