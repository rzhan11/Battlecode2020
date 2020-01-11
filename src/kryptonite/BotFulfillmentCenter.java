package kryptonite;

import battlecode.common.*;

public class BotFulfillmentCenter extends Globals {

	public static int dronesMade = 0;
	public static int maxDronesMade = 12;

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
		if (rc.isReady()) {
			for (Direction d : Direction.allDirections()) {
				if (teamSoup >= RobotType.DELIVERY_DRONE.cost && rc.canBuildRobot(RobotType.DELIVERY_DRONE, d) && dronesMade < maxDronesMade) {
					rc.buildRobot(RobotType.DELIVERY_DRONE, d);
					teamSoup = rc.getTeamSoup();
					dronesMade++;
				}
			}
		}
		return;
	}
}
