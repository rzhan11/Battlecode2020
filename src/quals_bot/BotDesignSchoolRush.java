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
