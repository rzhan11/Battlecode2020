package rush_bot;

import battlecode.common.*;

import static rush_bot.Debug.*;
import static rush_bot.Wall.*;

public class Communication extends Globals {

	final public static int RESUBMIT_EARLY = 10;
	final public static int RESUBMIT_INTERVAL = 100;
	final public static int RUSH_STATUS_INTERVAL = 50;

	final public static int MAX_UNSENT_TRANSACTIONS_LENGTH = 25;
	final public static int READ_TRANSACTION_MIN_BYTECODE = 500; // how many bytecodes required to read a transaction
	final public static int READ_BIG_TRANSACTION_MIN_BYTECODE = 1500; // how many bytecodes required to read a costly transaction

	final public static int FIRST_TURN_DYNAMIC_COST = 1;

	// each of these signals should be different
	// MUST BE LESS THAN 256
	final public static int HQ_FIRST_TURN_SIGNAL = 0;
	final public static int ENEMY_HQ_LOCATION_SIGNAL = 1;
	final public static int SOUP_CLUSTER_SIGNAL = 2;
	final public static int REFINERY_BUILT_SIGNAL = 4;
	final public static int BUILD_INSTRUCTION_SIGNAL = 5;
	final public static int WALL_STATUS_SIGNAL = 6;
	final public static int FLOODING_FOUND_SIGNAL = 7;
	final public static int ENEMY_RUSH_SIGNAL = 8;
	final public static int ASSIGN_PLATFORM_SIGNAL = 9;
	final public static int PLATFORM_COMPLETED_SIGNAL = 10;
	final public static int PLATFORM_BUILDINGS_COMPLETED_SIGNAL = 11;
	final public static int RUSH_STATUS_SIGNAL = 12;

	final public static int ALL_ZONE_STATUS_SIGNAL = 103;
	final public static int REVIEW_SIGNAL = 104;

	// used to alter our own data
	public static int secretKey;
	public static int[] secretXORKeys = {0B01011011101111011111011111101111,
										 0B10110111011110111110111111011110,
										 0B01101110111101111101111110111101,
										 0B11011101111011111011111101111010,
										 0B10111011110111110111111011110101,
										 0B01110111101111101111110111101011,
										 0B11101111011111011111101111010110};

	public static int dynamicCost;

	public static int[][] unsentMessages = new int[MAX_UNSENT_TRANSACTIONS_LENGTH][GameConstants.BLOCKCHAIN_TRANSACTION_LENGTH];
	public static int unsentTransactionsIndex = 0;
	public static int unsentTransactionsLength = 0;


	/*
		Communication is made up of 7 integers (32-bit)
		The first integer is used for security (confirming that it is our message)
		The second integer is used to denote the "type" of message
		The rest of the five integers are used as data
	*/

	/*
	Encrypts and decrypts an entire message with XOR key
	*/
	public static void xorMessage (int[] message) {
		for (int i = 0; i < message.length; i++) {
			message[i] ^= secretXORKeys[i];
		}
	}

	/*
	Returns the integer value of the code based on the id
	*/
	public static int encryptID (int id) {
	    return id * (1 << 16) + ((id + secretKey) % (1 << 16));
	}

	/*
	Returns the id value of the transaction submitter
	Returns -1 if not our team
	*/
	public static int decryptID (int code) {
		int id = code / (1 << 16);
		int confirm = code & ((1 << 16) - 1);
		if (id == (confirm - secretKey + (1 << 16)) % (1 << 16)) {
			return id;
		} else {
			return -1;
		}
	}

	/*
	Returns the minimum cost that would be guaranteed to have been in last round's transactions
	If first round, returns preset constant
	 */
	public static void calculateDynamicCost () throws GameActionException {
		if (roundNum == 1) {
			dynamicCost = FIRST_TURN_DYNAMIC_COST;
		} else {
			Transaction[] messages = rc.getBlock(roundNum - 1);
			if (messages.length < GameConstants.BLOCKCHAIN_TRANSACTION_LENGTH) {
				dynamicCost = 1;
			} else {
				dynamicCost = messages[GameConstants.BLOCKCHAIN_TRANSACTION_LENGTH - 1].getCost() + 1;
			}
		}
	}

