package sprint;

import battlecode.common.*;

public class BotFulfillmentCenter extends Globals {

	public static int dronesMade = 0;
	public static int[] droneCheckpoints = {7, 8};
	public static boolean[] checkpointSent = {false, false};

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

		Direction[] dirToEnemyHQ = getCloseDirections(here.directionTo(symmetryHQLocations[2]));

		if (visibleEnemies.length > 0) {
			for (RobotInfo ri : visibleEnemies) {
				if (ri.type == RobotType.LANDSCAPER) {
					if (teamSoup >= RobotType.DELIVERY_DRONE.cost) {
						Debug.tlog("Landscapers detected, building drones");
						boolean didBuild = tryBuild(RobotType.DELIVERY_DRONE, dirToEnemyHQ);
						if (didBuild) {
							dronesMade++;
							if (dronesMade >= droneCheckpoints[0] && !checkpointSent[0]) {
								Communication.writeTransactionDroneCheckpoint(0);
								checkpointSent[0] = true;
							} else if (dronesMade >= droneCheckpoints[1] && !checkpointSent[1]) {
								Communication.writeTransactionDroneCheckpoint(1);
								checkpointSent[1] = true;
							}
						}
					}
					return;
				}
			}
		}

		// initial drones made
		if (dronesMade < droneCheckpoints[0]) {
			Debug.tlog("Drone checkpoint 0 not reached");
			// leave enough to build a refinery
			if (teamSoup >= RobotType.DELIVERY_DRONE.cost + RobotType.REFINERY.cost) {
				Debug.tlog("Trying to build delivery drone");
				boolean didBuild = tryBuild(RobotType.DELIVERY_DRONE, dirToEnemyHQ);
				if (didBuild) {
					dronesMade++;
					if (dronesMade >= droneCheckpoints[0] && !checkpointSent[0]) {
						Communication.writeTransactionDroneCheckpoint(0);
						checkpointSent[0] = true;
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
			if (teamSoup >= RobotType.DELIVERY_DRONE.cost + RobotType.REFINERY.cost) {
				Debug.tlog("Trying to build delivery drone");
				boolean didBuild = tryBuild(RobotType.DELIVERY_DRONE, dirToEnemyHQ);
				if (didBuild) {
					dronesMade++;
					if (dronesMade >= droneCheckpoints[1] && !checkpointSent[1]) {
						Communication.writeTransactionDroneCheckpoint(1);
						checkpointSent[1] = true;
					}
				}
			} else {
				Debug.tlog("Can't afford delivery drone");
			}
			return;
		} else {
			Debug.tlog("Drone checkpoint 1 reached");
		}

		// after netgun is done
		if (reachedNetgunCheckpoint) {
			Debug.tlog("Continuing: reachedNetgunCheckpoint checkpoint reached");
		} else {
			Debug.tlog("Returning: reachedNetgunCheckpoint checkpoint not reached");
			return;
		}

		// after large wall is full
		if (largeWallFull) {
			Debug.tlog("Continuing: largeWallFull checkpoint reached");
		} else {
			Debug.tlog("Returning: largeWallFull checkpoint not reached");
			return;
		}

		// after second landscaper checkpoint, make as many drones as possible

		if (teamSoup >= RobotType.DELIVERY_DRONE.cost + RobotType.REFINERY.cost) {
			Debug.tlog("Trying to build delivery drone");
			boolean didBuild = tryBuild(RobotType.DELIVERY_DRONE, directions);
			if (didBuild) {
				dronesMade++;
			}
		} else {
			Debug.tlog("Can't afford delivery drone");
		}


		return;
	}
}
