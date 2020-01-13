package kryptonite;

import battlecode.common.*;

public class BotBuilderMiner extends BotMiner {

	// private

	private static boolean designSchoolBuilt = false;
	private static boolean fulfillmentCenterBuilt = false;

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

	public static int[] maxNetGuns = {4,8};

	public static int maxVaporators = 4;

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
		if (teamSoup >= RobotType.FULFILLMENT_CENTER.cost + RobotType.REFINERY.cost + Communication.REFINERY_BUILT_COST && !fulfillmentCenterBuilt) {
			/*
			// potential bug - what if we are already on the fulfillmentCenterLocation?
			if (here.isAdjacentTo(fulfillmentCenterLocation)) {
				// build fulfillment center
				Direction dir = here.directionTo(fulfillmentCenterLocation);
				MapLocation loc = rc.adjacentLocation(dir);
				if (!rc.senseFlooding(loc) && Nav.checkElevation(loc) && rc.senseRobotAtLocation(loc) == null) {
					Debug.tlog("Building fulfillment center at " + loc);
					if (rc.isReady()) {
						rc.buildRobot(RobotType.FULFILLMENT_CENTER, dir);
						teamSoup = rc.getTeamSoup();
						fulfillmentCenterBuilt = true;
						Debug.ttlog("Success");
					} else {
						Debug.ttlog("But not ready");
					}
					return;
				}
			} else {
				Debug.tlog("Going to fulfillmentCenterLocation");
				moveLog(fulfillmentCenterLocation);
				return;
			}
			*/


			//find a spot in the 5x5 where it can build fulfillment center
			boolean built = false;
			for(int dx = -2; dx <= 2; dx++){
				for(int dy = -2; dy <= 2; dy++){
					MapLocation buildLocation = new MapLocation(HQLocation.x + dx, HQLocation.y+dy);
					Direction dir = here.directionTo(buildLocation);
					if(rc.canBuildRobot(RobotType.FULFILLMENT_CENTER,dir)){
						rc.buildRobot(RobotType.FULFILLMENT_CENTER,dir);
						built = true;
						teamSoup = rc.getTeamSoup();
						fulfillmentCenterBuilt = true;
						Debug.ttlog("Fulfillment Center Built");
						return;
					}
				}
			}
			if(!built){
				Debug.ttlog("Fulfillment Center Not Built");
				moveLog(HQLocation);
			}
		}

