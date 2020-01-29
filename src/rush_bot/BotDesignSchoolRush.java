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

public class BotDesignSchoolRush extends BotDesignSchool {

    public static boolean seesEnemyDrone = false;
    public static boolean seesEnemyFulfillmentCenter = false;
    public static boolean seesAllyNetGun = false;
    public static boolean seesAllyMiner = false;

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
        MapLocation netGunLoc = null;
        for (RobotInfo ri: visibleAllies) {
            if (maxXYDistance(enemyHQLoc, ri.location) <= 2) {
                switch (ri.type) {
                    case NET_GUN:
                        seesAllyNetGun = true;
                        netGunLoc = ri.location;
                        break;
                }
            }
        }

        seesAllyMiner = false;
        if (rc.canSenseRobot(rushMinerID)) {
            RobotInfo rushMinerInfo = rc.senseRobot(rushMinerID);
            if (maxXYDistance(enemyHQLoc, rushMinerInfo.location) <= 2) {
                seesAllyMiner = true;
            }
        }

        if (seesEnemyDrone && !seesAllyNetGun) {
            return;
        }

        if (seesEnemyFulfillmentCenter) {
            if (!seesAllyNetGun && !seesAllyMiner) {
                return;
            }
        }

        if (landscapersBuilt < 8) {
            if (netGunLoc != null) {
                buildLandscaper(getCloseDirections(here.directionTo(netGunLoc)), RobotType.LANDSCAPER.cost);
            } else {
                buildLandscaper(getCloseDirections(here.directionTo(enemyHQLoc)), RobotType.LANDSCAPER.cost);
            }
            return;
        }
    }
}
