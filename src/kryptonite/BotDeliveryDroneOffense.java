package kryptonite;

import battlecode.common.*;

import static kryptonite.Communication.*;
import static kryptonite.Constants.*;
import static kryptonite.Debug.*;
import static kryptonite.Map.*;

public class BotDeliveryDroneOffense extends BotDeliveryDrone {
//
//    public static boolean initialized = false;
//
//    // used when we pick up an ally robot so it isn't blocking us
//    // it will be put back later
//    public static boolean holdingTemporaryRobot;
//    public static MapLocation holdingTemporaryRobotLocation;
//    public static boolean crossedTemporaryRobotLocation;
//
//    public static void init() throws GameActionException {
//
//        initialized = true;
//    }
//
    public static void turn() throws GameActionException {
        log("OFFENSE DRONE");
//
//        if (!rc.isReady()) {
//            log("Not ready");
//            return;
//        }
//
//        if (roundNum % 500 == 0) {
//            isDroneSwarming = true;
//        }
//
//        if (!isDroneSwarming) {
//            int avoidDangerResult = Nav.avoidDanger();
//            if (avoidDangerResult == 1) {
//                return;
//            }
//
//            // only move in cardinal directions
//            for (int i = 1; i < isDirMoveable.length; i+=2) {
//                isDirMoveable[i] = false;
//            }
//        }
//
//        if (!initialized) {
//            init();
//        }
//
//        locateFlooding();
//        log("floodingMemory: " + floodingMemory);
//
//        // if we are temporarily holding a robot, try to put it back
//        if (holdingTemporaryRobot) {
//            // if we are on the holdingTemporaryRobotLocation
//            // let the turn continue and use the default pathing
//            if (here.equals(holdingTemporaryRobotLocation)) {
//                log("Trying to get off of the holdingTemporaryRobotLocation at " + holdingTemporaryRobotLocation);
//                // do not return
//            } else if (!crossedTemporaryRobotLocation) {
//                // STATE == we have not moved since we picked up the robot
//                // if we haven't crossed holdingTemporaryRobotLocation, try to get onto it
//                Direction dir = here.directionTo(holdingTemporaryRobotLocation);
//                log("Moving to holdingTemporaryRobotLocation at " + holdingTemporaryRobotLocation);
//                if (rc.canSenseLocation(holdingTemporaryRobotLocation) && rc.senseRobotAtLocation(holdingTemporaryRobotLocation) == null) {
//                    Actions.doMove(here.directionTo(holdingTemporaryRobotLocation));
//                    return;
//                }
//                tlog("Waiting for it to open up");
//                return;
//            } else {
//                // STATE == crossedTemporaryRobotLocation
//                // should be adjacent to but not on the holdingTemporaryRobotLocation
//                log("Putting temporary robot back to " + holdingTemporaryRobotLocation);
//                if (rc.canSenseLocation(holdingTemporaryRobotLocation) && rc.senseRobotAtLocation(holdingTemporaryRobotLocation) == null) {
//                    Actions.doDropUnit(here.directionTo(holdingTemporaryRobotLocation));
//                    holdingTemporaryRobot = false;
//                    holdingTemporaryRobotLocation = null;
//                    crossedTemporaryRobotLocation = false;
//                    return;
//                }
//                tlog("Waiting for it to open up");
//                return;
//            }
//        }
//
//        // chase enemies and drop them into water
//        boolean sawEnemy = tryKillRobots(visibleEnemies, them);
//        if (sawEnemy) {
//            return;
//        }
//        boolean sawCow = tryKillRobots(visibleCows, cowTeam);
//        if (sawCow) {
//            return;
//        }
//
//        // if enemyHQLocation not found, go to exploreSymmetryLocation
//        MapLocation targetLoc = getSymmetryLocation();
//        if (enemyHQLocation == null) {
//            log("Moving towards symmetry location at " + targetLoc);
//        } else {
//            // STATE == enemyHQLocation found (AKA not null)
//            log("Moving towards enemyHQLocation at " + enemyHQLocation);
//        }
//
//        Direction move = moveLog(targetLoc);
//        if (holdingTemporaryRobot && move != null) {
//            crossedTemporaryRobotLocation = true;
//        }
////        if (!holdingTemporaryRobot && move == null) {
////            if (!rc.isCurrentlyHoldingUnit()) {
////                log("Trying to force a move");
////                Direction dirToEnemyHQ = here.directionTo(targetLoc);
////                move = Nav.tryForceMoveInGeneralDirection(dirToEnemyHQ);
////                if (move != null) { // STATE == picked up an ally
////                    holdingTemporaryRobot = true;
////                    holdingTemporaryRobotLocation = rc.adjacentLocation(move);
////                    crossedTemporaryRobotLocation = false;
////                }
////            }
////        }
//        return;
    }

}
