package seeding;

import battlecode.common.*;

import static seeding.Debug.*;
import static seeding.Map.*;

public class BotMinerBuilder extends BotMiner {

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
		designSchoolLocation = new MapLocation(HQLoc.x-1, HQLoc.y);
		fulfillmentCenterLocation = new MapLocation(HQLoc.x+1, HQLoc.y);

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

		if (!rc.isReady()) {
			log("Not ready");
			return;
		}

//		log("checkpoints: ");
//		tlog("drone " + reachedDroneCheckpoint);
//		tlog("landscaper " + reachedLandscaperCheckpoint);
//		tlog("netgun " + reachedNetgunCheckpoint);
//		tlog("vaporator " + reachedVaporatorCheckpoint);

		// build design school if possible
		if (!designSchoolBuilt) {
			log("Trying for designSchool");
			for (Direction dir : directions) {
				MapLocation loc = rc.adjacentLocation(dir);
				if (!rc.onTheMap(loc)) {
					continue;
				}
				if (maxXYDistance(HQLoc, loc) == 3 && isBuildLocation(loc)) {
					if (isDirDryFlatEmpty(dir)) {
						if (rc.getTeamSoup() >= RobotType.DESIGN_SCHOOL.cost) {
							Actions.doBuildRobot(RobotType.DESIGN_SCHOOL, dir);
							designSchoolBuilt = true;
							tlog("Design School Built");
						} else {
							tlog("In position for Design School, not enough soup");
						}
						// return if in position/built it
						return;
					}
				}
			}

			//find a spot in the 7x7 where it can build design school
			for (int[] dir : senseDirections) {
				// ignore locs that are out of sensor range or within build range (since they are not flat)
				if (dir[2] <= 2 || actualSensorRadiusSquared < dir[2]) {
					continue;
				}
				MapLocation buildLocation = here.translate(dir[0], dir[1]);
				if (!rc.onTheMap(buildLocation)) {
					continue;
				}
				// forces it to be on 7x7 ring
				if (maxXYDistance(HQLoc, buildLocation) == 3 && isBuildLocation(buildLocation)) {
					if (isLocDryEmpty(buildLocation)) {
						log("Moving to build design school");
						moveLog(buildLocation);
						return;
					}
				}
			}
			return;
		}

		if (!fulfillmentCenterBuilt) {
			log("Trying for fulfillment center");
			for (Direction dir : directions) {
				MapLocation loc = rc.adjacentLocation(dir);
				if (!rc.onTheMap(loc)) {
					continue;
				}
				if (maxXYDistance(HQLoc, loc) == 3 && isBuildLocation(loc)) {
					if (isDirDryFlatEmpty(dir)) {
						if (rc.getTeamSoup() >= RobotType.FULFILLMENT_CENTER.cost) {
							Actions.doBuildRobot(RobotType.FULFILLMENT_CENTER, dir);
							fulfillmentCenterBuilt = true;
							tlog("Fulfillment center built");
						} else {
							tlog("In position for fulfillment center, not enough soup");
						}
						// return if in position/built it
						return;
					}
				}
			}

			//find a spot in the 7x7 where it can build fulfillment center
			for (int[] dir : senseDirections) {
				// ignore locs that are out of sensor range or within build range (since they are not flat)
				if (dir[2] <= 2 || actualSensorRadiusSquared < dir[2]) {
					continue;
				}
				MapLocation buildLocation = here.translate(dir[0], dir[1]);
				if (!rc.onTheMap(buildLocation)) {
					continue;
				}
				// forces it to be on 7x7 ring
				if (maxXYDistance(HQLoc, buildLocation) == 3 && isBuildLocation(buildLocation)) {
					if (isLocDryEmpty(buildLocation)) {
						log("Moving to build fulfillment center");
						moveLog(buildLocation);
						return;
					}
				}
			}
			return;
		}

		// unsets role as builder miner
		// turns builder miner back into normal miner
		builderMinerID = -1;


