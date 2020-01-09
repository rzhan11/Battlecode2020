package kryptonite;

import battlecode.common.*;

public class Nav extends Globals {

	private static MapLocation prevBugTarget = null;
	private static int closestDistance;

	/*
	NOTE: This is a temporary method that is in place of rc.onTheMap() since I believe there is an engine bug
	Please DO NOT USE this method if possible - if you see places that use it, please let Richard know
	*/
	public static boolean onMap (MapLocation loc) {
		return 0 <= loc.x && loc.x < mapWidth && 0 <= loc.y && loc.y < mapHeight;
	}

	/*
	Uses the bug pathfinding algorithm to navigate around obstacles towards a target MapLocation
	Details here: https://www.cs.cmu.edu/~motionplanning/lecture/Chap2-Bug-Alg_howie.pdf
	We are using an implementation of "Bug 2"

	Returns the Direction we moved in
	Returns null if did not move
	*/
	public static Direction bugNavigate (MapLocation target) throws GameActionException {
		if (!target.equals(prevBugTarget)) {
			prevBugTarget = target;
			closestDistance = here.distanceSquaredTo(target);
		}

		Direction moveDir = null;

		Direction curDir = here.directionTo(target);
		MapLocation curDest = rc.adjacentLocation(curDir);
		int curDist = curDest.distanceSquaredTo(target);
		if (rc.canMove(curDir) && !rc.senseFlooding(curDest) && curDist < closestDistance) {
			rc.move(curDir);
			closestDistance = Math.min(closestDistance, curDist);
			moveDir = curDir;
		} else {
			int count = 1;
			curDir = curDir.rotateLeft();
			curDest = rc.adjacentLocation(curDir);
			curDist = curDest.distanceSquaredTo(target);
			while ((!rc.canMove(curDir) || rc.senseFlooding(curDest)) && count < 8) {
				curDir = curDir.rotateLeft();
				curDest = rc.adjacentLocation(curDir);
				count++;
			}
			if (count < 8) {
				rc.move(curDir);
				closestDistance = Math.min(closestDistance, curDist);
				moveDir = curDir;
			}
		}

		return moveDir;
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
				if (Nav.onMap(loc) && rc.senseFlooding(loc)) {
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
