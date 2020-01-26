package orig_rush_bot;

import battlecode.common.*;

import static orig_rush_bot.Actions.*;
import static orig_rush_bot.Communication.*;
import static orig_rush_bot.Debug.*;
import static orig_rush_bot.Globals.*;
import static orig_rush_bot.Map.*;
import static orig_rush_bot.Nav.*;
import static orig_rush_bot.Utils.*;
import static orig_rush_bot.Wall.*;
import static orig_rush_bot.Zones.*;

public class BotVaporator extends Globals {

	// after I signal my death, if I am still alive after this many rounds, resignal that I am actually alive
	final public static int NUM_ROUNDS_SIGNAL_STILL_ALIVE = 10;

	public static int signalDeathRound = -1;


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
		int maxDirtThreat = 0; // max amount of dirt that can be put onto this building in one turn
		RobotInfo[] adjEnemies = rc.senseNearbyRobots(2, them);
		for (RobotInfo ri: adjEnemies) {
			if (ri.type == RobotType.LANDSCAPER &&
				ri.cooldownTurns < 2) {
				maxDirtThreat++;
			}
		}

		boolean canBeKilled = rc.getDirtCarrying() + maxDirtThreat >= myType.dirtLimit;

		for (Direction dir: directions) {
			MapLocation loc = rc.adjacentLocation(dir);
			if (rc.canSenseLocation(loc) && isLocWet(loc) && waterLevel >= myElevation) {
				canBeKilled = true;
			}
		}

		if (signalDeathRound != -1) {
			if (canBeKilled) {
//				writeTransactionVaporatorStatus(-1);
				signalDeathRound = roundNum;
			} else if (roundNum - signalDeathRound > NUM_ROUNDS_SIGNAL_STILL_ALIVE) {
//				writeTransactionVaporatorStatus(1);
				signalDeathRound = -1;
			}
		}

	}
}
