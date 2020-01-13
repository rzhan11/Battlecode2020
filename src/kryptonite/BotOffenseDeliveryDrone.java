package kryptonite;

import battlecode.common.*;

import java.rmi.MarshalledObject;

public class BotOffenseDeliveryDrone extends BotDeliveryDrone {

    public static boolean initialized = false;
    public static int symmetryHQLocationsIndex;
    public static MapLocation exploreSymmetryLocation;
    public static MapLocation enemyHQLocation = null;

    // used when we pick up an ally robot so it isn't blocking us
    // it will be put back later
    public static boolean holdingTemporaryRobot;
    public static MapLocation holdingTemporaryRobotLocation;
    public static boolean crossedTemporaryRobotLocation;

    public static void init() throws GameActionException {
        symmetryHQLocationsIndex = myID % symmetryHQLocations.length;
        exploreSymmetryLocation= symmetryHQLocations[symmetryHQLocationsIndex];

        initialized = true;
    }

    public static void turn() throws GameActionException {
        Debug.tlog("OFFENSE DRONE");

        if (!initialized) {
            init();
        }

        locateFlooding();
        Debug.tlog("floodingMemory: " + floodingMemory);

        // tries to determine enemyHQLocation if not already determined
        if (enemyHQLocation == null) {

            checkPossibleSymmetry();

            // try to visually check enemyHQLocation to determine symmetry
            if (rc.canSenseLocation(exploreSymmetryLocation)) {
                RobotInfo ri = rc.senseRobotAtLocation(exploreSymmetryLocation);
                if (ri != null && ri.type == RobotType.HQ) {
                    //STATE == enemy FOUND

                    Debug.tlog("Found enemy HQ at " + enemyHQLocation);
                    enemyHQLocation = exploreSymmetryLocation;
                    isSymmetry[symmetryHQLocationsIndex] = 1;

                    Communication.writeTransactionEnemyHQLocation(symmetryHQLocationsIndex, 1);
                } else {
                    //STATE == enemy NOT FOUND

                    Debug.tlog("Did not find enemy HQ at " + exploreSymmetryLocation);
                    isSymmetry[symmetryHQLocationsIndex] = 0;

                    Communication.writeTransactionEnemyHQLocation(symmetryHQLocationsIndex, 0);

                    checkPossibleSymmetry();
                }
            }
        }

        Debug.log();
        Debug.tlog("holdingTemporaryRobot: " + holdingTemporaryRobot);
        Debug.tlog("holdingTemporaryRobotLocation: " + holdingTemporaryRobotLocation);
        Debug.tlog("crossedTemporaryRobotLocation: " + crossedTemporaryRobotLocation);
        // if we are temporarily holding a robot, try to put it back
        if (holdingTemporaryRobot) {
            // if we are on the holdingTemporaryRobotLocation
            // let the turn continue and use the default pathing
            if (here.equals(holdingTemporaryRobotLocation)) {
                Debug.tlog("Trying to get off of the holdingTemporaryRobotLocation at " + holdingTemporaryRobotLocation);
                // do not return
            } else if (!crossedTemporaryRobotLocation) {
                // STATE == we have not moved since we picked up the robot
                // if we haven't crossed holdingTemporaryRobotLocation, try to get onto it
                Direction dir = here.directionTo(holdingTemporaryRobotLocation);
                Debug.tlog("Moving to holdingTemporaryRobotLocation at " + holdingTemporaryRobotLocation);
                if (rc.canSenseLocation(holdingTemporaryRobotLocation) && rc.senseRobotAtLocation(holdingTemporaryRobotLocation) == null) {
                    if (rc.isReady()) {
                        Actions.doMove(here.directionTo(holdingTemporaryRobotLocation));
                        Debug.ttlog("Success");
                    } else {
                        Debug.ttlog("But not ready");
                    }
                    return;
                }
                Debug.ttlog("Waiting for it to open up");
                return;
            } else {
                // STATE == crossedTemporaryRobotLocation
                // should be adjacent to but not on the holdingTemporaryRobotLocation
                Debug.tlog("Putting temporary robot back to " + holdingTemporaryRobotLocation);
                if (rc.canSenseLocation(holdingTemporaryRobotLocation) && rc.senseRobotAtLocation(holdingTemporaryRobotLocation) == null) {
                    if (rc.isReady()) {
                        Actions.doDropUnit(here.directionTo(holdingTemporaryRobotLocation));
                        Debug.ttlog("Success");
                        holdingTemporaryRobot = false;
                        holdingTemporaryRobotLocation = null;
                        crossedTemporaryRobotLocation = false;
                    } else {
                        Debug.ttlog("But not ready");
                    }
                    return;
                }
                Debug.ttlog("Waiting for it to open up");
                return;
            }
        }

        // if enemyHQLocation not found, go to exploreSymmetryLocation
        MapLocation targetLoc = null;
        if (enemyHQLocation == null) {
            Debug.tlog("Moving towards exploreSymmetryLocation at " + exploreSymmetryLocation);
            targetLoc = exploreSymmetryLocation;
        } else {
            // STATE == enemyHQLocation found (AKA not null)
            // chase enemies and drop them into water
            boolean sawEnemy = tryKillRobots(visibleEnemies, them);
            if (sawEnemy) {
                return;
            }
            boolean sawCow = tryKillRobots(visibleCows, cowTeam);
            if (sawCow) {
                return;
            }

            Debug.tlog("Moving towards enemyHQLocation at " + enemyHQLocation);
            targetLoc = enemyHQLocation;
        }

//            int[] color = Actions.WHITE;
//            rc.setIndicatorLine(here, targetLoc, color[0], color[1], color[2]);
        Direction move = moveLog(targetLoc);
        if (holdingTemporaryRobot && move != null) {
            crossedTemporaryRobotLocation = true;
        }
        if (!holdingTemporaryRobot && move == null && rc.isReady()) {
            Debug.tlog("Trying to force a move");
            Direction dirToEnemyHQ = here.directionTo(targetLoc);
            move = Nav.tryForceMoveInGeneralDirection(dirToEnemyHQ);
            if (move != null) { // STATE == picked up an ally
                holdingTemporaryRobot = true;
                holdingTemporaryRobotLocation = rc.adjacentLocation(move);
                crossedTemporaryRobotLocation = false;
            }
        }
        return;
    }

    /*
    Checks if we can tell what the symmetry is based on denied symmetries

    Checks if the current target symmetry is possible
    If not, iterate to a possible one
     */
    public static void checkPossibleSymmetry () {
        // if two symmetries have been confirmed negatives, then it must be the last symmetry
        int denyCount = 0;
        int notDenyIndex = -1;
        for (int i = 0; i < symmetryHQLocations.length; i++) {
            if (isSymmetry[i] == 0) {
                denyCount++;
            } else {
                notDenyIndex = i;
            }
        }
        if (denyCount == 2) {
            Debug.tlog("Determined through 2 denials that enemy HQ is at " + enemyHQLocation);
            enemyHQLocation = symmetryHQLocations[notDenyIndex];
            isSymmetry[notDenyIndex] = 1;
            return;
        }

        while (isSymmetry[symmetryHQLocationsIndex] == 0) {
            symmetryHQLocationsIndex++;
            symmetryHQLocationsIndex %= symmetryHQLocations.length;
        }
        exploreSymmetryLocation = symmetryHQLocations[symmetryHQLocationsIndex];
        Debug.tlog("Retargetting exploreSymmetryLocation to " + exploreSymmetryLocation);
    }

}
