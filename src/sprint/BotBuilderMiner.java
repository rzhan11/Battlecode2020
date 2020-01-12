package sprint;

import battlecode.common.*;

public class BotBuilderMiner extends BotMiner {

	// private

	private static boolean designSchoolBuilt = false;
	private static boolean fulfillmentCenterBuilt = false;

	public static MapLocation buildRefineryLocation = null;
	public static int buildRefineryVisibleSoup;

	private static MapLocation designSchoolLocation;
	private static MapLocation fulfillmentCenterLocation;

	private static int netGunsBuilt = 0;
	private static int[][] dnetGunLocations;					//where netguns are to be built relative to hq
	private static int[][] dnetGunBuildLocations;			//where builderminer is when it builds netguns relative to hq

	private static int vaporatorsBuilt = 0;
	private static int[][] dvaporatorLocations;					//where vaporators are to be built relative to hq
	private static int[][] dvaporatorBuildLocations;			//where builderminer is when it builds vaporator relative to hq

	private static boolean initialized = false;

	public static int droneCheckpoint = 0;
	public static int landscaperCheckpoint = 0;

	public static void init() throws GameActionException {
		// hardcoded locations of design school and fulfillment center
		designSchoolLocation = new MapLocation(HQLocation.x-1,HQLocation.y);
		fulfillmentCenterLocation = new MapLocation(HQLocation.x+1,HQLocation.y);

		dnetGunBuildLocations = new int[][]{{-2,2},{-2,-2},{2,-2},{2,2},{-2,2},{-2,-2},{2,-2},{2,2}};
		dnetGunLocations = new int[][]{{-2,3},{-2,-3},{2,-3},{2,3},{-3,2},{-3,-2},{3,-2},{3,2}};

		dvaporatorBuildLocations = new int[][]{{0,-1},{0,-1},{0,1},{0,1}};
		dvaporatorLocations = new int[][]{{-1,-1},{1,-1},{-1,1},{1,1}};

		initialized = true;
	}

	public static void turn() throws GameActionException {
		if (!initialized) {
			init();
		}
		// first step is to build the fulfillment center
		// This fragment of code checks to build the center if the cost of the refinery, sending that signal and the fufillment center are all
		// less than teamSoup
		if (!fulfillmentCenterBuilt) {
			if (teamSoup >= RobotType.FULFILLMENT_CENTER.cost + RobotType.REFINERY.cost + Communication.REFINERY_BUILT_COST) {
				// potential bug - what if we are already on the fulfillmentCenterLocation?
				if (here.isAdjacentTo(fulfillmentCenterLocation)) {
					// build fulfillment center
					Direction dir = here.directionTo(fulfillmentCenterLocation);
					if (rc.isReady() && rc.canBuildRobot(RobotType.FULFILLMENT_CENTER, dir)) {
						rc.buildRobot(RobotType.FULFILLMENT_CENTER, dir);
						teamSoup = rc.getTeamSoup();
						fulfillmentCenterBuilt = true;
					}
				} else {
					Nav.bugNavigate(fulfillmentCenterLocation);
					Debug.tlog("Going to fulfillmentCenterLocation");
				}

			} else {
				return;
			}
		// after the drone checkpoint has been reached, this fragment then builds the designSchool with the same cost requirements
		// as the fulfillment center
		} else if (droneCheckpoint == 1 && !designSchoolBuilt) {
			if (teamSoup >= RobotType.DESIGN_SCHOOL.cost + RobotType.REFINERY.cost + Communication.REFINERY_BUILT_COST) {
				// potential bug - what if we are already on the designSchoolLocation?
				if(here.isAdjacentTo(designSchoolLocation)){
					//build design school
					Direction dir = here.directionTo(designSchoolLocation);
					if (rc.canBuildRobot(RobotType.DESIGN_SCHOOL, dir)) {
						rc.buildRobot(RobotType.DESIGN_SCHOOL, dir);
						teamSoup = rc.getTeamSoup();
						designSchoolBuilt = true;
					}

				} else {
					Nav.bugNavigate(designSchoolLocation);
					Debug.tlog("Going to designSchoolLocation");
				}
			}
		} else if (landscaperCheckpoint == 1) {
			Debug.tlog("Message received");
		}


	}
}
