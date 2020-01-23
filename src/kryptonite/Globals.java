package kryptonite;

import battlecode.common.*;

import java.util.Random;

import static kryptonite.Actions.*;
import static kryptonite.Communication.*;
import static kryptonite.Debug.*;
import static kryptonite.Map.*;
import static kryptonite.Nav.*;
import static kryptonite.Utils.*;
import static kryptonite.Zones.*;

public class Globals extends Constants {
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

    public static Random rand;

	/*
	Values that might change each turn
	*/

    public static int roundNum;
    public static boolean firstTurn = true;
    public static MapLocation here;
    public static int[] myZone;
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

    public static MapLocation[] visibleSoupLocs = null;

    public static MapLocation closestVisibleSoupLoc = null;
    public static MapLocation targetVisibleSoupLoc = null;
    public static MapLocation closestSoupZone = null;
    public static MapLocation targetSoupZone = null;

    public static MapLocation closestUnexploredZone = null;
    public static MapLocation targetUnexploredZone = null;
    public static int unexploredZoneStartRound = -1;

    public static MapLocation targetNavLoc = null;

    public static boolean[] isDirMoveable = new boolean[8];
    public static boolean[] isDirDanger = new boolean[8];
    public static boolean inDanger;

    // checks if drones picked up and dropped this unit
    public static boolean droppedLastTurn = false;
    public static int lastActiveTurn = 0;

    public static int oldBlocksLength = -1;
    public static int oldBlocksIndex = 1;
    public static int oldTransactionsIndex = 0;

    // symmetry
    public static MapLocation HQLoc = null;
    public static int HQElevation;

    public static MapLocation[] symmetryHQLocations = new MapLocation[3];
    public static int[] isSymmetryHQLocation = {-1, -1, -1}; // -1 is unknown, 0 is false, 1 is true
    public static int symmetryHQLocationsIndex; // current symmetry that we are exploring
    public static MapLocation enemyHQLoc = null;


    public static void init(RobotController theRC) throws GameActionException {

        rc = theRC;
        spawnRound = rc.getRoundNum();

        us = rc.getTeam();
        them = us.opponent();
        cowTeam = Team.NEUTRAL;

        myID = rc.getID();
        myType = rc.getType();
        senseDirections = HardCode.getSenseDirections(myType);
        rand = new Random(myID);

        mapWidth = rc.getMapWidth();
        mapHeight = rc.getMapHeight();

        if (us == Team.A) {
            Communication.secretKey = 1337;
        } else {
            Communication.secretKey = 7331;
        }

        lastActiveTurn = rc.getRoundNum() - 1;

        updateBasic();

        // find HQ location and symmetries if not already found
        findHQLocation();
        symmetryHQLocationsIndex = myID % symmetryHQLocations.length;
        log("Initial exploreSymmetryLocation: " + symmetryHQLocations[symmetryHQLocationsIndex]);

        if (!isLowBytecodeLimit(myType)) {
            loadZoneInformation();
        }

        oldBlocksIndex = Math.max(oldBlocksIndex, roundNum - RESUBMIT_INTERVAL);
        oldBlocksLength = roundNum - 1;
    }

    /*
    These updates should be cheap and independent of other updates
     */
    public static void updateBasic () throws GameActionException {
        here = rc.getLocation();
        myZone = locToZonePair(here);
        roundNum = rc.getRoundNum();

        myElevation = rc.senseElevation(here);
        waterLevel = (int) GameConstants.getWaterLevel(roundNum);

        myPollution = rc.sensePollution(here);
        actualSensorRadiusSquared = rc.getCurrentSensorRadiusSquared();
        extremePollution = actualSensorRadiusSquared < 2;
        if (extremePollution) {
            logi("WARNING: Extreme pollution has made actualSensorRadiusSquared < 2, so errors may occur. Ask Richard.");
        }

        newSoupStatusesLength = 0;

        calculateDynamicCost();

        visibleAllies = rc.senseNearbyRobots(-1, us); // -1 uses all robots within sense radius
        visibleEnemies = rc.senseNearbyRobots(-1, them);
        visibleCows = rc.senseNearbyRobots(-1, cowTeam);

        if (!isLowBytecodeLimit(myType)) {
            adjacentAllies = rc.senseNearbyRobots(2, us);
            adjacentEnemies = rc.senseNearbyRobots(2, them);
            adjacentCows = rc.senseNearbyRobots(2, cowTeam);

            visibleSoupLocs = rc.senseNearbySoup();
        }

        // dropped by drone information
        if (roundNum <= lastActiveTurn + 1) {
            droppedLastTurn = false;
        } else {
            droppedLastTurn = true;
            log("Was dropped last turn");
            Nav.bugTracing = false;
            Nav.bugLastWall = null;
            Nav.bugClosestDistanceToTarget = P_INF;
        }
        lastActiveTurn = rc.getRoundNum();
    }

