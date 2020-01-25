package rush;

import battlecode.common.*;

import static rush.Communication.*;
import static rush.Constants.*;
import static rush.Debug.*;
import static rush.Map.*;

public class BotVaporator extends Globals {

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
