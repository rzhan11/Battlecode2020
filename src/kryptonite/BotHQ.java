package kryptonite;

import battlecode.common.*;

public class BotHQ extends Globals {

	public static void loop() throws GameActionException {
		while (true) {
			int startTurn = rc.getRoundNum();
			try {
				Globals.update();
			    turn();
			} catch (Exception e) {
				e.printStackTrace();
			}
			int endTurn = rc.getRoundNum();
			if (startTurn != endTurn) {
				System.out.println("OVER BYTECODE LIMIT");
			}
			Clock.yield();
		}
	}

	public static void turn() throws GameActionException {

	}
}