	/*
	If a message was not sent due to cost, save it and try to send later
	*/
	public static void saveUnsentTransaction (int[] message, int cost) {
		if (unsentTransactionsLength == MAX_UNSENT_TRANSACTIONS_LENGTH) {
			logi("ERROR: unsentTransactionsLength reached MAX_UNSENT_TRANSACTIONS_LENGTH limit");
			return;
		}
		unsentMessages[unsentTransactionsLength] = message;
		unsentTransactionsLength++;
	}

	public static void submitUnsentTransactions () throws GameActionException {
		while (unsentTransactionsIndex < unsentTransactionsLength) {
			int[] message = unsentMessages[unsentTransactionsIndex];
			if (dynamicCost <= rc.getTeamSoup()) {
				rc.submitTransaction(message, dynamicCost);

				unsentTransactionsIndex++;

				xorMessage(message);
				log("Submitted unsent transaction with signal of " + message[1]);
			} else {
				break;
			}
		}
	}

	/*
	Every interval of rounds, resend important messages
	 */
	public static void resubmitImportantTransactions () throws GameActionException {
		if (roundNum % RESUBMIT_INTERVAL == RESUBMIT_INTERVAL - RESUBMIT_EARLY) {
			writeTransactionReview();
		}
	}

	public static void readOldBlocks() throws GameActionException {
		if (oldBlocksIndex >= oldBlocksLength) {
			log("NO OLD BLOCKS");
			return;
		} else {
			log("READING OLD BLOCKS");
		}
		int startBlockIndex = oldBlocksIndex;
		int startTransactionsIndex = oldTransactionsIndex;
		int totalReadTransactions = 0;
		while (oldBlocksIndex < oldBlocksLength) { // -1 is since we always read previous transactions, so spawnRound - 1 is already read
			if (Clock.getBytecodesLeft() < READ_TRANSACTION_MIN_BYTECODE) {
				break;
			}
			int res = readBlock(oldBlocksIndex, oldTransactionsIndex);
			totalReadTransactions += Math.abs(res);
			if (res < 0) {
				oldTransactionsIndex = -res - 1;
				break;
			} else {
				oldBlocksIndex++;
				oldTransactionsIndex = 0;
			}
		}
		tlog("Read old blocks from " + startBlockIndex + "-" + startTransactionsIndex + " to " +
				oldBlocksIndex + "-" + oldTransactionsIndex);
		ttlog("Read " + totalReadTransactions + " transactions");
	}

	/*
	Reads in transactions that were submitted last round
	Returns number of transactions read
	If we do not have enough bytecode to read all transactions, returns -1 * # of transactions read
	*/
	public static int readBlock(int round, int startIndex) throws GameActionException {
		Transaction[] block = rc.getBlock(round);
		int index = 0;
		for (Transaction t : block) {
			if (index < startIndex) {
				continue;
			}

			// if this is not the previous round, and we are almost out of bytecode, skip
			if (Clock.getBytecodesLeft() < READ_TRANSACTION_MIN_BYTECODE && round != roundNum - 1) {
				return -(index + 1);
			}

			int[] message = t.getMessage();
			xorMessage(message);

			int submitterID = decryptID(message[0]);
			if (submitterID == -1) {
				tlog("Found opponent's transaction");
				continue; // not submitted by our team
			} else {
				switch (message[1] & ((1 << 8) - 1)) {
					case HQ_FIRST_TURN_SIGNAL:
						readTransactionHQFirstTurn(message, round);
						break;
					case ENEMY_HQ_LOCATION_SIGNAL:
						readTransactionEnemyHQLoc(message, round);
						break;
					case SOUP_CLUSTER_SIGNAL:
						if (myType == RobotType.MINER) {
							readTransactionSoupCluster(message, round);
							break;
						}
					case REFINERY_BUILT_SIGNAL:
						if (myType == RobotType.MINER) {
							if (Clock.getBytecodesLeft() < READ_BIG_TRANSACTION_MIN_BYTECODE && round != roundNum - 1) {
								return -(index + 1);
							}
							readTransactionRefineryBuilt(message, round);
						}
						break;
					case BUILD_INSTRUCTION_SIGNAL:
						readTransactionBuildInstruction(message, round);
						break;
					case WALL_STATUS_SIGNAL:
						readTransactionWallStatus(message, round);
						break;
					case FLOODING_FOUND_SIGNAL:
						if (myType == RobotType.HQ || myType == RobotType.DELIVERY_DRONE) {
							readTransactionFloodingFound(message, round);
						}
						break;
					case ENEMY_RUSH_SIGNAL:
						readTransactionEnemyRush(message, round);
						break;
					case ASSIGN_PLATFORM_SIGNAL:
						readTransactionAssignPlatform(message, round);
						break;
					case REVIEW_SIGNAL:
						readTransactionReview(message, round);
						break;
					case PLATFORM_COMPLETED_SIGNAL:
						readTransactionPlatformCompleted(message, round);
						break;
					case PLATFORM_BUILDINGS_COMPLETED_SIGNAL:
						readTransactionPlatformBuildingsCompleted(message, round);
						break;
					case RUSH_STATUS_SIGNAL:
						readTransactionRushStatus(message, round);
						break;
				}
			}

			index++;
		}

		return index;
	}

