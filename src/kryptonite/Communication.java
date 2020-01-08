package kryptonite;

import battlecode.common.*;

public class Communication extends Globals {

	// used to alter our own data
	public static int secretKey;

	// each of these signals should be different
	final public static int SOUP_CLUSTER_SIGNAL = 0;
	final public static int REFINERY_BUILT_SIGNAL = 1;

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

	/*
	Reads in transactions that were submitted last round
	*/
	public static void readTransactions () throws GameActionException {
		if (roundNum > 1) {
			Transaction[] block = rc.getBlock(roundNum - 1);
			for (Transaction t: block) {
				int[] message = t.getMessage();
				int submitterID = decryptID(message[0]);
				if (submitterID == -1) {
					continue; // not submitted by our team
				} else {
			        switch (message[1]) {
			            case SOUP_CLUSTER_SIGNAL:
							readTransactionSoupCluster(message);
							break;
			            case REFINERY_BUILT_SIGNAL:
							readTransactionRefineryBuilt(message);
							break;
			        }
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
			Debug.tlog("WARNING: Could not afford transaction");
		}
	}

	public static void readTransactionSoupCluster (int[] message) {
		BotMiner.addToSoupClusters(new MapLocation(message[2], message[3]));
		Debug.tlog("Reading transaction from id " + decryptID(message[0]) + " for 'soup cluster' at " + BotMiner.soupClusters[BotMiner.soupClustersSize - 1]);
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
			Debug.tlog("WARNING: Could not afford transaction");
		}
	}

	public static void readTransactionRefineryBuilt (int[] message) {
		BotMiner.addToRefineries(new MapLocation(message[2], message[3]));
		Debug.tlog("Reading transaction from id " + decryptID(message[0]) + " for 'refinery built' at " + BotMiner.refineries[BotMiner.refineriesSize - 1]);
	}
}
