package sprint;

import battlecode.common.*;

public class Nav extends Globals {

	// if true, ignore flooding
	public static boolean isDrone = false;

	/*
	Returns true if we can move in a direction to a tile that is not occupied and not flooded
	Returns false otherwise
	*/
	public static boolean checkDirectionMoveable (Direction dir) throws GameActionException {
		MapLocation loc = rc.adjacentLocation(dir);
		return inMap(loc) && rc.canMove(dir) && (isDrone || !rc.senseFlooding(loc));
	}

	/*
	Assumes that we can sense this tile
	Returns true if this tile's elevation is within +/-3 of our tile's elevation
	Returns false otherwise
	*/
	public static boolean checkElevation (MapLocation loc) throws GameActionException {
		return Math.abs(rc.senseElevation(loc) - myElevation) <= GameConstants.MAX_DIRT_DIFFERENCE;
	}

	/*
	Assumes that we can sense both tiles
	Returns true if the two tiles' elevation are within +/-3 of each other
	Returns false otherwise
	*/
	public static boolean checkElevation (MapLocation loc1, MapLocation loc2) throws GameActionException {
		return Math.abs(rc.senseElevation(loc1) - rc.senseElevation(loc2)) <= GameConstants.MAX_DIRT_DIFFERENCE;
	}

	/*
	Returns if we can move (without getting killed by flood) in any of the eight directions
	Ignores if we are ready
	Drones ignore flooding
	*/
	public static boolean canMove () throws GameActionException {
		for (Direction dir: directions) {
			if (checkDirectionMoveable(dir)) {
				return true;
			}
		}
		return false;
	}

	/*
	Only drones can call this method
	Tries to move in the target direction
	Ignores flooded tiles
	Will pick up allies that are in the way

	Returns the Direction that we moved in
	Returns null if did not move
	*/
	public static Direction tryForceMoveInDirection (Direction dir) throws GameActionException {
		Direction move = tryMoveInDirection(dir);
		if (move == null) {
			MapLocation loc = rc.adjacentLocation(dir);
			RobotInfo ri = rc.senseRobotAtLocation(loc);
			if (ri != null && canPickUpType(ri.type)) {
				if (ri.ID == builderMinerID) {
					Debug.ttlog("Cannot force through builder miner");
				} else {
					Debug.ttlog("Picking up ally at " + loc);
					Actions.doPickUpUnit(ri.ID);
					move = dir;
				}
			}
		}
		return move;
	}

	/*
	Only drones can call this method
	Tries to move in the target direction
	Ignores flooded tiles
	Will pick up allies that are in the way

	Returns the Direction that we moved in
	Returns null if did not move
	*/
	public static Direction tryForceMoveInGeneralDirection (Direction dir) throws GameActionException {
		Direction leftDir = dir.rotateLeft();
		Direction rightDir = dir.rotateRight();

		Direction move = tryForceMoveInDirection(dir);
		if (move == null) {
			move = tryForceMoveInDirection(leftDir);
		}
		if (move == null) {
			move = tryForceMoveInDirection(rightDir);
		}
		return move;
	}

	/*
	Tries to move in the target direction
	If we are not a drone, it does not move into flooded tiles
	Returns the Direction that we moved in
	Returns null if did not move
	*/
	public static Direction tryMoveInDirection (Direction dir) throws GameActionException {
		if (checkDirectionMoveable(dir)) {
			Actions.doMove(dir);
			return dir;
		}
		return null;
	}