	public static boolean hasReadReviewTransaction = false;

	public static void readReviewBlock (int round, int count) throws GameActionException {
		if (hasReadReviewTransaction) {
			return;
		}

		if (count == 5) {
			return;
		}

		while (round >= roundNum) {
			log("WAITING FOR REVIEW BLOCK " + round);
			log("EARLY END");
			Clock.yield();
			Globals.updateBasic();
		}

		Transaction[] block = rc.getBlock(round);
		for (Transaction t : block) {
			// if this is not the previous round, and we are almost out of bytecode, skip
			int[] message = t.getMessage();
			xorMessage(message);

			int submitterID = decryptID(message[0]);
			if (submitterID !=-1) {
				switch (message[1] & ((1 << 8) - 1)) {
					case REVIEW_SIGNAL:
						readTransactionReview(message, round);
						break;
				}
			}
		}
		readReviewBlock(round + 1, count + 1);
	}

	/*
	message[2] = x coordinate of our HQ
	message[3] = y coordinate of our HQ
	message[4] = elevation of our HQ
	*/
	public static void writeTransactionHQFirstTurn (MapLocation myHQLoc) throws GameActionException {
		log("Writing transaction for 'HQ First Turn' at " + myHQLoc);
		int[] message = new int[GameConstants.BLOCKCHAIN_TRANSACTION_LENGTH];
		message[0] = encryptID(myID);
		message[1] = HQ_FIRST_TURN_SIGNAL;
		message[2] = myHQLoc.x;
		message[3] = myHQLoc.y;
		message[4] = myElevation;
		message[5] = rushMinerID;

		xorMessage(message);
		if (rc.getTeamSoup() >= dynamicCost) {
			rc.submitTransaction(message, dynamicCost);

		} else {
			tlog("Could not afford transaction");
			saveUnsentTransaction(message, dynamicCost);
		}
	}

	public static void readTransactionHQFirstTurn (int[] message, int round) {
		tlog("Reading 'HQ First Turn' transaction");
		HQLoc = new MapLocation(message[2], message[3]);
		HQElevation = message[4];
		rushMinerID = message[5];
		ttlog("Submitter ID: " + decryptID(message[0]));
		ttlog("Location: " + HQLoc);
		ttlog("Elevation: " + HQElevation);
		ttlog("rushMinerID: " + rushMinerID);
		ttlog("Posted round: " + round);
	}

	/*
	message[2] = symmetryIndex
	message[3] = exists (0 = false, 1 = true)

	 */
	public static void writeTransactionEnemyHQLoc (int symmetryIndex, int exists) throws GameActionException{
		log("Writing transaction for 'Enemy HQ Location'");
		int[] message = new int[GameConstants.BLOCKCHAIN_TRANSACTION_LENGTH];
		message[0] = encryptID(myID);
		message[1] = ENEMY_HQ_LOCATION_SIGNAL;
		message[2] = symmetryIndex;
		message[3] = exists;

		xorMessage(message);
		if (rc.getTeamSoup() >= dynamicCost) {
			rc.submitTransaction(message, dynamicCost);

		} else {
			tlog("Could not afford transaction");
			saveUnsentTransaction(message, dynamicCost);
		}
	}

