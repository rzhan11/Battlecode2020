package kryptonite;

import battlecode.common.*;

public class BotMiner extends Globals {

	private static MapLocation HQLocation;

	// Miner moves in the direction that it was spawned in, until it sees a soup deposit or hits a map edge
	// If it hits a map edge, it will rotate left the currentExploringDirection

	private static Direction currentExploringDirection;

	// a soup deposit is a single soup location
	private static MapLocation soupDeposit = null;
	private static int soupCarrying;
	private static boolean adjacentToSoup = false;
	private static boolean returningToHQ = false;


	private static boolean designSchoolMaker = false;
	private static boolean designSchoolMade = false;
	private static MapLocation designSchoolLocation;

	public static void loop() throws GameActionException {

		while (true) {
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
						Debug.tlogi("ERROR: Failed sanity check - Cannot find HQLocation");
					} else {
						Debug.tlog("HQ is located at " + HQLocation);
					}
					currentExploringDirection = HQLocation.directionTo(here);

					//If this is the first robot made, designate that robot as the one to build the design school
					if(currentExploringDirection.equals(directions[0])){
						designSchoolMaker = true;
						designSchoolLocation = new MapLocation(HQLocation.x-1,HQLocation.y);
					}
				}

				turn();
			} catch (Exception e) {
				e.printStackTrace();
			}
			Globals.endTurn();
		}
	}

	public static void turn() throws GameActionException {

		if(teamSoup >= RobotType.DESIGN_SCHOOL.cost && designSchoolMaker && !designSchoolMade) {
			if(here.isAdjacentTo(designSchoolLocation)){
				//build design school
				if(rc.isReady() && rc.canBuildRobot(RobotType.DESIGN_SCHOOL,here.directionTo(designSchoolLocation))){
					rc.buildRobot(RobotType.DESIGN_SCHOOL,here.directionTo(designSchoolLocation));
					designSchoolMade = true;
				}

			} else {
				Nav.bugNavigate(designSchoolLocation);
				Debug.tlog("Going to Design School Location");
			}
			return;
		}

		soupCarrying = rc.getSoupCarrying();
		adjacentToSoup = soupDeposit != null && here.isAdjacentTo(soupDeposit);

		Debug.tlog("soupCarrying: " + soupCarrying);

		Nav.avoidWater();

		/*
		Check if soupDeposit is depleted or if we are carrying maximum soup
		*/
		if (soupDeposit != null && adjacentToSoup && rc.senseSoup(soupDeposit) == 0) {
			soupDeposit = null;
			adjacentToSoup = false;
			if (soupCarrying > 0) {
				returningToHQ = true;
			}
		}
		if (soupCarrying == RobotType.MINER.soupLimit) {
			returningToHQ = true;
		}

		/*
		Bug navigate to HQLocation and deposit soup
		*/
		if (returningToHQ) {
			if (here.isAdjacentTo(HQLocation)) {
				if (rc.isReady()) {
					rc.depositSoup(here.directionTo(HQLocation), soupCarrying);
					returningToHQ = false;
					Debug.tlog("Deposited " + soupCarrying + " soup at HQ");
				}
				return;
			}
			Nav.bugNavigate(HQLocation);
			Debug.tlog("Returning to HQ");
			return;
		}

		/*
		mine dat soup
		*/
		if (adjacentToSoup) {
			Debug.tlog("Mining soup");
			if (rc.isReady()) {
				rc.mineSoup(here.directionTo(soupDeposit));
			}
			return;
		}

		/*
		if I have already found a soup deposit or I see a soup deposit
			flag this event and bug-nav towards it
		else
			move in the currentExploringDirection if possible
			if I encounter a map edge, rotateLeft the currentExploringDirection
			if I encounter a water/elevation obstacle, bug-nav around it
		*/

		if (soupDeposit == null) {
			if (Communication.soupClustersIndex < Communication.soupClustersSize) {
				soupDeposit = Communication.soupClusters[Communication.soupClustersIndex];
				Communication.soupClustersIndex++;
				Debug.tlog("Using soupCluster at " + soupDeposit);
			} else {
				Debug.tlog("Found soupDeposit at " + soupDeposit);
				locateOpenSoupDeposit();
			}
		}

		if (soupDeposit != null) {
			if (rc.isReady()) {
				Nav.bugNavigate(soupDeposit);
			}
			Debug.tlog("Going to soupDeposit at " + soupDeposit);

		} else { // explore
			if (rc.isReady()) {
				MapLocation dest = rc.adjacentLocation(currentExploringDirection);
				// finds a Direction that points to a valid MapLocation
				int count = 0; // technically not needed in non 1x1 maps
				while (!rc.onTheMap(dest) && count < 4) {
					currentExploringDirection = currentExploringDirection.rotateLeft();
					currentExploringDirection = currentExploringDirection.rotateLeft();
					dest = rc.adjacentLocation(currentExploringDirection);
					count++;
				}
				// currentExploringDirection = currentExploringDirection.rotateLeft();
				// while (!rc.onTheMap(dest) && count < 4) {
				// 	currentExploringDirection = currentExploringDirection.rotateLeft();
				// 	currentExploringDirection = currentExploringDirection.rotateLeft();
				// 	dest = rc.adjacentLocation(currentExploringDirection);
				// 	count++;
				// }

				Debug.tlog("Exploring " + currentExploringDirection);

				count = 0;
				Direction curDir = currentExploringDirection;
				while ((!rc.canMove(curDir) || rc.senseFlooding(dest)) && count < 8) {
					curDir = curDir.rotateLeft();
					dest = rc.adjacentLocation(curDir);
					count++;
				}
				if (count < 8) {
					rc.move(curDir);
				}
			}
			myElevation = rc.senseElevation(here);
		}
	}

	/*
	Returns false if no soup deposit was found
	Returns true if soup deposit was found and also saves the MapLocation in variable 'soupDeposit'
	*/
	public static boolean locateOpenSoupDeposit() throws GameActionException {
		for (int[] pair: sensableDirections) {
			MapLocation loc = here.translate(pair[0], pair[1]);
			if (rc.canSenseLocation(loc) && rc.senseSoup(loc) > 0) {
				soupDeposit = loc;
				Communication.writeClusterTransaction(loc.x, loc.y);
				return true;
			}
		}
		return false;
	}


}
