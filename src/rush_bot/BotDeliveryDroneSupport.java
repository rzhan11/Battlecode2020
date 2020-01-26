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


public class BotDeliveryDroneSupport extends BotDeliveryDrone {

    public static boolean initializedDroneSupport = false;
    public static int targetID = -1;
    public static MapLocation targetDropLoc = null;

    public static void initDroneSupport() throws GameActionException {

        myRole = DRONE_SUPPORT_ROLE;

        initializedDroneSupport = true;

        Globals.endTurn();
        Globals.update();
    }

    public static void turn() throws GameActionException {
        log("SUPPORT DRONE");
        log("smallWallFull " + smallWallFull);
        log("supportFull " + supportFull);

        if (!initializedDroneSupport) {
            initDroneSupport();
        }

        if (smallWallFull && supportFull) {
            myRole = DRONE_WALL_ROLE;
            return;
        }

        if (rc.isCurrentlyHoldingUnit()) {

            MapLocation[] targetLocs = null;
            int targetLocsLength = -1;
            if (!smallWallFull) {
                targetLocs = smallWallLocs;
                targetLocsLength = smallWallLocsLength;
            } else if (!supportFull) {
                targetLocs = supportWallLocs;
                targetLocsLength = supportWallLocsLength;
            }

            MapLocation closestLoc = null;
            int closestDist = P_INF;
            for (int i = 0; i < targetLocsLength; i++) {
                MapLocation loc = targetLocs[i];
                if (rc.canSenseLocation(loc) && isLocDryEmpty(loc)) {
                    if (here.isAdjacentTo(loc)) {
                        Actions.doDropUnit(here.directionTo(loc));
                        return;
                    }
                    int dist = getSymmetryLoc().distanceSquaredTo(loc);
                    if (dist < closestDist) {
                        closestDist = dist;
                        closestLoc = loc;
                    }
                }
            }
            if (closestLoc == null) {
                closestLoc = HQLoc;
            }
            moveLog(closestLoc);
        } else {
            for (RobotInfo ri: adjacentAllies) {
                if (ri.type.canBePickedUp()) {
                    if (ri.location.isAdjacentTo(HQLoc)) {
                        continue;
                    }
                    if (maxXYDistance(HQLoc, ri.location) <= 2 && !inArray(digLocs2x2, ri.location, digLocs2x2.length)) {
                        continue;
                    }
                    Actions.doPickUpUnit(ri.ID);
                    return;
                }
            }

            MapLocation closestLoc = null;
            int closestDist = P_INF;
            for (RobotInfo ri: visibleAllies) {
                if (ri.location.isAdjacentTo(HQLoc)) {
                    continue;
                }
                if (maxXYDistance(HQLoc, ri.location) <= 2 && !inArray(digLocs2x2, ri.location, digLocs2x2.length)) {
                    continue;
                }
                int dist = here.distanceSquaredTo(ri.location);
                if (dist < closestDist) {
                    closestDist = dist;
                    closestLoc = ri.location;
                }
            }
            moveLog(closestLoc);
        }
    }
}