	public static void readTransactionEnemyHQLoc (int[] message, int round) throws GameActionException {
		if (message[3] == 1) {
			enemyHQLoc = symmetryHQLocs[message[2]];
			symmetryHQLocsIndex = message[2];
		}
		isSymmetryHQLoc[message[2]] = message[3];
		tlog("Reading transaction for 'Enemy HQ Location'");
		ttlog("Submitter ID: " + decryptID(message[0]));
		ttlog("Location: " + symmetryHQLocs[message[2]]);
		ttlog("Exists: " + isSymmetryHQLoc[message[2]]);
		ttlog("Posted round: " + round);
	}
	/*
	message[2] = x coordinate of cluster
	message[3] = y coordinate of cluster

	*/
	public static void writeTransactionSoupCluster (MapLocation soupClusterLocation) throws GameActionException {
		tlog("Writing transaction for 'Soup Cluster' at " + soupClusterLocation);
		int[] message = new int[GameConstants.BLOCKCHAIN_TRANSACTION_LENGTH];
		message[0] = encryptID(myID);
		message[1] = SOUP_CLUSTER_SIGNAL;
		message[2] = soupClusterLocation.x;
		message[3] = soupClusterLocation.y;

		xorMessage(message);
		if (rc.getTeamSoup() >= dynamicCost) {
			rc.submitTransaction(message, dynamicCost);

		} else {
			tlog("Could not afford transaction");
			saveUnsentTransaction(message, dynamicCost);
		}
	}

	public static void readTransactionSoupCluster (int[] message, int round) {
		MapLocation loc = new MapLocation(message[2], message[3]);
		tlog("Reading 'Soup Cluster' transaction");
		ttlog("Submitter ID: " + decryptID(message[0]));
		ttlog("Location: " + loc);
		ttlog("Posted round: " + round);
		BotMinerResource.addToSoupClusters(new MapLocation(message[2], message[3]));
	}

	/*
	message[2] = x coordinate of refinery
	message[3] = y coordinate of refinery

	*/
	public static void writeTransactionRefineryBuilt (MapLocation refineryLocation) throws GameActionException {
		// check money
		log("Writing transaction for 'Refinery Built' at " + refineryLocation);
		int[] message = new int[GameConstants.BLOCKCHAIN_TRANSACTION_LENGTH];
		message[0] = encryptID(myID);
		message[1] = REFINERY_BUILT_SIGNAL;
		message[2] = refineryLocation.x;
		message[3] = refineryLocation.y;

		xorMessage(message);
		if (rc.getTeamSoup() >= dynamicCost) {
			rc.submitTransaction(message, dynamicCost);

		} else {
			tlog("Could not afford transaction");
			saveUnsentTransaction(message, dynamicCost);
		}
	}

	public static void readTransactionRefineryBuilt (int[] message, int round) {
		MapLocation loc = new MapLocation(message[2], message[3]);
		tlog("Reading 'Refinery Built' transaction");
		ttlog("Submitter ID: " + decryptID(message[0]));
		ttlog("Location: " + loc);
		ttlog("Posted round: " + round);
		BotMinerResource.addToRefineries(loc);
	}

	final public static int BUILD_CLOSE_FULFILLMENT_CENTER = 1;
	final public static int BUILD_CLOSE_DESIGN_SCHOOL = 2;
	final public static int BUILD_CLOSE_VAPORATOR = 3;
	final public static int BUILD_CLOSE_REFINERY = 4;
	final public static int BUILD_PLATFORM = 5;

	/*
	message[2] = miner id
	message[3] = round assigned
	message[4] = build instruction
	message[5] = details about build instruction


	*/
	public static void writeTransactionBuildInstruction (int minerID, int instruction, int details) throws GameActionException {
		// check money
		log("Writing transaction for 'Build Instruction' at " + minerID + ": " + instruction);
		int[] message = new int[GameConstants.BLOCKCHAIN_TRANSACTION_LENGTH];
		message[0] = encryptID(myID);
		message[1] = BUILD_INSTRUCTION_SIGNAL;
		message[2] = minerID;
		message[3] = roundNum;
		message[4] = instruction;
		message[5] = details;

		xorMessage(message);
		if (rc.getTeamSoup() >= dynamicCost) {
			rc.submitTransaction(message, dynamicCost);

		} else {
			tlog("Could not afford transaction");
			saveUnsentTransaction(message, dynamicCost);
		}
	}

