package kryptonite;

import battlecode.common.*;

import static kryptonite.Constants.*;
import static kryptonite.Debug.*;

public class Zones extends Globals {

    public static int zoneSize = 4;
    public static int numLocsinZone;
    public static int numXZones;
    public static int numYZones;

    // holds if the zone has been fully explored, cannot be fully explored, heavily polluted, etc
    // 0 is unexplored
    // 1 is explored
//    public static int[][] orderedZones = null;
    public static int[][] zoneStatus = null;


    public static int[] soupLevels = {-1, 0, 10, 20, 40, 80, 160, 320, 640, 1280, 2560, 5120, 10240};

    // which locations in soupzones have soup (uses indices)
    public static MapLocation[] newSoupLocs = null;
    public static int newSoupLocsLength = 0;

    public static int[][][] soupZonesLocs = null;
    public static int[][] soupZonesLocsLength = null;

    // how much soup at a given soup location
    public static int[][][] soupZonesAmount = null;

    public static boolean hasLoadedZones = false;

    /*
    Loads zone information
    Takes 2 turns
     */

    public static void loadZoneInformation () throws GameActionException {

        if (hasLoadedZones) {
            return;
        }

        log("LOADING ZONE INFORMATION 0");

        Globals.endTurn(true);
        Globals.update();

        log("LOADING ZONE INFORMATION 1");

        numLocsinZone = zoneSize * zoneSize;
        numXZones = (mapWidth + zoneSize - 1) / zoneSize;
        numYZones = (mapHeight + zoneSize - 1) / zoneSize;

//        orderedZones = HardCode.getZoneLocations();
        zoneStatus = new int[numXZones][numYZones];

        soupZonesLocs = new int[numXZones][numYZones][numLocsinZone];
        soupZonesLocsLength = new int[numXZones][numYZones];
        newSoupLocs = new MapLocation[senseDirections.length];

        Globals.endTurn(true);
        Globals.update();

        log("LOADING ZONE INFORMATION 2");

        soupZonesAmount = new int[numXZones][numYZones][numLocsinZone];

        hasLoadedZones = true;

        log("FINISHED LOADING ZONE INFORMATION");

        Globals.endTurn(true);
        Globals.update();
    }

    /*
    Returns which index of the zone this MapLocation is in
     */
    public static MapLocation indexToLoc (int xZone, int yZone, int index) throws GameActionException {
        return new MapLocation(xZone * zoneSize + index / zoneSize, yZone * zoneSize + index % zoneSize);
    }

    /*
    Returns which index of the zone this MapLocation is in
     */
    public static int zoneLocIndex (MapLocation loc) throws GameActionException {
        return (loc.x % zoneSize) * zoneSize + loc.y % zoneSize;
    }

    /*
    Returns the xZone and yZone of this MapLocation
     */
    public static int[] zoneIndex(MapLocation loc) throws GameActionException {
        return new int[] {loc.x / zoneSize, loc.y / zoneSize};
    }

    public static void updateSoupZones(MapLocation loc, int soupLevel, boolean fromVision) throws GameActionException {

        if (!hasLoadedZones) {
            loadZoneInformation();
        }

        int[] zone = zoneIndex(loc);
        int zoneLoc = zoneLocIndex(loc);
        int[] locs = soupZonesLocs[zone[0]][zone[1]];
        int locsLength = soupZonesLocsLength[zone[0]][zone[1]];

        int origAmount = soupZonesAmount[zone[0]][zone[1]][zoneLoc];
        if (origAmount == 0 || soupLevel < origAmount) {
            soupZonesAmount[zone[0]][zone[1]][zoneLoc] = soupLevel;
            // if this deposit became empty, signal
            if (fromVision && soupLevel == soupToIndex(0)) {
                newSoupLocs[newSoupLocsLength] = loc;
                newSoupLocsLength++;
            }
            if (inArray(locs, zoneLoc, locsLength)) {
                tlog("Updated soupLoc at " + loc);
                drawLine(here, loc, CYAN);
            } else {
                tlog("Found newSoupLoc at " + loc);
                drawLine(here, loc, WHITE);
                soupZonesLocs[zone[0]][zone[1]][locsLength] = zoneLoc;
                soupZonesLocsLength[zone[0]][zone[1]]++;
            }
        }
    }

    public static int soupToIndex (int soup) throws GameActionException {
        for (int i = 0; i < soupLevels.length; i++) {
            if (soup <= soupLevels[i]) {
                return i;
            }
        }
        return soupLevels.length;
    }
}
