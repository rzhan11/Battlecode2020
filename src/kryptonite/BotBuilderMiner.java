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

		Debug.tlog("checkpoints: ");
		Debug.ttlog("drone " + reachedDroneCheckpoint);
		Debug.ttlog("landscaper " + reachedLandscaperCheckpoint);
		Debug.ttlog("netgun " + reachedNetgunCheckpoint);
		Debug.ttlog("vaporator " + reachedVaporatorCheckpoint);

		// first step is to build the fulfillment center
		// This fragment of code checks to build the center if the cost of the refinery, sending that signal and the fufillment center are all
		// less than teamSoup

		if (!fulfillmentCenterBuilt) {
			Debug.tlog("Trying for fulfillmentCenter");
			if (teamSoup >= RobotType.FULFILLMENT_CENTER.cost + RobotType.REFINERY.cost) {

				//find a spot in the 5x5 where it can build fulfillment center
				boolean built = false;
				for (Direction dir: directions) {
					MapLocation buildLocation = rc.adjacentLocation(dir);
					// forces it to be on 5x5 ring
					if (maxXYDistance(HQLocation, buildLocation) != 2) {
						continue;
					}
					Debug.tlog("working " + buildLocation);
					if(rc.canBuildRobot(RobotType.FULFILLMENT_CENTER,dir)){
						Actions.doBuildRobot(RobotType.FULFILLMENT_CENTER,dir);
						built = true;
						teamSoup = rc.getTeamSoup();
						fulfillmentCenterBuilt = true;
						Debug.ttlog("Fulfillment Center Built");
						return;
					}
				}
				if(!built){
					Debug.ttlog("Fulfillment Center Not Built");

					int dx = HQLocation.x - here.x;
					int dy = HQLocation.y - here.y;
					MapLocation reflectLoc = new MapLocation(HQLocation.x + dx, HQLocation.y + dy);

					Debug.tlog("Going to reflection at " + reflectLoc);
					moveLog(reflectLoc);
				}
			} else {
				Debug.tlog("Not enough soup for Fulfillment Center + Refinery");
			}
			return;
		}

		// after the drone checkpoint has been reached, this fragment then builds the designSchool with the same cost requirements as the fulfillment center
		if (!designSchoolBuilt) {
			Debug.tlog("Trying for designSchool");
			if (reachedDroneCheckpoint == 0) {
				if (teamSoup >= RobotType.DESIGN_SCHOOL.cost + RobotType.REFINERY.cost) {
					// potential bug - what if we are already on the designSchoolLocation?
					//find a spot in the 5x5 where it can build fulfillment center
					boolean built = false;
					for (Direction dir: directions) {
						MapLocation buildLocation = rc.adjacentLocation(dir);
						// forces it to be on 5x5 ring
						if (maxXYDistance(HQLocation, buildLocation) != 2) {
							continue;
						}
						Debug.tlog("working " + buildLocation);
						if(rc.canBuildRobot(RobotType.DESIGN_SCHOOL,dir)){
							Actions.doBuildRobot(RobotType.DESIGN_SCHOOL,dir);
							built = true;
							teamSoup = rc.getTeamSoup();
							designSchoolBuilt = true;
							Debug.ttlog("Design School Built");
							return;
						}
					}
					if(!built){
						Debug.ttlog("Design School Not Built");

						int dx = HQLocation.x - here.x;
						int dy = HQLocation.y - here.y;
						MapLocation reflectLoc = new MapLocation(HQLocation.x + dx, HQLocation.y + dy);

						Debug.tlog("Going to reflection at " + reflectLoc);
						moveLog(reflectLoc);
					}
				} else {
					Debug.tlog("Not enough soup for Design School + Refinery");
				}
			}
			return;
		}

		// now building the 4 net guns then vaporators then 8 netguns
		if (netGunsBuilt < maxNetGuns[0]) {
			Debug.tlog("Attempting net guns phase 0");
			if (reachedLandscaperCheckpoint == 0) {
				Debug.tlog("landscaper checkpoint 0 met");
				int[][] netGunQuadrant = HardCode.netGunLU[netGunsBuilt % 4];
				//loop through
				boolean built = false;
				for (int k = 0; k < netGunQuadrant.length; k++) {
					MapLocation buildLocation = new MapLocation(HQLocation.x + netGunQuadrant[k][0], HQLocation.y + netGunQuadrant[k][1]);
					Direction dir = here.directionTo(buildLocation);
					if (here.isAdjacentTo(buildLocation) && teamSoup >= RobotType.NET_GUN.cost + RobotType.REFINERY.cost
							&& !rc.senseFlooding(buildLocation) && Nav.checkElevation(buildLocation)
							&& rc.senseRobotAtLocation(buildLocation) == null) {
						Debug.tlog("Building netgun at " + buildLocation);
						if (rc.isReady()) {
							Actions.doBuildRobot(RobotType.NET_GUN, dir);
							teamSoup = rc.getTeamSoup();
							netGunsBuilt++;
							built = true;
							if (netGunsBuilt >= maxNetGuns[0]) {
								Communication.writeTransactionNetgunCheckpoint();
							}
							Debug.ttlog("Success");
							return;
						} else {
							Debug.ttlog("But not ready");
						}
					}
				}

				if (!built) {
					MapLocation loc = new MapLocation(HQLocation.x + HardCode.netGunBuildLocations[netGunsBuilt % 4][0], HQLocation.y + HardCode.netGunBuildLocations[netGunsBuilt % 4][1]);
					Debug.ttlog("Moving to " + loc);
					moveLog(loc);
				}
			} else {
				Debug.tlog("landscaper checkpoint 0 not met");
			}
			return;
		}

		// now building the vaporators
		if (vaporatorsBuilt < maxVaporators) {
			Debug.tlog("Attempting vaporators");
			if (reachedLandscaperCheckpoint == 0) {
				Debug.tlog("landscaper checkpoint 0 met");
				//find a spot in the 5x5 where it can build fulfillment center
				boolean built = false;
				for (int dx = -2; dx <= 2; dx++) {
					for (int dy = -2; dy <= 2; dy++) {
						MapLocation buildLocation = new MapLocation(HQLocation.x + dx, HQLocation.y + dy);
						// vaporators must be adjacent to HQ
						if (!buildLocation.isAdjacentTo(HQLocation)) {
							continue;
						}
						Direction dir = here.directionTo(buildLocation);
						if (here.isAdjacentTo(buildLocation) && teamSoup >= RobotType.VAPORATOR.cost + RobotType.REFINERY.cost
								&& !rc.senseFlooding(buildLocation) && Nav.checkElevation(buildLocation)
								&& rc.senseRobotAtLocation(buildLocation) == null) {
							Debug.tlog("Building vaporator at " + buildLocation);
							if (rc.isReady()) {
								Actions.doBuildRobot(RobotType.VAPORATOR, dir);
								built = true;
								teamSoup = rc.getTeamSoup();
								vaporatorsBuilt++;
								Debug.ttlog("VAPORATOR Built");
								if (vaporatorsBuilt >= maxVaporators) {
									Communication.writeTransactionVaporatorCheckpoint();
								}
								return;
							}
						}
					}
				}
				if (!built) {
					Debug.ttlog("Vaporator Not Built");
					if (teamSoup >= RobotType.VAPORATOR.cost + RobotType.REFINERY.cost) {
						moveLog(HQLocation);
					}
				}
			} else {
				Debug.tlog("landscaper checkpoint not met");
			}
			return;
		}

		// now build 8 netguns
		if (netGunsBuilt < maxNetGuns[1]) {
			Debug.tlog("Attempting net guns phase 1");
			if (reachedDroneCheckpoint == 1) {
				Debug.tlog("drone checkpoint 1 met");
				int[][] netGunQuadrant = HardCode.netGunLU[netGunsBuilt % 4];
				//loop through
				boolean built = false;
				for (int k = 0; k < netGunQuadrant.length; k++) {
					MapLocation buildLocation = new MapLocation(HQLocation.x + netGunQuadrant[k][0], HQLocation.y + netGunQuadrant[k][1]);
					Direction dir = here.directionTo(buildLocation);
					if (here.isAdjacentTo(buildLocation) && teamSoup >= RobotType.NET_GUN.cost + RobotType.REFINERY.cost
							&& !rc.senseFlooding(buildLocation) && Nav.checkElevation(buildLocation)
							&& rc.senseRobotAtLocation(buildLocation) == null) {
						Debug.tlog("Building netgun at " + buildLocation);
						if (rc.isReady()) {
							Actions.doBuildRobot(RobotType.NET_GUN, dir);
							teamSoup = rc.getTeamSoup();
							netGunsBuilt++;
							built = true;
							if (netGunsBuilt >= maxNetGuns[1]) {
								Communication.writeTransactionNetgunCheckpoint();
							}
							Debug.ttlog("Success");
							return;
						} else {
							Debug.ttlog("But not ready");
						}
					}
				}

				if (!built) {
					MapLocation loc = new MapLocation(HQLocation.x + HardCode.netGunBuildLocations[netGunsBuilt % 4][0], HQLocation.y + HardCode.netGunBuildLocations[netGunsBuilt % 4][1]);
					Debug.ttlog("Moving to " + loc);
					moveLog(loc);
				}
			} else {
				Debug.tlog("Drone checkpoint 1 not met");
			}
			return;
		}

		if (reachedLandscaperCheckpoint == 1){
			Debug.tlog("Landscaper checkpoint received");
		}

		return;


	}
}
