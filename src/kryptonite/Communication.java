package kryptonite;

import battlecode.common.*;

import static kryptonite.Actions.*;
import static kryptonite.Communication.*;
import static kryptonite.Debug.*;
import static kryptonite.Map.*;
import static kryptonite.Nav.*;
import static kryptonite.Utils.*;
import static kryptonite.Zones.*;

public class Communication extends Globals {

	final public static int RESUBMIT_INTERVAL = 100;

	final public static int MAX_UNSENT_TRANSACTIONS_LENGTH = 25;
	final public static int READ_TRANSACTION_MIN_BYTECODE = 500; // how many bytecodes required to read a transaction
	final public static int READ_BIG_TRANSACTION_MIN_BYTECODE = 1500; // how many bytecodes required to read a costly transaction

	final public static int FIRST_TURN_DYNAMIC_COST = 1;

	// each of these signals should be different
	final public static int HQ_FIRST_TURN_SIGNAL = 0;
	final public static int ENEMY_HQ_LOCATION_SIGNAL = 1;
	final public static int EXPLORED_ZONE_STATUS = 2;
	final public static int SOUP_ZONE_STATUS_SIGNAL = 3;
	final public static int REFINERY_BUILT_SIGNAL = 4;

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
		if (roundNum % RESUBMIT_INTERVAL == RESUBMIT_INTERVAL - 5) {
//			if (wallFull) {
//				Communication.writeTransactionWallFull();
//			}
//			if (supportFull) {
//				Communication.writeTransactionSupportWallFull();
//			}
			if (enemyHQLoc != null) {
			 	Communication.writeTransactionEnemyHQLocation(symmetryHQLocationsIndex, 1);
			}
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
				switch (message[1] % (1 << 8)) {
					case HQ_FIRST_TURN_SIGNAL:
						readTransactionHQFirstTurn(message, round);
						break;
					case ENEMY_HQ_LOCATION_SIGNAL:
						readTransactionEnemyHQLocation(message, round);
						break;
					case EXPLORED_ZONE_STATUS:
						if (!isLowBytecodeLimit(myType)) {
							readTransactionExploredZoneStatus(message, round);
						}
						break;
					case SOUP_ZONE_STATUS_SIGNAL:
						if (!isLowBytecodeLimit(myType)) {
							if (Clock.getBytecodesLeft() < READ_BIG_TRANSACTION_MIN_BYTECODE && round != roundNum - 1) {
								return -(index + 1);
							}
							readTransactionSoupStatus(message, round);
						}
						break;
					case REFINERY_BUILT_SIGNAL:
						if (myType == RobotType.MINER) {
							if (Clock.getBytecodesLeft() < READ_BIG_TRANSACTION_MIN_BYTECODE && round != roundNum - 1) {
								return -(index + 1);
							}
							readTransactionRefineryBuilt(message, round);
						}
						break;
				}
			}

