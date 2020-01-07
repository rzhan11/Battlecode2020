package kryptonite;

import battlecode.common.*;

public class Nav extends Globals {

	private static MapLocation prevBugTarget = null;
	private static int closestDistance;

	/*
	To be implemented
	*/
	public static void bugNavigate (MapLocation target) throws GameActionException {
		if (!target.equals(prevBugTarget)) {
			prevBugTarget = target;
			closestDistance = here.distanceSquaredTo(target) + 1;
		}

		Direction curDir = here.directionTo(target);
		MapLocation curDest = here.add(curDir);
		int curDist = here.distanceSquaredTo(target);
		if (rc.canMove(curDir) && !rc.senseFlooding(curDest) && curDist < closestDistance) {
			rc.move(curDir);
		} else {
			int count = 1;
			curDir = curDir.rotateLeft();
			curDest = here.add(curDir);
			while ((!rc.canMove(curDir) || rc.senseFlooding(curDest)) && count < 8) {
				curDir = curDir.rotateLeft();
				curDest = here.add(curDir);
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

}
