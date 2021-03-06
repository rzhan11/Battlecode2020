package quals_bot;

import battlecode.common.*;

import static quals_bot.Actions.*;
import static quals_bot.Communication.*;
import static quals_bot.Debug.*;
import static quals_bot.Globals.*;
import static quals_bot.Map.*;
import static quals_bot.Nav.*;
import static quals_bot.Utils.*;
import static quals_bot.Wall.*;
import static quals_bot.Zones.*;

public class Nav extends Globals {

	// if true, ignore flooding
	public static boolean isDrone = false;

	/*
	Returns true if we can move in a direction to a tile that is not dangerous to our type
	Returns false otherwise
	*/
	public static boolean checkDirMoveable(Direction dir) throws GameActionException {
//		MapLocation loc = rc.adjacentLocation(dir);
//		return rc.onTheMap(loc) && rc.canMove(dir) && (isDrone || !rc.senseFlooding(loc));
		switch (dir) {
			case NORTH:
				return isDirMoveable[0];
			case NORTHEAST:
				return isDirMoveable[1];
			case EAST:
				return isDirMoveable[2];
			case SOUTHEAST:
				return isDirMoveable[3];
			case SOUTH:
				return isDirMoveable[4];
			case SOUTHWEST:
				return isDirMoveable[5];
			case WEST:
				return isDirMoveable[6];
			case NORTHWEST:
				return isDirMoveable[7];
		}
		return false;
	}

	/*
	Returns true if we cannot move (without being in danger) in any of the eight directions
	Ignores if we are ready
	Drones ignore flooding
	*/
	public static boolean isTrapped () throws GameActionException {
		for (Direction dir: directions) {
			if (checkDirMoveable(dir)) {
				return false;
			}
		}
		return true;
	}

	/*
	Tries to move in the target direction
	If we are not a drone, it does not move into flooded tiles
	Returns the Direction that we moved in
	Returns null if did not move
	*/
	public static Direction tryMoveInDirection (Direction dir) throws GameActionException {
		if (checkDirMoveable(dir)) {
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
		if (checkDirMoveable(dir)) {
			Actions.doMove(dir);
			return dir;
		}
		Direction leftDir = dir.rotateLeft();
		if (checkDirMoveable(leftDir)) {
			Actions.doMove(leftDir);
			return leftDir;
		}
		Direction rightDir = dir.rotateRight();
		if (checkDirMoveable(rightDir)) {
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

	final public static int MAX_BUG_HISTORY_LENGTH = 100;

	public static MapLocation bugTarget = null;

	public static boolean bugTracing = false;
	public static MapLocation bugLastWall = null;
	public static int bugClosestDistanceToTarget = P_INF;
	public static int bugTurnsWithoutWall = 0;
	public static boolean bugRotateLeft = true; // whether we are rotating left or right

	public static MapLocation[] bugVisitedLocations = null;
	public static int bugVisitedLocationsIndex;
	public static int bugVisitedLocationsLength;

	public static Direction bugNavigate (MapLocation target) throws GameActionException {
		log("Bug navigating to " + target);

		if (isTrapped()) {
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

		// log("BUG_NAVIGATE");
		// tlog("bugTarget: " + bugTarget);
		// tlog("bugClosestDistanceToTarget: " + bugClosestDistanceToTarget);
		// tlog("destDir: " + destDir);
		// tlog("bugTracing: " + bugTracing);

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
		bugTracing = true;

		bugVisitedLocations = new MapLocation[MAX_BUG_HISTORY_LENGTH];
		bugVisitedLocationsIndex = 0;
		bugVisitedLocationsLength = 0;

		bugTurnsWithoutWall = 0;
		bugClosestDistanceToTarget = P_INF;

		Direction destDir = here.directionTo(bugTarget);

		Direction leftDir = destDir;
		MapLocation leftDest;
		int leftDist = Integer.MAX_VALUE;
		for (int i = 0; i < 8; ++i) {
			leftDir = leftDir.rotateLeft();
			leftDest = rc.adjacentLocation(leftDir);
			if (checkDirMoveable(leftDir)) {
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
			if (checkDirMoveable(rightDir)) {
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
		// log("START_TRACING");
		// tlog("bugRotateLeft: " + bugRotateLeft);
		// tlog("bugLastWall: " + bugLastWall);
	}

	/*
	Returns the Direction that we moved in
	Returns null if we did not move
	*/
	public static Direction bugTraceMove(boolean recursed) throws GameActionException {

		Direction curDir = here.directionTo(bugLastWall);

		// adds to array
		bugVisitedLocations[bugVisitedLocationsIndex] = here;
		bugVisitedLocationsIndex = (bugVisitedLocationsIndex + 1) % MAX_BUG_HISTORY_LENGTH;
		bugVisitedLocationsLength = Math.min(bugVisitedLocationsLength + 1, MAX_BUG_HISTORY_LENGTH);

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
			MapLocation curDest = rc.adjacentLocation(curDir);
			if (!rc.onTheMap(curDest) && !recursed) {
				// tlog("Hit the edge of map, reverse and recurse");
				// if we hit the edge of the map, reverse direction and recurse
				bugRotateLeft = !bugRotateLeft;
				return bugTraceMove(true);
			}
			if (checkDirMoveable(curDir)) {
				Actions.doMove(curDir);
				if (inArray(bugVisitedLocations, curDest, bugVisitedLocationsLength)) {
					log("Resetting bugTracing");
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
	Return 0 if not in danger
	Return 1 if was in danger, but moved out of it
	Return -1 if cannot move out of danger
	 */
	public static int avoidDanger() throws GameActionException {
		if (!inDanger) {
			log("Not in danger");
			return 0;
		}

		for (int i = 0; i < directions.length; i++) {
			MapLocation loc = rc.adjacentLocation(directions[i]);
			if (!rc.onTheMap(loc)) {
				continue;
			}
			if (isDirMoveable[i]) {
				log("Avoiding danger");
				Actions.doMove(directions[i]);
				return 1;
			}
		}

		log("Cannot avoid danger");
		return -1;
	}

	/*
	Checks if water will kill us next turn and moves away from it if capable
	Returns true if we were in danger and moved away from water
	Returns false if we did not move away from danger
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
			if (rc.onTheMap(loc) && rc.senseFlooding(loc)) {
				danger = true;
				break;
			}
		}
		if (!danger) {
			return false;
		}

		log("This tile will be flooded next turn");

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
				if (rc.onTheMap(loc)) {
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
				if (checkDirMoveable(dir)) {
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
				// logi("dir " + dir);
				if (rc.canMove(dir) && !dangerDirection[index]) {
					if (bestIndex == -1 || elevationDirection[index] > elevationDirection[bestIndex]) {
						bestIndex = index;
					}
				}
				index++;
			}
			if (bestIndex == -1) {
				log("No safe directions. I am dying to water! :(");
			} else {
				Actions.doMove(directions[bestIndex]);
				return true;
			}
		}

		return false;
	}
}
