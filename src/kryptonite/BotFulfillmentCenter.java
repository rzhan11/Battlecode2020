package kryptonite;

import battlecode.common.*;

public class BotFulfillmentCenter extends Globals {

	public static void loop() throws GameActionException {
		while (true) {
			try {
			    turn();
			} catch (Exception e) {
				e.printStackTrace();
			}
			System.out.println("-");
			Clock.yield();
		}
	}

	public static void turn() throws GameActionException {

	}
}
