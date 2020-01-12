package kryptonite;

import battlecode.common.*;

public class Communication extends Globals {

	final public static int MAX_UNSENT_TRANSACTIONS_LENGTH = 100;
	final public static int READ_OLD_TRANSACTIONS_COST = 2000; // how many bytecodes readOldTransactions() will leave available

	// each of these signals should be different
	final public static int HQ_FIRST_TURN_SIGNAL = 0;
	final public static int SOUP_CLUSTER_SIGNAL = 1;
	final public static int REFINERY_BUILT_SIGNAL = 2;
	final public static int SYMMETRY_MINER_BUILT_SIGNAL = 3;
	final public static int BUILDER_MINER_BUILT_SIGNAL = 4;
	final public static int SMALL_WALL_BUILD_SIGNAL = 5;
	final public static int DRONE_CHECKPOINT = 6;
	final public static int LANDSCAPER_CHECKPOINT = 7;

	// the cost of each of the transaction signals
	final public static int REFINERY_BUILT_COST = 1;

	// used to alter our own data
	public static int secretKey;
	public static int[] secretXORKeys = {0B01011011101111011111011111101111,
										 0B10110111011110111110111111011110,
										 0B01101110111101111101111110111101,
										 0B11011101111011111011111101111010,
										 0B10111011110111110111111011110101,
										 0B01110111101111101111110111101011,
										 0B11101111011111011111101111010110};


