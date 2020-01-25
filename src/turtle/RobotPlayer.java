package turtle;

import battlecode.common.*;

import static turtle.Actions.*;
import static turtle.Communication.*;
import static turtle.Debug.*;
import static turtle.Globals.*;
import static turtle.Map.*;
import static turtle.Nav.*;
import static turtle.Utils.*;
import static turtle.Wall.*;
import static turtle.Zones.*;

public strictfp class RobotPlayer {
    @SuppressWarnings("unused")
    public static void run(RobotController rc) throws GameActionException {
        try {
            Globals.init(rc);
        } catch (Exception e) {
            e.printStackTrace();
        }
        switch (rc.getType()) {
            case HQ:                 BotHQ.loop();                break;
            case MINER:              BotMiner.loop();             break;
            case REFINERY:           BotRefinery.loop();          break;
            case VAPORATOR:          BotVaporator.loop();         break;
            case DESIGN_SCHOOL:      BotDesignSchool.loop();      break;
            case FULFILLMENT_CENTER: BotFulfillmentCenter.loop(); break;
            case LANDSCAPER:         BotLandscaper.loop();        break;
            case DELIVERY_DRONE:     BotDeliveryDrone.loop();     break;
            case NET_GUN:            BotNetGun.loop();            break;
        }
    }
}
