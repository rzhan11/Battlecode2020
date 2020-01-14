package kryptonite;

import battlecode.common.*;

import static kryptonite.Constants.*;
import static kryptonite.Debug.*;
import static kryptonite.Map.*;

public class BotOffenseDeliveryDrone extends BotDeliveryDrone {

    public static boolean initialized = false;

    // used when we pick up an ally robot so it isn't blocking us
    // it will be put back later
    public static boolean holdingTemporaryRobot;
    public static MapLocation holdingTemporaryRobotLocation;
    public static boolean crossedTemporaryRobotLocation;

    public static void init() throws GameActionException {

        initialized = true;
    }

    public static void turn() throws GameActionException {
        Debug.tlog("OFFENSE DRONE");

        if (!initialized) {
            init();
        }

        locateFlooding();
        Debug.tlog("floodingMemory: " + floodingMemory);

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
        MapLocation targetLoc = getSymmetryLocation();
        if (enemyHQLocation == null) {
            Debug.tlog("Moving towards symmetry location at " + targetLoc);
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

}
