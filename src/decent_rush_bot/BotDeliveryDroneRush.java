package decent_rush_bot;

import battlecode.common.*;

import static decent_rush_bot.Actions.*;
import static decent_rush_bot.Communication.*;
import static decent_rush_bot.Debug.*;
import static decent_rush_bot.Globals.*;
import static decent_rush_bot.Map.*;
import static decent_rush_bot.Nav.*;
import static decent_rush_bot.Utils.*;
import static decent_rush_bot.Wall.*;
import static decent_rush_bot.Zones.*;

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
                for (Direction dir: orderedDirections) {
                    if (isLocDryEmpty(rc.adjacentLocation(dir))) {
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
