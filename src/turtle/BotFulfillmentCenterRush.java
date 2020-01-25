package turtle;

import battlecode.common.*;


import static turtle.Communication.*;
import static turtle.Debug.*;
import static turtle.Map.*;
import static turtle.Nav.*;
import static turtle.Utils.*;
import static turtle.Zones.*;

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

        if (dronesBuilt == 0) {
            buildDrone(getCloseDirections(here.directionTo(rc.senseRobot(rushMinerID).location)), RobotType.DELIVERY_DRONE.cost);
            return;
        }
    }
}
