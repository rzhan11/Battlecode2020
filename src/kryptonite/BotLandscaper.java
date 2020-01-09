package kryptonite;

import battlecode.common.*;

public class BotLandscaper extends Globals {

	private static MapLocation HQLocation;
	private static MapLocation[] digSpots, smallWall, largeWall, completed;
	private static int lsCount, digSpotsLength, smallWallLength, largeWallLength, smallWallDepth, completedLength;


	public static void loop() throws GameActionException {
		while (true) {
			try {
				Globals.update();
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

					digSpots = new MapLocation[15];
					MapLocation templ = HQLocation.translate(3,3);
					int index = 0;
					for(int i = 0; i < 4; i++) for(int j = 0; j < 4; j++) {
						MapLocation newl = templ.translate(-2*i, -2*j);
						if(isInMap(newl) && !HQLocation.equals(newl)) {
							digSpots[index] = newl;
							index++;
						}
					}
					digSpotsLength = index;

					smallWall = new MapLocation[20];
					completed = new MapLocation[20];
					index = 0;
					templ = HQLocation.translate(2, 2);
					for(int i = 0; i < 5; i++) for(int j = 0; j < 5; j++) {
						MapLocation newl = templ.translate(-1 * i, -1 * j);
						if (isInMap(newl) && !HQLocation.equals(newl) && !inArray(digSpots, newl, digSpotsLength)) {
							smallWall[index] = newl;
							index++;
						}
					}
					smallWallLength = index;
					smallWallDepth = rc.senseElevation(HQLocation) + 3;
					completedLength = 0;

					largeWall = new MapLocation[56];
					index = 0;
					templ = HQLocation.translate(4, 4);
					for(int i = 0; i < 2; i++) for(int j = 0; j < 9; j++) {
						MapLocation newl = templ.translate(-1*i, -1*j);
						if(isInMap(newl) && !inArray(digSpots, newl, digSpotsLength)) {
							largeWall[index] = newl;
							index++;
						}
					}
					templ = HQLocation.translate(-4, -4);
					for(int i = 0; i < 2; i++) for(int j = 0; j < 9; j++) {
						MapLocation newl = templ.translate(i, j);
						if(isInMap(newl) && !inArray(digSpots, newl, digSpotsLength)) {
							largeWall[index] = newl;
							index++;
						}
					}
					templ = HQLocation.translate(2, 4);
					for(int i = 0; i < 5; i++) for(int j = 0; j < 2; j++) {
						MapLocation newl = templ.translate(-1*i, -1*j);
						if(isInMap(newl) && !inArray(digSpots, newl, digSpotsLength)) {
							largeWall[index] = newl;
							index++;
						}
					}
					templ = HQLocation.translate(2, -4);
					for(int i = 0; i < 5; i++) for(int j = 0; j < 2; j++) {
						MapLocation newl = templ.translate(-1*i, j);
						if(isInMap(newl) && !inArray(digSpots, newl, digSpotsLength)) {
							largeWall[index] = newl;
							index++;
						}
					}
					largeWallLength = index;
				}
				turn();
			} catch (Exception e) {
				e.printStackTrace();
			}
			Globals.endTurn();
		}
	}

	public static void turn() throws GameActionException {
		if(!rc.isReady()) return;
		// If in a Dig Spot Place, move to the Outward Radius
		if(inArray(digSpots, here, digSpotsLength)) {
			Direction move = HQLocation.directionTo(here);
			landscaperMove(move);
		}

		// If the First Wall isn't Complete and you can do stuff in your current position, do it
		if(!wallComplete()) {
			// Update Completed Array
			for(Direction d: Direction.allDirections()) {
				MapLocation tempLoc = here.add(d);
				if(inArray(smallWall, tempLoc, smallWallLength) && !inArray(completed, tempLoc, completedLength)) {
					if(rc.senseElevation(tempLoc) == smallWallDepth) {
						completed[completedLength] = tempLoc;
						completedLength++;
					}
				}
			}
			// Find an action to do
			boolean actionComplete = false;
			for(Direction d: Direction.allDirections()) {
				MapLocation tempLoc = here.add(d);
				if(inArray(smallWall, tempLoc, smallWallLength) && !inArray(completed, tempLoc, completedLength)) {
					if(rc.senseElevation(tempLoc) < smallWallDepth) {
						actionComplete = true;
						if(rc.getDirtCarrying() == 0) {
							landscaperDig();
						}
						else {
							if(rc.canDepositDirt(d)) rc.depositDirt(d);
						}
					}
					else if (rc.senseElevation(tempLoc) > smallWallDepth) {
						actionComplete = true;
						if(rc.canDigDirt(d)) rc.digDirt(d);
					}
				}
			}
			// The Wall here is Done, Rotate Clockwise
			if(!actionComplete) {
				Direction d = getClockwiseDir();
				MapLocation moveLoc = here.add(d);
				if(inArray(digSpots, moveLoc, digSpotsLength)) {
					// Go to Outer Ring
					Direction move = HQLocation.directionTo(here);
					landscaperMove(move);
				}
				else {
					if(rc.canMove(d)) rc.move(d);
 				}
			}
		}
		// Inner Wall Complete, Just Start Building Outer Wall
		else {
			// TODO: Implement
		}
	}

	private static boolean inArray(Object[] arr, Object item, int length) {
		for(int i = 0; i < length; i++) if(arr[i].equals(item)) return true;
		return false;
	}

	private static Direction getClockwiseDir() {
		int delx = HQLocation.x - here.x;
		int dely = HQLocation.y - here.y;
		int absx = Math.abs(delx);
		int absy = Math.abs(dely);
		boolean xPos = absx == delx;
		boolean yPos = absy == dely;

		if(absx == absy) {
			if(xPos && yPos) return Direction.SOUTH;
			if(xPos) return Direction.WEST;
			if(yPos) return Direction.EAST;
			return Direction.NORTH;
		}
		else if(absx > absy) {
			if(xPos) return Direction.SOUTH;
			return Direction.NORTH;
		}
		else {
			if(yPos) return Direction.EAST;
			return Direction.WEST;
		}
	}

	private static boolean wallComplete() {
		return smallWallLength == completedLength;
	}

	private static void landscaperMove(Direction d) throws GameActionException {
		if(rc.canMove(d)) rc.move(d);
		else if(rc.senseElevation(here.add(d)) > 3 + rc.senseElevation(here))
			if(rc.canDigDirt(d)) rc.digDirt(d);
		else
			if(rc.getDirtCarrying() >= 0)
				if(rc.canDepositDirt(d)) rc.depositDirt(d);
	}

	private static void landscaperDig() throws GameActionException {
		for(Direction d: Direction.allDirections()) {
			if(inArray(digSpots, here.add(d), digSpotsLength)) if(rc.canDigDirt(d)) rc.digDirt(d);
		}
	}
}
