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

public class BotDeliveryDroneRush extends BotDeliveryDrone {

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
        if (abortRush) {
            myRole = DRONE_SUPPORT_ROLE;
            return;
        }

        if (rc.isCurrentlyHoldingUnit()) {
            if (roundNum > START_RUSH_STATUS_ROUND && roundNum % RUSH_STATUS_INTERVAL == 0) {
                writeTransactionRushStatus(CONTINUE_RUSH_FLAG);
            }
        }

        if (!rc.isReady()) {
            log("Not ready");
            return;
        }

        log("DRONE RUSH");

        int avoidDangerResult = Nav.avoidDanger();
        if (avoidDangerResult == 1) {
            Nav.bugTracing = false;
            return;
        }
        if (avoidDangerResult == -1) {
            // when danger is unavoidable, reset isDirMoveable to ignore danger tiles
            updateIsDirMoveable();
        }

        if (here.distanceSquaredTo(getSymmetryLoc()) <= 25) {
            for (int i = 1; i < directions.length; i++) {
                isDirMoveable[i] = false;
            }
        }

        if (!rc.isCurrentlyHoldingUnit()) {
            if (!rc.canSenseRobot(rushMinerID)) {
                log("Cannot see rush miner");
                return;
            }
            MapLocation loc = rc.senseRobot(rushMinerID).location;
            if (here.isAdjacentTo(loc)) {
                Actions.doPickUpUnit(rushMinerID);
                return;
            } else {
                moveLog(loc);
                return;
            }
        } else {
            if (enemyHQLoc == null) {
                moveLog(symmetryHQLocs[getClosestSymmetryIndex()]);
                return;
            }
            // STATE = enemyHQLoc is known
            if (here.distanceSquaredTo(enemyHQLoc) <= RobotType.DELIVERY_DRONE.sensorRadiusSquared) {
                Direction[] orderedDirections = getCloseDirections(here.directionTo(enemyHQLoc));
                outer: for (Direction dir: orderedDirections) {
                    MapLocation loc = rc.adjacentLocation(dir);
                    if (!rc.onTheMap(loc)) {
                        continue;
                    }
                    if (isLocDryEmpty(rc.adjacentLocation(dir))) {
                        // avoid placing into enemy drone pick up range
                        RobotInfo[] possibleEnemyDrones = rc.senseNearbyRobots(loc, 2, them);
                        for (RobotInfo ri: possibleEnemyDrones) {
                            if (ri.type == RobotType.DELIVERY_DRONE) {
                                continue outer;
                            }
                        }
                        Actions.doDropUnit(dir);
                        myRole = DRONE_SUPPORT_ROLE;
                        return;
                    }
                }
            }

            moveLog(enemyHQLoc);
            return;
        }
        // end
    }
}
