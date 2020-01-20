package kryptonite;

import battlecode.common.*;

import static kryptonite.Communication.*;
import static kryptonite.Constants.*;
import static kryptonite.Debug.*;
import static kryptonite.Map.*;

public class BotFulfillmentCenter extends Globals {

	public static int roundParity = -1;
	public static int dronesMade = 0;

	public static void loop() throws GameActionException {
		while (true) {
			try {
				Globals.update();
				if (firstTurn) {
					roundParity = (roundNum - 1) % 2;
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

		if (visibleEnemies.length > 0 && dronesMade < 5) {
			if (rc.getTeamSoup() > RobotType.DELIVERY_DRONE.cost) {
				Direction res = tryBuild(RobotType.DELIVERY_DRONE, directions);
				if (res != null) {
					dronesMade++;
					log("Built emergency drone");
				}
			}
			return;
		}

		if (dronesMade < 1) {
			if (rc.getTeamSoup() > RobotType.DELIVERY_DRONE.cost) {
				Direction res = tryBuild(RobotType.DELIVERY_DRONE, directions);
				if (res != null) {
					dronesMade++;
					log("Built first drone");
				}
			}
			return;
		}

		if (wallFull) {
			log("Continuing: Wall full checkpoint reached");
		} else {
			log("Returning: Wall full checkpoint not reached");
			return;
		}

		if (dronesMade < 4) {
			if (rc.getTeamSoup() > RobotType.DELIVERY_DRONE.cost) {
				Direction res = tryBuild(RobotType.DELIVERY_DRONE, directions);
				if (res != null) {
					dronesMade++;
				}
			}
			return;
		}

		if (supportFull) {
			log("Continuing: Support full checkpoint reached");
		} else {
			log("Returning: Support full checkpoint not reached");
			return;
		}

		if (rc.getTeamSoup() > RobotType.DELIVERY_DRONE.cost) {
			Direction res = tryBuild(RobotType.DELIVERY_DRONE, directions);
			if (res != null) {
				dronesMade++;
			}
			return;
		}

		// one type of fulfillment center
//		if (roundParity == 0) {
//
//		} else {
//
//		}
	}
}
