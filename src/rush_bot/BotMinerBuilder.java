package rush_bot;

import battlecode.common.*;

import static rush_bot.Communication.*;
import static rush_bot.Debug.*;
import static rush_bot.Map.*;
import static rush_bot.Utils.*;
import static rush_bot.Wall.*;

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

        if (buildInstruction != BUILD_PLATFORM) {
            if (roundNum - assignRound >= BotHQ.REASSIGN_ROUND_NUM) {
                log("Assignment expired");
                uninitMinerBuilder();
                return;
            }
        }

        log("MINER BUILDER");
        drawDot(here, BROWN);

        switch (buildInstruction) {
            case BUILD_CLOSE_FULFILLMENT_CENTER:
                if (buildCloseBuilding(RobotType.FULFILLMENT_CENTER, 3)) {
                    uninitMinerBuilder();
                }
                break;
            case BUILD_CLOSE_VAPORATOR:
                if (buildCloseBuilding(RobotType.VAPORATOR, 3)) {
                    uninitMinerBuilder();
                }
                break;
            case BUILD_CLOSE_DESIGN_SCHOOL:
                if (buildCloseBuilding(RobotType.DESIGN_SCHOOL, 3)) {
                    uninitMinerBuilder();
                }
                break;
            case BUILD_CLOSE_REFINERY:
                if (buildCloseBuilding(RobotType.REFINERY, 3)) {
                    uninitMinerBuilder();
                }
                break;
            case BUILD_PLATFORM:
                buildPlatform();
                if (builtVaporator && builtFulfillmentCenter && builtDesignSchool) {
                    writeTransactionPlatformBuildingsCompleted();
                    rc.disintegrate();
                }
                break;
        }
    }

    public static boolean builtVaporator = false;
    public static boolean builtFulfillmentCenter = false;
    public static boolean builtDesignSchool = false;

    public static void buildPlatform () throws GameActionException{
        log("BUILDER MINER: PLATFORM");
        if (!inArray(platformLocs, here, platformLocs.length)) {
            log("hi " + here);
            moveLog(platformCornerLoc);
            return;
        }
        if (!builtVaporator) {
            if (rc.getTeamSoup() < RobotType.VAPORATOR.cost) {
                log("Cannot afford platform vaporator");
                return;
            }
            for (Direction dir: directions) {
                MapLocation loc = rc.adjacentLocation(dir);
                if (!rc.onTheMap(loc)) {
                    continue;
                }
                if (!inArray(platformLocs, loc, platformLocs.length)) {
                    continue;
                }
                if (rc.senseElevation(loc) == PLATFORM_ELEVATION && isLocDryFlatEmpty(loc)) {
                    Actions.doBuildRobot(RobotType.VAPORATOR, dir);
                    builtVaporator = true;
                    return;
                }
            }
        } else if (!builtFulfillmentCenter) {
            log("fc");
            if (rc.getTeamSoup() < RobotType.FULFILLMENT_CENTER.cost) {
                log("Cannot afford platform fulfillment center");
                return;
            }
            for (Direction dir: directions) {
                MapLocation loc = rc.adjacentLocation(dir);
                if (!rc.onTheMap(loc)) {
                    continue;
                }
                if (!inArray(platformLocs, loc, platformLocs.length)) {
                    continue;
                }
                if (rc.senseElevation(loc) == PLATFORM_ELEVATION && isLocDryFlatEmpty(loc)) {
                    Actions.doBuildRobot(RobotType.FULFILLMENT_CENTER, dir);
                    builtFulfillmentCenter = true;
                    return;
                }
            }
        } else if (!builtDesignSchool) {
            log("ds");
            if (rc.getTeamSoup() < RobotType.DESIGN_SCHOOL.cost + dynamicCost) {
                log("Cannot afford platform design school");
                return;
            }
            for (Direction dir: directions) {
                MapLocation loc = rc.adjacentLocation(dir);
                if (!rc.onTheMap(loc)) {
                    continue;
                }
                if (!inArray(platformLocs, loc, platformLocs.length)) {
                    continue;
                }
                if (rc.senseElevation(loc) == PLATFORM_ELEVATION && isLocDryFlatEmpty(loc)) {
                    Actions.doBuildRobot(RobotType.DESIGN_SCHOOL, dir);
                    builtDesignSchool = true;
                    return;
                }
            }
        }
    }

    public static boolean buildCloseBuilding (RobotType rt, int targetRing) throws GameActionException {
        log("BUILDER MINER: Close " + rt);
        for (Direction dir : directions) {
            MapLocation loc = rc.adjacentLocation(dir);
            if (!rc.onTheMap(loc)) {
                continue;
            }
            if (maxXYDistance(HQLoc, loc) == targetRing) {
                if (isLocDryFlatEmpty(loc)) {
                    if (rc.getTeamSoup() >= rt.cost) {
                        Actions.doBuildRobot(rt, dir);
                        return true;
                    } else {
                        tlog("In position, not enough soup");
                    }
                    // return if in position/built it
                    return false;
                }
            }
        }

        for (int[] dir : senseDirections) {
            // ignore locs that are out of sensor range or within build range (since they are not flat)
            if (dir[2] <= 2 || actualSensorRadiusSquared < dir[2]) {
                continue;
            }
            MapLocation buildLocation = here.translate(dir[0], dir[1]);
            if (!rc.onTheMap(buildLocation)) {
                continue;
            }
            if (maxXYDistance(HQLoc, buildLocation) == targetRing) {
                if (isLocDryEmpty(buildLocation)) {
                    log("Moving to build " + rt);
                    moveLog(buildLocation);
                    return false;
                }
            }
        }
        return false;
    }
}
