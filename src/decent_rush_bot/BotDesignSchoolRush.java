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

public class BotDesignSchoolRush extends BotDesignSchool {

    public static boolean seesEnemyDrone = false;
    public static boolean seesEnemyFulfillmentCenter = false;
    public static boolean seesAllyNetGun = false;

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

        seesEnemyDrone = false;
        seesEnemyFulfillmentCenter = false;
        for (RobotInfo ri: visibleEnemies) {
            switch (ri.type) {
                case DELIVERY_DRONE:
                    seesEnemyDrone = true;
                    break;
                case FULFILLMENT_CENTER:
                    seesEnemyFulfillmentCenter = true;
                    break;
            }
        }

        seesAllyNetGun = false;
        for (RobotInfo ri: visibleAllies) {
            switch (ri.type) {
                case NET_GUN:
                    seesAllyNetGun = true;
                    break;
            }
        }


        if (!seesAllyNetGun) {
            if (seesEnemyDrone || seesEnemyFulfillmentCenter) {
                return;
            }
        }

        if (landscapersBuilt < 8) {
            buildLandscaper(getCloseDirections(here.directionTo(enemyHQLoc)), RobotType.LANDSCAPER.cost);
            return;
        }
    }
}
