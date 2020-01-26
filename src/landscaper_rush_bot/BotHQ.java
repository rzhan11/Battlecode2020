package landscaper_rush_bot;

import battlecode.common.*;

import static landscaper_rush_bot.Communication.*;
import static landscaper_rush_bot.Constants.*;
import static landscaper_rush_bot.Debug.*;
import static landscaper_rush_bot.Map.*;

public class BotHQ extends Globals {

	final public static int RELAX_LARGE_WALL_FULL_ROUND_NUM = 1000;
	final public static int RELAX_LARGE_WALL_FULL_AMOUNT = 4;

	private static boolean[] exploredDirections = new boolean[8]; // whether or not a Miner has been sent in this direction
	private static int explorerMinerCount = 0;

	// has miner been built to check horizontal, vertical, rotational symmetries
	private static boolean[] builtSymmetryMiner = new boolean[3];
	private static int symmetryMinerCount = 0;

	private static boolean madeBuilderMiner = false;

	public static MapLocation soupLocation = null;

	final private static int BLOCK_LANDSCAPER_DIST = 13;

	public static void loop() throws GameActionException {
		while (true) {
			try {
				Globals.update();
				if (firstTurn) {
					log("Possible enemy HQ locations");
					tlog("" + symmetryHQLocations[0]);
					tlog("" + symmetryHQLocations[1]);
					tlog("" + symmetryHQLocations[2]);

					writeTransactionHQFirstTurn(here);

					// finds visible soup locations
					locateNearbySoup();

				}
			    turn();
			} catch (Exception e) {
				e.printStackTrace();
			}
			Globals.endTurn(false);
		}
	}

	public static void turn() throws GameActionException {

		if (!rc.isReady()) {
			log("Not ready");
			return;
		}

		if (explorerMinerCount < 8 && teamSoup >= RobotType.MINER.cost) {
			buildMiner();
		} else {
			log("Not enough soup to build explorer miner");
		}

		//if explorer miners have been build and have enough money, build a BuilderMiner
		if (!madeBuilderMiner && teamSoup >= RobotType.MINER.cost) {
			//try building
			for (int k = 0; k < 8; k++) {
				if (rc.canBuildRobot(RobotType.MINER, directions[k])) {
					log("Building builder miner");
					Actions.doBuildRobot(RobotType.MINER, directions[k]);
					teamSoup = rc.getTeamSoup();
					madeBuilderMiner = true;

					//make transaction
					RobotInfo ri = rc.senseRobotAtLocation(here.add(directions[k]));
					//SEND TRANSACTION
					writeTransactionBuilderMinerBuilt(ri.ID);

					return;
				}
			}
		}
	}

	/*
	Does not affect HQ yet - Remind Richard
	*/
	public static void locateNearbySoup () throws GameActionException {
		for (int[] dir: senseDirections) {
			if (actualSensorRadiusSquared < dir[2]) {
				break;
			}
			MapLocation loc = here.translate(dir[0], dir[1]);
			if (rc.canSenseLocation(loc) && rc.senseSoup(loc) > 0) {
				soupLocation = loc;
				break;
			}
		}
		return;
	}

	/*
	Tries to build a miner in an unexplored direction
	Returns true if built a miner
	Returns false if did not build a miner
	*/
	public static boolean buildMiner() throws GameActionException {
		Direction[] orderedDirections;
		if (soupLocation == null) {
			log("d1");
			orderedDirections = getCloseDirections(here.directionTo(getSymmetryLocation()));
		} else {
			log("d2");
			orderedDirections = getCloseDirections(here.directionTo(soupLocation));
		}
		for (Direction dir: orderedDirections) {
			log("dir " +  dir);
		}

		for (int i = 0; i < orderedDirections.length; i++) {
			Direction dir = orderedDirections[i];
			MapLocation loc = rc.adjacentLocation(dir);
			if (rc.canBuildRobot(RobotType.MINER, dir)) {
				Actions.doBuildRobot(RobotType.MINER, dir);
				teamSoup = rc.getTeamSoup();
				explorerMinerCount++;
				return true;
			}
		}

		return false;
	}

	/*
	Tries to build a symmetry miner if not all three symmetry miners have been built
	Returns true if built a symmetry miner
	Returns false if did not build a symmetry miner
	*/
	public static boolean buildSymmetryMiner() throws GameActionException {
		for (int i = 0; i < symmetryHQLocations.length; i++) {
			Direction dir = here.directionTo(symmetryHQLocations[i]);
			MapLocation loc = rc.adjacentLocation(dir);
			if (!builtSymmetryMiner[i] && rc.canBuildRobot(RobotType.MINER, dir)) {
				Actions.doBuildRobot(RobotType.MINER, dir);
				teamSoup = rc.getTeamSoup();
				builtSymmetryMiner[i] = true;
				symmetryMinerCount++;

				RobotInfo ri = rc.senseRobotAtLocation(loc);
				writeTransactionSymmetryMinerBuilt(ri.ID, symmetryHQLocations[i]);
				return true;
			}
		}
		return false;
	}
}