			index++;
		}

		return index;
	}

	/*
	message[2] = x coordinate of our HQ
	message[3] = y coordinate of our HQ
	message[4] = elevation of our HQ
	*/
	public static void writeTransactionHQFirstTurn (MapLocation myHQLocation) throws GameActionException {
		log("Writing transaction for 'HQ First Turn' at " + myHQLocation);
		int[] message = new int[GameConstants.BLOCKCHAIN_TRANSACTION_LENGTH];
		message[0] = encryptID(myID);
		message[1] = HQ_FIRST_TURN_SIGNAL;
		message[2] = myHQLocation.x;
		message[3] = myHQLocation.y;
		message[4] = myElevation;

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
		ttlog("Submitter ID: " + decryptID(message[0]));
		ttlog("Location: " + HQLoc);
		ttlog("Elevation: " + HQElevation);
		ttlog("Posted round: " + round);
	}

	/*
	message[2] = symmetryIndex
	message[3] = exists (0 = false, 1 = true)

	 */
	public static void writeTransactionEnemyHQLocation (int symmetryIndex, int exists) throws GameActionException{
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

	public static void readTransactionEnemyHQLocation (int[] message, int round) throws GameActionException {
		if (message[3] == 1) {
			enemyHQLoc = symmetryHQLocations[message[2]];
			symmetryHQLocationsIndex = message[2];
		}
		isSymmetryHQLocation[message[2]] = message[3];
		log("Reading transaction for 'Enemy HQ Location'");
		log("Submitter ID: " + decryptID(message[0]));
		log("Location: " + symmetryHQLocations[message[2]]);
		log("Exists: " + isSymmetryHQLocation[message[2]]);
		log("Posted round: " + round);
	}

	/*
	message[2] = xZone, yZone, status
	message[3] =

	 */
	public static void writeTransactionExploredZoneStatus(int xZone, int yZone, int status) throws GameActionException {
		log("Writing transaction for 'Explored Zone Status'");
		int[] message = new int[GameConstants.BLOCKCHAIN_TRANSACTION_LENGTH];
		message[0] = encryptID(myID);
		message[1] = EXPLORED_ZONE_STATUS;
		message[2] = xZone + (yZone << 6) + (status << 12);

		xorMessage(message);
		if (rc.getTeamSoup() >= dynamicCost) {
			rc.submitTransaction(message, dynamicCost);

		} else {
			tlog("Could not afford transaction");
			saveUnsentTransaction(message, dynamicCost);
		}
	}

	public static void readTransactionExploredZoneStatus(int[] message, int round) throws GameActionException {
		int xZone = message[2] & ((1 << 6) - 1);
		int yZone = ((message[2] >>> 6) & ((1 << 6) - 1));
		int status = message[2] >>> 12;

		exploredZoneStatus[xZone][yZone] = status;

		tlog("Reading transaction for 'Explored Zone Status'");
		ttlog("Submitter ID: " + decryptID(message[0]));
		ttlog("[xZone, yZone]: " + xZone + " " + yZone);
		ttlog("status: " + status);
		ttlog("Posted round: " + round);
	}

	/*
	message[1] = signal & number of locations
	Can report up to 10 zones
	 */
	public static void writeTransactionSoupZoneStatus(int[] soupZoneIndices, int[] statuses, int index, int length) throws GameActionException {
		while (index < length) {
			log("Writing transaction for 'Soup Zone Status'");
			int[] message = new int[GameConstants.BLOCKCHAIN_TRANSACTION_LENGTH];
			message[0] = encryptID(myID);
			message[1] = SOUP_ZONE_STATUS_SIGNAL;
			int i_message = 4;
			for (; index < length && (i_message / 2) < message.length; index++) {
				int temp = soupZoneIndices[index] + (statuses[index] << 8);
				if (i_message % 2 == 1) {
					temp = temp << 16;
				}
				message[i_message / 2] |= temp;
				i_message++;
			}
			message[1] += (i_message - 4) << 8;

			xorMessage(message);
			if (rc.getTeamSoup() >= dynamicCost) {
				rc.submitTransaction(message, dynamicCost);

			} else {
				tlog("Could not afford transaction");
				saveUnsentTransaction(message, dynamicCost);
			}
		}
	}

	public static void readTransactionSoupStatus(int[] message, int round) throws GameActionException {
		tlog("Reading transaction for 'Soup Zone Status'");
		ttlog("Submitter ID: " + decryptID(message[0]));
		int numZones = message[1] >>> 8;
		int i_message = 4;
		for (int i = 0; i < numZones; i++) {
			int m = message[i_message / 2];
			if (i_message % 2 == 0) {
				m = m & ((1 << 16) - 1);
			} else {
				m = m >>> 16;
			}
			int zoneIndex = m & ((1 << 8) - 1);
			int status = m >>> 8;
			int[] zone = zoneIndexToPair(zoneIndex);
			ttlog("Zone " + zone[0] + " " + zone[1] + " status: " + status);
			updateKnownSoupZones(zoneIndex, status, false);

			i_message++;
		}

		ttlog("Posted round: " + round);
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
		log("Reading 'Refinery Built' transaction");
		tlog("Submitter ID: " + decryptID(message[0]));
		tlog("Location: " + loc);
		tlog("Posted round: " + round);
		BotMiner.addToRefineries(loc);
	}
}
