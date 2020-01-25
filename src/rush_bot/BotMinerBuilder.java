package rush_bot;

import battlecode.common.*;

import static rush_bot.Actions.*;
import static rush_bot.Communication.*;
import static rush_bot.Debug.*;
import static rush_bot.Globals.*;
import static rush_bot.Map.*;
import static rush_bot.Nav.*;
import static rush_bot.Utils.*;
import static rush_bot.Wall.*;
import static rush_bot.Zones.*;

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
                if (isDirDryFlatEmpty(dir)) {
                    if (rc.getTeamSoup() >= rt.cost) {
                        Actions.doBuildRobot(rt, dir);
                        if (rt == RobotType.VAPORATOR) {
                            writeTransactionVaporatorStatus(1);
                        }
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
