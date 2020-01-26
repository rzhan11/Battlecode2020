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

public class Utils extends Globals {

    public static boolean inArray(Object[] arr, Object item, int length) {
        for(int i = 0; i < length; i++) {
            if(arr[i].equals(item)) {
                return true;
            }
        }
        return false;
    }

    public static boolean inArray(int[] arr, int item, int length) {
        for(int i = 0; i < length; i++) {
            if(arr[i] == item) {
                return true;
            }
        }
        return false;
    }

    /*
    Given a direction, return an array of 8 directions (excludes CENTER)
    that is ordered by how close they are to the given direction
     */
    public static Direction[] getCloseDirections (Direction dir) {
        if (dir == Direction.CENTER) {
            logi("WARNING: Tried to getCloseDirections of center");
            return null;
        }
        Direction[] dirs = new Direction[8];
        dirs[0] = dir;
        dirs[1] = dir.rotateRight();
        dirs[2] = dir.rotateLeft();
        dirs[3] = dirs[1].rotateRight();
        dirs[4] = dirs[2].rotateLeft();
        dirs[5] = dirs[3].rotateRight();
        dirs[6] = dirs[4].rotateLeft();
        dirs[7] = dir.opposite();;
        return dirs;
    }

    public static Direction tryBuild (RobotType rt, Direction[] givenDirections) throws GameActionException {
        for (Direction dir : givenDirections) {
            MapLocation loc = rc.adjacentLocation(dir);
            if (isLocDryEmpty(loc) && (rt == RobotType.DELIVERY_DRONE || isLocFlat(loc))) {
                Actions.doBuildRobot(rt, dir);
                tlog("Built " + rt + " " + dir);
                return dir;
            }
        }
        tlog("No open spots found");
        return null;
    }

}