		/*
		if (reachedLandscaperCheckpoint >= 0) {
			log("Continuing: Landscaper checkpoint 0 reached");
		} else {
			log("Returning: Landscaper checkpoint 0 not reached");
			return;
		}
		// now building the 4 net guns
		if (netGunsBuilt < maxNetGuns[0]) {
			log("Attempting net guns phase 0");
			int[][] netGunQuadrant = HardCode.netGunLU[netGunsBuilt % 4];
			//loop through
			boolean built = false;
			for (int k = 0; k < netGunQuadrant.length; k++) {
				MapLocation buildLocation = new MapLocation(HQLocation.x + netGunQuadrant[k][0], HQLocation.y + netGunQuadrant[k][1]);
				Direction dir = here.directionTo(buildLocation);
				if (here.isAdjacentTo(buildLocation) && rc.getTeamSoup() >= RobotType.NET_GUN.cost + RobotType.REFINERY.cost
						&& !rc.senseFlooding(buildLocation) && isLocFlat(buildLocation)
						&& rc.senseRobotAtLocation(buildLocation) == null) {
					log("Building netgun at " + buildLocation);
					Actions.doBuildRobot(RobotType.NET_GUN, dir);

					netGunsBuilt++;
					built = true;
					return;
				}
			}

			if (!built) {
				MapLocation loc = new MapLocation(HQLocation.x + HardCode.netGunBuildLocations[netGunsBuilt % 4][0], HQLocation.y + HardCode.netGunBuildLocations[netGunsBuilt % 4][1]);
				tlog("Moving to " + loc);
				moveLog(loc);
			}
			return;
		}

		// now building the vaporators
		if (vaporatorsBuilt < maxVaporators) {
			log("Attempting vaporators");
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
					if (here.isAdjacentTo(buildLocation) && rc.getTeamSoup() >= RobotType.VAPORATOR.cost + RobotType.REFINERY.cost
							&& !rc.senseFlooding(buildLocation) && isLocFlat(buildLocation)
							&& rc.senseRobotAtLocation(buildLocation) == null) {
						log("Building vaporator at " + buildLocation);
						Actions.doBuildRobot(RobotType.VAPORATOR, dir);
						built = true;

						vaporatorsBuilt++;
						tlog("VAPORATOR Built");
						if (vaporatorsBuilt >= maxVaporators) {
							writeTransactionVaporatorCheckpoint();
						}
						return;
					}
				}
			}
			if (!built) {
				tlog("Vaporator Not Built");
				if (rc.getTeamSoup() >= RobotType.VAPORATOR.cost + RobotType.REFINERY.cost) {
					moveLog(HQLocation);
				}
			}
			return;
		}

		if (reachedDroneCheckpoint >= 1) {
			log("Continuing: Drone checkpoint 1 reached");
		} else {
			log("Returning: Drone checkpoint 1 not reached");
			return;
		}

		// now build 8 netguns
		if (netGunsBuilt < maxNetGuns[1]) {
			log("Attempting net guns phase 1");
			int[][] netGunQuadrant = HardCode.netGunLU[netGunsBuilt % 4];
			//loop through
			boolean built = false;
			for (int k = 0; k < netGunQuadrant.length; k++) {
				MapLocation buildLocation = new MapLocation(HQLocation.x + netGunQuadrant[k][0], HQLocation.y + netGunQuadrant[k][1]);
				Direction dir = here.directionTo(buildLocation);
				if (here.isAdjacentTo(buildLocation) && rc.getTeamSoup() >= RobotType.NET_GUN.cost + RobotType.REFINERY.cost
						&& !rc.senseFlooding(buildLocation) && isLocFlat(buildLocation)
						&& rc.senseRobotAtLocation(buildLocation) == null) {
					log("Building netgun at " + buildLocation);
					Actions.doBuildRobot(RobotType.NET_GUN, dir);

					netGunsBuilt++;
					built = true;
					if (netGunsBuilt >= maxNetGuns[1]) {
						writeTransactionNetgunCheckpoint();
					}
					return;
				}
			}

			if (!built) {
				MapLocation loc = new MapLocation(HQLocation.x + HardCode.netGunBuildLocations[netGunsBuilt % 4][0], HQLocation.y + HardCode.netGunBuildLocations[netGunsBuilt % 4][1]);
				tlog("Moving to " + loc);
				moveLog(loc);
			}
			return;
		}

		if (reachedLandscaperCheckpoint >= 1) {
			log("Continuing: Landscaper checkpoint 1 reached");
		} else {
			log("Returning: Landscaper checkpoint 1 not reached");
			return;
		}

		if (here.isAdjacentTo(HQLocation)) {
			return;
		}
		moveLog(HQLocation);
		*/
		return;
	}
}