		// after the drone checkpoint has been reached, this fragment then builds the designSchool with the same cost requirements
		// as the fulfillment center
		else if (droneCheckpoint == 1 && !designSchoolBuilt) {
			if (teamSoup >= RobotType.DESIGN_SCHOOL.cost + RobotType.REFINERY.cost + Communication.REFINERY_BUILT_COST) {
				// potential bug - what if we are already on the designSchoolLocation?
				/*
				if (here.isAdjacentTo(designSchoolLocation)) {
					//build design school
					Direction dir = here.directionTo(designSchoolLocation);
					MapLocation loc = rc.adjacentLocation(dir);
					if (!rc.senseFlooding(loc) && Nav.checkElevation(loc) && rc.senseRobotAtLocation(loc) == null) {
						Debug.tlog("Building design school at " + loc);
						if (rc.isReady()) {
							rc.buildRobot(RobotType.DESIGN_SCHOOL, dir);
							teamSoup = rc.getTeamSoup();
							designSchoolBuilt = true;
							Debug.ttlog("Success");
						} else
							Debug.ttlog("But not ready");
						return;
					}

				} else {
					Debug.tlog("Going to designSchoolLocation");
					moveLog(designSchoolLocation);
					return;
				}
				*/
				//find a spot in the 5x5 where it can build fulfillment center
				boolean built = false;
				for(int dx = -2; dx <= 2; dx++){
					for(int dy = -2; dy <= 2; dy++){
						MapLocation buildLocation = new MapLocation(HQLocation.x + dx, HQLocation.y+dy);
						Direction dir = here.directionTo(buildLocation);
						if(rc.canBuildRobot(RobotType.DESIGN_SCHOOL,dir)){
							rc.buildRobot(RobotType.DESIGN_SCHOOL,dir);
							built = true;
							teamSoup = rc.getTeamSoup();
							designSchoolBuilt = true;
							Debug.ttlog("Design School Built");
							return;
						}
					}
				}
				if(!built){
					Debug.ttlog("Design School Not Built");
					moveLog(HQLocation);
				}
			}
		}
		// now building the 4 net guns then vaporators then 8 netguns
		else if (landscaperCheckpoint == 1) {
			if (netGunsBuilt < maxNetGuns[droneCheckpoint-1]) {
				/*
				MapLocation buildFromLocation = new MapLocation(HQLocation.x + dnetGunBuildLocations[netGunsBuilt][0], HQLocation.y + dnetGunBuildLocations[netGunsBuilt][1]);
				MapLocation buildAtLocation = new MapLocation(HQLocation.x + dnetGunLocations[netGunsBuilt][0], HQLocation.y + dnetGunLocations[netGunsBuilt][1]);
				if (here.equals(buildFromLocation)) {
					MapLocation loc = rc.adjacentLocation(here.directionTo(buildAtLocation));
					if (teamSoup >= RobotType.NET_GUN.cost + RobotType.REFINERY.cost + Communication.REFINERY_BUILT_COST
							&& !rc.senseFlooding(loc) && Nav.checkElevation(loc)
							&& rc.senseRobotAtLocation(loc) == null) {
						Debug.tlog("Building netgun at " + loc);
						if (rc.isReady()) {
							rc.buildRobot(RobotType.NET_GUN, here.directionTo(buildAtLocation));
							teamSoup = rc.getTeamSoup();
							netGunsBuilt++;
							if (netGunsBuilt >= maxNetGuns[1]) {
								Communication.writeTransactionNetgunCheckpoint();
							}
							Debug.ttlog("Success");
						} else
							Debug.ttlog("But not ready");
						return;
					}
				} else {
					Debug.tlog("Going to netgun location at " + buildFromLocation);
					moveLog(buildFromLocation);
					return;
				}*/

				int[][] netGunQuadrant = HardCode.netGunLU[netGunsBuilt%4];
				//loop through
				boolean built = false;
				for(int k = 0; k < 5; k++){
					MapLocation buildLocation = new MapLocation(HQLocation.x+netGunQuadrant[k][0],HQLocation.y+netGunQuadrant[k][1]);
					Direction dir = here.directionTo(buildLocation);
					if (here.isAdjacentTo(buildLocation) && teamSoup >= RobotType.NET_GUN.cost + RobotType.REFINERY.cost + Communication.REFINERY_BUILT_COST
							&& !rc.senseFlooding(buildLocation) && Nav.checkElevation(buildLocation)
							&& rc.senseRobotAtLocation(buildLocation) == null) {
						Debug.tlog("Building netgun at " + buildLocation);
						if (rc.isReady()) {
							rc.buildRobot(RobotType.NET_GUN, dir);
							teamSoup = rc.getTeamSoup();
							netGunsBuilt++;
							built = true;
							if (netGunsBuilt >= maxNetGuns[1]) {
								Communication.writeTransactionNetgunCheckpoint();
							}
							Debug.ttlog("Success");
							return;
						} else
							Debug.ttlog("But not ready");

					}
				}

				if(!built){
					moveLog(new MapLocation(HQLocation.x+HardCode.netGunBuildLocations[netGunsBuilt%4][0],HQLocation.y+HardCode.netGunBuildLocations[netGunsBuilt%4][1]));
				}


			}
			// now building the vaporators
			else if (vaporatorsBuilt < maxVaporators) {
				/*
				MapLocation buildFromLocation = new MapLocation(HQLocation.x + dvaporatorBuildLocations[vaporatorsBuilt][0],HQLocation.y + dvaporatorBuildLocations[vaporatorsBuilt][1]);
				MapLocation buildAtLocation = new MapLocation(HQLocation.x + dvaporatorLocations[vaporatorsBuilt][0],HQLocation.y + dvaporatorLocations[vaporatorsBuilt][1]);
				Debug.tlog("build from location " + buildFromLocation);
				if(here.equals(buildFromLocation)){
					MapLocation loc = rc.adjacentLocation(here.directionTo(buildAtLocation));
					if(teamSoup >= RobotType.VAPORATOR.cost + RobotType.REFINERY.cost + Communication.REFINERY_BUILT_COST
							&& !rc.senseFlooding(loc) && Nav.checkElevation(loc)
							&& rc.senseRobotAtLocation(loc) == null) {
						Debug.tlog("Building vaporator at " + loc);
						if (rc.isReady()) {
							rc.buildRobot(RobotType.VAPORATOR, here.directionTo(buildAtLocation));
							teamSoup = rc.getTeamSoup();
							vaporatorsBuilt++;
							if (vaporatorsBuilt >= maxVaporators) {
								Communication.writeTransactionVaporatorCheckpoint();
							}
							Debug.ttlog("Success");
						} else
							Debug.ttlog("But not ready");
						return;
					}
				} else {
					Debug.tlog("Moving to buildFromLocation at " + buildFromLocation);
					moveLog(buildFromLocation);
					return;
				}*/


				//find a spot in the 5x5 where it can build fulfillment center
				boolean built = false;
				for(int dx = -2; dx <= 2; dx++){
					for(int dy = -2; dy <= 2; dy++){
						MapLocation buildLocation = new MapLocation(HQLocation.x + dx, HQLocation.y+dy);
						Direction dir = here.directionTo(buildLocation);
						if(rc.canBuildRobot(RobotType.VAPORATOR,dir)){
							rc.buildRobot(RobotType.VAPORATOR,dir);
							built = true;
							teamSoup = rc.getTeamSoup();
							vaporatorsBuilt++;
							Debug.ttlog("VAPORATOR Built");
							return;
						}
					}
				}
				if(!built){
					Debug.ttlog("Vaporator Not Built");
					moveLog(HQLocation);
				}
			}
		}

		else if (landscaperCheckpoint == 2){
			Debug.tlog("Message Received");
		}

		return;


	}
}
