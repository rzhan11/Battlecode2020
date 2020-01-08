package kryptonite;

import battlecode.common.*;

public class Communication extends Globals {

	public static int secretKey;

	public static MapLocation[] soupClusters = new MapLocation[500];
	public static int soupClustersIndex = 0;
	public static int soupClustersSize = 0;

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
					if (message[1] == 0) {
						readClusterTransaction(message);
					}
				}
			}
		}
	}

	/*
	message[2] = x coordinate of cluster
	message[3] = y coordinate of cluster

	*/
	public static void writeClusterTransaction (int x, int y) throws GameActionException {
		Debug.tlog("Writing transaction for cluster at " + new MapLocation(x, y));
		int[] message = new int[7];
		message[0] = encryptID(myID);
		message[1] = 0;
		message[2] = x;
		message[3] = y;

		rc.submitTransaction(message, 1);
	}

	public static void readClusterTransaction (int[] message) {
		soupClusters[soupClustersSize] = new MapLocation(message[2], message[3]);
		Debug.tlog("Reading transaction from id " + decryptID(message[0]) + " for cluster at " + soupClusters[soupClustersSize]);
		soupClustersSize++;
	}
}
