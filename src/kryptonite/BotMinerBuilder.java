package kryptonite;

import battlecode.common.*;

import static kryptonite.Communication.*;
import static kryptonite.Debug.*;
import static kryptonite.Map.*;
import static kryptonite.Nav.*;
import static kryptonite.Utils.*;
import static kryptonite.Wall.*;
import static kryptonite.Zones.*;

public class BotMinerBuilder extends BotMiner {
    public static boolean initializedMinerBuilder = false;

    public static int buildInstruction = -1;
    public static int buildDetail = -1;
    public static int assignRound = -1;

    public static void initMinerBuilder() throws GameActionException {

        initializedMinerBuilder = true;

//        Globals.endTurn();
//        Globals.update();
    }

    public static void uninitMinerBuilder() throws GameActionException {
        myRole = MINER_RESOURCE_ROLE;
        initializedMinerBuilder = false;
        buildInstruction = -1;
        buildDetail = -1;
    }

    public static void turn() throws GameActionException {
        if (!initializedMinerBuilder) {
            initMinerBuilder();
        }

        if (roundNum - assignRound >= BotHQ.REASSIGN_ROUND_NUM) {
            log("Assignment expired");
            uninitMinerBuilder();
            return;
        }

        log("MINER BUILDER");
        drawDot(here, BROWN);

        switch (buildInstruction) {
            case BUILD_CLOSE_FULFILLMENT_CENTER:
                if (buildCloseFulfillmentCenter()) {
                    uninitMinerBuilder();
                }
                break;
            case BUILD_CLOSE_VAPORATOR:
                if (buildCloseVaporator()) {
                    uninitMinerBuilder();
                }
                break;
            case BUILD_CLOSE_DESIGN_SCHOOL:
                if (buildCloseDesignSchool()) {
                    uninitMinerBuilder();
                }
                break;
        }
    }

    /*
    Returns if task was completed
     */
    public static boolean buildCloseFulfillmentCenter () throws GameActionException {
        log("BUILDER MINER: Close Fulfillment Center");
        int targetRing = 3;
        for (Direction dir : directions) {
            MapLocation loc = rc.adjacentLocation(dir);
            if (!rc.onTheMap(loc)) {
                continue;
            }
            if (maxXYDistance(HQLoc, loc) == targetRing && isCloseBuildLoc(loc)) {
                if (isDirDryFlatEmpty(dir)) {
                    if (rc.getTeamSoup() >= RobotType.FULFILLMENT_CENTER.cost) {
                        Actions.doBuildRobot(RobotType.FULFILLMENT_CENTER, dir);
                        tlog("Fulfillment Center Built");
                        return true;
                    } else {
                        tlog("In position, not enough soup");
                    }
                    return false;
                }
            }
        }

        int closestWallLocDist = P_INF;
        MapLocation closestWallLoc = null;
        for (int i = 0; i < wallLocsLength; i++) {
            MapLocation loc = wallLocs[i].add(wallLocs[i].directionTo(HQLoc));
            if (here.isAdjacentTo(loc)) {
                continue;
            }
            if (rc.canSenseLocation(loc) && isCloseBuildLoc(loc) && isLocDryEmpty(loc)) {
                int dist = here.distanceSquaredTo(loc);
                if (dist < closestWallLocDist) {
                    closestWallLocDist = dist;
                    closestWallLoc = loc;
                }
            }
        }

        if (closestWallLoc == null) {
            if (maxXYDistance(HQLoc, here) < wallRingRadius) {
                closestWallLoc = getSymmetryLoc();
            } else {
                closestWallLoc = HQLoc;
            }
        }

        log("Moving to build fulfillment center");
        moveLog(closestWallLoc);
        return false;
    }

    public static boolean buildCloseVaporator () throws GameActionException {
        log("BUILDER MINER: Close Vaporator");
        int targetRing = 3;
        for (Direction dir : directions) {
            MapLocation loc = rc.adjacentLocation(dir);
            if (!rc.onTheMap(loc)) {
                continue;
            }
            if (maxXYDistance(HQLoc, loc) == targetRing && isCloseBuildLoc(loc)) {
                if (isDirDryFlatEmpty(dir)) {
                    if (rc.getTeamSoup() >= RobotType.VAPORATOR.cost) {
                        Actions.doBuildRobot(RobotType.VAPORATOR, dir);
                        tlog("Vaporator Built");
                        writeTransactionVaporatorStatus(1);
                        return true;
                    } else {
                        tlog("In position, not enough soup");
                    }
                    return false;
                }
            }
        }

        int closestWallLocDist = P_INF;
        MapLocation closestWallLoc = null;
        for (int i = 0; i < wallLocsLength; i++) {
            MapLocation loc = wallLocs[i].add(wallLocs[i].directionTo(HQLoc));
            if (here.isAdjacentTo(loc)) {
                continue;
            }
            if (rc.canSenseLocation(loc) && isCloseBuildLoc(loc) && isLocDryEmpty(loc)) {
                int dist = here.distanceSquaredTo(loc);
                if (dist < closestWallLocDist) {
                    closestWallLocDist = dist;
                    closestWallLoc = loc;
                }
            }
        }

        if (closestWallLoc == null) {
            if (maxXYDistance(HQLoc, here) < wallRingRadius) {
                closestWallLoc = getSymmetryLoc();
            } else {
                closestWallLoc = HQLoc;
            }
        }

        log("Moving to build vaporator");
        moveLog(closestWallLoc);
        return false;
    }

    public static boolean buildCloseDesignSchool () throws GameActionException {
        log("BUILDER MINER: Close Design School");
        int targetRing = 3;
        for (Direction dir : directions) {
            MapLocation loc = rc.adjacentLocation(dir);
            if (!rc.onTheMap(loc)) {
                continue;
            }
            if (maxXYDistance(HQLoc, loc) == targetRing && isCloseBuildLoc(loc)) {
                if (isDirDryFlatEmpty(dir)) {
                    if (rc.getTeamSoup() >= RobotType.DESIGN_SCHOOL.cost) {
                        Actions.doBuildRobot(RobotType.DESIGN_SCHOOL, dir);
                        tlog("Design School Built");
                        return true;
                    } else {
                        tlog("In position, not enough soup");
                    }
                    return false;
                }
            }
        }

        int closestWallLocDist = P_INF;
        MapLocation closestWallLoc = null;
        for (int i = 0; i < wallLocsLength; i++) {
            MapLocation loc = wallLocs[i].add(wallLocs[i].directionTo(HQLoc));
            if (here.isAdjacentTo(loc)) {
                continue;
            }
            if (rc.canSenseLocation(loc) && isCloseBuildLoc(loc) && isLocDryEmpty(loc)) {
                int dist = here.distanceSquaredTo(loc);
                if (dist < closestWallLocDist) {
                    closestWallLocDist = dist;
                    closestWallLoc = loc;
                }
            }
        }

        if (closestWallLoc == null) {
            if (maxXYDistance(HQLoc, here) < wallRingRadius) {
                closestWallLoc = getSymmetryLoc();
            } else {
                closestWallLoc = HQLoc;
            }
        }

        log("Moving to build design school");
        moveLog(closestWallLoc);
        return false;
    }
}
