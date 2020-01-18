package kryptonite;

import battlecode.common.*;

import static kryptonite.Debug.*;

public class Zones extends Globals {

    final public static int MAX_NUM_ZONES = 16;

    public static int zoneSize = 4;
    public static int numLocsinZone;
    public static int numXZones;
    public static int numYZones;

    // holds if the zone has been fully explored, cannot be fully explored, heavily polluted, etc
    // 0 is unexplored
    // 1 is explored
//    public static int[][] orderedZones = null;
    public static int[][] zoneStatus = null;

    // which locations in soupzones have soup (uses indices)
    public static int[] newSoupStatusIndices = null;
    public static int[] newSoupStatuses = null;
    public static int newSoupStatusesLength = 0;

    // 0 is unknown, 1 is confirmed soup, -1 is denied soup
    public static int[][] hasSoupLocs = null;
    // 0 is unknown, 1 is confirmed soup, -1 is denied soup
    public static int[][] hasSoupZones = null;
    // number of tiles in this zone that contains soup
    public static int[][] numSoupLocsInZones = null;
    public static int[][] numNoSoupLocsInZones = null;

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

        newSoupStatusIndices = new int[senseDirections.length];
        newSoupStatuses = new int[senseDirections.length];

        hasSoupLocs = new int[mapWidth][mapHeight];
        hasSoupZones = new int[numXZones][numYZones];
        numSoupLocsInZones = new int[numXZones][numYZones];
        numNoSoupLocsInZones = new int[numXZones][numYZones];

        log("FINISHED LOADING ZONE INFORMATION");

        hasLoadedZones = true;

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
    Returns the xZone and yZone of this MapLocation
     */
    public static int[] locToZonePair(MapLocation loc) throws GameActionException {
        return new int[] {loc.x / zoneSize, loc.y / zoneSize};
    }

    /*
    Returns the xZone and yZone of this MapLocation
     */
    public static int zonePairToIndex(int[] pair) throws GameActionException {
        return pair[0] * MAX_NUM_ZONES + pair[1];
    }

    /*
    Returns the xZone and yZone of this MapLocation
     */
    public static int zonePairToIndex(int x, int y) throws GameActionException {
        return x * MAX_NUM_ZONES + y;
    }

    /*
    Returns the xZone and yZone of this MapLocation
     */
    public static int[] zoneIndexToPair(int index) throws GameActionException {
        return new int[] {index / MAX_NUM_ZONES,  index % MAX_NUM_ZONES};
    }

    /*
    Returns the hasSoupZone status of the given MapLocation's zone
     */
    public static int getHasSoupZonesOfLoc(MapLocation loc) throws GameActionException {
        int[] zone = locToZonePair(loc);
        return hasSoupZones[zone[0]][zone[1]];
    }

    /*
    Returns the zone status of the given MapLocation's zone
     */
    public static int getZoneStatusOfLoc(MapLocation loc) throws GameActionException {
        int[] zone = locToZonePair(loc);
        return zoneStatus[zone[0]][zone[1]];
    }

    public static boolean canSenseEntireCurrentZone () throws GameActionException {
        int mx = here.x % zoneSize;
        int my = here.y % zoneSize;
        mx = Math.max(mx, zoneSize - 1 - mx);
        my = Math.max(my, zoneSize - 1 - my);
        return actualSensorRadiusSquared >= mx * mx + my * my;
    }

    /*
    Updates based on vision
    status = -1 means no soup
    status = 1 means soup
     */
    public static void updateKnownSoupLocs(MapLocation loc, int status) throws GameActionException {

        if (!hasLoadedZones) {
            loadZoneInformation();
        }

        int[] zone = locToZonePair(loc);

        if (status == -1) {
            // no soup confirmed
            if (hasSoupLocs[loc.x][loc.y] != -1) {
                if (hasSoupLocs[loc.x][loc.y] == 0) {
                    // this tile was previously unknown
                    hasSoupLocs[loc.x][loc.y] = -1;
                    numNoSoupLocsInZones[zone[0]][zone[1]]++;
                } else if (hasSoupLocs[loc.x][loc.y] == 1) {
                    // this tile previously had soup, but now is empty
                    hasSoupLocs[loc.x][loc.y] = -1;
                    numNoSoupLocsInZones[zone[0]][zone[1]]++;
                    numSoupLocsInZones[zone[0]][zone[1]]--;
                }

                if (numNoSoupLocsInZones[zone[0]][zone[1]] == numLocsinZone) {
                    // zone is completely devoid of soup
                    if (hasSoupZones[zone[0]][zone[1]] != -1) {
                        // this information is new and worth communicating
                        newSoupStatusIndices[newSoupStatusesLength] = zonePairToIndex(zone);
                        newSoupStatuses[newSoupStatusesLength] = -1;
                        newSoupStatusesLength++;
                    }
                }
            }
        } else if (status == 1) {
            // tile has soup confirmed
            if (hasSoupLocs[loc.x][loc.y] == 1) {
                if (hasSoupLocs[loc.x][loc.y] == -1) {
                    // this tile was previously confirmed to not have soup
                    logi("ERROR: Sanity check failed - tile without soup now has soup");
                } else if (hasSoupLocs[loc.x][loc.y] == 0) {
                    // this tile was previously unknown

                    if (hasSoupZones[zone[0]][zone[1]] != 1) {
                            // this information is new and worth communicating
                            newSoupStatuses[newSoupStatusesLength] = 1;
                            newSoupStatusesLength++;
                    }

                    numSoupLocsInZones[zone[0]][zone[1]]++;
                    hasSoupLocs[loc.x][loc.y] = 1;
                }

            }
        } else {
            log ("WARNING: Weird status in updateKnownSoupLocs: " + status);
        }
    }

    public static void updateKnownSoupZones(int index, int status, boolean fromVision) throws GameActionException {

        if (!hasLoadedZones) {
            loadZoneInformation();
        }

        int[] zone = zoneIndexToPair(index);

        if (status == -1) {
            if (hasSoupZones[zone[0]][zone[1]] != 1) {
                hasSoupZones[zone[0]][zone[1]] = -1;
            }
        } else if (status == 1) {
            // ignore if we have confirmed no soup or if we already know that there is soup in this zone
            if (hasSoupZones[zone[0]][zone[1]] == 0) {
                hasSoupZones[zone[0]][zone[1]] = 1;
            }
        } else {
            log ("WARNING: Weird status in updateKnownSoupZones: " + status);
        }
    }
}
