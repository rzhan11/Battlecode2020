package turtle;

import battlecode.common.*;


import static turtle.Communication.*;
import static turtle.Debug.*;
import static turtle.Map.*;
import static turtle.Nav.*;
import static turtle.Utils.*;
import static turtle.Zones.*;

public class BotDesignSchoolRush extends BotDesignSchool {

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

        if (landscapersBuilt < 8) {
            buildLandscaper(getCloseDirections(here.directionTo(enemyHQLoc)), RobotType.LANDSCAPER.cost);
            return;
        }
    }
}
