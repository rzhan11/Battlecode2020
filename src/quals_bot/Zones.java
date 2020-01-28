package quals_bot;

import battlecode.common.*;

import static quals_bot.Actions.*;
import static quals_bot.Communication.*;
import static quals_bot.Debug.*;
import static quals_bot.Globals.*;
import static quals_bot.Map.*;
import static quals_bot.Nav.*;
import static quals_bot.Utils.*;
import static quals_bot.Wall.*;
import static quals_bot.Zones.*;

public class Zones extends Globals {

    final public static int MAX_NUM_ZONES = 16;
    final public static int VISIBLE_SOUP_LOCS_LIMIT = 10;

    public static int zoneSize = 4;
    public static int numLocsinZone;
    public static int numXZones;
    public static int numYZones;

    public static MapLocation[] soupClusters = new MapLocation[BIG_ARRAY_SIZE];
    public static boolean[] emptySoupClusters = new boolean[BIG_ARRAY_SIZE];
    public static int soupClustersLength = 0;
    private static int soupClusterIndex = -1;

    // holds if the zone has been fully explored, cannot be fully explored, heavily polluted, etc
    // 0 is unexplored
    // 1 is explored
    public static int[][] exploredZoneStatus = null;

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
            log("EARLY END");
            Clock.yield();
            Globals.updateBasic();
        }

        log("LOADING ZONE INFORMATION 1");

        numLocsinZone = zoneSize * zoneSize;
        numXZones = (mapWidth + zoneSize - 1) / zoneSize;
        numYZones = (mapHeight + zoneSize - 1) / zoneSize;

        exploredZoneStatus = new int[numXZones][numYZones];

        log("FINISHED LOADING ZONE INFORMATION");

        hasLoadedZones = true;

        if (myType != RobotType.HQ) {
            log("EARLY END");
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
    public static MapLocation findClosestUnexploredZone () throws GameActionException {
//		int startByte = Clock.getBytecodesLeft();
        int range = 3;
        int x_lower = Math.max(0, myZone[0] - range);
        int x_upper = Math.min(numXZones - 1, myZone[0] + range);
        int y_lower = Math.max(0, myZone[1] - range);
        int y_upper = Math.min(numYZones - 1, myZone[1] + range);

        int closestUnexploredDist = P_INF;
        MapLocation closestUnexploredLoc = null;
        for (int x = x_lower; x <= x_upper; x++) {
            for (int y = y_lower; y <= y_upper; y++) {
                MapLocation targetLoc = new MapLocation(x * zoneSize + zoneSize / 2, y * zoneSize + zoneSize / 2);
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
//		tlog("FIND CLOSEST UNEXPLORED ZONE BYTES: " + (startByte - Clock.getBytecodesLeft()));
        return closestUnexploredLoc;
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

    public static int findClosestSoupCluster () throws GameActionException {

        int closestDistance = P_INF;
        int closestIndex = -1;
        log("len " + soupClustersLength);
        for (int i = 0; i < soupClustersLength; i++) {
            if (!emptySoupClusters[i]) {
                log("soup i " + i + " " + soupClusters[i]);
                int dist = here.distanceSquaredTo(soupClusters[i]);
                if (dist < closestDistance) {
                    closestDistance = dist;
                    closestIndex = i;
                }

            }
        }
        return closestIndex;
    }
}
