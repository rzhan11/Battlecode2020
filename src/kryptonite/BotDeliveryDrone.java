package kryptonite;

import battlecode.common.*;

public class BotDeliveryDrone extends Globals {

	public static void loop() throws GameActionException {
		while (true) {
			try {
				Globals.update();
			    turn();
			} catch (Exception e) {
				e.printStackTrace();
			}
			Globals.endTurn();
		}
	}

	public static void turn() throws GameActionException {

	}
}
