package turtle;

import battlecode.common.*;

import java.util.Random;


import static turtle.Communication.*;
import static turtle.Debug.*;
import static turtle.Map.*;
import static turtle.Wall.*;
import static turtle.Zones.*;

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

    public static MapLocation[] symmetryHQLocs = new MapLocation[3];
    public static int[] isSymmetryHQLoc = new int[3]; // 0 is unknown, 1 is confirmed, 2 is denied
    public static int symmetryHQLocsIndex; // current symmetry that we are exploring
    public static MapLocation enemyHQLoc = null;

    public static int totalVaporators = 0;

    public static int rushMinerID = -1;

    public static void init(RobotController theRC) throws GameActionException {

        rc = theRC;
        spawnRound = rc.getRoundNum();

        us = rc.getTeam();
        them = us.opponent();
        cowTeam = Team.NEUTRAL;

        myID = rc.getID();
        myType = rc.getType();

        log("Pre-init: " + Clock.getBytecodesLeft());

        HardCode.getSenseDirections(myType);
        log("Post-senseDirection: " + Clock.getBytecodesLeft());
        rand = new Random(myID);

        mapWidth = rc.getMapWidth();
        mapHeight = rc.getMapHeight();

        if (us == Team.A) {
            Communication.secretKey = 1337;
        } else {
            Communication.secretKey = 7331;
        }

        lastActiveTurn = rc.getRoundNum() - 1;

        log("Pre-updateBasic: " + Clock.getBytecodesLeft());
        Globals.updateBasic();
        log("Post-updateBasic: " + Clock.getBytecodesLeft());

        // find HQ location and symmetries if not already found
        findHQLoc();
        symmetryHQLocsIndex = myID % symmetryHQLocs.length;
        log("Initial exploreSymmetryLocation: " + symmetryHQLocs[symmetryHQLocsIndex]);
        log("Post-findHQLoc: " + Clock.getBytecodesLeft());

        if (!isLowBytecodeLimit(myType)) {
            loadWallInfo();
            log("Post-loadWallInfo: " + Clock.getBytecodesLeft());
            loadZoneInfo();
            log("Post-loadZoneInfo: " + Clock.getBytecodesLeft());
        } else {
            Clock.yield();
            Globals.updateBasic();
        }

        int recentReviewRound = ((roundNum + RESUBMIT_EARLY - 1) / RESUBMIT_INTERVAL) * RESUBMIT_INTERVAL - RESUBMIT_EARLY;
        if (recentReviewRound >= 1) {
            log("SEARCHING FOR REVIEW BLOCKS");
            int startByte = Clock.getBytecodesLeft();
            readReviewBlock(recentReviewRound, 0);
            oldBlocksIndex = recentReviewRound;
            log("Review round bytecode: " + (startByte - Clock.getBytecodesLeft()));
        } else {
            oldBlocksIndex = 1;
        }

        if (myType != RobotType.HQ) {
            Clock.yield();
            Globals.updateBasic();
        }
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
        Globals.updateBasic();

        printMyInfo();

        if (roundNum > 1) {
            if (oldBlocksLength == -1) {
                oldBlocksLength = roundNum - 2;
            }
            log("Reading the previous round's transactions");
            int result = readBlock(roundNum - 1, 0);
            if (result < 0) {
                logi("WARNING: Did not fully read the previous round's transactions");
            } else {
                log("Done reading the previous round's transactions");
            }
            log("Post-readPrevBlock: " + Clock.getBytecodesLeft());
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

        log("Bytecode after update() " + Clock.getBytecodesLeft());
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
        if (myType == RobotType.HQ && roundNum == 1) {
            writeTransactionHQFirstTurn(here);
        }

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
            log("END TURN");
            log("Bytecode left: " + Clock.getBytecodesLeft());
            log("------------------------------\n");
        }
        Clock.yield();
    }

    public static void findHQLoc() throws GameActionException {
        // tries to find our HQLoc and HQElevation by reading messages
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
        symmetryHQLocs[0] = new MapLocation(mapWidth - 1 - HQLoc.x, HQLoc.y);
        symmetryHQLocs[1] = new MapLocation(HQLoc.x, mapHeight - 1 - HQLoc.y);
        symmetryHQLocs[2] = new MapLocation(mapWidth - 1 - HQLoc.x, mapHeight - 1 - HQLoc.y);
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
        if (move == null) {
            tlog("But no move found");
        }
        return move;
    }

    public static void updateSymmetry () throws GameActionException {

        if (enemyHQLoc != null) {
            return;
        }

        // if the HQ is on a horizontal/vertical/rotational symmetry that generates the same symmetryHQLocs
        if ((mapWidth % 2 == 1 && mapWidth / 2 == HQLoc.x) ||
                (mapHeight % 2 == 1 && mapHeight / 2 == HQLoc.y)) {
            isSymmetryHQLoc[0] = 1;
            isSymmetryHQLoc[1] = 2;
            isSymmetryHQLoc[2] = 2;
            enemyHQLoc = symmetryHQLocs[0];
            symmetryHQLocsIndex = 0;
            return;
        }

        // try to visually check unknown enemyHQLocs
        for (int i = 0; i < symmetryHQLocs.length; i++) {
            MapLocation loc = symmetryHQLocs[i];
            if (isSymmetryHQLoc[i] == 0 && rc.canSenseLocation(loc)) {
                RobotInfo ri = rc.senseRobotAtLocation(loc);
                if (ri != null && ri.type == RobotType.HQ) {
                    //STATE == enemy FOUND

                    enemyHQLoc = loc;
                    isSymmetryHQLoc[i] = 1;
                    symmetryHQLocsIndex = i;

                    log("Found enemy HQ at " + enemyHQLoc);

                    writeTransactionEnemyHQLoc(i, 1);
                } else {
                    //STATE == enemy NOT FOUND

                    log("Denied enemy HQ at " + loc);
                    isSymmetryHQLoc[i] = 2;

                    writeTransactionEnemyHQLoc(i, 2);
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
        for (int i = 0; i < symmetryHQLocs.length; i++) {
            if (isSymmetryHQLoc[i] == 2) {
                denyCount++;
            } else {
                notDenyIndex = i;
            }
        }
        if (denyCount == 2) {
            enemyHQLoc = symmetryHQLocs[notDenyIndex];
            isSymmetryHQLoc[notDenyIndex] = 1;
            symmetryHQLocsIndex = notDenyIndex;
            log("Determined through 2 denials that enemy HQ is at " + enemyHQLoc);
            return;
        }

        while (isSymmetryHQLoc[symmetryHQLocsIndex] == 2) {
            symmetryHQLocsIndex++;
            symmetryHQLocsIndex %= symmetryHQLocs.length;
            log("Retargeting symmetry that we are exploring to " + symmetryHQLocs[symmetryHQLocsIndex]);
        }
    }

    /*
    If found which symmetry the enemy HQ location is on, return that location
    Else, return the current symmetry that we are exploring
     */
    public static MapLocation getSymmetryLoc() {
        if (enemyHQLoc == null) {
            return symmetryHQLocs[symmetryHQLocsIndex];
        } else {
            return enemyHQLoc;
        }
    }

    public static int getClosestSymmetryIndex() {
        int bestIndex = -1;
        int bestDist = P_INF;
        for (int i = 0; i < symmetryHQLocs.length; i++) {
            if (isSymmetryHQLoc[i] != 2) {
                int dist = here.distanceSquaredTo(symmetryHQLocs[i]);
                if (dist < bestDist) {
                    bestIndex = i;
                    bestDist = dist;
                }
            }
        }
        return bestIndex;
    }
}
