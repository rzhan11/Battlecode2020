package turtle;

import battlecode.common.*;

import static turtle.Debug.*;
import static turtle.Map.*;

public class BotMinerRush extends BotMiner {
    public static boolean initializedMinerRush = false;

    public static MapLocation rushDSLoc = null;
    public static MapLocation rushFCLoc = null;
    public static MapLocation rushNGLoc = null;
    public static MapLocation annoyLoc = null;

    public static boolean seesEnemyDrone = false;
    public static boolean seesEnemyFulfillmentCenter = false;

    public static void initMinerBuilder() throws GameActionException {

        initializedMinerRush = true;

//        Globals.endTurn();
//        Globals.update();
    }

    public static void turn() throws GameActionException {
        if (!initializedMinerRush) {
            initMinerBuilder();
        }

        log("MINER RUSH");

        for (RobotInfo ri: visibleEnemies) {
            switch (ri.type) {
                case DELIVERY_DRONE:
                    seesEnemyDrone = true;
                case FULFILLMENT_CENTER:
                    seesEnemyFulfillmentCenter = true;
            }
        }

        if (enemyHQLoc == null) {
            moveLog(getSymmetryLoc());
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
                return;
            } else {
                if (seesEnemyDrone || seesEnemyFulfillmentCenter) {
                    Direction buildDir = null;
                    if (rushNGLoc == null) {
                        for (Direction dir: directions) {
                            MapLocation loc = rc.adjacentLocation(dir);
                            if (!rc.onTheMap(loc)) {
                                continue;
                            }
                            if (isLocDryFlatEmpty(loc) && loc.isAdjacentTo(enemyHQLoc)) {
                                buildDir = dir;
                            }
                        }
                    }
                    if (buildDir != null && rc.getTeamSoup() >= RobotType.NET_GUN.cost) {
                        Actions.doBuildRobot(RobotType.NET_GUN, buildDir);
                        rushNGLoc = rc.adjacentLocation(buildDir);
                        return;
                    }
                }
                if (here.distanceSquaredTo(enemyHQLoc) == 1) {
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