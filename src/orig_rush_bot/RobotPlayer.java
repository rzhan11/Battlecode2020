package orig_rush_bot;

import battlecode.common.*;

import static orig_rush_bot.Actions.*;
import static orig_rush_bot.Communication.*;
import static orig_rush_bot.Debug.*;
import static orig_rush_bot.Globals.*;
import static orig_rush_bot.Map.*;
import static orig_rush_bot.Nav.*;
import static orig_rush_bot.Utils.*;
import static orig_rush_bot.Wall.*;
import static orig_rush_bot.Zones.*;

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
