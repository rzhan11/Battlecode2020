package kryptonite;

import battlecode.common.*;

import static kryptonite.Communication.*;
import static kryptonite.Constants.*;
import static kryptonite.Debug.*;
import static kryptonite.Map.*;
import static kryptonite.Zones.*;

public class Globals {
	/*
	Constants that will never change
	*/
	public static RobotController rc;
	public static int spawnRound; // the first round this robot was called through RobotPlayer.java
	public static Team us;
	public static Team them;
	public static Team cowTeam;
	public static int myID;
	public static RobotType myType;

	public static int[][] senseDirections = null; // stores (dx, dy, magnitude) of locations that can be sensed

	public static int mapWidth;
	public static int mapHeight;
	public static Direction[] directions = {Direction.NORTH, Direction.NORTHEAST, Direction.EAST, Direction.SOUTHEAST, Direction.SOUTH, Direction.SOUTHWEST, Direction.WEST, Direction.NORTHWEST}; // 8 directions
	public static Direction[] allDirections = {Direction.NORTH, Direction.NORTHEAST, Direction.EAST, Direction.SOUTHEAST, Direction.SOUTH, Direction.SOUTHWEST, Direction.WEST, Direction.NORTHWEST, Direction.CENTER}; // includes center
	public static Direction[] cardinalDirections = {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST}; // four cardinal directions
	public static Direction[] diagonalDirections = {Direction.NORTHEAST, Direction.SOUTHEAST, Direction.SOUTHWEST, Direction.NORTHWEST}; // four diagonals

	/*
	Values that might change each turn
	*/

	public static boolean firstTurn = true;

	public static int roundNum;

	public static MapLocation here;
	public static int myElevation;
	public static int waterLevel;

	public static int myPollution;
	public static int actualSensorRadiusSquared;
	public static boolean extremePollution; // when

	public static RobotInfo[] visibleAllies = null;
	public static RobotInfo[] visibleEnemies = null;
	public static RobotInfo[] visibleCows = null;

	public static RobotInfo[] adjacentAllies = null;
	public static RobotInfo[] adjacentEnemies = null;
	public static RobotInfo[] adjacentCows = null;

	public static MapLocation[] visibleSoupLocations = null;
	public static MapLocation closestSoupLocation = null;

	public static boolean[] isDirMoveable = new boolean[8];
	public static boolean[] isDirDanger = new boolean[8];
	public static boolean inDanger;

	// checks if drones picked up and dropped this unit
	public static boolean droppedLastTurn = false;
	public static int lastActiveTurn = 0;

	public static int oldBlocksLength = -1;
	public static int oldBlocksIndex = 1;
	public static int oldTransactionsIndex = 0;

	public static int builderMinerID = -1;

	// symmetry
	public static MapLocation HQLocation = null;
	public static int HQElevation;

	public static MapLocation[] symmetryHQLocations = new MapLocation[3];
	public static int[] isSymmetryHQLocation = {-1, -1, -1}; // -1 is unknown, 0 is false, 1 is true
	public static int symmetryHQLocationsIndex; // current symmetry that we are exploring
	public static MapLocation enemyHQLocation = null;

	/*
	CHECKPOINTS
	 */
	public static boolean reachedVaporatorCheckpoint = false;
	public static boolean reachedNetgunCheckpoint = false;
	public static int reachedDroneCheckpoint = -1;
	public static int reachedLandscaperCheckpoint = -1;


	public static void init(RobotController theRC) throws GameActionException {
		int startByte = Clock.getBytecodesLeft();
		rc = theRC;

		us = rc.getTeam();
		them = us.opponent();
		cowTeam = Team.NEUTRAL;

		myID = rc.getID();
		myType = rc.getType();
		senseDirections = HardCode.getSenseDirections(myType);

		mapWidth = rc.getMapWidth();
		mapHeight = rc.getMapHeight();

		if (us == Team.A) {
			secretKey = 1337;
		} else {
			secretKey = 7331;
		}

		here = rc.getLocation();
		roundNum = rc.getRoundNum();
		spawnRound = roundNum;

		lastActiveTurn = spawnRound - 1;

		log("Init cost: " + (startByte - Clock.getBytecodesLeft()));
	}

