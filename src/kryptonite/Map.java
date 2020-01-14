package kryptonite;

import battlecode.common.*;

public class Map extends Globals {
    public static boolean isDirDryFlatEmpty (Direction dir) throws GameActionException {
        MapLocation loc = rc.adjacentLocation(dir);
        return !rc.senseFlooding(loc) && rc.senseRobotAtLocation(loc) == null && Math.abs(rc.senseElevation(loc) - rc.senseElevation(here)) <= GameConstants.MAX_DIRT_DIFFERENCE;
    }

    public static boolean isDirEmpty (Direction dir) throws GameActionException {
        MapLocation loc = rc.adjacentLocation(dir);
        return rc.senseRobotAtLocation(loc) == null;
    }


    public static boolean isDirWetEmpty (Direction dir) throws GameActionException {
        MapLocation loc = rc.adjacentLocation(dir);
        return rc.senseFlooding(loc) && rc.senseRobotAtLocation(loc) == null;
    }

    /*
    Return 0 if occupied, 1 if empty
    Return -1 if cannot sense
     */
    public static int isLocEmpty (Direction dir) throws GameActionException {
        MapLocation loc = rc.adjacentLocation(dir);
        if (rc.canSenseLocation(loc)) {
            if (rc.senseRobotAtLocation(loc) == null) {
                return 0;
            } else {
                return 1;
            }
        } else {
            return -1;
        }
    }

    /*
    Assumes that we can sense this tile
    Returns true if this tile's elevation is within +/-3 of our tile's elevation
    Returns false otherwise
    */
    public static boolean checkElevation (MapLocation loc) throws GameActionException {
        return Math.abs(rc.senseElevation(loc) - myElevation) <= GameConstants.MAX_DIRT_DIFFERENCE;
    }

    /*
    Assumes that we can sense both tiles
    Returns true if the two tiles' elevation are within +/-3 of each other
    Returns false otherwise
    */
    public static boolean checkElevation (MapLocation loc1, MapLocation loc2) throws GameActionException {
        return Math.abs(rc.senseElevation(loc1) - rc.senseElevation(loc2)) <= GameConstants.MAX_DIRT_DIFFERENCE;
    }
}
