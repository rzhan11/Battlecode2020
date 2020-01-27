package rush_bot;

import battlecode.common.*;

import static rush_bot.Debug.*;
import static rush_bot.Map.*;
import static rush_bot.Utils.*;

public class Wall extends Globals {

    public static boolean hasLoadedWall = false;

    public static boolean startBuildInnerWall = false;
    public static MapLocation[] wallLocs;
    public static int wallLocsLength;
    public static MapLocation[] supportWallLocs;
    public static int supportWallLocsLength;
    public static MapLocation[] digLocs2x2 = new MapLocation[4];

    public static boolean initialWallSetup;
    public static boolean wallFull;
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
        wallLocs = new MapLocation[8];

        int index = 0;
        for (Direction dir: directions) {
            MapLocation loc = HQLoc.add(dir);
            if (rc.onTheMap(loc.add(dir))) {
                wallLocs[index] = loc;
                index++;
            } else if (rc.onTheMap(loc.add(dir.rotateLeft()))) {
                wallLocs[index] = loc;
                index++;
            } else if (rc.onTheMap(loc.add(dir.rotateRight()))) {
                wallLocs[index] = loc;
                index++;
            }
        }
        wallLocsLength = index;
        tlog("WALL_LOCS_LENGTH: " + wallLocsLength);


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

        log("FINISHED LOADING WALL INFORMATION");