	public static void update() throws GameActionException {
		int startByte = Clock.getBytecodesLeft();
		here = rc.getLocation();
		roundNum = rc.getRoundNum();


		if (firstTurn) {
			log("------------------------------\n");
			log("FIRST TURN");
			log("------------------------------\n");
		}

		myElevation = rc.senseElevation(here);
		waterLevel = (int) GameConstants.getWaterLevel(roundNum);

		myPollution = rc.sensePollution(here);
		actualSensorRadiusSquared = rc.getCurrentSensorRadiusSquared();
		extremePollution = actualSensorRadiusSquared < 2;
		if (extremePollution) {
			logi("WARNING: Extreme pollution has made actualSensorRadiusSquared < 2, so errors may occur. Ask Richard.");
		}

		newSoupLocsLength = 0;

		printMyInfo();

		visibleAllies = rc.senseNearbyRobots(-1, us); // -1 uses all robots within sense radius
		visibleEnemies = rc.senseNearbyRobots(-1, them);
		visibleCows = rc.senseNearbyRobots(-1, cowTeam);

		if (!isLowBytecodeLimit(myType)) {
			adjacentAllies = rc.senseNearbyRobots(2, us);
			adjacentEnemies = rc.senseNearbyRobots(2, them);
			adjacentCows = rc.senseNearbyRobots(2, cowTeam);

			visibleSoupLocations = rc.senseNearbySoup();
		}

		// update moveable directions
		updateIsDirMoveable();
		// updates dangerous directions and modifies isdirmoveable based on this
		updateIsDirDanger();

		if (roundNum == lastActiveTurn + 1) {
			droppedLastTurn = false;
		} else {
			droppedLastTurn = true;
			log("Was dropped last turn");
			Nav.bugTracing = false;
			Nav.bugLastWall = null;
			Nav.bugClosestDistanceToTarget = P_INF;
		}

		// visually checks for enemy HQ location at target explore symmetry point

		calculateDynamicCost();

		// find HQ location and symmetries if not already found
		if (HQLocation == null) {
			findHQLocation();
			symmetryHQLocationsIndex = myID % symmetryHQLocations.length;
			log("Initial exploreSymmetryLocation: " + symmetryHQLocations[symmetryHQLocationsIndex]);
		}

		// read previous round's transactions
		if (!firstTurn) {
			if (oldBlocksLength == -1) {
				oldBlocksIndex = Math.max(1, roundNum - RESUBMIT_INTERVAL);
				oldBlocksLength = roundNum - 1;
			}
			log("Reading the previous round's transactions");
			int result = readBlock(roundNum - 1, 0);
			if (result < 0) {
				logi("WARNING: Did not fully read the previous round's transactions");
			} else {
				log("Done reading the previous round's transactions");
			}
		}


		updateSymmetry();

		// tries to submit unsent messages from previous turns
		submitUnsentTransactions();

		log("Update cost: " + (startByte - Clock.getBytecodesLeft()));
	}

	/*
	Prints various useful debuggin information
	*/
	final public static boolean noTurnLog = false;

	public static void printMyInfo () {
		if(noTurnLog) return;
		log("------------------------------\n");
//		log("Robot: " + myType);
//		log("roundNum: " + roundNum);
//		log("ID: " + myID);
		log("*Location: " + here);
		log("*Cooldown: " + rc.getCooldownTurns());
		log("*actualSensorRadiusSquared: " + actualSensorRadiusSquared);
		log("*dynamicCost: " + dynamicCost);
		log("------------------------------\n");
		if (isBuilderMiner(myID)) {
			drawDot(here, GRAY);
			log("I am the builder miner");
		}
	}