    public static void update() throws GameActionException {
        updateBasic();

        printMyInfo();

        if (roundNum > 1) {
            log("Reading the previous round's transactions");
            int result = readBlock(roundNum - 1, 0);
            if (result < 0) {
                logi("WARNING: Did not fully read the previous round's transactions");
            } else {
                log("Done reading the previous round's transactions");
            }
            log("Bytecode after reading prev. transactions " + Clock.getBytecodesLeft());
        }

        if (!isLowBytecodeLimit(myType)) {
            checkZone();
            locateSoup();

            // update moveable directions
            updateIsDirMoveable();
            // updates dangerous directions and modifies isDirMoveable based on this
            updateIsDirDanger();
        }

        updateSymmetry();

        // tries to submit unsent messages from previous turns
        submitUnsentTransactions();
    }

    /*
    Prints various useful debugging information
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
    }

    /*
    earlyEnd should be true, unless this method was called at the end of the loop() method
    */
    public static void endTurn () throws GameActionException {
        if (myType == RobotType.HQ) {
            drawZoneStatus();
        }

        readOldBlocks();

        // check if we went over the bytecode limit
        int endTurn = rc.getRoundNum();
        firstTurn = false; // if early end, do not count as full turn
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
            HQLoc = rc.getLocation();
            HQElevation = rc.senseElevation(HQLoc);
        } else {
            int index = 1;
            while (HQLoc == null) {
                while (index >= rc.getRoundNum()) {
                    log("YIELDING TO FIND HQ LOC");
                    Clock.yield();
                    Globals.updateBasic();
                }

                Transaction[] block = rc.getBlock(index);
                for (Transaction t : block) {
                    int[] message = t.getMessage();
                    xorMessage(message);
                    int submitterID = decryptID(message[0]);
                    if (submitterID == -1) {
                        tlog("Found opponent's transaction");
                        continue; // not submitted by our team
                    } else {
                        if (message[1] % (1 << 8) == HQ_FIRST_TURN_SIGNAL) {
                            readTransactionHQFirstTurn(message, index);
                        }
                    }
                }
                index++;
            }
        }

        // calculates possible enemy HQ locations
        symmetryHQLocations[0] = new MapLocation(mapWidth - 1 - HQLoc.x, HQLoc.y);
        symmetryHQLocations[1] = new MapLocation(HQLoc.x, mapHeight - 1 - HQLoc.y);
        symmetryHQLocations[2] = new MapLocation(mapWidth - 1 - HQLoc.x, mapHeight - 1 - HQLoc.y);
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

    public static Direction moveLog(MapLocation loc) throws GameActionException {
        Direction move = Nav.bugNavigate(loc);
        if (move != null) {
            tlog("Moved " + move);
        } else {
            tlog("But no move found");
        }
        return move;
    }

    public static void updateSymmetry () throws GameActionException {

        if (enemyHQLoc != null) {
            return;
        }

        // if the HQ is on a horizontal/vertical/rotational symmetry that generates the same symmetryHQLOcations
        if ((mapWidth % 2 == 1 && mapWidth / 2 == HQLoc.x) ||
                (mapHeight % 2 == 1 && mapHeight / 2 == HQLoc.y)) {
            isSymmetryHQLocation[0] = 1;
            isSymmetryHQLocation[1] = -1;
            isSymmetryHQLocation[2] = -1;
            enemyHQLoc = symmetryHQLocations[0];
            symmetryHQLocationsIndex = 0;
            return;
        }

        // try to visually check unknown enemyHQLocations
        for (int i = 0; i < symmetryHQLocations.length; i++) {
            MapLocation loc = symmetryHQLocations[i];
            if (isSymmetryHQLocation[i] == -1 && rc.canSenseLocation(loc)) {
                RobotInfo ri = rc.senseRobotAtLocation(loc);
                if (ri != null && ri.type == RobotType.HQ) {
                    //STATE == enemy FOUND

                    enemyHQLoc = loc;
                    isSymmetryHQLocation[i] = 1;
                    symmetryHQLocationsIndex = i;

                    log("Found enemy HQ at " + enemyHQLoc);

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
            enemyHQLoc = symmetryHQLocations[notDenyIndex];
            isSymmetryHQLocation[notDenyIndex] = 1;
            symmetryHQLocationsIndex = notDenyIndex;
            log("Determined through 2 denials that enemy HQ is at " + enemyHQLoc);
            return;
        }

        while (isSymmetryHQLocation[symmetryHQLocationsIndex] == 0) {
            symmetryHQLocationsIndex++;
            symmetryHQLocationsIndex %= symmetryHQLocations.length;
            log("Retargeting symmetry that we are exploring to " + symmetryHQLocations[symmetryHQLocationsIndex]);
        }
    }

    /*
    If found which symmetry the enemy HQ location is on, return that location
    Else, return the current symmetry that we are exploring
     */
    public static MapLocation getSymmetryLoc() {
        if (enemyHQLoc == null) {
            return symmetryHQLocations[symmetryHQLocationsIndex];
        } else {
            return enemyHQLoc;
        }
    }
}
