package kryptonite;

import battlecode.common.*;

public class BotFulfillmentCenter {

	public static void loop() throws GameActionException {
		while (true) {
			try {
			    turn();
			} catch (Exception e) {
				e.printStackTrace();
			}
			Clock.yield();
		}
	}

	public static void turn() throws GameActionException {

	}
}