	/*
	earlyEnd should be true, unless this method was called at the end of the loop() method
	*/
	public static void endTurn (boolean earlyEnd) throws GameActionException {
		firstTurn &= earlyEnd; // if early end, do not count as full turn
		lastActiveTurn = roundNum;
		readOldBlocks();
		// check if we went over the bytecode limit
		int endTurn = rc.getRoundNum();
		if (roundNum != endTurn) {
			printMyInfo();
			logi("BYTECODE LIMIT EXCEEDED");
			int bytecodeOver = Clock.getBytecodeNum();
			int turns = endTurn - roundNum;
			tlogi("Overused bytecode: " + (bytecodeOver + (turns - 1) * myType.bytecodeLimit));
			tlogi("Skipped turns: " + turns);
			// catch up on missed Transactions
			for (int i = roundNum; i < endTurn; i++) {
				readBlock(i, 0);
			}
		}
		if(!noTurnLog) {
			log("------------------------------\n");
			if (earlyEnd) {
				log("EARLY");
			}
			log("END TURN");
			log("Bytecode left: " + Clock.getBytecodesLeft());
			log("------------------------------\n");
		}
		Clock.yield();
	}

	public static void findHQLocation() throws GameActionException {
		// tries to find our HQLocation and HQElevation by reading messages
		// will skip turn if not found
		if (myType == RobotType.HQ) {
			HQLocation = here;
			HQElevation = myElevation;
		} else {
			readBlock(oldBlocksIndex, 0);
			oldBlocksIndex++;
			while (HQLocation == null) {
				log("Did not find HQLocation in block " + (oldBlocksIndex - 1));
				if (oldBlocksIndex >= spawnRound) {
					Globals.endTurn(true);
					Globals.update();
				}
				if (isLowBytecodeLimit(myType)) {
					tlog("Low bytecode limit");
					Globals.endTurn(true);
					Globals.update();
				}
				readBlock(oldBlocksIndex, 0);
				oldBlocksIndex++;
			}
		}

		// calculates possible enemy HQ locations
		symmetryHQLocations[0] = new MapLocation(mapWidth - 1 - HQLocation.x, HQLocation.y);
		symmetryHQLocations[1] = new MapLocation(HQLocation.x, mapHeight - 1 - HQLocation.y);
		symmetryHQLocations[2] = new MapLocation(mapWidth - 1 - HQLocation.x, mapHeight - 1 - HQLocation.y);
	}

	public static boolean inArray(Object[] arr, Object item, int length) {
		for(int i = 0; i < length; i++) if(arr[i].equals(item)) return true;
		return false;
	}

	public static boolean inArray(int[] arr, int item, int length) {
		for(int i = 0; i < length; i++) if(arr[i] == item) return true;
		return false;
	}

	/*
	Returns true if this robot ID is the builder miner
	*/
	public static boolean isBuilderMiner (int id) {
		return id == builderMinerID;
	}

	/*
	Returns true if this RobotType has a low limit <= 7000
	*/
	public static boolean isLowBytecodeLimit (RobotType rt) {
		switch (rt) {
			case DESIGN_SCHOOL:
			case FULFILLMENT_CENTER:
			case NET_GUN:
			case REFINERY:
			case VAPORATOR:
				return true;
			default:
				return false;
		}
	}

	public static boolean canBePickedUpType(RobotType rt) {
		return rt == RobotType.MINER || rt == RobotType.LANDSCAPER || rt == RobotType.COW;
	}

	public static boolean canShootType (RobotType rt) {
		return rt == RobotType.NET_GUN || rt == RobotType.HQ;
	}

	public static Direction moveLog(MapLocation loc) throws GameActionException {
		Direction move = null;
		move = Nav.bugNavigate(loc);
		if (move != null) {
			tlog("Moved " + move);
		} else {
			tlog("But no move found");
		}
		return move;
	}

	public static boolean isDigLocation(MapLocation ml) {
		return (ml.x - HQLocation.x) % 2 == 0 && (ml.y - HQLocation.y) % 2 == 0;
	}