        hasLoadedWall = true;
    }

    // minimum height, subject to change based on conditions
    public static int PLATFORM_ELEVATION = 10;

    public static void loadPlatformInfo() throws GameActionException {

        int ringRadius = 4;
        int ringSize = 2 * ringRadius + 1;
        MapLocation[] possibleLocs = new MapLocation[9 * ringRadius];
        int index = 0;
        MapLocation templ = HQLoc.translate(ringRadius, ringRadius);
        for(int i = 0; i < ringSize; i++) {
            MapLocation newl = templ.translate(0, -i);
            possibleLocs[index] = newl;
            index++;
        }
        templ = HQLoc.translate(ringRadius, -ringRadius - 1);
        for(int i = 0; i < ringSize; i++) {
            MapLocation newl = templ.translate(-i, 0);
            possibleLocs[index] = newl;
            index++;
        }
        templ = HQLoc.translate(-ringRadius - 1, -ringRadius - 1);
        for(int i = 0; i < ringSize; i++) {
            MapLocation newl = templ.translate(0, i);
            possibleLocs[index] = newl;
            index++;
        }
        templ = HQLoc.translate(-ringRadius - 1, ringRadius);
        for(int i = 0; i < ringSize; i++) {
            MapLocation newl = templ.translate(i, 0);
            possibleLocs[index] = newl;
            index++;
        }

        // finding optimal platform locations
        int bestMaxElevation = -1;
        int minChange = P_INF;
        MapLocation bestLoc = null;
        outer: for (MapLocation initLoc: possibleLocs) {
            MapLocation[] arr = {initLoc, initLoc.translate(1,0), initLoc.translate(0, 1), initLoc.translate(1,1)};
            int maxElevation = PLATFORM_ELEVATION;
            for (MapLocation loc: arr) {
                if (!rc.onTheMap(loc)) {
                    continue outer;
                }
                // ignore not fully visible locs
                if (!rc.canSenseLocation(loc)) {
                    continue outer;
                }
                maxElevation = Math.max(maxElevation, rc.senseElevation(loc));
            }
            int change = 0;
            for (MapLocation loc: arr) {
                change += Math.abs(maxElevation - rc.senseElevation(loc));
            }
            if(change < minChange) {
                bestMaxElevation = maxElevation;
                minChange = change;
                bestLoc = initLoc;
            }
        }

        if(bestLoc == null) {
            log("ERROR: Sanity check failed - No platform location found");
        } else {
            log("PLATFORM LOCATION: " + bestLoc);
            PLATFORM_ELEVATION = bestMaxElevation;
            platformCornerLoc = bestLoc;

            platformLocs = new MapLocation[4];
            platformLocs[0] = platformCornerLoc;
            platformLocs[1] = platformCornerLoc.translate(1,0);
            platformLocs[2] = platformCornerLoc.translate(1,1);
            platformLocs[3] = platformCornerLoc.translate(0,1);
        }
    }

    public static MapLocation[] droneWallLocs;
    public static int droneWallLocsLength;

    public static void loadDroneWallInfo () {
        int ringRadius = 3;
        int ringSize = 2 * ringRadius + 1;

        droneWallLocs = new MapLocation[8 *ringRadius];

        // move right along right wall
        int index = 0;
        MapLocation templ = HQLoc.translate(ringRadius, ringRadius);
        for(int i = 0; i < ringSize - 1; i++) {
            MapLocation newl = templ.translate(0, -i);
            if(rc.onTheMap(newl)) {
                for (Direction dir: directions) {
                    MapLocation loc = newl.add(dir);
                    if (!rc.onTheMap(loc)) {
                        continue;
                    }
                    if (maxXYDistance(HQLoc, loc) == ringRadius + 1) {
                        droneWallLocs[index] = newl;
                        index++;
                        break;
                    }
                }
            }
        }

        // move right along bottom wall
        templ = HQLoc.translate(ringRadius, -ringRadius);
        for(int i = 0; i < ringSize - 1; i++) {
            MapLocation newl = templ.translate(-i, 0);
            if(rc.onTheMap(newl)) {
                for (Direction dir: directions) {
                    MapLocation loc = newl.add(dir);
                    if (!rc.onTheMap(loc)) {
                        continue;
                    }
                    if (maxXYDistance(HQLoc, loc) == ringRadius + 1) {
                        droneWallLocs[index] = newl;
                        index++;
                        break;
                    }
                }
            }
        }

        // move right along left wall
        templ = HQLoc.translate(-ringRadius, -ringRadius);
        for(int i = 0; i < ringSize - 1; i++) {
            MapLocation newl = templ.translate(0, i);
            if(rc.onTheMap(newl)) {
                for (Direction dir: directions) {
                    MapLocation loc = newl.add(dir);
                    if (!rc.onTheMap(loc)) {
                        continue;
                    }
                    if (maxXYDistance(HQLoc, loc) == ringRadius + 1) {
                        droneWallLocs[index] = newl;
                        index++;
                        break;
                    }
                }
            }
        }

        // move right along top wall
        templ = HQLoc.translate(-ringRadius, ringRadius);
        for(int i = 0; i < ringSize - 1; i++) {
            MapLocation newl = templ.translate(i, 0);
            if(rc.onTheMap(newl)) {
                for (Direction dir: directions) {
                    MapLocation loc = newl.add(dir);
                    if (!rc.onTheMap(loc)) {
                        continue;
                    }
                    if (maxXYDistance(HQLoc, loc) == ringRadius + 1) {
                        droneWallLocs[index] = newl;
                        index++;
                        break;
                    }
                }
            }
        }

        droneWallLocsLength = index;
        tlog("DRONE_WALL_LOCS_LENGTH: " + droneWallLocsLength);
    }

    public static boolean checkInitialWallSetup () throws GameActionException {
        for (int i = 0; i < wallLocsLength; i++) {
            MapLocation adjLoc = wallLocs[i];
            boolean isAdjToAllyLandscaper = false;
            for (Direction dir: allDirections) {
                MapLocation loc = adjLoc.add(dir);
                if (!rc.onTheMap(loc)) {
                    continue;
                }
                if (loc.isAdjacentTo(HQLoc)) {
                    if (rc.canSenseLocation(loc) && isLocAllyLandscaper(loc)) {
                        isAdjToAllyLandscaper = true;
                        break;
                    }
                }
            }
            if (!isAdjToAllyLandscaper) {
                log("no adjloc " + adjLoc);
                return false;
            }
        }
        return true;
    }
}
