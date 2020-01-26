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

public class Wall extends Globals {

    public static boolean hasLoadedWall = false;

    public static int wallRingRadius = 3; // 7x7 ring
    public static int wallRingSize = 2 * wallRingRadius + 1; // 7x7 ring
    public static int wallMinX, wallMinY, wallMaxX, wallMaxY;
    public static MapLocation[] wallLocs = null;
    public static int wallLocsLength;
    public static boolean wallCompleted;
    public static int terraDepth = 8;

    public static boolean startBuildInnerWall = false;
    public static MapLocation[] smallWallLocs;
    public static int smallWallLocsLength;
    public static MapLocation[] supportWallLocs;
    public static int supportWallLocsLength;
    public static MapLocation[] digLocs2x2 = new MapLocation[4];

    public static boolean smallWallFull;
    public static boolean supportFull;

    /*
    Loads wall information
     */

    public static void loadWallInfo() throws GameActionException {

        if (hasLoadedWall) {
            return;
        }

        log("LOADING WALL INFORMATION");

        for (int i = 0; i < cardinalDirections.length; i++) {
            digLocs2x2[i] = HQLoc.add(cardinalDirections[i]).add(cardinalDirections[i]);
        }

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

        loadInnerWallInfo();

        log("FINISHED LOADING WALL INFORMATION");

        hasLoadedWall = true;
    }

    public static void loadInnerWallInfo () {

        smallWallLocs = new MapLocation[8];

        int index = 0;
        for (Direction dir: directions) {
            MapLocation loc = HQLoc.add(dir);
            if (rc.onTheMap(loc.add(dir))) {
                smallWallLocs[index] = loc;
                index++;
            } else if (rc.onTheMap(loc.add(dir.rotateLeft()))) {
                smallWallLocs[index] = loc;
                index++;
            } else if (rc.onTheMap(loc.add(dir.rotateRight()))) {
                smallWallLocs[index] = loc;
                index++;
            }
        }
        smallWallLocsLength = index;
        tlog("SMALL_WALL_LOCS_LENGTH: " + smallWallLocsLength);


        supportWallLocs = new MapLocation[12];

        index = 0;

        int ringRadius = 2;
        int ringSize = 2 * ringRadius + 1;
        // move down along right wall
        MapLocation templ = HQLoc.translate(ringRadius, ringRadius);
        for(int i = 0; i < ringSize - 1; i++) {
            MapLocation newl = templ.translate(0, -i);
            Direction dir = HQLoc.directionTo(newl);
            // skip cardinal directions
            if (inArray(cardinalDirections, dir, cardinalDirections.length) && HQLoc.add(dir).add(dir).equals(newl)) {
                continue;
            }
            if(rc.onTheMap(newl)) {
                if (rc.onTheMap(newl.add(dir))) {
                    supportWallLocs[index] = newl;
                    index++;
                } else if (rc.onTheMap(newl.add(dir.rotateLeft()))) {
                    supportWallLocs[index] = newl;
                    index++;
                } else if (rc.onTheMap(newl.add(dir.rotateRight()))) {
                    supportWallLocs[index] = newl;
                    index++;
                }
            }
        }
        // move left along bottom wall
        templ = HQLoc.translate(ringRadius, -ringRadius);
        for(int i = 0; i < ringSize - 1; i++) {
            MapLocation newl = templ.translate(-i, 0);
            Direction dir = HQLoc.directionTo(newl);
            // skip cardinal directions
            if (inArray(cardinalDirections, dir, cardinalDirections.length) && HQLoc.add(dir).add(dir).equals(newl)) {
                continue;
            }
            if(rc.onTheMap(newl)) {
                if (rc.onTheMap(newl.add(dir))) {
                    supportWallLocs[index] = newl;
                    index++;
                } else if (rc.onTheMap(newl.add(dir.rotateLeft()))) {
                    supportWallLocs[index] = newl;
                    index++;
                } else if (rc.onTheMap(newl.add(dir.rotateRight()))) {
                    supportWallLocs[index] = newl;
                    index++;
                }
            }
        }
        // move up along left wall
        templ = HQLoc.translate(-ringRadius, -ringRadius);
        for(int i = 0; i < ringSize - 1; i++) {
            MapLocation newl = templ.translate(0, i);
            Direction dir = HQLoc.directionTo(newl);
            // skip cardinal directions
            if (inArray(cardinalDirections, dir, cardinalDirections.length) && HQLoc.add(dir).add(dir).equals(newl)) {
                continue;
            }
            if(rc.onTheMap(newl)) {
                if (rc.onTheMap(newl.add(dir))) {
                    supportWallLocs[index] = newl;
                    index++;
                } else if (rc.onTheMap(newl.add(dir.rotateLeft()))) {
                    supportWallLocs[index] = newl;
                    index++;
                } else if (rc.onTheMap(newl.add(dir.rotateRight()))) {
                    supportWallLocs[index] = newl;
                    index++;
                }
            }
        }
        // move right along top wall
        templ = HQLoc.translate(-ringRadius, ringRadius);
        for(int i = 0; i < ringSize - 1; i++) {
            MapLocation newl = templ.translate(i, 0);
            Direction dir = HQLoc.directionTo(newl);
            // skip cardinal directions
            if (inArray(cardinalDirections, dir, cardinalDirections.length) && HQLoc.add(dir).add(dir).equals(newl)) {
                continue;
            }
            if(rc.onTheMap(newl)) {
                if (rc.onTheMap(newl.add(dir))) {
                    supportWallLocs[index] = newl;
                    index++;
                } else if (rc.onTheMap(newl.add(dir.rotateLeft()))) {
                    supportWallLocs[index] = newl;
                    index++;
                } else if (rc.onTheMap(newl.add(dir.rotateRight()))) {
                    supportWallLocs[index] = newl;
                    index++;
                }
            }
        }

        supportWallLocsLength = index;
        tlog("SUPPORT_WALL_LOCS_LENGTH: " + supportWallLocsLength);
    }

}
