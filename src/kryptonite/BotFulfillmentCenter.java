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

		if (wallFull) {
			log("Continuing: Wall full checkpoint reached");
		} else {
			log("Returning: Wall full checkpoint not reached");
			return;
		}

		int minSoup = RobotType.DELIVERY_DRONE.cost;
//		if (roundNum < 100) {
//			minSoup += RobotType.VAPORATOR.cost;
//		}
//		if (roundNum < 500) {
//			minSoup += RobotType.REFINERY.cost;
//		}
		if (dronesMade < 4) {
			minSoup = RobotType.DELIVERY_DRONE.cost;
		} else {
			minSoup = P_INF;
		}

		if (rc.getTeamSoup() > minSoup) {
			Direction res = tryBuild(RobotType.DELIVERY_DRONE, directions);
			if (res != null) {
				dronesMade++;
			}
		}


		// one type of fufillmentcenter
//		if (roundParity == 0) {
//
//		} else {
//
//		}
	}
}
