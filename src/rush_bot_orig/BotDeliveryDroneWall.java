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

public class BotDeliveryDroneWall extends BotDeliveryDrone {

    public static boolean initializedDroneWall = false;

    public static MapLocation[] wallLocations;
    public static boolean atWall = false;

    public static void initDroneWall() throws GameActionException {

        initializedDroneWall = true;
        wallLocations = new MapLocation[25];
        int temp = 0;
        for (int i = -3; i <= 3; i++) {
            for (int j = -3; j <= 3; j++) {
                if (maxXYDistance(HQLoc, HQLoc.translate(i,j)) >= 3) {
                    wallLocations[temp++] = HQLoc.translate(i,j);
                }
            }
        }

        Globals.endTurn();
        Globals.update();
    }

    public static void turn() throws GameActionException {
        log("WALL DRONE ");

        if (!initializedDroneWall) {
            initDroneWall();
        }

        if (!rc.isReady()) {
            log("Not Ready");
            return;
        }

        // Move to wall
        if (!atWall) {
            for (MapLocation ml : wallLocations) {
                if (rc.senseRobotAtLocation(ml) == null) {
                    moveLog(ml);
                    if (inArray(wallLocations, here, wallLocations.length)) {
                        atWall = true;
                    }
                    return;
                }
            }
        }
        return;

    }
}
