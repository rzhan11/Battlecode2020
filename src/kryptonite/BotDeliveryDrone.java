package kryptonite;

import battlecode.common.*;

import static kryptonite.Communication.*;
import static kryptonite.Debug.*;
import static kryptonite.Map.*;
import static kryptonite.Nav.*;
import static kryptonite.Wall.*;
import static kryptonite.Utils.*;
import static kryptonite.Zones.*;

public class BotDeliveryDrone extends Globals {

	public static void loop() throws GameActionException {
		while (true) {
			try {
				Globals.update();

				if (!initialized) {
					initDeliveryDrone();
				}

				turn();
			} catch (Exception e) {
				e.printStackTrace();
			}
			Globals.endTurn();
		}
	}

	public static boolean initialized = false;

	public static void initDeliveryDrone() throws GameActionException {
		loadWallInfo();

		initialized = true;

		Globals.endTurn();
		Globals.update();
	}

	public static void turn() throws GameActionException {
//		log("temp return");
//		return;
		if (!rc.isReady()) {
			log("Not ready");
			return;
		}

		Direction dirToHQ = HQLoc.directionTo(here);
		Direction targetDir = dirToHQ.rotateRight().rotateRight();
		MapLocation targetLoc = HQLoc.add(targetDir).add(targetDir);
		log("Trying to rotate around ally HQ");
		moveLog(targetLoc);
	}
}
