package kryptonite;

import battlecode.common.*;


import static kryptonite.Communication.*;
import static kryptonite.Debug.*;
import static kryptonite.Map.*;

public class BotMinerBuilder extends BotMiner {
    public static boolean initializedMinerBuilder = false;

    public static int buildInstruction = -1;

    public static void initMinerBuilder() throws GameActionException {

        initializedMinerBuilder = true;

//        Globals.endTurn();
//        Globals.update();
    }

    public static void uninitMinerBuilder() throws GameActionException {

        initializedMinerBuilder = false;
        buildInstruction = -1;
    }

    public static void turn() throws GameActionException {
        if (!initializedMinerBuilder) {
            initMinerBuilder();
        }
        drawDot(here, BROWN);

        if (!rc.isReady()) {
            log("Not ready");
            return;
        }

        switch (buildInstruction) {
            case BUILD_CLOSE_FULFILLMENT_CENTER:
                if (buildCloseFulfillmentCenter()) {
                    uninitMinerBuilder();
                }
                break;
            case BUILD_CLOSE_VAPORATOR:
                if (buildCloseVaporator()) {
                    vaporatorBuiltCount++;
                }
                if (vaporatorBuiltCount >= 3) {
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
        for (Direction dir : directions) {
            MapLocation loc = rc.adjacentLocation(dir);
            if (!rc.onTheMap(loc)) {
                continue;
            }
            if (maxXYDistance(HQLoc, loc) == 1) {
                if (isDirDryFlatEmpty(dir)) {
                    if (rc.getTeamSoup() >= RobotType.FULFILLMENT_CENTER.cost) {
                        Actions.doBuildRobot(RobotType.FULFILLMENT_CENTER, dir);
                        tlog("Fulfillment Center Built");
                        return true;
                    } else {
                        tlog("In position, not enough soup");
                    }
                    // return if in position/built it
                    return false;
                }
            }
        }

        //find a spot in the 7x7 where it can build design school
        for (int[] dir : senseDirections) {
            // ignore locs that are out of sensor range or within build range (since they are not flat)
            if (dir[2] <= 2 || actualSensorRadiusSquared < dir[2]) {
                continue;
            }
            MapLocation buildLocation = here.translate(dir[0], dir[1]);
            if (!rc.onTheMap(buildLocation)) {
                continue;
            }

            if (maxXYDistance(HQLoc, buildLocation) == 1) {
                if (isLocDryEmpty(buildLocation)) {
                    log("Moving to build fulfillment center");
                    moveLog(buildLocation);
                    return false;
                }
            }
        }
        return false;
    }

    public static int vaporatorBuiltCount = 0;

    public static boolean buildCloseVaporator () throws GameActionException {
        log("BUILDER MINER: Close Vaporator");
        for (Direction dir : directions) {
            MapLocation loc = rc.adjacentLocation(dir);
            if (!rc.onTheMap(loc)) {
                continue;
            }
            if (maxXYDistance(HQLoc, loc) == 2) {
                if (isDirDryFlatEmpty(dir)) {
                    if (rc.getTeamSoup() >= RobotType.VAPORATOR.cost) {
                        Actions.doBuildRobot(RobotType.VAPORATOR, dir);
                        tlog("Vaporator Built");
                        return true;
                    } else {
                        tlog("In position, not enough soup");
                    }
                    // return if in position/built it
                    return false;
                }
            }
        }

        //find a spot in the 5x5 where it can build vaporator
        for (int[] dir : senseDirections) {
            // ignore locs that are out of sensor range or within build range (since they are not flat)
            if (dir[2] <= 2 || actualSensorRadiusSquared < dir[2]) {
                continue;
            }
            MapLocation buildLocation = here.translate(dir[0], dir[1]);
            if (!rc.onTheMap(buildLocation)) {
                continue;
            }
            // forces it to be on 5x5 ring
            if (maxXYDistance(HQLoc, buildLocation) == 2) {
                if (isLocDryEmpty(buildLocation)) {
                    log("Moving to build vaporator");
                    moveLog(buildLocation);
                    return false;
                }
            }
        }
        return false;
    }

    /*
    Returns if task was completed
     */
    public static boolean buildCloseDesignSchool () throws GameActionException {
        log("BUILDER MINER: Close Design School");
        for (Direction dir : directions) {
            MapLocation loc = rc.adjacentLocation(dir);
            if (!rc.onTheMap(loc)) {
                continue;
            }
            if (maxXYDistance(HQLoc, loc) == 1) {
                if (isDirDryFlatEmpty(dir)) {
                    if (rc.getTeamSoup() >= RobotType.DESIGN_SCHOOL.cost) {
                        Actions.doBuildRobot(RobotType.DESIGN_SCHOOL, dir);
                        tlog("Design School Built");
                        return true;
                    } else {
                        tlog("In position, not enough soup");
                    }
                    // return if in position/built it
                    return false;
                }
            }
        }

        //find a spot in the 3x3 where it can build design school
        for (int[] dir : senseDirections) {
            // ignore locs that are out of sensor range or within build range (since they are not flat)
            if (dir[2] <= 2 || actualSensorRadiusSquared < dir[2]) {
                continue;
            }
            MapLocation buildLocation = here.translate(dir[0], dir[1]);
            if (!rc.onTheMap(buildLocation)) {
                continue;
            }
            // forces it to be on 3x3 ring
            if (maxXYDistance(HQLoc, buildLocation) == 1) {
                if (isLocDryEmpty(buildLocation)) {
                    log("Moving to build design school");
                    moveLog(buildLocation);
                    return false;
                }
            }
        }
        return false;
    }
}
