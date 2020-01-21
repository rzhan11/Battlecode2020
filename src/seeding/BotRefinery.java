package seeding;

import battlecode.common.*;

import static seeding.Communication.*;
import static seeding.Constants.*;
import static seeding.Debug.*;
import static seeding.Map.*;

public class BotRefinery extends Globals {

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

	}
}
