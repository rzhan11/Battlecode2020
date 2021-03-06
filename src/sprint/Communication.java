package sprint;

import battlecode.common.*;

public class Communication extends Globals {

	final public static int MAX_UNSENT_TRANSACTIONS_LENGTH = 100;
	final public static int READ_OLD_TRANSACTIONS_COST = 2500; // how many bytecodes readOldTransactions() will leave available

	final public static int FIRST_TURN_DYNAMIC_COST = 3;

	// each of these signals should be different
	final public static int HQ_FIRST_TURN_SIGNAL = 0;
	final public static int SOUP_CLUSTER_SIGNAL = 1;
	final public static int REFINERY_BUILT_SIGNAL = 2;
	final public static int SYMMETRY_MINER_BUILT_SIGNAL = 3;
	final public static int BUILDER_MINER_BUILT_SIGNAL = 4;
	final public static int SMALL_WALL_BUILD_SIGNAL = 5;
	final public static int DRONE_CHECKPOINT_SIGNAL = 6;
	final public static int LANDSCAPER_CHECKPOINT_SIGNAL = 7;
	final public static int VAPORATOR_CHECKPOINT_SIGNAL = 8;
	final public static int NETGUN_CHECKPOINT_SIGNAL = 9;
	final public static int FLOODING_FOUND_SIGNAL = 10;
	final public static int ENEMY_HQ_LOCATION_SIGNAL = 11;
	final public static int LARGE_WALL_FULL_SIGNAL = 12;

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
	    return id * 65536 + ((id + secretKey) % 65536);
	}

	/*
	Returns the id value of the transaction submitter
	Returns -1 if not our team
	*/
	public static int decryptID (int code) {
		int id = code / 65536;
		int confirm = code % 65536;
		if (id == (confirm - secretKey + 65536) % 65536) {
			return id;
		} else {
			return -1;
		}
	}

	public static void readOldTransactions () throws GameActionException {
		if (oldTransactionsIndex >= spawnRound - 1) {
			Debug.tlog("No old transactions to be read");
			return;
		} else {
			Debug.tlog("Trying to read old transactions");
		}
		int startTransactionsIndex = oldTransactionsIndex;
//		Debug.tlog("b " + Clock.getBytecodesLeft());
		while (Clock.getBytecodesLeft() >= READ_OLD_TRANSACTIONS_COST && oldTransactionsIndex < spawnRound - 1) {
			readTransactions(oldTransactionsIndex);
			oldTransactionsIndex++;
//			Debug.tlog("b " + Clock.getBytecodesLeft());
		}
		if (oldTransactionsIndex > startTransactionsIndex) {
			Debug.tlog("Read old transactions " + startTransactionsIndex + " to " + (oldTransactionsIndex - 1));
		} else {
			Debug.tlog("Unable to read old transactions due to bytecode limit");
		}
	}

	/*
	Reads in transactions that were submitted last round
	*/
	public static void readTransactions (int round) throws GameActionException {
		if (round < 1 || round >= rc.getRoundNum()) {
			Debug.tlog("Tried to read Transactions of round " + round + " but not possible");
			return;
		}
		Transaction[] block = rc.getBlock(round);
		for (Transaction t: block) {
			int[] message = t.getMessage();
			xorMessage(message);

			int submitterID = decryptID(message[0]);
			if (submitterID == -1) {
				Debug.tlog("Found opponent Transactions");
				continue; // not submitted by our team
			} else {
		        switch (message[1]) {
		            case HQ_FIRST_TURN_SIGNAL:
						readTransactionHQFirstTurn(message, round);
						break;
		            case SOUP_CLUSTER_SIGNAL:
						if (myType == RobotType.MINER) {
							readTransactionSoupCluster(message, round);
						}
						break;
		            case REFINERY_BUILT_SIGNAL:
						if (myType == RobotType.MINER) {
							readTransactionRefineryBuilt(message, round);
						}
						break;
		            case SYMMETRY_MINER_BUILT_SIGNAL:
						if (myType == RobotType.MINER) {
							readTransactionSymmetryMinerBuilt(message, round);
						}
						break;

					case BUILDER_MINER_BUILT_SIGNAL:
						readTransactionBuilderMinerBuilt(message, round);
						break;

					case SMALL_WALL_BUILD_SIGNAL:
						readTransactionSmallWallComplete(message, round);
						break;

					case DRONE_CHECKPOINT_SIGNAL:
						readTransactionDroneCheckpoint(message, round);
						break;

					case LANDSCAPER_CHECKPOINT_SIGNAL:
						readTransactionLandscaperCheckpoint(message, round);
						break;

					case VAPORATOR_CHECKPOINT_SIGNAL:
						readTransactionVaporatorCheckpoint(message, round);
						break;

					case NETGUN_CHECKPOINT_SIGNAL:
						readTransactionNetgunCheckpoint(message, round);
						break;

					case FLOODING_FOUND_SIGNAL:
						readTransactionFloodingFound(message, round);
						break;

					case ENEMY_HQ_LOCATION_SIGNAL:
						readTransactionEnemyHQLocation(message, round);
						break;

					case LARGE_WALL_FULL_SIGNAL:
						readTransactionLargeWallFull(message, round);
						break;
		        }
			}
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
			Debug.tlogi("ERROR: unsentTransactionsLength reached MAX_UNSENT_TRANSACTIONS_LENGTH limit");
			return;
		}
		unsentMessages[unsentTransactionsLength] = message;
		unsentTransactionsLength++;
	}

	public static void submitUnsentTransactions () throws GameActionException {
		while (unsentTransactionsIndex < unsentTransactionsLength) {
			int[] message = unsentMessages[unsentTransactionsIndex];
			if (dynamicCost <= teamSoup) {
				rc.submitTransaction(message, dynamicCost);
				teamSoup = rc.getTeamSoup();
				unsentTransactionsIndex++;

				xorMessage(message);
				Debug.tlog("Submitted unsent transaction with signal of " + message[1]);
			} else {
				break;
			}
		}
	}

	/*
	message[2] = x coordinate of our HQ
	message[3] = y coordinate of our HQ
	message[4] = elevation of our HQ
	*/
	public static void writeTransactionHQFirstTurn (MapLocation myHQLocation) throws GameActionException {
		Debug.tlog("Writing transaction for 'HQ First Turn' at " + myHQLocation);
		int[] message = new int[GameConstants.BLOCKCHAIN_TRANSACTION_LENGTH];
		message[0] = encryptID(myID);
		message[1] = HQ_FIRST_TURN_SIGNAL;
		message[2] = myHQLocation.x;
		message[3] = myHQLocation.y;
		message[4] = myElevation;

		xorMessage(message);
		if (teamSoup >= dynamicCost) {
			rc.submitTransaction(message, dynamicCost);
			teamSoup = rc.getTeamSoup();
		} else {
			Debug.tlog("Could not afford transaction");
			saveUnsentTransaction(message, dynamicCost);
		}
	}

	public static void readTransactionHQFirstTurn (int[] message, int round) {
		Debug.tlog("Reading 'HQ First Turn' transaction");
		HQLocation = new MapLocation(message[2], message[3]);
		HQElevation = message[4];
		Debug.ttlog("Submitter ID: " + decryptID(message[0]));
		Debug.ttlog("Location: " + HQLocation);
		Debug.ttlog("Elevation: " + HQElevation);
		Debug.ttlog("Posted round: " + round);
	}

	/*
message[2] = x coordinate of our HQ
message[3] = y coordinate of our HQ

*/
	public static void writeTransactionSmallWallComplete () throws GameActionException {
		Debug.tlog("Writing transaction for 'Small Wall Complete'");
		int[] message = new int[GameConstants.BLOCKCHAIN_TRANSACTION_LENGTH];
		message[0] = encryptID(myID);
		message[1] = SMALL_WALL_BUILD_SIGNAL;

		xorMessage(message);
		if (teamSoup >= dynamicCost) {
			rc.submitTransaction(message, dynamicCost);
			teamSoup = rc.getTeamSoup();
		} else {
			Debug.tlog("Could not afford transaction");
			saveUnsentTransaction(message, dynamicCost);
		}
	}

	public static void readTransactionSmallWallComplete (int[] message, int round) {
		Debug.tlog("Reading 'Small Wall Complete' transaction");
		smallWallFinished = true;
		Debug.ttlog("Submitter ID: " + decryptID(message[0]));
		Debug.ttlog("Posted round: " + round);
	}

	/*
	message[2] = x coordinate of cluster
	message[3] = y coordinate of cluster

	*/
	public static void writeTransactionSoupCluster (MapLocation soupClusterLocation) throws GameActionException {
		Debug.tlog("Writing transaction for 'Soup Cluster' at " + soupClusterLocation);
		int[] message = new int[GameConstants.BLOCKCHAIN_TRANSACTION_LENGTH];
		message[0] = encryptID(myID);
		message[1] = SOUP_CLUSTER_SIGNAL;
		message[2] = soupClusterLocation.x;
		message[3] = soupClusterLocation.y;

		xorMessage(message);
		if (teamSoup >= dynamicCost) {
			rc.submitTransaction(message, dynamicCost);
			teamSoup = rc.getTeamSoup();
		} else {
			Debug.tlog("Could not afford transaction");
			saveUnsentTransaction(message, dynamicCost);
		}
	}

	public static void readTransactionSoupCluster (int[] message, int round) {
		MapLocation loc = new MapLocation(message[2], message[3]);
		Debug.tlog("Reading 'Soup Cluster' transaction");
		Debug.ttlog("Submitter ID: " + decryptID(message[0]));
		Debug.ttlog("Location: " + loc);
		Debug.ttlog("Posted round: " + round);
		BotMiner.addToSoupClusters(new MapLocation(message[2], message[3]));
	}

	/*
	message[2] = x coordinate of refinery
	message[3] = y coordinate of refinery

	*/
	public static void writeTransactionRefineryBuilt (MapLocation refineryLocation) throws GameActionException {
		// check money
		Debug.tlog("Writing transaction for 'Refinery Built' at " + refineryLocation);
		int[] message = new int[GameConstants.BLOCKCHAIN_TRANSACTION_LENGTH];
		message[0] = encryptID(myID);
		message[1] = REFINERY_BUILT_SIGNAL;
		message[2] = refineryLocation.x;
		message[3] = refineryLocation.y;

		xorMessage(message);
		if (teamSoup >= dynamicCost) {
			rc.submitTransaction(message, dynamicCost);
			teamSoup = rc.getTeamSoup();
		} else {
			Debug.tlog("Could not afford transaction");
			saveUnsentTransaction(message, dynamicCost);
		}
	}

	public static void readTransactionRefineryBuilt (int[] message, int round) {
		MapLocation loc = new MapLocation(message[2], message[3]);
		Debug.tlog("Reading 'Refinery Built' transaction");
		Debug.ttlog("Submitter ID: " + decryptID(message[0]));
		Debug.ttlog("Location: " + loc);
		Debug.ttlog("Posted round: " + round);
		BotMiner.addToRefineries(loc);
	}

	/*
	@todo: Remind Richard to finish this
	message[2] = x coordinate of ...
	message[3] = y coordinate of ...

	*/
	public static void writeTransactionSymmetryMinerBuilt(int symmetryMinerID, MapLocation symmetryLocation) throws GameActionException {
		// check money
		Debug.tlog("Writing transaction for 'Symmetry Miner Built' with ID " + symmetryMinerID + " finding " + symmetryLocation);
		int[] message = new int[GameConstants.BLOCKCHAIN_TRANSACTION_LENGTH];
		message[0] = encryptID(myID);
		message[1] = SYMMETRY_MINER_BUILT_SIGNAL;
		message[2] = symmetryMinerID;
		message[3] = symmetryLocation.x;
		message[4] = symmetryLocation.y;

		xorMessage(message);
		if (teamSoup >= dynamicCost) {
			rc.submitTransaction(message, dynamicCost);
			teamSoup = rc.getTeamSoup();
		} else {
			Debug.tlog("Could not afford transaction");
			saveUnsentTransaction(message, dynamicCost);
		}
	}

	public static void readTransactionSymmetryMinerBuilt (int[] message, int round) {
		Debug.tlog("Reading 'Symmetry Miner Built' transaction");
		int symmetryMinerID = message[2];
		MapLocation loc = new MapLocation(message[3], message[4]);
		Debug.ttlog("Submitter ID: " + decryptID(message[0]));
		Debug.ttlog("symmetryMinerID: " + symmetryMinerID);
		Debug.ttlog("symmetryLocation: " + loc);
		Debug.ttlog("Posted round: " + round);
		if (myID == symmetryMinerID) {
			BotMiner.isSymmetryMiner = true;
			BotMiner.symmetryLocation = loc;
			Debug.ttlog("I am the symmetry miner");
		}
	}

	/*
	message[2] = builderMinerID
	 */

	public static void writeTransactionBuilderMinerBuilt(int id) throws GameActionException{
		Debug.tlog("Writing transaction for Builder Miner of ID: " + id);
		int[] message = new int[GameConstants.BLOCKCHAIN_TRANSACTION_LENGTH];
		message[0] = encryptID(myID);
		message[1] = BUILDER_MINER_BUILT_SIGNAL;
		message[2] = id;

		xorMessage(message);
		if (teamSoup >= dynamicCost) {
			rc.submitTransaction(message, dynamicCost);
			teamSoup = rc.getTeamSoup();
		} else {
			Debug.tlog("Could not afford transaction");
			saveUnsentTransaction(message, dynamicCost);
		}
	}

	public static void readTransactionBuilderMinerBuilt(int[] message, int round) {
		BotMiner.builderMinerID = message[2];
		Debug.tlog("Reading 'Builder Miner Built' transaction");
		Debug.ttlog("Submitter ID: " + decryptID(message[0]));
		Debug.ttlog("builderMinerID: " + BotMiner.builderMinerID);
		Debug.ttlog("Posted round: " + round);
	}

	/*
	message[2] = checkpoint
	 */

	public static void writeTransactionDroneCheckpoint(int checkpoint) throws GameActionException{
		Debug.tlog("Writing transaction for drone checkpoint " + checkpoint );
		int[] message = new int[GameConstants.BLOCKCHAIN_TRANSACTION_LENGTH];
		message[0] = encryptID(myID);
		message[1] = DRONE_CHECKPOINT_SIGNAL;
		message[2] = checkpoint;

		xorMessage(message);
		if (teamSoup >= dynamicCost) {
			rc.submitTransaction(message, dynamicCost);
			teamSoup = rc.getTeamSoup();
		} else {
			Debug.tlog("Could not afford transaction");
			saveUnsentTransaction(message, dynamicCost);
		}

	}

	public static void readTransactionDroneCheckpoint(int[] message, int round) {
		int checkpoint_number = message[2];
		Debug.tlog("Reading 'Drone checkpoint'");
		Debug.tlog("Submitter ID: " + decryptID(message[0]));
		Debug.tlog("Checkpoint Number " + checkpoint_number);
		Debug.tlog("Posted round: " + round);
		BotBuilderMiner.reachedDroneCheckpoint = checkpoint_number;
	}

	/*
	message[2] = checkpoint
	 */

	public static void writeTransactionLandscaperCheckpoint(int checkpoint) throws GameActionException{
		Debug.tlog("Writing transaction for landscaper checkpoint " + checkpoint);
		int[] message = new int[GameConstants.BLOCKCHAIN_TRANSACTION_LENGTH];
		message[0] = encryptID(myID);
		message[1] = LANDSCAPER_CHECKPOINT_SIGNAL;
		message[2] = checkpoint;
		xorMessage(message);
		if (teamSoup >= dynamicCost) {
			rc.submitTransaction(message, dynamicCost);
			teamSoup = rc.getTeamSoup();
		} else {
			Debug.tlog("Could not afford transaction");
			saveUnsentTransaction(message, dynamicCost);
		}

	}

	public static void readTransactionLandscaperCheckpoint(int[] message, int round) {
		int checkpoint_number = message[2];
		Debug.tlog("Reading 'Landscaper checkpoint'");
		Debug.tlog("Submitter ID: " + decryptID(message[0]));
		Debug.tlog("Posted round: " + round);
		Debug.tlog("Checkpoint Number " + checkpoint_number);
		BotBuilderMiner.reachedLandscaperCheckpoint = checkpoint_number;
	}

	/*
	message[2] = checkpoint
	 */

	public static void writeTransactionVaporatorCheckpoint() throws GameActionException{
		Debug.tlog("Writing transaction for vaporator checkpoint");
		int[] message = new int[GameConstants.BLOCKCHAIN_TRANSACTION_LENGTH];
		message[0] = encryptID(myID);
		message[1] = VAPORATOR_CHECKPOINT_SIGNAL;
		xorMessage(message);
		if (teamSoup >= dynamicCost) {
			rc.submitTransaction(message, dynamicCost);
			teamSoup = rc.getTeamSoup();
		} else {
			Debug.tlog("Could not afford transaction");
			saveUnsentTransaction(message, dynamicCost);
		}
	}

	public static void readTransactionVaporatorCheckpoint(int[] message, int round) {
		Debug.tlog("Reading 'Vaporator checkpoint'");
		Debug.tlog("Submitter ID: " + decryptID(message[0]));
		Debug.tlog("Posted round: " + round);
		BotFulfillmentCenter.reachedVaporatorCheckpoint = true;
	}

	/*
	message[2] = checkpoint
	 */

	public static void writeTransactionNetgunCheckpoint() throws GameActionException{
		Debug.tlog("Writing transaction for netgun checkpoint");
		int[] message = new int[GameConstants.BLOCKCHAIN_TRANSACTION_LENGTH];
		message[0] = encryptID(myID);
		message[1] = NETGUN_CHECKPOINT_SIGNAL;
		xorMessage(message);
		if (teamSoup >= dynamicCost) {
			rc.submitTransaction(message, dynamicCost);
			teamSoup = rc.getTeamSoup();
		} else {
			Debug.tlog("Could not afford transaction");
			saveUnsentTransaction(message, dynamicCost);
		}
	}

	public static void readTransactionNetgunCheckpoint(int[] message, int round) {
		Debug.tlog("Reading 'Netgun checkpoint'");
		Debug.tlog("Submitter ID: " + decryptID(message[0]));
		Debug.tlog("Posted round: " + round);
		BotDesignSchool.reachedNetgunCheckpoint = true;
	}

	/*
	message[2] = flooded tile x
	message[3] = flooded tile y
	 */

	public static void writeTransactionFloodingFound (MapLocation loc) throws GameActionException{
		Debug.tlog("Writing transaction for 'Flooding Found'");
		int[] message = new int[GameConstants.BLOCKCHAIN_TRANSACTION_LENGTH];
		message[0] = encryptID(myID);
		message[1] = FLOODING_FOUND_SIGNAL;
		message[2] = loc.x;
		message[3] = loc.y;

		xorMessage(message);
		if (teamSoup >= dynamicCost) {
			rc.submitTransaction(message, dynamicCost);
			teamSoup = rc.getTeamSoup();
		} else {
			Debug.tlog("Could not afford transaction");
			saveUnsentTransaction(message, dynamicCost);
		}
	}

	public static void readTransactionFloodingFound (int[] message, int round) throws GameActionException {
		BotDeliveryDrone.floodingMemory = new MapLocation(message[2], message[3]);
		Debug.tlog("Reading transaction for 'Flooding Found'");
		Debug.tlog("Submitter ID: " + decryptID(message[0]));
		Debug.tlog("Location: " + BotDeliveryDrone.floodingMemory);
		Debug.tlog("Posted round: " + round);
	}

	/*
	message[2] = symmetryIndex
	message[3] = exists (0 = false, 1 = true)

	 */
	public static void writeTransactionEnemyHQLocation (int symmetryIndex, int exists) throws GameActionException{
		Debug.tlog("Writing transaction for 'Enemy HQ Location'");
		int[] message = new int[GameConstants.BLOCKCHAIN_TRANSACTION_LENGTH];
		message[0] = encryptID(myID);
		message[1] = ENEMY_HQ_LOCATION_SIGNAL;
		message[2] = symmetryIndex;
		message[3] = exists;

		xorMessage(message);
		if (teamSoup >= dynamicCost) {
			rc.submitTransaction(message, dynamicCost);
			teamSoup = rc.getTeamSoup();
		} else {
			Debug.tlog("Could not afford transaction");
			saveUnsentTransaction(message, dynamicCost);
		}
	}

	public static void readTransactionEnemyHQLocation (int[] message, int round) throws GameActionException {
		if (message[3] == 1) {
			BotOffenseDeliveryDrone.enemyHQLocation = symmetryHQLocations[message[2]];
		}
		BotOffenseDeliveryDrone.isSymmetry[message[2]] = message[3];
		Debug.tlog("Reading transaction for 'Enemy HQ Location'");
		Debug.tlog("Submitter ID: " + decryptID(message[0]));
		Debug.tlog("Location: " + symmetryHQLocations[message[2]]);
		Debug.tlog("Exists: " + BotOffenseDeliveryDrone.isSymmetry[message[2]]);
		Debug.tlog("Posted round: " + round);
	}

	/*
	none

	 */
	public static void writeTransactionLargeWallFull () throws GameActionException{
		Debug.tlog("Writing transaction for 'Large Wall Full'");
		int[] message = new int[GameConstants.BLOCKCHAIN_TRANSACTION_LENGTH];
		message[0] = encryptID(myID);
		message[1] = LARGE_WALL_FULL_SIGNAL;

		xorMessage(message);
		if (teamSoup >= dynamicCost) {
			rc.submitTransaction(message, dynamicCost);
			teamSoup = rc.getTeamSoup();
		} else {
			Debug.tlog("Could not afford transaction");
			saveUnsentTransaction(message, dynamicCost);
		}
	}

	public static void readTransactionLargeWallFull (int[] message, int round) throws GameActionException {
		largeWallFull = true;
		Debug.tlog("Reading transaction for 'Large Wall Full'");
		Debug.tlog("Submitter ID: " + decryptID(message[0]));
		Debug.tlog("Posted round: " + round);
	}
}
