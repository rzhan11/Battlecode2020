package kryptonite;

import battlecode.common.*;

import static kryptonite.Communication.*;
import static kryptonite.Constants.*;
import static kryptonite.Debug.*;
import static kryptonite.Map.*;

public class Map extends Globals {

    /*
    Returns true if the location is within the map boundaries
    Returns false if not
    */
    public static boolean inMap(MapLocation ml) {
        return ml.x >= 0 && ml.x < mapWidth && ml.y >= 0 && ml.y < mapHeight;
    }

    /*
    Useful for ring structures
    */
    public static int maxXYDistance(MapLocation ml1, MapLocation ml2) {
        return Math.max(Math.abs(ml1.x - ml2.x), Math.abs(ml1.y - ml2.y));
    }

    /*
    Useful for ring structures
    */
    public static int manhattanDistance(MapLocation ml1, MapLocation ml2) {
        return Math.abs(ml1.x - ml2.x) + Math.abs(ml1.y - ml2.y);
    }

    public static boolean isDirDryFlatEmpty (Direction dir) throws GameActionException {
        MapLocation loc = rc.adjacentLocation(dir);
        return !rc.senseFlooding(loc) && rc.senseRobotAtLocation(loc) == null && Math.abs(rc.senseElevation(loc) - rc.senseElevation(here)) <= GameConstants.MAX_DIRT_DIFFERENCE;
    }

    public static boolean isDirEmpty (Direction dir) throws GameActionException {
        return rc.senseRobotAtLocation(rc.adjacentLocation(dir)) == null;
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
    Returns true if this direction's elevation is within +/-3 of our tile's elevation
    Returns false otherwise
    */
    public static boolean isDirFlat(Direction dir) throws GameActionException {
        return Math.abs(rc.senseElevation(rc.adjacentLocation(dir)) - myElevation) <= GameConstants.MAX_DIRT_DIFFERENCE;
    }

    /*
    Assumes that we can sense this tile
    Returns true if this tile's elevation is within +/-3 of our tile's elevation
    Returns false otherwise
    */
    public static boolean isLocFlat(MapLocation loc) throws GameActionException {
        return Math.abs(rc.senseElevation(loc) - myElevation) <= GameConstants.MAX_DIRT_DIFFERENCE;
    }

    /*
    Assumes that we can sense both tiles
    Returns true if the two tiles' elevation are within +/-3 of each other
    Returns false otherwise
    */
    public static boolean checkElevation(MapLocation loc1, MapLocation loc2) throws GameActionException {
        return Math.abs(rc.senseElevation(loc1) - rc.senseElevation(loc2)) <= GameConstants.MAX_DIRT_DIFFERENCE;
    }

    public static void updateIsDirMoveable() throws GameActionException {
        if (!myType.isBuilding()) {
            RobotInfo[] nearbyEnemies = null;
            if (myType == RobotType.MINER) {
                nearbyEnemies = rc.senseNearbyRobots(8, them);;
            }
            outer: for (int i = 0; i < directions.length; i++) {
                MapLocation adjLoc = rc.adjacentLocation(directions[i]);

                if (!inMap(adjLoc)) {
                    isDirMoveable[i] = false;
                    continue;
                }

                if (myType == RobotType.DELIVERY_DRONE) {
                    if (!isDirEmpty(directions[i])) {
                        isDirMoveable[i] = false;
                        continue;
                    }
                    // checks for dangerous netguns
                    // add check for if we are ignoring netguns
                    if (!BotDeliveryDrone.isDroneSwarming) {
                        for (RobotInfo ri : visibleEnemies) {
                            if (canShootType(ri.type)) {
                                if (adjLoc.distanceSquaredTo(ri.location) <= GameConstants.NET_GUN_SHOOT_RADIUS_SQUARED) {
                                    isDirMoveable[i] = false;
                                    continue outer;
                                }
                            }
                        }
                    }
                } else if (myType == RobotType.LANDSCAPER) {
                    if (!isDirDryFlatEmpty(directions[i])) {
                        isDirMoveable[i] = false;
                        continue;
                    }
                } else if (myType == RobotType.MINER) {
                    if (!isDirDryFlatEmpty(directions[i])) {
                        isDirMoveable[i] = false;
                        continue;
                    }
                    // checks for dangerous drones
                    for (RobotInfo ri : nearbyEnemies) {
                        if (ri.type == RobotType.DELIVERY_DRONE) {
                            if (ri.location.isAdjacentTo(adjLoc)) {
                                isDirMoveable[i] = false;
                                continue outer;
                            }
                        }
                    }
                } else {
                    tlogi("ERROR: Sanity check failed - unknown type");
                }

                isDirMoveable[i] = true;
            }
        }
    }
}
