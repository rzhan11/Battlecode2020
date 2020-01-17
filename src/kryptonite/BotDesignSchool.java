package kryptonite;

import battlecode.common.*;

import static kryptonite.Communication.*;
import static kryptonite.Constants.*;
import static kryptonite.Debug.*;
import static kryptonite.Map.*;

public class BotDesignSchool extends Globals {

	public static int roundParity = -1;

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
		if (roundParity == -1) {
			roundParity = rc.getRoundNum() % 2;
		}

		// close to hq one
		if (roundParity == 0) {

		}

		// other type of design school
		else {

		}
	}
}
