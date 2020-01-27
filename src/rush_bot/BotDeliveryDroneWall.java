package rush_bot;

import battlecode.common.*;

import javax.xml.bind.annotation.XmlInlineBinaryData;

import static rush_bot.Debug.*;
import static rush_bot.Map.*;
import static rush_bot.Utils.*;
import static rush_bot.Wall.*;

public class BotDeliveryDroneWall extends BotDeliveryDrone {

    public static boolean initializedDroneWall = false;

    public static void initDroneWall() throws GameActionException {

        initializedDroneWall = true;

        loadDroneWallInfo();
        for (int i = 0; i < droneWallLocsLength; i++) {
            log("loc " + i + " " + droneWallLocs[i]);
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

        if (inArray(droneWallLocs, here, droneWallLocsLength)) {
            log("At wall");
            return;
        }

        // Move to wall
        log("Moving to wall");
        int minDist = P_INF;
        MapLocation minLoc = null;
        for (int i = 0; i < droneWallLocsLength; i++) {
            MapLocation loc = droneWallLocs[i];
            if (rc.canSenseLocation(loc) && isLocEmpty(loc)) {
                int dist = here.distanceSquaredTo(loc);
                if (dist < minDist) {
                    minDist = dist;
                    minLoc = loc;
                }
            }
        }
        if (minLoc != null) {
            moveLog(minLoc);
            return;
        } else {
            Direction rotateDir = HQLoc.directionTo(here).rotateRight().rotateRight();
            MapLocation rotateLoc = HQLoc.add(rotateDir).add(rotateDir).add(rotateDir);
            moveLog(rotateLoc);
            return;
        }

    }
}
