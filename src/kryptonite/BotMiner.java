package kryptonite;

import battlecode.common.*;

import static kryptonite.Communication.*;
import static kryptonite.Debug.*;
import static kryptonite.Map.*;
import static kryptonite.Utils.*;
import static kryptonite.Wall.wallCompleted;
import static kryptonite.Zones.*;

public class BotMiner extends Globals {

	final public static int
			MINER_RESOURCE_ROLE = 1,
			MINER_BUILDER_ROLE = 2,
			MINER_MIDGAME_ROLE = 3;

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

		if (wallCompleted) {
			myRole = MINER_MIDGAME_ROLE;
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

		int avoidDangerResult = Nav.avoidDanger();
		if (avoidDangerResult == 1) {
			return;
		}

		// after wall is completed, do not walk into dig locations
		if (wallCompleted) {
			for (int i = 0; i < directions.length; i++) {
				if (isDigLoc(rc.adjacentLocation(directions[i]))) {
					isDirMoveable[i] = false;
				}
			}
		}

		switch (myRole) {
			case MINER_RESOURCE_ROLE:
				BotMinerResource.turn();
				break;
			case MINER_BUILDER_ROLE:
				BotMinerBuilder.turn();
				break;
			case MINER_MIDGAME_ROLE:
				BotMinerMidgame.turn();
				break;
		}

	}
}