	public static void readTransactionBuildInstruction (int[] message, int round) {
		tlog("Reading 'Build Instruction' transaction");
		ttlog("Submitter ID: " + decryptID(message[0]));
		ttlog("Builder Miner ID: " + message[2]);
		ttlog("Round Assigned: " + message[3]);
		ttlog("Instruction: " + message[4]);
		ttlog("Detail: " + message[5]);
		ttlog("Posted round: " + round);
		if (message[4] == BUILD_PLATFORM) {
			platformMinerID = message[2];
		}
		if (myType == RobotType.MINER) {
			if (myID == message[2]) {
				BotMiner.myRole = BotMiner.MINER_BUILDER_ROLE;
				BotMinerBuilder.assignRound = message[3];
				BotMinerBuilder.buildInstruction = message[4];
				BotMinerBuilder.buildDetail = message[5];
			}
		}
	}

	final public static int INITIAL_WALL_SETUP_FLAG = 1;
	final public static int WALL_FULL_FLAG = 2;
	final public static int SUPPORT_FULL_FLAG = 3;
	final public static int PLATFORM_BUILDINGS_COMPLETED_FLAG = 4;
	/*
	message[2] = status
	 */
	public static void writeTransactionWallStatus(int status) throws GameActionException {
		log("Writing transaction for 'Wall Status'");
		int[] message = new int[GameConstants.BLOCKCHAIN_TRANSACTION_LENGTH];
		message[0] = encryptID(myID);
		message[1] = WALL_STATUS_SIGNAL;
		message[2] = status;

		xorMessage(message);
		if (rc.getTeamSoup() >= dynamicCost) {
			rc.submitTransaction(message, dynamicCost);

		} else {
			tlog("Could not afford transaction");
			saveUnsentTransaction(message, dynamicCost);
		}
	}

	public static void readTransactionWallStatus(int[] message, int round) throws GameActionException {
		tlog("Reading transaction for 'Wall Status'");
		ttlog("Submitter ID: " + decryptID(message[0]));
		ttlog("Wall Status " + message[2]);
		ttlog("Posted round: " + round);
		switch (message[2]) {
			case INITIAL_WALL_SETUP_FLAG:
				initialWallSetup = true;
				break;
			case WALL_FULL_FLAG:
				wallFull = true;
				break;
			case SUPPORT_FULL_FLAG:
				supportFull = true;
				break;
		}
	}

	/*
	message[2] = flooded tile x
	message[3] = flooded tile y
	 */

	public static void writeTransactionFloodingFound (MapLocation loc) throws GameActionException{
		log("Writing transaction for 'Flooding Found'");
		int[] message = new int[GameConstants.BLOCKCHAIN_TRANSACTION_LENGTH];
		message[0] = encryptID(myID);
		message[1] = FLOODING_FOUND_SIGNAL;
		message[2] = loc.x;
		message[3] = loc.y;

		xorMessage(message);
		if (rc.getTeamSoup() >= dynamicCost) {
			rc.submitTransaction(message, dynamicCost);

		} else {
			tlog("Could not afford transaction");
			saveUnsentTransaction(message, dynamicCost);
		}
	}

	public static void readTransactionFloodingFound (int[] message, int round) throws GameActionException {
		BotDeliveryDrone.floodingMemory = new MapLocation(message[2], message[3]);
		log("Reading transaction for 'Flooding Found'");
		log("Submitter ID: " + decryptID(message[0]));
		log("Location: " + BotDeliveryDrone.floodingMemory);
		log("Posted round: " + round);
	}

	public static void writeTransactionEnemyRush () throws GameActionException{
		log("Writing transaction for 'Enemy Rush'");
		int[] message = new int[GameConstants.BLOCKCHAIN_TRANSACTION_LENGTH];
		message[0] = encryptID(myID);
		message[1] = ENEMY_RUSH_SIGNAL;

		xorMessage(message);
		if (rc.getTeamSoup() >= dynamicCost) {
			rc.submitTransaction(message, dynamicCost);

		} else {
			tlog("Could not afford transaction");
			saveUnsentTransaction(message, dynamicCost);
		}
	}

