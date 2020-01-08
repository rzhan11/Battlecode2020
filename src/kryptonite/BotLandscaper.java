package kryptonite;

import battlecode.common.*;

public class BotLandscaper extends Globals {

	private static MapLocation HQLocation;
	private static MapLocation buildLocation;
	private static MapLocation[] wall;


	public static void loop() throws GameActionException {
		while (true) {
			int startTurn = rc.getRoundNum();
			try {
				Globals.update();
				Globals.updateRobot();
				if (firstTurn) {
					// Identify HQ Location and Number of Prior Landscapers
					int lsCount = 0;
					for (RobotInfo ri: visibleAllies) {
						if (ri.team == us && ri.type == RobotType.HQ) HQLocation = ri.location;
						if(ri.type == RobotType.LANDSCAPER) lsCount++;
					}
					if (HQLocation == null) {
						Debug.tlogi("ERROR: Failed sanity check - Cannot find HQLocation");
					} else {
						Debug.tlog("HQ is located at " + HQLocation);
					}
					wall = new  MapLocation[] {
							new MapLocation(HQLocation.x-2, HQLocation.y),
							new MapLocation(HQLocation.x-2, HQLocation.y-1),
							new MapLocation(HQLocation.x-2, HQLocation.y-2),
							new MapLocation(HQLocation.x-2, HQLocation.y+1),
							new MapLocation(HQLocation.x-2, HQLocation.y+2),
							new MapLocation(HQLocation.x+2, HQLocation.y),
							new MapLocation(HQLocation.x+2, HQLocation.y-1),
							new MapLocation(HQLocation.x+2, HQLocation.y-2),
							new MapLocation(HQLocation.x+2, HQLocation.y+1),
							new MapLocation(HQLocation.x+2, HQLocation.y+2),
							new MapLocation(HQLocation.x-1, HQLocation.y-2),
							new MapLocation(HQLocation.x-1, HQLocation.y+2),
							new MapLocation(HQLocation.x+1, HQLocation.y-2),
							new MapLocation(HQLocation.x+1, HQLocation.y+2),
							new MapLocation(HQLocation.x, HQLocation.y-2),
							new MapLocation(HQLocation.x, HQLocation.y+2),
					};
					// Determine Assigned Block
					/*
					Ideal Layout
					[A3][A6][A5][A2][B1]
					[A7][DA][EM][EM][B4]
					[A8][EM][HQ][EM][B8]
					[A4][EM][EM][FC][B7]
					[A1][B2][B5][B6][B3]
					*/
					boolean isA = true;
					MapLocation curloc = rc.getLocation();
					buildLocation = wall[lsCount];
				}

				turn();
			} catch (Exception e) {
				e.printStackTrace();
			}
			Globals.endTurn();
		}
	}

	public static void turn() throws GameActionException {

	}
}
