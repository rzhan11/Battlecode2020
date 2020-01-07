package kryptonite;

import battlecode.common.*;

public class BotMiner extends Globals {

	private static boolean firstTurn = true;

	private static MapLocation HQLocation;

	// Miner moves in the direction that it was spawned in, until it sees a soup deposit or hits a map edge
	// If it hits a map edge, it will rotate left the currentExploringDirection

	private static Direction currentExploringDirection;

	private static MapLocation locatedSoupDeposit = null;

	public static void loop() throws GameActionException {

		while (true) {
			int startTurn = rc.getRoundNum();
			try {
				Globals.update();
				Globals.updateRobot();

				// Do first turn code here
				if (firstTurn) {
					// identify HQ MapLocation
					for (RobotInfo ri: visibleAllies) {
						if (ri.team == us && ri.type == RobotType.HQ) {
							HQLocation = ri.location;
						}
					}
					if (HQLocation == null) {
						System.out.println("ERROR: Failed sanity check - Cannot find HQLocation");
					} else {
						System.out.println("HQ is located at " + HQLocation);
					}
					currentExploringDirection = HQLocation.directionTo(here);
					System.out.println("Exploring " + currentExploringDirection);
				}

			    turn();
				firstTurn = false;
			} catch (Exception e) {
				e.printStackTrace();
			}
			int endTurn = rc.getRoundNum();
			if (startTurn != endTurn) {
				System.out.println("OVER BYTECODE LIMIT");
			}
			Clock.yield();
		}
	}

	public static void turn() throws GameActionException {
		/*
		if I have already found a soup deposit or I see a soup deposit
			flag this event and bug-nav towards it
		else
			move in the currentExploringDirection if possible
			if I encounter a map edge, rotateLeft the currentExploringDirection
			if I encounter a water/elevation obstacle, bug-nav around it
		*/

		if (locatedSoupDeposit != null || locateSoupDeposit()) {
			System.out.println("Found soup at " + locatedSoupDeposit);
		} else {
			if (rc.isReady()) {
				int count = 0;
				Direction curDir = currentExploringDirection;
				while (!rc.canMove(curDir) && count < 8) {
					curDir = curDir.rotateLeft();
					count++;
				}
				if (count < 8) {
					rc.move(curDir);
				}
			}
		}
	}

	/*
	To be implemented
	*/
	public static void bugNavigate (MapLocation target) throws GameActionException {


	}

	/*
	Returns false if no soup deposit was found
	Returns true if soup deposit was found and also saves the MapLocation in variable 'locatedSoupDeposit'
	*/
	public static boolean locateSoupDeposit() throws GameActionException {
		for (int[] pair: sensableDirections) {
			MapLocation loc = new MapLocation(here.x + pair[0], here.y + pair[1]);
			if (rc.senseSoup(loc) >= 0) {
				locatedSoupDeposit = loc;
				return true;
			}
		}
		return false;
	}
}
