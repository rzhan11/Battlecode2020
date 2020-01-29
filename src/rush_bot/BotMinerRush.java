package rush_bot;

import battlecode.common.*;

import static rush_bot.Communication.*;
import static rush_bot.Debug.*;
import static rush_bot.Map.*;


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
    public static boolean builtNearNetgun = false;

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

        /*
        Abort rush if past a certain round
        Abort rush if too many enemy landscapers
         */
        if (rushDSLoc == null && !builtNearNetgun) {
            // check number of visible enemy landscapers
            int count = 0;
            for (RobotInfo ri: visibleEnemies) {
                if (ri.type == RobotType.LANDSCAPER) {
                    count++;
                }
            }
            if (count >= 3) {
                abortRush = true;
            }
        }

        if (abortRush) {
            myRole = MINER_RESOURCE_ROLE;
            writeTransactionRushStatus(ABORT_RUSH_FLAG);
            return;
        }

        if (roundNum > START_RUSH_STATUS_ROUND && roundNum % RUSH_STATUS_INTERVAL == 0) {
            writeTransactionRushStatus(CONTINUE_RUSH_FLAG);
        }

        if (droppedLastTurn) {
            usedDrone = true;
        }

        if (!rc.isReady()) {
            log("Not ready");
            return;
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

        // if somewhat close to enemy hq, sees enemy drone, and sees fulfillment center
        // build netgun
        if (!enemyRush && !builtNearNetgun && rc.getTeamSoup() >= RobotType.NET_GUN.cost) {
            if (seesEnemyDrone && rushDSLoc == null) {
                if (maxXYDistance(enemyHQLoc, here) == 3 || maxXYDistance(enemyHQLoc, here) == 4) {
                    int bestDist = P_INF;
                    Direction bestDir = null;
                    for (Direction dir: directions) {
                        MapLocation loc = rc.adjacentLocation(dir);
                        if (!rc.onTheMap(loc)) {
                            continue;
                        }
                        if (maxXYDistance(enemyHQLoc, loc) == 3 && isLocDryFlatEmpty(loc)) {
                            int dist = loc.distanceSquaredTo(enemyHQLoc);
                            if (dist < bestDist) {
                                bestDir = dir;
                                bestDist = dist;
                            }
                        }
                    }
                    if (bestDir != null) {
                        Actions.doBuildRobot(RobotType.NET_GUN, bestDir);
                        builtNearNetgun = true;
                        return;
                    }
                }
            }
        }

        // build netgun if potential drone threat is seen
        if (enemyHQLoc != null && rushDSLoc != null && rushNGLoc == null) {
            if (true || seesEnemyDrone || seesEnemyFulfillmentCenter) {

                Direction buildDiagonalDir = null;
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
                            buildDiagonalDir = here.directionTo(loc);
                            if (loc.isAdjacentTo(rushDSLoc)) {
                                break;
                            }
                        }
                    }
                }

                Direction buildCardinalDir = null;
                for (Direction dir: cardinalDirections) {
                    MapLocation loc = enemyHQLoc.add(dir);
                    if (!rc.onTheMap(loc)) {
                        continue;
                    }
                    if (!rc.canSenseLocation(loc)) {
                        continue;
                    }
                    if (here.isAdjacentTo(loc)) {
                        if (isLocDryFlatEmpty(loc)) {
                            buildCardinalDir = here.directionTo(loc);
                            if (loc.isAdjacentTo(rushDSLoc)) {
                                break;
                            }
                        }
                    }
                }

                Direction buildDir = buildDiagonalDir;
                // try diagonals if no cardinal directions are available
                if (buildDir == null) {
                    buildDir = buildCardinalDir;
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
            Nav.bugTracing = false;
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
                if (rushNGLoc == null) {
                    if (here.equals(annoyLoc)) {
                        log("At ANNOY annoy loc");
                        return;
                    } else {
                        log("Trying to move to ANNOY annoy loc");
                        moveLog(annoyLoc);
                        return;
                    }
                } else {
                    if (here.isAdjacentTo(rushNGLoc)) {
                        if (here.isAdjacentTo(rushDSLoc)) {
                            // move to tile adjacent to net gun, not adjacent to design school
                            for (Direction dir: directions) {
                                MapLocation loc = rc.adjacentLocation(dir);
                                if (!rc.onTheMap(loc)) {
                                    continue;
                                }
                                if (isLocDryFlatEmpty(loc) && loc.isAdjacentTo(rushNGLoc) && !loc.isAdjacentTo(rushDSLoc)) {
                                    Actions.doMove(dir);
                                    log("At ok netgun annoy loc, moving to a good one");
                                    return;
                                }
                            }
                            log("At ok netgun annoy loc, can't find a good one");
                            return;
                        } else {
                            log("At good netgun annoy loc");
                            return;
                        }
                    } else {
                        log("Trying to move to any netgun annoy loc");
                        moveLog(rushNGLoc);
                        return;
                    }
                }
            }
        }
    }
}