	public static void readTransactionEnemyRush (int[] message, int round) throws GameActionException {
		enemyRush = true;
		log("Reading transaction for 'Enemy Rush'");
		log("Submitter ID: " + decryptID(message[0]));
		log("Posted round: " + round);
	}

	public static void writeTransactionAssignPlatform (int assignID) throws GameActionException {
		// check money
		log("Writing transaction for 'Assign Platform'" + assignID);
		int[] message = new int[GameConstants.BLOCKCHAIN_TRANSACTION_LENGTH];
		message[0] = encryptID(myID);
		message[1] = ASSIGN_PLATFORM_SIGNAL;
		message[2] = assignID;

		xorMessage(message);
		if (rc.getTeamSoup() >= dynamicCost) {
			rc.submitTransaction(message, dynamicCost);

		} else {
			tlog("Could not afford transaction");
			saveUnsentTransaction(message, dynamicCost);
		}
	}

	public static void readTransactionAssignPlatform (int[] message, int round) {
		platformLandscaperID = message[2];
		tlog("Reading 'Assign Platform' transaction");
		ttlog("Submitter ID: " + decryptID(message[0]));
		ttlog("platformerID: " + platformLandscaperID);
		ttlog("Posted round: " + round);
	}

	/*
	message[2] = wallFull and supportFull
	message[3] = explored symmetries
	message[4] = location.x + location.y
	message[5] = platformerID
	 */
	public static void writeTransactionReview() throws GameActionException{
		log("Writing transaction for 'Review'");
		int[] message = new int[GameConstants.BLOCKCHAIN_TRANSACTION_LENGTH];
		message[0] = encryptID(myID);
		message[1] = REVIEW_SIGNAL;

		if (initialWallSetup) {
			message[2] |= (1 << INITIAL_WALL_SETUP_FLAG);
		}
		if (wallFull) {
			message[2] |= (1 << WALL_FULL_FLAG);
		}
		if (supportFull) {
			message[2] |= (1 << SUPPORT_FULL_FLAG);
		}
		if (platformBuildingsCompleted) {
			message[2] |= (1 << PLATFORM_BUILDINGS_COMPLETED_FLAG);
		}

		if (enemyHQLoc == null) {
			for (int i = 0; i < symmetryHQLocs.length; i++) {
				if (isSymmetryHQLoc[i] == 2) {
					message[3] |= (1 << i);
				}
			}
		} else {
			message[3] = (1 << 16) | symmetryHQLocsIndex;
		}

		message[4] = (PLATFORM_ELEVATION << 12) | (platformCornerLoc.x << 6) | platformCornerLoc.y;
		message[5] = platformLandscaperID;

		xorMessage(message);
		if (rc.getTeamSoup() >= dynamicCost) {
			rc.submitTransaction(message, dynamicCost);

		} else {
			tlog("Could not afford transaction");
			saveUnsentTransaction(message, dynamicCost);
		}
	}

	public static void readTransactionReview(int[] message, int round) throws GameActionException {
		hasReadReviewTransaction = true;

		tlog("Reading transaction for 'Review'");
		ttlog("Submitter ID: " + decryptID(message[0]));

		initialWallSetup = (message[2] & (1 << INITIAL_WALL_SETUP_FLAG)) > 0;
		wallFull = (message[2] & (1 << WALL_FULL_FLAG)) > 0;
		supportFull = (message[2] & (1 << SUPPORT_FULL_FLAG)) > 0;
		platformBuildingsCompleted = (message[2] & (1 << PLATFORM_BUILDINGS_COMPLETED_FLAG)) > 0;
		ttlog("initialWallSetup " + initialWallSetup);
		ttlog("wallFull " + wallFull);
		ttlog("supportFull " + supportFull);
		ttlog("platformBuildingsCompleted " + platformBuildingsCompleted);

		if ((message[3] & (1 << 16)) == 0) {
			for (int i = 0; i < symmetryHQLocs.length; i++) {
				if ((message[3] & (1 << i)) > 0) {
					ttlog("Symmetry " + i + " denied");
					isSymmetryHQLoc[i] = 2;
				}
			}
		} else {
			symmetryHQLocsIndex = message[3] & ((1 << 16) - 1);

			enemyHQLoc = symmetryHQLocs[symmetryHQLocsIndex];
			ttlog("Symmetry " + symmetryHQLocsIndex + " confirmed");
		}

		PLATFORM_ELEVATION = message[4] >>> 12;
		platformCornerLoc = new MapLocation((message[4] >>> 6) & ((1 << 6) - 1), message[4] & ((1 << 6) - 1));
		platformLocs = new MapLocation[4];
		platformLocs[0] = platformCornerLoc;
		platformLocs[1] = platformCornerLoc.translate(1,0);
		platformLocs[2] = platformCornerLoc.translate(1,1);
		platformLocs[3] = platformCornerLoc.translate(0,1);

		platformLandscaperID = message[5];
		ttlog("Platform elevation " + PLATFORM_ELEVATION);
		ttlog("Platform location " + platformCornerLoc);
		ttlog("platformerID " + platformLandscaperID);

		ttlog("Posted round: " + round);
	}

