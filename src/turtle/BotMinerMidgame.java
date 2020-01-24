package kryptonite;

import battlecode.common.*;


import static kryptonite.Communication.*;
import static kryptonite.Debug.*;
import static kryptonite.Map.*;
import static kryptonite.Wall.*;

public class BotMinerMidgame extends BotMiner {
    public static boolean initializedMinerMidgame = false;

    public static void initMinerMidgame() throws GameActionException {

        initializedMinerMidgame = true;

//        Globals.endTurn();
//        Globals.update();
    }

    public static void turn() throws GameActionException {
        if (!initializedMinerMidgame) {
            initMinerMidgame();
        }

        log("MINER MIDGAME");

        // if outside the plot, do not go back in
        if (maxXYDistance(HQLoc, here) >= wallRingRadius) {
            for (int i = 0; i < directions.length; i++) {
                MapLocation loc = rc.adjacentLocation(directions[i]);
                if (maxXYDistance(HQLoc, loc) < wallRingRadius)  {
                    isDirMoveable[i] = false;
                }
            }
        }

        // if on the terra plot, do not move onto non-terra tiles
        if (myElevation == terraDepth) {
            for (int i = 0; i < directions.length; i++) {
                MapLocation loc = rc.adjacentLocation(directions[i]);
                if (rc.canSenseLocation(loc) && rc.senseElevation(loc) != terraDepth)  {
                    isDirMoveable[i] = false;
                }
            }
        }

        if (myElevation == terraDepth) {
            buildTerraVaporator();
        } else {
            if (maxXYDistance(HQLoc, here) >= wallRingRadius) {
                moveLog(HQLoc);
            } else {
                moveLog(getSymmetryLoc());
            }
        }

    }

    /*
    Returns if task was completed
     */
    public static boolean buildTerraFulfillmentCenter () throws GameActionException {
        log("MIDGAME MINER: Terra Fulfillment Center");
        for (Direction dir : directions) {
            MapLocation loc = rc.adjacentLocation(dir);
            if (!rc.onTheMap(loc)) {
                continue;
            }
            if (isBuildLoc(loc) && maxXYDistance(HQLoc, loc) == wallRingRadius + 2) {
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

        //find a spot in the 11x11 where it can build design school
        for (int[] dir : senseDirections) {
            // ignore locs that are out of sensor range or within build range (since they are not flat)
            if (dir[2] <= 2 || actualSensorRadiusSquared < dir[2]) {
                continue;
            }
            MapLocation loc = here.translate(dir[0], dir[1]);
            if (!rc.onTheMap(loc)) {
                continue;
            }

            if (isBuildLoc(loc) && maxXYDistance(HQLoc, loc) == wallRingRadius + 2) {
                if (isLocDryEmpty(loc)) {
                    log("Moving to build fulfillment center");
                    moveLog(loc);
                    return false;
                }
            }
        }
        return false;
    }

    public static boolean buildTerraVaporator() throws GameActionException {
        log("MIDGAME MINER: Terra Vaporator");
        for (Direction dir : directions) {
            MapLocation loc = rc.adjacentLocation(dir);
            if (!rc.onTheMap(loc)) {
                continue;
            }
            int ring = maxXYDistance(HQLoc, loc);
            if (isBuildLoc(loc) && ring != wallRingRadius + 2 && ring > wallRingRadius) {
                if (isDirDryFlatEmpty(dir)) {
                    if (rc.getTeamSoup() >= RobotType.VAPORATOR.cost) {
                        Actions.doBuildRobot(RobotType.VAPORATOR, dir);
                        writeTransactionVaporatorStatus(1);
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
            MapLocation loc = here.translate(dir[0], dir[1]);
            if (!rc.onTheMap(loc)) {
                continue;
            }
            int ring = maxXYDistance(HQLoc, loc);
            if (isBuildLoc(loc) && ring != wallRingRadius + 2 && ring > wallRingRadius) {
                if (isLocDryEmpty(loc)) {
                    log("Moving to build vaporator");
                    moveLog(loc);
                    return false;
                }
            }
        }
        return false;
    }

    /*
    Returns if task was completed
     */
    public static boolean buildTerraDesignSchool () throws GameActionException {
        log("MIDGAME MINER: Terra Design School");
        for (Direction dir : directions) {
            MapLocation loc = rc.adjacentLocation(dir);
            if (!rc.onTheMap(loc)) {
                continue;
            }
            if (isBuildLoc(loc) && maxXYDistance(HQLoc, loc) == wallRingRadius + 2) {
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

        for (int[] dir : senseDirections) {
            // ignore locs that are out of sensor range or within build range (since they are not flat)
            if (dir[2] <= 2 || actualSensorRadiusSquared < dir[2]) {
                continue;
            }
            MapLocation loc = here.translate(dir[0], dir[1]);
            if (!rc.onTheMap(loc)) {
                continue;
            }
            // forces it to be on 11x11 ring
            if (isBuildLoc(loc) && maxXYDistance(HQLoc, loc) == wallRingRadius + 2) {
                if (isLocDryEmpty(loc)) {
                    log("Moving to build design school");
                    moveLog(loc);
                    return false;
                }
            }
        }
        return false;
    }
}
