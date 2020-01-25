package kryptonite;

import battlecode.common.*;

import static kryptonite.Communication.*;
import static kryptonite.Debug.*;
import static kryptonite.Map.*;
import static kryptonite.Nav.*;
import static kryptonite.Utils.*;
import static kryptonite.Zones.*;

public class Wall extends Globals {

    public static boolean hasLoadedWall = false;

    public static int wallRingRadius = 4; // 9x9 ring
    public static int wallRingSize = 2 * wallRingRadius + 1;
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
