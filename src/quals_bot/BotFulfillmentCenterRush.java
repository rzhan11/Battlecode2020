package quals_bot;

import battlecode.common.*;

import static quals_bot.Actions.*;
import static quals_bot.Communication.*;
import static quals_bot.Debug.*;
import static quals_bot.Globals.*;
import static quals_bot.Map.*;
import static quals_bot.Nav.*;
import static quals_bot.Utils.*;
import static quals_bot.Wall.*;
import static quals_bot.Zones.*;

public class BotFulfillmentCenterRush extends BotFulfillmentCenter {

    public static void loop() throws GameActionException {
        while (true) {
            try {
                Globals.update();

                turn();
            } catch (Exception e) {
                e.printStackTrace();
            }
            Globals.endTurn();
        }
    }

    public static void turn() throws GameActionException {

        log("FULFILLMENT CENTER RUSH");

        if (!rc.isReady()) {
            log("Not ready");
            return;
        }

        if (!rc.canSenseRobot(rushMinerID)) {
            log("Cannot sense rush miner");
            return;
        }

        if (dronesBuilt == 0 && rc.getTeamSoup() >= RobotType.DELIVERY_DRONE.cost) {
            buildRushDrone();
            return;
        }
    }

    public static void buildRushDrone() throws GameActionException {
        MapLocation targetLoc = symmetryHQLocs[getClosestSymmetryIndex()];
        MapLocation rushMinerLoc = rc.senseRobot(rushMinerID).location;
        int minDist = P_INF;
        Direction minDir = null;
        boolean isMinAdjToRushMiner = false;
        for (Direction dir: directions) {
            MapLocation loc = rc.adjacentLocation(dir);
            if (!rc.onTheMap(loc)) {
                continue;
            }
            if (isLocEmpty(loc)) {
                if (isMinAdjToRushMiner) {
                    if (loc.isAdjacentTo(rushMinerLoc)) {
                        int dist = loc.distanceSquaredTo(targetLoc);
                        if (dist < minDist) {
                            minDir = dir;
                            minDist = dist;
                        }
                    }
                } else {
                    int dist = loc.distanceSquaredTo(targetLoc);
                    if (loc.isAdjacentTo(rushMinerLoc) || dist < minDist) {
                        minDir = dir;
                        minDist = dist;
                        isMinAdjToRushMiner = true;
                    }
                }
            }
        }
        if (minDir != null) {
            Actions.doBuildRobot(RobotType.DELIVERY_DRONE, minDir);
            dronesBuilt++;
            return;
        }
    }
}
