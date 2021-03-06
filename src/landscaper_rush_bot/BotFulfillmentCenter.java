package landscaper_rush_bot;

import battlecode.common.*;

import static landscaper_rush_bot.Communication.*;
import static landscaper_rush_bot.Constants.*;
import static landscaper_rush_bot.Debug.*;
import static landscaper_rush_bot.Map.*;

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

		if (!rc.isReady()) {
			log("Not ready");
			return;
		}

		Direction[] dirToEnemyHQ = getCloseDirections(here.directionTo(getSymmetryLocation()));

		if (visibleEnemies.length > 0) {
			for (RobotInfo ri : visibleEnemies) {
				if (ri.type == RobotType.LANDSCAPER) {
					if (teamSoup >= RobotType.DELIVERY_DRONE.cost) {
						log("Landscapers detected, building drones");
						boolean didBuild = tryBuild(RobotType.DELIVERY_DRONE, dirToEnemyHQ);
						if (didBuild) {
							dronesMade++;
							if (dronesMade >= droneCheckpoints[0] && !checkpointSent[0]) {
								writeTransactionDroneCheckpoint(0);
								checkpointSent[0] = true;
							} else if (dronesMade >= droneCheckpoints[1] && !checkpointSent[1]) {
								writeTransactionDroneCheckpoint(1);
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
			log("Drone checkpoint 0 not reached");
			// leave enough to build a refinery
			if (teamSoup >= RobotType.DELIVERY_DRONE.cost + RobotType.REFINERY.cost) {
				log("Trying to build delivery drone");
				boolean didBuild = tryBuild(RobotType.DELIVERY_DRONE, dirToEnemyHQ);
				if (didBuild) {
					dronesMade++;
					if (dronesMade >= droneCheckpoints[0] && !checkpointSent[0]) {
						writeTransactionDroneCheckpoint(0);
						checkpointSent[0] = true;
					}
				}
			}
			return;
		} else {
			log("Drone checkpoint 0 reached");
		}

		// after all the vaporators have been built continue
		if (reachedVaporatorCheckpoint) {
			log("Continuing: Vaporator checkpoint reached");
		} else {
			log("Returning: Vaporator checkpoint not reached");
			return;
		}

		if (dronesMade < droneCheckpoints[1]) {
			log("Drone checkpoint 1 not reached");
			// leave enough to build a refinery
			if (teamSoup >= RobotType.DELIVERY_DRONE.cost + RobotType.REFINERY.cost) {
				log("Trying to build delivery drone");
				boolean didBuild = tryBuild(RobotType.DELIVERY_DRONE, dirToEnemyHQ);
				if (didBuild) {
					dronesMade++;
					if (dronesMade >= droneCheckpoints[1] && !checkpointSent[1]) {
						writeTransactionDroneCheckpoint(1);
						checkpointSent[1] = true;
					}
				}
			} else {
				log("Can't afford delivery drone");
			}
			return;
		} else {
			log("Drone checkpoint 1 reached");
		}

		// after netgun is done
		if (reachedNetgunCheckpoint) {
			log("Continuing: reachedNetgunCheckpoint checkpoint reached");
		} else {
			log("Returning: reachedNetgunCheckpoint checkpoint not reached");
			return;
		}

		// after large wall is full
		if (largeWallFull) {
			log("Continuing: largeWallFull checkpoint reached");
		} else {
			log("Returning: largeWallFull checkpoint not reached");
			return;
		}

		// after second landscaper checkpoint, make as many drones as possible

		if (teamSoup >= RobotType.DELIVERY_DRONE.cost + RobotType.REFINERY.cost) {
			log("Trying to build delivery drone");
			boolean didBuild = tryBuild(RobotType.DELIVERY_DRONE, directions);
			if (didBuild) {
				dronesMade++;
			}
		} else {
			log("Can't afford delivery drone");
		}


		return;
	}
}
