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
