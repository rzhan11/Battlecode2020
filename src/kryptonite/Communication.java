package kryptonite;

import battlecode.common.*;

public class Communication extends Globals {

	final public static int READ_OLD_TRANSACTIONS_COST = 2000; // how many bytecodes readOldTransactions() will leave available

	// each of these signals should be different
	final public static int SOUP_CLUSTER_SIGNAL = 0;
	final public static int REFINERY_BUILT_SIGNAL = 1;

	// used to alter our own data
	public static int secretKey;

	/*
		Communication is made up of 7 integers (32-bit)
		The first integer is used for security (confirming that it is our message)
		The second integer is used to denote the "type" of message
		The rest of the five integers are used as data
	*/

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
		if (oldTransactionsIndex <= 0) {
			return;
		}
		int startTransactionIndex = oldTransactionsIndex;
		while (Clock.getBytecodesLeft() >= READ_OLD_TRANSACTIONS_COST && oldTransactionsIndex > 0) {
			readTransactions(oldTransactionsIndex);
			oldTransactionsIndex--;
		}
		if (oldTransactionsIndex < startTransactionIndex) {
			Debug.tlog("Read old transactions " + (oldTransactionsIndex + 1) + " to " + startTransactionIndex);
		} else {
			Debug.tlog("Unable to read old transactions due to bytecode limit");
		}
	}

	/*
	Reads in transactions that were submitted last round
	*/
	public static void readTransactions (int round) throws GameActionException {
		if (round < 1 || round >= roundNum) {
			Debug.tlog("Tried to read Transactions of round " + round + " but not possible");
			return;
		}
		Transaction[] block = rc.getBlock(round);
		for (Transaction t: block) {
			int[] message = t.getMessage();
			int submitterID = decryptID(message[0]);
			if (submitterID == -1) {
				continue; // not submitted by our team
			} else {
		        switch (message[1]) {
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
		        }
			}
		}
	}

	/*
	message[2] = x coordinate of cluster
	message[3] = y coordinate of cluster

	*/
	public static void writeTransactionSoupCluster (MapLocation soupClusterLoc) throws GameActionException {
		Debug.tlog("Writing transaction for 'soup cluster' at " + soupClusterLoc);
		int[] message = new int[7];
		message[0] = encryptID(myID);
		message[1] = SOUP_CLUSTER_SIGNAL;
		message[2] = soupClusterLoc.x;
		message[3] = soupClusterLoc.y;

		if (teamSoup >= 1) {
			rc.submitTransaction(message, 1);
		} else {
			Debug.tlog("Could not afford transaction");
		}
	}

	public static void readTransactionSoupCluster (int[] message, int round) {
		MapLocation loc = new MapLocation(message[2], message[3]);
		Debug.tlog("Reading 'soup cluster' transaction");
		Debug.ttlog("Submitter ID: " + decryptID(message[0]));
		Debug.ttlog("Location: " + loc);
		Debug.ttlog("Posted round: " + round);
		BotMiner.addToSoupClusters(new MapLocation(message[2], message[3]));
	}

	/*
	message[2] = x coordinate of refinery
	message[3] = y coordinate of refinery

	*/
	public static void writeTransactionRefineryBuilt (MapLocation refineryLoc) throws GameActionException {
		// check money
		Debug.tlog("Writing transaction for 'refinery built' at " + refineryLoc);
		int[] message = new int[7];
		message[0] = encryptID(myID);
		message[1] = REFINERY_BUILT_SIGNAL;
		message[2] = refineryLoc.x;
		message[3] = refineryLoc.y;

		if (teamSoup >= 1) {
			rc.submitTransaction(message, 1);
		} else {
			Debug.tlog("Could not afford transaction");
		}
	}

	public static void readTransactionRefineryBuilt (int[] message, int round) {
		MapLocation loc = new MapLocation(message[2], message[3]);
		Debug.tlog("Reading 'refinery' transaction");
		Debug.ttlog("Submitter ID: " + decryptID(message[0]));
		Debug.ttlog("Location: " + loc);
		Debug.ttlog("Posted round: " + round);
		BotMiner.addToRefineries(loc);
	}
}
