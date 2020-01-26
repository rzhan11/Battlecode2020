package rush_bot;

import battlecode.common.*;

import static rush_bot.Debug.*;
import static rush_bot.Wall.*;
import static rush_bot.Zones.*;

public class Communication extends Globals {

	final public static int RESUBMIT_EARLY = 10;
	final public static int RESUBMIT_INTERVAL = 100;

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
			writeTransactionAllExploredZones();
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
						if (myType == RobotType.MINER) {
							readTransactionBuildInstruction(message, round);
						}
						break;
					case WALL_STATUS_SIGNAL:
						if (!isLowBytecodeLimit(myType)) {
							readTransactionWallStatus(message, round);
						}
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
					/*case ALL_ZONE_STATUS_SIGNAL:
						if (!isLowBytecodeLimit(myType)) {
							readTransactionAllExploredZones(message, round);
						}
						break;*/
					case REVIEW_SIGNAL:
						readTransactionReview(message, round);
						break;
				}
			}

			index++;
		}

		return index;
	}

	public static boolean[] hasReadAllExploredZones = {false, false};
	public static boolean hasReadReviewTransaction = false;
	public static boolean hasReadAllReviews = false;

	public static void readReviewBlock (int round, int count) throws GameActionException {
		if (hasReadAllReviews) {
			return;
		}

		if (count == 5) {
			return;
		}

		while (round >= roundNum) {
			log("WAITING FOR REVIEW BLOCK " + round);
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
					case ALL_ZONE_STATUS_SIGNAL:
						if (!isLowBytecodeLimit(myType)) {
							readTransactionAllExploredZones(message, round);
						}
						break;
					case REVIEW_SIGNAL:
						readTransactionReview(message, round);
						break;
				}
			}
		}
		if (hasReadAllExploredZones[0] && hasReadAllExploredZones[1] && hasReadReviewTransaction) {
			hasReadAllReviews = true;
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
		if (myID == message[2]) {
			BotMiner.myRole = BotMiner.MINER_BUILDER_ROLE;
			BotMinerBuilder.assignRound = message[3];
			BotMinerBuilder.buildInstruction = message[4];
			BotMinerBuilder.buildDetail = message[5];
		}
	}

	/*
	status:
		1 = wallFull
		2 = supportFull
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
			case 1:
				wallFull = true;
				break;
			case 2:
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

	/*
	message[2] = flooded tile x
	message[3] = flooded tile y
	 */

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

	public static void writeTransactionAllExploredZones() throws GameActionException{
		for (int part = 0; part < 2; part++) {
			log("Writing transaction for 'All Explored Zones' part " + part);
			int[] message = new int[GameConstants.BLOCKCHAIN_TRANSACTION_LENGTH];
			message[0] = encryptID(myID);
			message[1] = ALL_ZONE_STATUS_SIGNAL;
			message[2] = part;
			for (int i = part * 8; i < Math.min(part * 8 + 8, numYZones); i++) {
				int i_message = (i % 8) / 2 + 3;
				for (int j = 0; j < numYZones; j++) {
					if (exploredZoneStatus[i][j] == 1) {
						int bitLoc = (i % 2) * 16 + j;
						message[i_message] |= (exploredZoneStatus[i][j] << bitLoc);
					}
				}
			}

			xorMessage(message);
			if (rc.getTeamSoup() >= dynamicCost) {
				rc.submitTransaction(message, dynamicCost);

			} else {
				tlog("Could not afford transaction");
				saveUnsentTransaction(message, dynamicCost);
			}
		}
	}

	public static void readTransactionAllExploredZones(int[] message, int round) throws GameActionException {
		int part = message[2];
		if (hasReadAllExploredZones[part]) {
			return;
		}
		hasReadAllExploredZones[part] = true;
		ttlog("Reading transaction for 'All Explored Zones'");
		tlog("Submitter ID: " + decryptID(message[0]));
		tlog("Part: " + part);
		for (int i = part * 8; i < Math.min(part * 8 + 8, numYZones); i++) {
			int i_message = (i % 8) / 2 + 3;
			for (int j = 0; j < numYZones; j++) {
				int bitLoc = (i % 2) * 16 + j;
				if ((message[i_message] & (1 << bitLoc)) > 0) {
					exploredZoneStatus[i][j] = 1;
				}
			}
		}
		tlog("Posted round: " + round);
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
		platformerID = message[2];
		tlog("Reading 'Assign Platform' transaction");
		ttlog("Submitter ID: " + decryptID(message[0]));
		ttlog("platformerID: " + platformerID);
		ttlog("Posted round: " + round);
	}

	/*
	message[2] = wallFull and supportFull
	message[3] = explored symmetries
	message[4] = location.x + location.y
	 */
	public static void writeTransactionReview() throws GameActionException{
		log("Writing transaction for 'Review'");
		int[] message = new int[GameConstants.BLOCKCHAIN_TRANSACTION_LENGTH];
		message[0] = encryptID(myID);
		message[1] = REVIEW_SIGNAL;
		if (wallFull) {
			message[2] |= 1;
		}
		if (supportFull) {
			message[2] |= 2;
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
		message[4] = (platformLoc.x << 16) | platformLoc.y;

		xorMessage(message);
		if (rc.getTeamSoup() >= dynamicCost) {
			rc.submitTransaction(message, dynamicCost);

		} else {
			tlog("Could not afford transaction");
			saveUnsentTransaction(message, dynamicCost);
		}
	}

	public static void readTransactionReview(int[] message, int round) throws GameActionException {
		if (hasReadReviewTransaction) {
			return;
		}
		hasReadReviewTransaction = true;

		tlog("Reading transaction for 'Review'");
		ttlog("Submitter ID: " + decryptID(message[0]));

		wallFull = (message[2] & 1) > 0;
		supportFull = (message[2] & 2) > 0;
		ttlog("wallFull " + wallFull);
		ttlog("supportFull " + supportFull);

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

		platformLoc = new MapLocation(message[4] >>> 16, message[4] & ((1 << 16) - 1));
		log("Platform loc " + platformLoc);

		ttlog("Posted round: " + round);
	}
}
