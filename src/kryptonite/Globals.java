package kryptonite;

import battlecode.common.*;

public class Globals {

	public static RobotController rc;

	public static void init(RobotController theRC) {
		rc = theRC;
		here = rc.getLocation();
	}

	public static MapLocation here;

	public static void update() {
		here = rc.getLocation();
	}
}
