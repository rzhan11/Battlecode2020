package kryptonite;

import battlecode.common.*;

public class Nav extends Globals {

	private static MapLocation prevBugTarget = null;
	private static int closestDistance;

	/*
	Uses the bug pathfinding algorithm to navigate around obstacles towards a target MapLocation
	Details here: https://www.cs.cmu.edu/~motionplanning/lecture/Chap2-Bug-Alg_howie.pdf
	We are using an implementation of "Bug 2"
	*/
	public static void bugNavigate (MapLocation target) throws GameActionException {
		if (!target.equals(prevBugTarget)) {
			prevBugTarget = target;
			closestDistance = here.distanceSquaredTo(target) + 1;
		}

		Direction curDir = here.directionTo(target);
		MapLocation curDest = rc.adjacentLocation(curDir);
		int curDist = here.distanceSquaredTo(target);
		if (rc.canMove(curDir) && !rc.senseFlooding(curDest) && curDist < closestDistance) {
			rc.move(curDir);
		} else {
			int count = 1;
			curDir = curDir.rotateLeft();
			curDest = rc.adjacentLocation(curDir);
			while ((!rc.canMove(curDir) || rc.senseFlooding(curDest)) && count < 8) {
				curDir = curDir.rotateLeft();
				curDest = rc.adjacentLocation(curDir);
				count++;
			}
			if (count < 8) {
				rc.move(curDir);
			}
		}
		if (curDist < closestDistance) {
			closestDistance = curDist;
		}

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
			if (rc.senseFlooding(loc)) {
				danger = true;
				break;
			}
		}
		if (!danger) {
			return false;
		}

		System.out.println("This tile will be flooded next turn");
		if (!rc.isReady()) {
			System.out.println("Cooldown is not ready. I am dying to water! :(");
			return false;
		}

		int size = 8;
		int[] elevationDirection = new int[size];
		boolean[] dangerDirection = new boolean[size];
		if (false) { //(actualSensorRadiusSquared >= 8) { // 5x5 square centered at 'here'
			// remind Richard about this
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
				elevationDirection[index] = rc.senseElevation(loc);
				index++;
			}

			// sets dangerDirection to true if next turn, it will be flooded by an adjacent tile
			index = 0;
			for (Direction dir: directions) {
				MapLocation loc = rc.adjacentLocation(dir);
				if (rc.senseFlooding(loc)) {
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
				if (!dangerDirection[index] && rc.senseRobotAtLocation(loc) == null) {
					if (bestIndex == -1 || elevationDirection[index] > elevationDirection[bestIndex]) {
						bestIndex = index;
					}
				}
				index++;
			}
			if (bestIndex == -1) {
				System.out.println("ERROR: Failed sanity check - Cannot find safe direction");
			} else {
				rc.move(directions[bestIndex]);
				return true;
			}
		}

		return false;
	}
}
