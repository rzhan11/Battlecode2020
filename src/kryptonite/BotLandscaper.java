package kryptonite;

import battlecode.common.*;

public class BotLandscaper extends Globals {

	private static boolean firstTurn = true;
	private static MapLocation HQLocation, buildLocation;
	private static MapLocation[] wallPlaces, wall;
	private static int lsCount;


	public static void loop() throws GameActionException {
		while (true) {
			int startTurn = rc.getRoundNum();
			try {
				Globals.update();
				Globals.updateRobot();
				if (firstTurn) {
					// Identify HQ Location and Number of Prior Landscapers
					for (RobotInfo ri: visibleAllies) {
						if (ri.team == us && ri.type == RobotType.HQ) HQLocation = ri.location;
						if (ri.team == us && ri.type == RobotType.LANDSCAPER) lsCount++;
					}
					if (HQLocation == null) {
						Debug.tlogi("ERROR: Failed sanity check - Cannot find HQLocation");
					} else {
						Debug.tlog("HQ is located at " + HQLocation);
					}
					wallPlaces = new MapLocation[] {
							new MapLocation(HQLocation.x-2, HQLocation.y),
							new MapLocation(HQLocation.x-2, HQLocation.y-2),
							new MapLocation(HQLocation.x-2, HQLocation.y+2),
							new MapLocation(HQLocation.x+2, HQLocation.y),
							new MapLocation(HQLocation.x+2, HQLocation.y-2),
							new MapLocation(HQLocation.x+2, HQLocation.y+2),
							new MapLocation(HQLocation.x, HQLocation.y-2),
							new MapLocation(HQLocation.x, HQLocation.y+2),
					};
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
					buildLocation = wallPlaces[lsCount];
				}
				firstTurn = false;
				turn();
			} catch (Exception e) {
				e.printStackTrace();
			}
			Globals.endTurn();
		}
	}

	public static void turn() throws GameActionException {
		System.out.println("LSCOUNT: " + lsCount + " , LOCATION: " + buildLocation);
		if (here.equals(buildLocation)) {
			if (rc.getDirtCarrying() > 5) {
				int minEle = 10000;
				Direction minD = null;
				for (Direction d : Direction.allDirections()) {
					MapLocation dropLoc = new MapLocation(here.x + d.getDeltaX(), here.y + d.getDeltaY());
					if(inArray(wall, dropLoc)) {
						int dropEle = rc.senseElevation(dropLoc);
						if (dropEle < minEle) {
							minEle = dropEle;
							minD = d;
						}
					}
				}
				if (rc.canDepositDirt(minD)) rc.depositDirt(minD);
			} else {
				for (Direction d : Direction.allDirections()) {
					MapLocation digSpot = new MapLocation(here.x + d.getDeltaX(),here.y + d.getDeltaY());
					if (!inArray(wall, digSpot) && d != Direction.CENTER)
						if(HQLocation.distanceSquaredTo(digSpot) > HQLocation.distanceSquaredTo(here))
							if(rc.canDigDirt(d)) rc.digDirt(d);
				}
			}
		} else {
			// TODO: More intelligence path finding (can pickup and deposit dirt on its path)
			Nav.bugNavigate(buildLocation);
		}
	}

	private static boolean inArray(Object[] arr, Object item) {
		for(Object o : arr) {
			if (o.equals(item)) return true;
		}
		return false;
	}
}
