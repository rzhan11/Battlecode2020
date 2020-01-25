package rush_bot;

import battlecode.common.*;

import static rush_bot.Actions.*;
import static rush_bot.Communication.*;
import static rush_bot.Debug.*;
import static rush_bot.Globals.*;
import static rush_bot.Map.*;
import static rush_bot.Nav.*;
import static rush_bot.Utils.*;
import static rush_bot.Wall.*;
import static rush_bot.Zones.*;

public class BotMiner extends Globals {

	final public static int
			MINER_RESOURCE_ROLE = 1,
			MINER_BUILDER_ROLE = 2,
			MINER_MIDGAME_ROLE = 3,
			MINER_RUSH_ROLE = 4;

	public static int myRole = -1;

	public static void loop() throws GameActionException {
		while (true) {
			try {
				Globals.update();

				if (!initializedMiner) {
					initMiner();
				}

				turn();

			} catch (Exception e) {
				e.printStackTrace();
			}
			Globals.endTurn();
		}
	}


	public static boolean initializedMiner = false;
	public static int evacuateRound = 0;

	public static void initMiner() throws GameActionException {

		if (spawnRound == 2) {
			myRole = MINER_RUSH_ROLE;
		} else {
			myRole = MINER_RESOURCE_ROLE;
		}
		evacuateRound = HardCode.getRoundFlooded(3) - 100;

		BotMinerResource.initMinerResource();

		initializedMiner = true;

		Globals.endTurn();
		Globals.update();
	}

	public static void turn() throws GameActionException {

		if (roundNum >= evacuateRound) {
			myRole = MINER_MIDGAME_ROLE;
		}

		if (!rc.isReady()) {
			log("Not ready");
			return;
		}

		if (myRole == MINER_RUSH_ROLE) {
			BotMinerRush.turn();
			return;
		}

		int avoidDangerResult = Nav.avoidDanger();
		if (avoidDangerResult == 1) {
			return;
		}
		if (avoidDangerResult == -1) {
			// when danger is unavoidable, reset isDirMoveable to ignore danger tiles
			updateIsDirMoveable();
		}

		switch (myRole) {
			case MINER_RESOURCE_ROLE:
				BotMinerResource.turn();
				break;
			case MINER_BUILDER_ROLE:
				BotMinerBuilder.turn();
				break;
		}

	}
}
