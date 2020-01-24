package turtle;

import battlecode.common.*;

import static turtle.Communication.*;
import static turtle.Debug.*;
import static turtle.Map.*;
import static turtle.Nav.*;
import static turtle.Utils.*;
import static turtle.Zones.*;

public class Wall extends Globals {

    public static boolean hasLoadedWall = false;

    public static int wallRingRadius = 3; // 7x7 ring
    public static int wallRingSize = 2 * wallRingRadius + 1; // 7x7 ring
    public static int wallMinX, wallMinY, wallMaxX, wallMaxY;
    public static MapLocation[] wallLocs = null;
    public static int wallLocsLength;
    public static boolean wallCompleted;
    public static int terraDepth = 8;

    /*
    Loads wall information
     */

    public static void loadWallInfo() throws GameActionException {

        if (hasLoadedWall) {
            return;
        }

        log("LOADING WALL INFORMATION");

        wallLocs = new MapLocation[wallRingRadius * 8];
        int index = 0;

        // move down along right wall
        MapLocation templ = HQLoc.translate(wallRingRadius, wallRingRadius);
        for(int i = 0; i < wallRingSize - 1; i++) {
            MapLocation newl = templ.translate(0, -i);
            if(rc.onTheMap(newl)) {
                wallLocs[index] = newl;
                index++;
            }
        }
        // move left along bottom wall
        templ = HQLoc.translate(wallRingRadius, -wallRingRadius);
        for(int i = 0; i < wallRingSize - 1; i++) {
            MapLocation newl = templ.translate(-i, 0);
            if(rc.onTheMap(newl)) {
                wallLocs[index] = newl;
                index++;
            }
        }
        // move up along left wall
        templ = HQLoc.translate(-wallRingRadius, -wallRingRadius);
        for(int i = 0; i < wallRingSize - 1; i++) {
            MapLocation newl = templ.translate(0, i);
            if(rc.onTheMap(newl)) {
                wallLocs[index] = newl;
                index++;
            }
        }
        // move right along top wall
        templ = HQLoc.translate(-wallRingRadius, wallRingRadius);
        for(int i = 0; i < wallRingSize - 1; i++) {
            MapLocation newl = templ.translate(i, 0);
            if(rc.onTheMap(newl)) {
                wallLocs[index] = newl;
                index++;
            }
        }
        wallLocsLength = index;
        tlog("WALL_LOCS_LENGTH: " + wallLocsLength);

        log("FINISHED LOADING WALL INFORMATION");

        hasLoadedWall = true;
    }

}
