package rush_bot_orig;

import battlecode.common.*;

import static rush_bot_orig.Actions.*;
import static rush_bot_orig.Communication.*;
import static rush_bot_orig.Debug.*;
import static rush_bot_orig.Globals.*;
import static rush_bot_orig.Map.*;
import static rush_bot_orig.Nav.*;
import static rush_bot_orig.Utils.*;
import static rush_bot_orig.Wall.*;
import static rush_bot_orig.Zones.*;

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
