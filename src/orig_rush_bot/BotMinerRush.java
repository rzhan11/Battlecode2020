package orig_rush_bot;

import battlecode.common.*;

import static orig_rush_bot.Actions.*;
import static orig_rush_bot.Communication.*;
import static orig_rush_bot.Debug.*;
import static orig_rush_bot.Globals.*;
import static orig_rush_bot.Map.*;
import static orig_rush_bot.Nav.*;
import static orig_rush_bot.Utils.*;
import static orig_rush_bot.Wall.*;
import static orig_rush_bot.Zones.*;

public class BotMinerRush extends BotMiner {
    public static boolean initializedMinerRush = false;

    public static MapLocation rushDSLoc = null;
    public static MapLocation rushFCLoc = null;
    public static MapLocation rushNGLoc = null;

    public static MapLocation annoyLoc = null;

    public static boolean seesEnemyDrone = false;
    public static boolean seesEnemyFulfillmentCenter = false;

    public static int rushSymmetryIndex;
    public static MapLocation rushSymmetryLoc;
    public static int closestDistToSymmetry;
    public static int movesSinceCloser;
    public static boolean usedDrone;

    final public static int BUILD_DRONE_NUM_MOVES = 3;

    public static void initMinerBuilder() throws GameActionException {

        rushSymmetryIndex = getClosestSymmetryIndex();
        rushSymmetryLoc = symmetryHQLocs[rushSymmetryIndex];
        closestDistToSymmetry = here.distanceSquaredTo(rushSymmetryLoc);
        movesSinceCloser = 0;

        usedDrone = false;

        initializedMinerRush = true;

//        Globals.endTurn();
//        Globals.update();
    }

    public static void turn() throws GameActionException {
        if (!initializedMinerRush) {
            initMinerBuilder();
        }

        log("MINER RUSH");

        if (droppedLastTurn) {
            usedDrone = true;
        }

        seesEnemyDrone = false;
        seesEnemyFulfillmentCenter = false;
        for (RobotInfo ri: visibleEnemies) {
            switch (ri.type) {
                case DELIVERY_DRONE:
                    seesEnemyDrone = true;
                    break;
                case FULFILLMENT_CENTER:
                    seesEnemyFulfillmentCenter = true;
                    break;
            }
        }

        // build netgun if potential drone threat is seen
        if (enemyHQLoc != null && rushDSLoc != null && rushNGLoc == null) {
            if (seesEnemyDrone || seesEnemyFulfillmentCenter) {

                Direction buildDir = null;
                int openCardinals = 0;
                for (Direction dir: cardinalDirections) {
                    MapLocation loc = enemyHQLoc.add(dir);
                    if (!rc.onTheMap(loc)) {
                        continue;
                    }
                    if (!rc.canSenseLocation(loc)) {
                        openCardinals++;
                        continue;
                    }
                    if (here.isAdjacentTo(loc)) {
                        if (isLocDryFlatEmpty(loc)) {
                            buildDir = here.directionTo(loc);
                        }
                    }
                    if (isLocDryEmpty(loc)) {
                        openCardinals++;
                    }
                }

                // try diagonals if no cardinal directions are available
                if (openCardinals == 0) {
                    for (Direction dir: diagonalDirections) {
                        MapLocation loc = enemyHQLoc.add(dir);
                        if (!rc.onTheMap(loc)) {
                            continue;
                        }
                        if (!rc.canSenseLocation(loc)) {
                            continue;
                        }
                        if (here.isAdjacentTo(loc)) {
                            if (isLocDryFlatEmpty(loc)) {
                                buildDir = here.directionTo(loc);
                            }
                        }
                    }
                }

                // build netgun
                if (buildDir != null && rc.getTeamSoup() >= RobotType.NET_GUN.cost) {
                    Actions.doBuildRobot(RobotType.NET_GUN, buildDir);
                    rushNGLoc = rc.adjacentLocation(buildDir);
                    return;
                }
            }
        }

        int avoidDangerResult = Nav.avoidDanger();
        if (avoidDangerResult == 1) {
            // for rush miner, reset closest dist if distracted by enemies
            closestDistToSymmetry = here.distanceSquaredTo(rushSymmetryLoc);
            movesSinceCloser = 0;
            return;
        }
        if (avoidDangerResult == -1) {
            // when danger is unavoidable, reset isDirMoveable to ignore danger tiles
            updateIsDirMoveable();
        }

        if (closestDistToSymmetry > myType.sensorRadiusSquared) {
            if (rushFCLoc != null && !usedDrone) {
                log("Waiting for drone");
                return;
            }

            // updates whether or not we are moving closer to the target symmetry
            int curDist = here.distanceSquaredTo(rushSymmetryLoc);
            if (curDist < closestDistToSymmetry) {
                closestDistToSymmetry = curDist;
                movesSinceCloser = 0;
            }

            // if we have not made progress in a while, make a drone carry us
            if (movesSinceCloser > BUILD_DRONE_NUM_MOVES && rushFCLoc == null) {
                if (rc.getTeamSoup() >= RobotType.FULFILLMENT_CENTER.cost) {
                    for (Direction dir: directions) {
                        MapLocation loc = rc.adjacentLocation(dir);
                        if (!rc.onTheMap(loc)) {
                            continue;
                        }
                        if (isLocDryFlatEmpty(loc)) {
                            Actions.doBuildRobot(RobotType.FULFILLMENT_CENTER, dir);
                            rushFCLoc = loc;
                            return;
                        }
                    }
                }
            }
        }

        if (enemyHQLoc == null) {
            // checks if our target symmetry has been denied
            if (isSymmetryHQLoc[rushSymmetryIndex] == 2) {
                rushSymmetryIndex = getClosestSymmetryIndex();
                rushSymmetryLoc = symmetryHQLocs[rushSymmetryIndex];
                closestDistToSymmetry = here.distanceSquaredTo(rushSymmetryLoc);
                movesSinceCloser = 0;
            }
            moveLog(rushSymmetryLoc);
            movesSinceCloser++;
            return;
        } else {
            if (rushDSLoc == null) {
                Direction buildDir = null;
                for (Direction dir: cardinalDirections) {
                    MapLocation loc = enemyHQLoc.add(dir);
                    if (!rc.canSenseLocation(loc)) {
                        continue;
                    }
                    int dist = here.distanceSquaredTo(loc);
                    if (isLocDryFlatEmpty(loc) && dist > 0 && dist <= 2) {
                        buildDir = here.directionTo(loc);
                    }
                }
                if (buildDir != null && rc.getTeamSoup() >= RobotType.DESIGN_SCHOOL.cost) {
                    Actions.doBuildRobot(RobotType.DESIGN_SCHOOL, buildDir);
                    rushDSLoc = rc.adjacentLocation(buildDir);
                    annoyLoc = reflect(rushDSLoc, enemyHQLoc);
                    return;
                }
                if (here.distanceSquaredTo(enemyHQLoc) == 1) {
                    log("In position for rush DS");
                    return;
                }
                moveLog(enemyHQLoc);
                movesSinceCloser++;
                return;
            } else {
                if (here.equals(annoyLoc)) {
                    log("At annoy loc");
                    return;
                } else {
                    log("Trying to move to annoy loc");
                    moveLog(annoyLoc);
                    return;
                }
            }
        }
    }
}