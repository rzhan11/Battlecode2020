package kryptonite;

import battlecode.common.*;

import static kryptonite.Actions.*;
import static kryptonite.Communication.*;
import static kryptonite.Debug.*;
import static kryptonite.Map.*;
import static kryptonite.Nav.*;
import static kryptonite.Utils.*;
import static kryptonite.Zones.*;

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
            if (isDirDryFlatEmpty(dir)) {
                Actions.doBuildRobot(rt, dir);
                tlog("Built " + rt + " " + dir);
                return dir;
            }
        }
        tlog("No open spots found");
        return null;
    }

}
