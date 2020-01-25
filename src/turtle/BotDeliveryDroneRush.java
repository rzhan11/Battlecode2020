package turtle;

import battlecode.common.*;


import static turtle.Communication.*;
import static turtle.Debug.*;
import static turtle.Map.*;
import static turtle.Nav.*;
import static turtle.Utils.*;
import static turtle.Zones.*;

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

        if (!rc.isReady()) {
            log("Not ready");
            return;
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
                        myRole = DRONE_HARASS_ROLE;
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
