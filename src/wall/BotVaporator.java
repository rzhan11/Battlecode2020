package wall;

import battlecode.common.*;

import static wall.Communication.*;
import static wall.Constants.*;
import static wall.Debug.*;
import static wall.Map.*;

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