	/*
	Tries to move in the target direction, or rotateLeft/rotateRight of it
	If we are not a drone, it does not move into flooded tiles
	Returns the Direction that we moved in
	Returns null if did not move
	*/
	public static Direction tryMoveInGeneralDirection (Direction dir) throws GameActionException {
		if (checkDirectionMoveable(dir)) {
			Actions.doMove(dir);
			return dir;
		}
		Direction leftDir = dir.rotateLeft();
		if (checkDirectionMoveable(leftDir)) {
			Actions.doMove(leftDir);
			return leftDir;
		}
		Direction rightDir = dir.rotateRight();
		if (checkDirectionMoveable(rightDir)) {
			Actions.doMove(rightDir);
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


	public static MapLocation bugTarget = null;

	public static boolean bugTracing = false;
	public static MapLocation bugLastWall = null;
	public static int bugClosestDistanceToTarget = P_INF;
	public static int bugTurnsWithoutWall = 0;
	public static boolean bugRotateLeft = true; // whether we are rotating left or right
	public static boolean[][] bugVisitedLocations;

	public static Direction bugNavigate (MapLocation target) throws GameActionException {
		if (!canMove()) {
			return null;
		}

		if (!target.equals(bugTarget)) {
			bugTarget = target;
			bugTracing = false;
			bugClosestDistanceToTarget = here.distanceSquaredTo(bugTarget);

		}

		if (here.equals(bugTarget)) {
			return null;
		}

		// bugClosestDistanceToTarget = Math.min(bugClosestDistanceToTarget, here.distanceSquaredTo(bugTarget));

		Direction destDir = here.directionTo(bugTarget);

		// Debug.tlog("BUG_NAVIGATE");
		// Debug.ttlog("bugTarget: " + bugTarget);
		// Debug.ttlog("bugClosestDistanceToTarget: " + bugClosestDistanceToTarget);
		// Debug.ttlog("destDir: " + destDir);
		// Debug.ttlog("bugTracing: " + bugTracing);

		if (!bugTracing) { // try to go directly towards the target
			Direction tryMoveResult = tryMoveInDirection(destDir);
			if (tryMoveResult != null) {
				return tryMoveResult;
			} else {
				bugStartTracing();
			}
		} else { // we are on obstacle, trying to get off of it
			if (here.distanceSquaredTo(bugTarget) < bugClosestDistanceToTarget) {
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
	Runs if we just encountered an obstacle
	*/
	public static void bugStartTracing() throws GameActionException {

		// Ends turn early to avoid exceeding bytecode limit due to large array creation
		Globals.endTurn(true);
		Globals.update();

		bugTracing = true;
		bugVisitedLocations = new boolean[MAX_MAP_SIZE][MAX_MAP_SIZE];

		bugTurnsWithoutWall = 0;
		bugClosestDistanceToTarget = P_INF;

		Direction destDir = here.directionTo(bugTarget);

		Direction leftDir = destDir;
		MapLocation leftDest;
		int leftDist = Integer.MAX_VALUE;
		for (int i = 0; i < 8; ++i) {
			leftDir = leftDir.rotateLeft();
			leftDest = rc.adjacentLocation(leftDir);
			if (checkDirectionMoveable(leftDir)) {
				leftDist = leftDest.distanceSquaredTo(bugTarget);
				break;
			}
		}

		Direction rightDir = destDir;
		MapLocation rightDest;
		int rightDist = Integer.MAX_VALUE;
		for (int i = 0; i < 8; ++i) {
			rightDir = rightDir.rotateRight();
			rightDest = rc.adjacentLocation(rightDir);
			if (checkDirectionMoveable(rightDir)) {
				rightDist = rightDest.distanceSquaredTo(bugTarget);
				break;
			}
		}


		if (leftDist < rightDist) { // prefer rotate right if equal
			bugRotateLeft = true;
			bugLastWall = rc.adjacentLocation(leftDir.rotateRight());
		} else {
			bugRotateLeft = false;
			bugLastWall = rc.adjacentLocation(rightDir.rotateLeft());
		}
		// Debug.tlog("START_TRACING");
		// Debug.ttlog("bugRotateLeft: " + bugRotateLeft);
		// Debug.ttlog("bugLastWall: " + bugLastWall);
	}

	/*
	Returns the Direction that we moved in
	Returns null if we did not move
	*/
	public static Direction bugTraceMove(boolean recursed) throws GameActionException {

		Direction curDir = here.directionTo(bugLastWall);
		bugVisitedLocations[here.x][here.y] = true;
		if (rc.canMove(curDir)) {
			bugTurnsWithoutWall += 1;
		} else {
			bugTurnsWithoutWall = 0;
		}
		// Debug.tlog("TRACING");
		// Debug.ttlog("bugRotateLeft: " + bugRotateLeft);
		// Debug.ttlog("bugLastWall: " + bugLastWall);

		for (int i = 0; i < 8; ++i) {
			if (bugRotateLeft) {
				curDir = curDir.rotateLeft();
			} else {
				curDir = curDir.rotateRight();
			}
			MapLocation curDest = rc.adjacentLocation(curDir);
			if (!inMap(curDest) && !recursed) {
				// Debug.ttlog("Hit the edge of map, reverse and recurse");
				// if we hit the edge of the map, reverse direction and recurse
				bugRotateLeft = !bugRotateLeft;
				return bugTraceMove(true);
			}
			if (checkDirectionMoveable(curDir)) {
				Actions.doMove(curDir);
				if (bugVisitedLocations[curDest.x][curDest.y]) {
					// Debug.tlog("Resetting bugTracing");
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
			if (inMap(loc) && rc.senseFlooding(loc)) {
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
				if (inMap(loc)) {
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
				Actions.doMove(directions[bestIndex]);
				return true;
			}
		}

		return false;
	}
}
