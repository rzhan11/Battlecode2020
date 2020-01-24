package kryptonite;

import battlecode.common.*;


import static kryptonite.Communication.*;
import static kryptonite.Debug.*;
import static kryptonite.Map.*;
import static kryptonite.Nav.*;
import static kryptonite.Utils.*;
import static kryptonite.Zones.*;

public class Zones extends Globals {

    final public static int MAX_NUM_ZONES = 16;
    final public static int VISIBLE_SOUP_LOCS_LIMIT = 10;

    public static int zoneSize = 4;
    public static int numLocsinZone;
    public static int numXZones;
    public static int numYZones;

    // holds if the zone has been fully explored, cannot be fully explored, heavily polluted, etc
    // 0 is unexplored
    // 1 is explored
//    public static int[][] orderedZones = null;
    public static int[][] exploredZoneStatus = null;

    // which locations in soupzones have soup (uses indices)
    public static int[] newSoupStatusIndices = null;
    public static int[] newSoupStatuses = null;
    public static int newSoupStatusesLength = 0;

    // 0 is unknown, 1 is confirmed soup, 2 is denied soup
    public static int[][] hasSoupLocs = null;
    // 0 is unknown, 1 is confirmed soup, 2 is denied soup
    public static int[][] hasSoupZones = null;
    // number of tiles in this zone that contains soup
    public static int[][] numSoupLocsInZones = null;
    public static int[][] numNoSoupLocsInZones = null;

    public static MapLocation floodingMemory;

    public static boolean hasLoadedZones = false;

    /*
    Loads zone information
    Takes 2 turns
     */

    public static void loadZoneInfo() throws GameActionException {

        if (hasLoadedZones) {
            return;
        }

        log("LOADING ZONE INFORMATION 0");

        if (myType != RobotType.HQ) {
            Clock.yield();
            Globals.updateBasic();
        }

        log("LOADING ZONE INFORMATION 1");

        numLocsinZone = zoneSize * zoneSize;
        numXZones = (mapWidth + zoneSize - 1) / zoneSize;
        numYZones = (mapHeight + zoneSize - 1) / zoneSize;

//        orderedZones = HardCode.getZoneLocations();
        exploredZoneStatus = new int[numXZones][numYZones];

        newSoupStatusIndices = new int[senseDirections.length];
        newSoupStatuses = new int[senseDirections.length];

        hasSoupLocs = new int[mapWidth][mapHeight];
        hasSoupZones = new int[numXZones][numYZones];
        numSoupLocsInZones = new int[numXZones][numYZones];
        numNoSoupLocsInZones = new int[numXZones][numYZones];

        log("FINISHED LOADING ZONE INFORMATION");

        hasLoadedZones = true;

        if (myType != RobotType.HQ) {
            Clock.yield();
            Globals.updateBasic();
        }
    }

