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