	public static void updateSymmetry () throws GameActionException {

		if (enemyHQLocation != null) {
			return;
		}

		// if the HQ is on a horizontal/vertical/rotational symmetry that generates the same symmetryHQLOcations
		if ((mapWidth % 2 == 1 && mapWidth / 2 == HQLocation.x) ||
				(mapHeight % 2 == 1 && mapHeight / 2 == HQLocation.y)) {
			isSymmetryHQLocation[0] = 1;
			isSymmetryHQLocation[1] = -1;
			isSymmetryHQLocation[2] = -1;
			enemyHQLocation = symmetryHQLocations[0];
			return;
		}

		// try to visually check unknown enemyHQLocations
		for (int i = 0; i < symmetryHQLocations.length; i++) {
			MapLocation loc = symmetryHQLocations[i];
			if (isSymmetryHQLocation[i] == -1 && rc.canSenseLocation(loc)) {
				RobotInfo ri = rc.senseRobotAtLocation(loc);
				if (ri != null && ri.type == RobotType.HQ) {
					//STATE == enemy FOUND

					enemyHQLocation = loc;
					isSymmetryHQLocation[i] = 1;

					log("Found enemy HQ at " + enemyHQLocation);

					writeTransactionEnemyHQLocation(i, 1);
				} else {
					//STATE == enemy NOT FOUND

					log("Denied enemy HQ at " + loc);
					isSymmetryHQLocation[i] = 0;

					writeTransactionEnemyHQLocation(i, 0);
				}
			}
		}

		checkPossibleSymmetry();

//		drawLine(here, getSymmetryLocation(), Actions.WHITE);
	}

	/*
    Checks if we can tell what the symmetry is based on denied symmetries

    Checks if the current target symmetry is possible
    If not, iterate to a possible one
     */
	public static void checkPossibleSymmetry () {
		// if two symmetries have been confirmed negatives, then the other one must be the last symmetry
		int denyCount = 0;
		int notDenyIndex = -1;
		for (int i = 0; i < symmetryHQLocations.length; i++) {
			if (isSymmetryHQLocation[i] == 0) {
				denyCount++;
			} else {
				notDenyIndex = i;
			}
		}
		if (denyCount == 2) {
			enemyHQLocation = symmetryHQLocations[notDenyIndex];
			isSymmetryHQLocation[notDenyIndex] = 1;
			log("Determined through 2 denials that enemy HQ is at " + enemyHQLocation);
			return;
		}

		while (isSymmetryHQLocation[symmetryHQLocationsIndex] == 0) {
			symmetryHQLocationsIndex++;
			symmetryHQLocationsIndex %= symmetryHQLocations.length;
			log("Retargetting symmetry that we are exploring to " + symmetryHQLocations[symmetryHQLocationsIndex]);
		}
	}

	/*
	If found which symmetry the enemy HQ location is on, return that location
	Else, return the current symmetry that we are exploring
	 */
	public static MapLocation getSymmetryLocation () {
		if (enemyHQLocation == null) {
			return symmetryHQLocations[symmetryHQLocationsIndex];
		} else {
			return enemyHQLocation;
		}
	}

	// information about digLocations
	public static boolean wallFull = false;
	public static boolean supportFull = false;


	/*
	Assumes cost is already checked
	Tries to build a given robot type in any direction
	 */
	public static boolean tryBuild (RobotType rt, Direction[] dir) throws GameActionException {
		for (Direction d : dir) {
			MapLocation loc = rc.adjacentLocation(d);
			if (!rc.senseFlooding(loc) && isLocFlat(loc) && rc.senseRobotAtLocation(loc) == null) {
				Actions.doBuildRobot(rt, d);

				tlog("Success");
				return true;
			}
		}
		tlog("No open spots found");
		return false;
	}


	/*
	Given a direction, return an array of 8 directions (excludes CENTER)
	that is ordered by how close they are to the given direction
	 */
	public static Direction[] getCloseDirections (Direction dir) {
		Direction[] dirs = new Direction[8];
		dirs[0] = dir;
		dirs[1] = dir.rotateRight();
		dirs[2] = dir.rotateLeft();
		dirs[3] = dirs[1].rotateRight();
		dirs[4] = dirs[2].rotateLeft();
		dirs[5] = dirs[3].rotateRight();
		dirs[6] = dirs[4].rotateLeft();
		dirs[7] = dir.opposite();;
		return dirs;
	}
}