    public static int getExactNumLocsInZone (int[] zone) throws GameActionException {
        int x, y;
        if (zone[0] == numXZones - 1) {
            x = mapWidth % zoneSize;
            if (x == 0) {
                x = zoneSize;
            }
        } else {
            x = zoneSize;
        }
        if (zone[1] == numYZones - 1) {
            y = mapHeight % zoneSize;
            if (y == 0) {
                y = zoneSize;
            }
        } else {
            y = zoneSize;
        }
        return x * y;
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
    public static int getHasSoupZonesAtLoc(MapLocation loc) throws GameActionException {
        int[] zone = locToZonePair(loc);
        return hasSoupZones[zone[0]][zone[1]];
    }

    /*
    Returns the zone status of the given MapLocation's zone
     */
    public static int getExploredZoneStatusAtLoc(MapLocation loc) throws GameActionException {
        int[] zone = locToZonePair(loc);
        return exploredZoneStatus[zone[0]][zone[1]];
    }

    public static boolean canSenseEntireCurrentZone () throws GameActionException {
        int mx = here.x % zoneSize;
        int my = here.y % zoneSize;
        mx = Math.max(mx, zoneSize - 1 - mx);
        my = Math.max(my, zoneSize - 1 - my);
        return actualSensorRadiusSquared >= mx * mx + my * my;
    }


    /*
    Checks if the current zone we are in is unexplored
     */
    public static void checkZone() throws GameActionException {
        if (exploredZoneStatus[myZone[0]][myZone[1]] == 0) {
            if (canSenseEntireCurrentZone()) {
                exploredZoneStatus[myZone[0]][myZone[1]] = 1;
                writeTransactionExploredZoneStatus(myZone[0], myZone[1], exploredZoneStatus[myZone[0]][myZone[1]]);
            }
        }
    }

    public static MapLocation getRandomUnexploredZone () throws GameActionException {
        for (int i = 0; i < 5; i++) {
            int[] zone = new int[] {rand.nextInt(numXZones), rand.nextInt(numYZones)};
            if (exploredZoneStatus[zone[0]][zone[1]] == 0) {
                return new MapLocation(zone[0] * zoneSize, zone[1] * zoneSize);
            }
        }
        log("Cannot find random unexplored zone, going to symmetry location zone");
        return getSymmetryLoc();
    }

    /*
    Based on hasSoupZones, find the closest soup zone that has been confirmed to contain soup
    Also finds the closest unexplored zone
    Returns a size two array that contains these two zones
     */
    public static MapLocation[] findClosestSoupAndUnexploredZone () throws GameActionException {
//		int startByte = Clock.getBytecodesLeft();
        int range = 3;
        int x_lower = Math.max(0, myZone[0] - range);
        int x_upper = Math.min(numXZones - 1, myZone[0] + range);
        int y_lower = Math.max(0, myZone[1] - range);
        int y_upper = Math.min(numYZones - 1, myZone[1] + range);

        int closestSoupDist = P_INF;
        MapLocation closestSoupLoc = null;
        int closestUnexploredDist = P_INF;
        MapLocation closestUnexploredLoc = null;
        for (int x = x_lower; x <= x_upper; x++) {
            for (int y = y_lower; y <= y_upper; y++) {
                MapLocation targetLoc = new MapLocation(x * zoneSize + zoneSize / 2, y * zoneSize + zoneSize / 2);
                if (hasSoupZones[x][y] == 1) {
                    int dist = here.distanceSquaredTo(targetLoc);
                    if (dist < closestSoupDist) {
                        closestSoupDist = dist;
                        closestSoupLoc = targetLoc;
                    }
                }
                if (exploredZoneStatus[x][y] == 0) {
                    int dist = here.distanceSquaredTo(targetLoc);
                    if (dist < closestUnexploredDist) {
                        closestUnexploredDist = dist;
                        closestUnexploredLoc = targetLoc;
                    }
                }
            }
        }
        if (closestUnexploredLoc == null) {
            log("Cannot find nearby unexplored zone, going to symmetry location zone");
            closestUnexploredLoc = getSymmetryLoc();
        }
//		tlog("FIND CLOSEST SOUP ZONE BYTES: " + (startByte - Clock.getBytecodesLeft()));
        return new MapLocation[] {closestSoupLoc, closestUnexploredLoc};
    }

    public static void locateSoup () throws GameActionException {
		/*
		Updates soup locations of this zone
		Only updates if this zone has not been denied of soup
		 */
        int x_lower = myZone[0] * zoneSize;
        int x_upper = Math.min(mapWidth, x_lower + zoneSize);
        int y_lower = myZone[1] * zoneSize;
        int y_upper = Math.min(mapHeight, y_lower + zoneSize);
        for (int x = x_lower; x < x_upper; x++) {
            for (int y = y_lower; y < y_upper; y++) {
                MapLocation loc = new MapLocation(x, y);
                if (rc.canSenseLocation(loc)) {
                    if (rc.senseSoup(loc) > 0) {
                        if (isLocDry(loc) || isAdjLocDry(loc)) {
                            updateKnownSoupLocs(loc, 1);
                        } else {
                            updateKnownSoupLocs(loc, 2);
                        }
                    } else {
                        updateKnownSoupLocs(loc, 2);
                    }
                }
            }
        }

        //
        if (numNoSoupLocsInZones[myZone[0]][myZone[1]] == getExactNumLocsInZone(myZone)) {
            // zone is completely devoid of soup
            if (hasSoupZones[myZone[0]][myZone[1]] != 2) {
                // this information is new and worth communicating
                hasSoupZones[myZone[0]][myZone[1]] = 2;
                newSoupStatusIndices[newSoupStatusesLength] = zonePairToIndex(myZone);
                newSoupStatuses[newSoupStatusesLength] = 2;
                newSoupStatusesLength++;
            }
        }

		/*
		Updates the known soup locations based on what we can sense
		 */

		if (visibleSoupLocs.length <= VISIBLE_SOUP_LOCS_LIMIT) {
            for (MapLocation loc: visibleSoupLocs) {
                if (isLocDry(loc) || isAdjLocDry(loc)) {
                    updateKnownSoupLocs(loc, 1);
                }
            }
        } else {
            int startByte = Clock.getBytecodesLeft();
            for (int i = 0; i < visibleSoupLocs.length; i++) {
                if (startByte - Clock.getBytecodesLeft() > 2000) {
                    log("Checked " + i + " random soup locations");
                    break;
                }
                MapLocation loc = visibleSoupLocs[i];
                if (isLocDry(loc) || isAdjLocDry(loc)) {
                    updateKnownSoupLocs(loc, 1);
                }
            }

        }

        if (newSoupStatusesLength > 0) {
            writeTransactionSoupZoneStatus(newSoupStatusIndices, newSoupStatuses, 0, newSoupStatusesLength);
        }
    }

    /*
    Based on hasSoupZones, find the closest soup zone that has been confirmed to contain soup

     */
    public static MapLocation findClosestVisibleSoupLoc (boolean ignoreSelf) throws GameActionException {
        
        if (visibleSoupLocs.length >= VISIBLE_SOUP_LOCS_LIMIT) {
            log("Approximating closest visible soup location");
            int[] radiusOrder = {2, 4, 9, 16, 25, 36, 49, 64};
            MapLocation[] closeSoupLocs;
            for (int radius: radiusOrder) {
                closeSoupLocs = rc.senseNearbySoup(radius);
                for (MapLocation loc: closeSoupLocs) {
                    if (ignoreSelf && loc.equals(here)) {
                        continue;
                    }
                    if (isLocDry(loc) || isAdjLocDry(loc)) {
                        return loc;
                    }
                }
            }
            logi("ERROR: Sanity check failed - Number of soup locations greater than limit but could not approximate the closest soup location");
            return null;
        }

        MapLocation closestLoc = null;
        int closestDist = P_INF;
        for (MapLocation loc: visibleSoupLocs) {
            if (ignoreSelf && loc.equals(here)) {
                continue;
            }
            int dist = here.distanceSquaredTo(loc);
            if (dist < closestDist) {
                if (isLocDry(loc) || isAdjLocDry(loc)) {
                    closestDist = dist;
                    closestLoc = loc;
                }
            }
        }
        return closestLoc;
    }

    /*
    Updates based on vision
    status = 2 means no soup
    status = 1 means soup
     */
    public static void updateKnownSoupLocs(MapLocation loc, int status) throws GameActionException {

        int[] zone = locToZonePair(loc);

        if (status == 2) {
            // no soup confirmed
            if (hasSoupLocs[loc.x][loc.y] != 2) {
                if (hasSoupLocs[loc.x][loc.y] == 0) {
                    // this tile was previously unknown
                    hasSoupLocs[loc.x][loc.y] = 2;
                    numNoSoupLocsInZones[zone[0]][zone[1]]++;
                } else if (hasSoupLocs[loc.x][loc.y] == 1) {
                    // this tile previously had soup, but now is empty
                    hasSoupLocs[loc.x][loc.y] = 2;
                    numNoSoupLocsInZones[zone[0]][zone[1]]++;
                    numSoupLocsInZones[zone[0]][zone[1]]--;
                }

                if (numNoSoupLocsInZones[zone[0]][zone[1]] == getExactNumLocsInZone(zone)) {
                    // zone is completely devoid of soup
                    if (hasSoupZones[zone[0]][zone[1]] != 2) {
                        // this information is new and worth communicating
                        hasSoupZones[zone[0]][zone[1]] = 2;
                        newSoupStatusIndices[newSoupStatusesLength] = zonePairToIndex(zone);
                        newSoupStatuses[newSoupStatusesLength] = 2;
                        newSoupStatusesLength++;
                    }
                }
            }
        } else if (status == 1) {
            // tile has soup confirmed
            if (hasSoupLocs[loc.x][loc.y] != 1) {
                if (hasSoupLocs[loc.x][loc.y] == 2) {
                    // this tile was previously confirmed to not have soup
                    hasSoupZones[zone[0]][zone[1]] = 1;
                    newSoupStatusIndices[newSoupStatusesLength] = zonePairToIndex(zone);
                    newSoupStatuses[newSoupStatusesLength] = 1;
                    newSoupStatusesLength++;
                    logi("WARNING: Sanity check failed - tile without soup now has soup");
                } else if (hasSoupLocs[loc.x][loc.y] == 0) {
                    // this tile was previously unknown

                    if (hasSoupZones[zone[0]][zone[1]] == 0) {
                        // the soup info about this zone was previously unknown
                        hasSoupZones[zone[0]][zone[1]] = 1;
                        newSoupStatusIndices[newSoupStatusesLength] = zonePairToIndex(zone);
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

        int[] zone = zoneIndexToPair(index);

        if (status == 2) {
            if (hasSoupZones[zone[0]][zone[1]] != 2) {
                hasSoupZones[zone[0]][zone[1]] = 2;
                if (fromVision) {
                    newSoupStatusIndices[newSoupStatusesLength] = zonePairToIndex(zone);
                    newSoupStatuses[newSoupStatusesLength] = 2;
                    newSoupStatusesLength++;
                }
            }
        } else if (status == 1) {
            // ignore if we have confirmed no soup or if we already know that there is soup in this zone
            if (hasSoupZones[zone[0]][zone[1]] != 1) {
                hasSoupZones[zone[0]][zone[1]] = 1;
                if (fromVision) {
                    newSoupStatusIndices[newSoupStatusesLength] = zonePairToIndex(zone);
                    newSoupStatuses[newSoupStatusesLength] = 1;
                    newSoupStatusesLength++;
                }
            }
        } else {
            log ("WARNING: Weird status in updateKnownSoupZones: " + status);
        }
    }

    /*
    If we do not already know a visible flooded tile
        Checks visible tiles for flooding
        Saves the flooded tile to memory
     */
    public static void locateFlooding () throws GameActionException {
        // checks if floodingMemory still exists
        if (floodingMemory != null && rc.canSenseLocation(floodingMemory)) {
            if (rc.senseFlooding(floodingMemory)) {
                log("Confirmed that floodingMemory at " + floodingMemory + " is flooded");
                return;
            } else {
                log("Resetting floodingMemory at " + floodingMemory + " since it is dry");
                floodingMemory = null;
            }
        }

        // runs if floodingMemory is not visible or is null
        // searches for a flooded tile that is empty
        for (int[] dir: senseDirections) {
            if (actualSensorRadiusSquared < dir[2]) {
                break;
            }
            if (myType != RobotType.DELIVERY_DRONE && dir[2] > 8) {
                break;
            }
            MapLocation loc = here.translate(dir[0], dir[1]);
            if (rc.onTheMap(loc) && rc.senseFlooding(loc) && rc.senseRobotAtLocation(loc) == null) {
                // floodingMemory[loc.x][loc.y] = rc.senseFlooding(loc);

                log("Found visible flooded tile at " + loc);

                // if floodingMemory is null, write a Transaction
                if (floodingMemory == null) {
                    writeTransactionFloodingFound(loc);
                }

                floodingMemory = loc;
                return;
            }
        }
    }
}