	public static int[][] unsentMessages = new int[MAX_UNSENT_TRANSACTIONS_LENGTH][7];
	public static int[] unsentCosts = new int[MAX_UNSENT_TRANSACTIONS_LENGTH];
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
			return;
		}
		int startTransactionsIndex = oldTransactionsIndex;
		while (Clock.getBytecodesLeft() >= READ_OLD_TRANSACTIONS_COST && oldTransactionsIndex < spawnRound - 1) {
			readTransactions(oldTransactionsIndex);
			oldTransactionsIndex++;
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
						readTransactionBuilderMinerBuilt(message,round);
						break;

					case SMALL_WALL_BUILD_SIGNAL:
						readTransactionSmallWallComplete(message,round);
						break;

					case DRONE_CHECKPOINT:
						readTransactionDroneCheckpoint(message,round);
						break;

					case LANDSCAPER_CHECKPOINT:
						readTransactionLandscaperCheckpoint(message,round);
						break;

		        }
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
		unsentCosts[unsentTransactionsLength] = cost;
		unsentTransactionsLength++;
	}

	public static void submitUnsentTransactions () throws GameActionException {
		while (unsentTransactionsIndex < unsentTransactionsLength) {
			int[] message = unsentMessages[unsentTransactionsIndex];
			int cost = unsentCosts[unsentTransactionsIndex];
			if (cost <= teamSoup) {
				rc.submitTransaction(message, cost);
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

	*/
	public static void writeTransactionHQFirstTurn (MapLocation myHQLocation) throws GameActionException {
		Debug.tlog("Writing transaction for 'HQ First Turn' at " + myHQLocation);
		int[] message = new int[7];
		message[0] = encryptID(myID);
		message[1] = HQ_FIRST_TURN_SIGNAL;
		message[2] = myHQLocation.x;
		message[3] = myHQLocation.y;

		xorMessage(message);
		if (teamSoup >= 10) {
			rc.submitTransaction(message, 10);
			teamSoup = rc.getTeamSoup();
		} else {
			Debug.tlog("Could not afford transaction");
			saveUnsentTransaction(message, 10);
		}
	}

	public static void readTransactionHQFirstTurn (int[] message, int round) {
		Debug.tlog("Reading 'HQ First Turn' transaction");
		HQLocation = new MapLocation(message[2], message[3]);
		Debug.ttlog("Submitter ID: " + decryptID(message[0]));
		Debug.ttlog("Location: " + HQLocation);
		Debug.ttlog("Posted round: " + round);
	}

	/*
message[2] = x coordinate of our HQ
message[3] = y coordinate of our HQ

*/
	public static void writeTransactionSmallWallComplete () throws GameActionException {
		Debug.tlog("Writing transaction for 'Small Wall Complete'");
		int[] message = new int[7];
		message[0] = encryptID(myID);
		message[1] = SMALL_WALL_BUILD_SIGNAL;

		xorMessage(message);
		if (teamSoup >= 10) {
			rc.submitTransaction(message, 10);
			teamSoup = rc.getTeamSoup();
		} else {
			Debug.tlog("Could not afford transaction");
			saveUnsentTransaction(message, 10);
		}
	}

	public static void readTransactionSmallWallComplete (int[] message, int round) {
		Debug.tlog("Reading 'Small Wall Complete' transaction");
		smallWallComplete = true;
		Debug.ttlog("Submitter ID: " + decryptID(message[0]));
		Debug.ttlog("Posted round: " + round);
	}

	/*
	message[2] = x coordinate of cluster
	message[3] = y coordinate of cluster

	*/
	public static void writeTransactionSoupCluster (MapLocation soupClusterLocation) throws GameActionException {
		Debug.tlog("Writing transaction for 'Soup Cluster' at " + soupClusterLocation);
		int[] message = new int[7];
		message[0] = encryptID(myID);
		message[1] = SOUP_CLUSTER_SIGNAL;
		message[2] = soupClusterLocation.x;
		message[3] = soupClusterLocation.y;

		xorMessage(message);
		if (teamSoup >= 1) {
			rc.submitTransaction(message, 1);
			teamSoup = rc.getTeamSoup();
		} else {
			Debug.tlog("Could not afford transaction");
			saveUnsentTransaction(message, 1);
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
		int[] message = new int[7];
		message[0] = encryptID(myID);
		message[1] = REFINERY_BUILT_SIGNAL;
		message[2] = refineryLocation.x;
		message[3] = refineryLocation.y;

		xorMessage(message);
		if (teamSoup >= 1) {
			rc.submitTransaction(message, 1);
			rc.submitTransaction(message, REFINERY_BUILT_COST);
			teamSoup = rc.getTeamSoup();
		} else {
			Debug.tlog("Could not afford transaction");
			saveUnsentTransaction(message, 1);
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
	message[2] = x coordinate of refinery
	message[3] = y coordinate of refinery

	*/
	public static void writeTransactionSymmetryMinerBuilt(int symmetryMinerID, MapLocation symmetryLocation) throws GameActionException {
		// check money
		Debug.tlog("Writing transaction for 'Symmetry Miner Built' with ID " + symmetryMinerID + " finding " + symmetryLocation);
		int[] message = new int[7];
		message[0] = encryptID(myID);
		message[1] = SYMMETRY_MINER_BUILT_SIGNAL;
		message[2] = symmetryMinerID;
		message[3] = symmetryLocation.x;
		message[4] = symmetryLocation.y;

		xorMessage(message);
		if (teamSoup >= 1) {
			rc.submitTransaction(message, 1);
			teamSoup = rc.getTeamSoup();
		} else {
			Debug.tlog("Could not afford transaction");
			saveUnsentTransaction(message, 1);
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

	public static void writeTransactionBuilderMinerBuilt(int id) throws GameActionException{
		Debug.tlog("Writing transaction for Builder Miner of ID: " + id);
		int[] message = new int[7];
		message[0] = encryptID(myID);
		message[1] = BUILDER_MINER_BUILT_SIGNAL;
		message[2] = id;

		xorMessage(message);
		if (teamSoup >= 1) {
			rc.submitTransaction(message, 1);
			teamSoup = rc.getTeamSoup();
		} else {
			Debug.tlog("Could not afford transaction");
			saveUnsentTransaction(message, 1);
		}
	}

	public static void readTransactionBuilderMinerBuilt(int[] message, int round) {
		int builderMinerID = message[2];
		Debug.tlog("Reading 'Builder Miner Built' transaction");
		Debug.ttlog("Submitter ID: " + decryptID(message[0]));
		Debug.ttlog("builderMinerID: " + builderMinerID);
		Debug.ttlog("Posted round: " + round);
		BotMiner.builderMinerID = builderMinerID;
	}

	public static void writeTransactionDroneCheckpoint(int checkpoint) throws GameActionException{
		Debug.tlog("Writing transaction for drone checkpoint " + checkpoint );
		int[] message = new int[7];
		message[0] = encryptID(myID);
		message[1] = DRONE_CHECKPOINT;
		message[2] = checkpoint;

		xorMessage(message);
		if (teamSoup >= 1) {
			rc.submitTransaction(message, 2);
			teamSoup = rc.getTeamSoup();
		} else {
			Debug.tlog("Could not afford transaction");
			saveUnsentTransaction(message, 2);
		}

	}

	public static void readTransactionDroneCheckpoint(int[] message, int round) {
		int checkpoint_number = message[2];
		Debug.tlog("Reading 'Drone checkpoint'");
		Debug.tlog("Submitter ID: " + decryptID(message[0]));
		Debug.tlog("Checkpoint Number " + checkpoint_number);
		Debug.tlog("Posted round: " + round);
		BotBuilderMiner.droneCheckpoint = checkpoint_number;
	}

	public static void writeTransactionLandscaperCheckpoint(int checkpoint) throws GameActionException{
		Debug.tlog("Writing transaction for landscaper checkpoint " + checkpoint );
		int[] message = new int[7];
		message[0] = encryptID(myID);
		message[1] = LANDSCAPER_CHECKPOINT;
		message[2] = checkpoint;
		xorMessage(message);
		if (teamSoup >= 1) {
			rc.submitTransaction(message, 2);
			teamSoup = rc.getTeamSoup();
		} else {
			Debug.tlog("Could not afford transaction");
			saveUnsentTransaction(message, 2;
		}

	}

	public static void readTransactionLandscaperCheckpoint(int[] message, int round) {
		int checkpoint_number = message[2];
		Debug.tlog("Reading 'Landscaper checkpoint'");
		Debug.tlog("Submitter ID: " + decryptID(message[0]));
		Debug.tlog("Checkpoint Number " + checkpoint_number);
		Debug.tlog("Posted round: " + round);
		BotBuilderMiner.landscaperCheckpoint = checkpoint_number;
	}
}
