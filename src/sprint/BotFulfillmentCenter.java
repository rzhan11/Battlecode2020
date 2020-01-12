package sprint;

import battlecode.common.*;

public class BotFulfillmentCenter extends Globals {

	public static int dronesMade = 0;
	public static int[] droneCheckpoints = {6};

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
				if (teamSoup >= RobotType.DELIVERY_DRONE.cost + RobotType.REFINERY.cost + Communication.REFINERY_BUILT_COST &&
						rc.canBuildRobot(RobotType.DELIVERY_DRONE, d) && dronesMade < droneCheckpoints[0]) {
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
		}



//		if(maxDronesMade == 12) {
//			RobotInfo[] nearbyRobots = rc.senseNearbyRobots();
//			int numVaporators = 0;
//			for (RobotInfo ri : nearbyRobots) {
//				if (ri.type == RobotType.VAPORATOR) {
//					numVaporators++;
//				}
//			}
//			if(numVaporators == 4){
//				maxDronesMade = 20;
//			}
//
//		}
//
//		if (rc.isReady()) {
//			for (Direction d : Direction.allDirections()) {
//				if (teamSoup >= RobotType.DELIVERY_DRONE.cost && rc.canBuildRobot(RobotType.DELIVERY_DRONE, d) && dronesMade < maxDronesMade) {
//					rc.buildRobot(RobotType.DELIVERY_DRONE, d);
//					teamSoup = rc.getTeamSoup();
//					dronesMade++;
//				}
//			}
//		}
//		return;
	}
}
