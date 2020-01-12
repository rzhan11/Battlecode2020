package kryptonite;

import battlecode.common.*;

public class BotFulfillmentCenter extends Globals {

	public static int dronesMade = 0;
	public static int[] droneCheckpoints = {6, 12};
	public static boolean vaporatorCheckpoint = false;

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
			for (Direction d : Direction.allDirections()) {
				MapLocation loc = rc.adjacentLocation(d);
				if (teamSoup >= RobotType.DELIVERY_DRONE.cost + RobotType.REFINERY.cost + Communication.REFINERY_BUILT_COST &&
						!rc.senseFlooding(loc) && Nav.checkElevation(loc) && dronesMade < droneCheckpoints[0] && rc.senseRobotAtLocation(loc) == null) {
					Debug.tlog("Building delivery drone at " + rc.adjacentLocation(d));
					if (rc.isReady()) {
						rc.buildRobot(RobotType.DELIVERY_DRONE, d);
						teamSoup = rc.getTeamSoup();
						dronesMade++;
						Debug.ttlog("Success");
						if (dronesMade >= droneCheckpoints[0]) {
							Communication.writeTransactionDroneCheckpoint(1);
						}
					} else {
						Debug.ttlog("But not ready");
					}
					return;
				}
			}

		// after all the vaporators have been built continue
		} else if (vaporatorCheckpoint && dronesMade < droneCheckpoints[1]) {
			Debug.tlog("HERHEHEHREHEHRERE");
			for (Direction d : Direction.allDirections()) {
				MapLocation loc = rc.adjacentLocation(d);
				if (teamSoup >= RobotType.DELIVERY_DRONE.cost + RobotType.REFINERY.cost + Communication.REFINERY_BUILT_COST &&
						!rc.senseFlooding(loc) && Nav.checkElevation(loc) && dronesMade < droneCheckpoints[1] && rc.senseRobotAtLocation(loc) == null) {
					Debug.tlog("Building delivery drone at " + rc.adjacentLocation(d));
					if (rc.isReady()) {
						rc.buildRobot(RobotType.DELIVERY_DRONE, d);
						teamSoup = rc.getTeamSoup();
						dronesMade++;
						Debug.ttlog("Success");
						if (dronesMade >= droneCheckpoints[1]) {
							Communication.writeTransactionDroneCheckpoint(2);
						}
					} else {
						Debug.ttlog("But not ready");
					}
					return;
				}
			}
		}
		return;

	}
}
