package kryptonite;

import battlecode.common.*;

public class BotFulfillmentCenter extends Globals {

	public static int dronesMade = 0;

	public static void loop() throws GameActionException {
		while (true) {
			try {
				if (firstTurn) {

				}
			    turn();
			} catch (Exception e) {
				e.printStackTrace();
			}
			Globals.endTurn();
		}
	}

	public static void turn() throws GameActionException {
		if (rc.isReady()) {
			for (Direction d : Direction.allDirections()) {
				if (teamSoup >= RobotType.DELIVERY_DRONE.cost && rc.canBuildRobot(RobotType.DELIVERY_DRONE, d) && dronesMade < 5) {
					rc.buildRobot(RobotType.DELIVERY_DRONE, d);
					teamSoup = rc.getTeamSoup();
					dronesMade++;
				}
			}
		}
		return;
	}
}
