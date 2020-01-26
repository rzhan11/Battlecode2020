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

public class BotDeliveryDroneWall extends BotDeliveryDrone {

    public static boolean initializedDroneWall = false;

    public static MapLocation[] wallLocations;
    public static boolean atWall = false;

    public static void initDroneWall() throws GameActionException {

        initializedDroneWall = true;
        wallLocations = new MapLocation[24];
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
        if (inArray(wallLocations, here, wallLocations.length)) {
            atWall = true;
        }
        // Move to wall
        if (!atWall) {
            log("Moving to wall");
            int min = Integer.MAX_VALUE;
            MapLocation wanted = null;
            for (MapLocation ml : wallLocations) {
                if (here.distanceSquaredTo(ml) < min
                        && here.distanceSquaredTo(ml) <= actualSensorRadiusSquared
                        && rc.senseRobotAtLocation(ml) == null) {
                    min = here.distanceSquaredTo(ml);
                    wanted = ml;
                }
            }
            if (wanted == null) {
                Direction d = HQLoc.directionTo(here).rotateRight();
                wanted = HQLoc.add(d).add(d);
            }
            for (Direction d : directions) {
                if (rc.adjacentLocation(d).equals(wanted)) {
                    rc.move(d);
                    return;
                }
            }
            moveLog(wanted);
            return;
        } else {
            log("At Position");
        }
        return;

    }
}
