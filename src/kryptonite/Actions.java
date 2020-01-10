package kryptonite;

import battlecode.common.*;

public class Actions extends Globals {

	public static int[] YELLOW = {255, 255, 0};

	public static void doMove (Direction dir) throws GameActionException {
		rc.setIndicatorLine(here, rc.adjacentLocation(dir), YELLOW[0], YELLOW[1], YELLOW[2]);
		rc.move(dir);
	}
}
