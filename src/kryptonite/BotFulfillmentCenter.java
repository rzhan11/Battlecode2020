package kryptonite;

import battlecode.common.*;

public class BotFulfillmentCenter extends Globals {

	public static int dronesMade = 0;
	public static int[] droneCheckpoints = {6, 20};

	public static void loop() throws GameActionException {
		while (true) {
			try {
				Globals.update();
				if (firstTurn) {

				}
			    turn();
			} catch (Exception e) {
				e.printStackTrace();
			}
			Globals.endTurn(false);
		}
	}

	public static void turn() throws GameActionException {
		// initial drones made
		if (dronesMade < droneCheckpoints[0]) {
			Debug.tlog("Drone checkpoint 0 not reached");
			// leave enough to build a refinery
			if (teamSoup >= RobotType.DELIVERY_DRONE.cost + RobotType.REFINERY.cost + Communication.REFINERY_BUILT_COST) {
				Debug.tlog("Trying to build delivery drone");
				boolean didBuild = tryBuild(RobotType.DELIVERY_DRONE);
				if (didBuild) {
					dronesMade++;
					if (dronesMade >= droneCheckpoints[0]) {
						Communication.writeTransactionDroneCheckpoint(0);
					}
				}
			}
			return;
		} else {
			Debug.tlog("Drone checkpoint 0 reached");
		}

		// after all the vaporators have been built continue
		if (reachedVaporatorCheckpoint) {
			Debug.tlog("Continuing: Vaporator checkpoint reached");
		} else {
			Debug.tlog("Returning: Vaporator checkpoint not reached");
			return;
		}

		if (dronesMade < droneCheckpoints[1]) {
			Debug.tlog("Drone checkpoint 1 not reached");
			// leave enough to build a refinery
			if (teamSoup >= RobotType.DELIVERY_DRONE.cost + RobotType.REFINERY.cost + Communication.REFINERY_BUILT_COST) {
				Debug.tlog("Trying to build delivery drone");
				boolean didBuild = tryBuild(RobotType.DELIVERY_DRONE);
				if (didBuild) {
					dronesMade++;
					if (dronesMade >= droneCheckpoints[1]) {
						Communication.writeTransactionDroneCheckpoint(1);
					}
				}
			} else {
				Debug.tlog("Can't afford delivery drone");
			}
			return;
		} else {
			Debug.tlog("Drone checkpoint 1 reached");
		}

		// after all the vaporators have been built continue
		if (reachedLandscaperCheckpoint == 1) {
			Debug.tlog("Continuing: Landscaper checkpoint 1 reached");
		} else {
			Debug.tlog("Returning: Landscaper checkpoint 1 not reached");
			return;
		}

		// after second landscaper checkpoint, make as many drones as possible
		if (teamSoup >= RobotType.DELIVERY_DRONE.cost + RobotType.REFINERY.cost + Communication.REFINERY_BUILT_COST) {
			Debug.tlog("Trying to build delivery drone");
			boolean didBuild = tryBuild(RobotType.DELIVERY_DRONE);
			if (didBuild) {
				dronesMade++;
			}
		} else {
			Debug.tlog("Can't afford delivery drone");
		}

		return;
	}
}
