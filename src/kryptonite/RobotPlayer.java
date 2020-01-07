package kryptonite;

import battlecode.common.*;

public strictfp class RobotPlayer {
    @SuppressWarnings("unused")
    public static void run(RobotController rc) throws GameActionException {
        System.out.println("I'm a " + rc.getType() + "! Location " + rc.getLocation());
        Globals.init(rc);
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
