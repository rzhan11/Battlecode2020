package kryptonite;

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

		if (teamSoup >= RobotType.DESIGN_SCHOOL.cost && !designSchoolBuilt) {
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
		} else if (teamSoup >= RobotType.FULFILLMENT_CENTER.cost && !fulfillmentCenterBuilt) {
			// potential bug - what if we are already on the fulfillmentCenterLocation?
			if (here.isAdjacentTo(fulfillmentCenterLocation)) {
				// build fulfillment center
				Direction dir = here.directionTo(fulfillmentCenterLocation);
				if(rc.isReady() && rc.canBuildRobot(RobotType.FULFILLMENT_CENTER, dir)){
					rc.buildRobot(RobotType.FULFILLMENT_CENTER, dir);
					teamSoup = rc.getTeamSoup();
					fulfillmentCenterBuilt = true;
				}

			} else {
				Nav.bugNavigate(fulfillmentCenterLocation);
				Debug.tlog("Going to fulfillmentCenterLocation");
			}
		} else if ((netGunsBuilt < 4 || (netGunsBuilt < 8 && vaporatorsBuilt >= 4)) && fulfillmentCenterBuilt && designSchoolBuilt && teamSoup >= RobotType.NET_GUN.cost){
			// first build four netguns
			// then build eight netguns after four vaporators, fulfillment center, and design school are built
			MapLocation buildFromLocation = new MapLocation(HQLocation.x + dnetGunBuildLocations[netGunsBuilt][0],HQLocation.y + dnetGunBuildLocations[netGunsBuilt][1]);
			MapLocation buildAtLocation = new MapLocation(HQLocation.x + dnetGunLocations[netGunsBuilt][0],HQLocation.y + dnetGunLocations[netGunsBuilt][1]);
			if(here.equals(buildFromLocation)){
				if(rc.canBuildRobot(RobotType.NET_GUN,here.directionTo(buildAtLocation))){
					rc.buildRobot(RobotType.NET_GUN,here.directionTo(buildAtLocation));
					netGunsBuilt++;
				}
			} else {
				Nav.bugNavigate(buildFromLocation);
			}
		} else if(netGunsBuilt >= 4 && vaporatorsBuilt < 4){
			// build vaporators after four netguns have been built
			MapLocation buildFromLocation = new MapLocation(HQLocation.x + dvaporatorBuildLocations[vaporatorsBuilt][0],HQLocation.y + dvaporatorBuildLocations[vaporatorsBuilt][1]);
			MapLocation buildAtLocation = new MapLocation(HQLocation.x + dvaporatorLocations[vaporatorsBuilt][0],HQLocation.y + dvaporatorLocations[vaporatorsBuilt][1]);
			Debug.tlog("build from location " + buildFromLocation);
			if(here.equals(buildFromLocation)){
				if(rc.canBuildRobot(RobotType.VAPORATOR,here.directionTo(buildAtLocation))){
					rc.buildRobot(RobotType.VAPORATOR,here.directionTo(buildAtLocation));
					vaporatorsBuilt++;
				}
			} else {
				Debug.tlog("Moving to buildFromLocation at " + buildFromLocation);
				if (rc.isReady()) {
					Direction move = Nav.bugNavigate(buildFromLocation);
					if (move != null) {
						Debug.ttlog("Moved " + move);
					} else {
						Debug.ttlog("But no move found");
					}
				} else {
					Debug.ttlog("But not ready");
				}
			}
		}

	}
}