	public static void writeTransactionPlatformCompleted() throws GameActionException {
		log("Writing transaction for 'Platform Completed'");
		int[] message = new int[GameConstants.BLOCKCHAIN_TRANSACTION_LENGTH];
		message[0] = encryptID(myID);
		message[1] = PLATFORM_COMPLETED_SIGNAL;
		message[2] = 1;
		xorMessage(message);
		if (rc.getTeamSoup() >= dynamicCost) {
			rc.submitTransaction(message, dynamicCost);
		} else {
			tlog("Could not afford transaction");
			saveUnsentTransaction(message, dynamicCost);
		}
	}

	public static void readTransactionPlatformCompleted(int[] message, int round) throws GameActionException {
		tlog("Reading transaction for 'Platform Completed'");
		ttlog("Submitter ID: " + decryptID(message[0]));

		if (message[2] == 1) {
			platformCompleted = true;
		}
		ttlog("Posted round: " + round);
	}

	public static void writeTransactionPlatformBuildingsCompleted() throws GameActionException {
		log("Writing transaction for 'Platform Buildings Completed'");
		int[] message = new int[GameConstants.BLOCKCHAIN_TRANSACTION_LENGTH];
		message[0] = encryptID(myID);
		message[1] = PLATFORM_BUILDINGS_COMPLETED_SIGNAL;
		message[2] = 1;
		xorMessage(message);
		if (rc.getTeamSoup() >= dynamicCost) {
			rc.submitTransaction(message, dynamicCost);
		} else {
			tlog("Could not afford transaction");
			saveUnsentTransaction(message, dynamicCost);
		}
	}

	public static void readTransactionPlatformBuildingsCompleted(int[] message, int round) throws GameActionException {
		tlog("Reading transaction for 'Platform Buildings Completed'");
		ttlog("Submitter ID: " + decryptID(message[0]));
		platformBuildingsCompleted = true;
		ttlog("Posted round: " + round);
	}

	/*
	status = 1 means continue
	status = -1 means abort
	 */
	final public static int ABORT_RUSH_FLAG = 1;
	final public static int CONTINUE_RUSH_FLAG = 2;
	public static void writeTransactionRushStatus(int status) throws GameActionException {
		log("Writing transaction for 'Rush Status'");
		int[] message = new int[GameConstants.BLOCKCHAIN_TRANSACTION_LENGTH];
		message[0] = encryptID(myID);
		message[1] = RUSH_STATUS_SIGNAL;
		message[2] = status;
		xorMessage(message);
		if (rc.getTeamSoup() >= dynamicCost) {
			rc.submitTransaction(message, dynamicCost);
		} else {
			tlog("Could not afford transaction");
			saveUnsentTransaction(message, dynamicCost);
		}
	}

	public static void readTransactionRushStatus(int[] message, int round) throws GameActionException {
		tlog("Reading transaction for 'Rush Status'");
		ttlog("Submitter ID: " + decryptID(message[0]));
		switch(message[2]) {
			case ABORT_RUSH_FLAG:
				ttlog("ABORTING RUSH");
				abortRush = true;
				break;
			case CONTINUE_RUSH_FLAG:
				ttlog("CONTINUE RUSH");
				continueRushSignalRound = round;
				break;
		}
		ttlog("Posted round: " + round);
	}
}
