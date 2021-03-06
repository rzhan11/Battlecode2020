package seeding;

import battlecode.common.*;

public class Map extends Globals {

    public static boolean isDigLoc(MapLocation ml) {
        return  Math.abs(ml.x - HQLoc.x) % 2 == 0 &&  Math.abs(ml.y - HQLoc.y) % 2 == 0;
    }

    public static boolean isBuildLocation(MapLocation ml) {
        return Math.abs(ml.x - HQLoc.x) % 2 == 1 &&  Math.abs(ml.y - HQLoc.y) % 2 == 1;
    }

    /*
    Reflects ml1 over ml2
     */
    public static MapLocation reflect(MapLocation ml1, MapLocation ml2) {
        return new MapLocation(2 * ml2.x - ml1.x, 2 * ml2.y - ml1.y);
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

    public static boolean isDirEmpty (Direction dir) throws GameActionException {
        return rc.senseRobotAtLocation(rc.adjacentLocation(dir)) == null;
    }

    /*
    Returns true if this direction's elevation is within +/-3 of our tile's elevation
    Returns false otherwise
    */
    public static boolean isDirFlat(Direction dir) throws GameActionException {
        return Math.abs(rc.senseElevation(rc.adjacentLocation(dir)) - myElevation) <= GameConstants.MAX_DIRT_DIFFERENCE;
    }

    public static boolean isDirWetEmpty (Direction dir) throws GameActionException {
        MapLocation loc = rc.adjacentLocation(dir);
        return rc.senseFlooding(loc) && rc.senseRobotAtLocation(loc) == null;
    }

    public static boolean isDirDryFlatEmpty (Direction dir) throws GameActionException {
        MapLocation loc = rc.adjacentLocation(dir);
        return !rc.senseFlooding(loc) && rc.senseRobotAtLocation(loc) == null && Math.abs(rc.senseElevation(loc) - rc.senseElevation(here)) <= GameConstants.MAX_DIRT_DIFFERENCE;
    }

    /*
    Assumes that we can sense this tile
    Returns true if this tile's elevation is within +/-3 of our tile's elevation
    Returns false otherwise
    */
    public static boolean isLocFlat(MapLocation loc) throws GameActionException {
        return Math.abs(rc.senseElevation(loc) - myElevation) <= GameConstants.MAX_DIRT_DIFFERENCE;
    }

    public static boolean isLocDry (MapLocation loc) throws GameActionException {
        return !rc.senseFlooding(loc);
    }

    public static boolean isLocEmpty (MapLocation loc) throws GameActionException {
        return rc.senseRobotAtLocation(loc) == null;
    }

    public static boolean isLocAllyLandscaper(MapLocation loc) throws GameActionException {
        RobotInfo ri = rc.senseRobotAtLocation(loc);
        return ri != null && ri.type == RobotType.LANDSCAPER && ri.team == us;
    }

    public static boolean isLocBuilding(MapLocation loc) throws GameActionException {
        RobotInfo ri = rc.senseRobotAtLocation(loc);
        return ri != null && ri.type.isBuilding();
    }

    public static boolean isLocEnemyBuilding(MapLocation loc) throws GameActionException {
        RobotInfo ri = rc.senseRobotAtLocation(loc);
        return ri != null && ri.type.isBuilding() && ri.team == them;
    }

    public static boolean isLocDryEmpty (MapLocation loc) throws GameActionException {
        return !rc.senseFlooding(loc) && rc.senseRobotAtLocation(loc) == null;
    }

    public static boolean isLocDryFlatEmpty (MapLocation loc) throws GameActionException {
        return !rc.senseFlooding(loc) && rc.senseRobotAtLocation(loc) == null && Math.abs(rc.senseElevation(loc) - rc.senseElevation(here)) <= GameConstants.MAX_DIRT_DIFFERENCE;
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
        if (myType.isBuilding()) {
            return;
        }

        for (int i = 0; i < directions.length; i++) {
            MapLocation adjLoc = rc.adjacentLocation(directions[i]);
            if (!rc.onTheMap(adjLoc)) {
                isDirMoveable[i] = false;
                continue;
            }

            if (myType == RobotType.DELIVERY_DRONE) {
                if (!isDirEmpty(directions[i])) {
                    isDirMoveable[i] = false;
                    continue;
                }
                if (BotDeliveryDrone.isOffenseDrone && !BotDeliveryDrone.isDroneSwarming) {
                    // only move in cardinal directions
                    for (int d = 1; d < isDirMoveable.length; d+=2) {
                        isDirMoveable[d] = false;
                    }
                }
            } else {
                // STATE == I am a miner/landscaper
                if (!isDirDryFlatEmpty(directions[i])) {
                    isDirMoveable[i] = false;
                    continue;
                }
            }
            isDirMoveable[i] = true;
        }

        // Miners avoid rings
        if (myType == RobotType.MINER && rc.getSoupCarrying() == 0) {
            int myRing = maxXYDistance(HQLoc, here);
            for (int i = 0; i < directions.length; i++) {
                MapLocation loc = rc.adjacentLocation(directions[i]);
                if (!rc.onTheMap(loc)) {
                    continue;
                }
                int curRing = maxXYDistance(HQLoc, loc);
                if (myRing > 1 && curRing == 1) {
                    isDirMoveable[i] = false;
                }
                if (myRing > 2 && curRing == 2) {
                    isDirMoveable[i] = false;
                }
            }
        }
    }

    public static void updateIsDirDanger() throws GameActionException {
        if (myType.isBuilding()) {
            return;
        }

        inDanger = false;
        RobotInfo[] nearbyEnemies;
        if (myType == RobotType.DELIVERY_DRONE) {
            nearbyEnemies = visibleEnemies;;
            for (RobotInfo ri : nearbyEnemies) {
                if (canShootType(ri.type)) {
                    if (here.distanceSquaredTo(ri.location) <= GameConstants.NET_GUN_SHOOT_RADIUS_SQUARED) {
                        inDanger = true;
                    }
                }
            }
        } else {
            nearbyEnemies = rc.senseNearbyRobots(8, them);;
            for (RobotInfo ri : nearbyEnemies) {
                if (ri.type == RobotType.DELIVERY_DRONE) {
                    if (ri.location.isAdjacentTo(here)) {
                        inDanger = true;
                    }
                }
            }
        }
        outer: for (int i = 0; i < directions.length; i++) {
            MapLocation adjLoc = rc.adjacentLocation(directions[i]);

            if (!rc.onTheMap(adjLoc)) {
                isDirMoveable[i] = false;
                isDirDanger[i] = false;
                continue;
            }

            if (myType == RobotType.DELIVERY_DRONE) {
                // checks for dangerous netguns
                // add check for if we are ignoring netguns
                if (!BotDeliveryDrone.isDroneSwarming) {
                    for (RobotInfo ri : nearbyEnemies) {
                        if (canShootType(ri.type)) {
                            if (adjLoc.distanceSquaredTo(ri.location) <= GameConstants.NET_GUN_SHOOT_RADIUS_SQUARED) {
                                isDirMoveable[i] = false;
                                isDirDanger[i] = true;
                                continue outer;
                            }
                        }
                    }
                }
            } else {
                // STATE == I am a miner/landscaper
                // checks for dangerous drones
                for (RobotInfo ri : nearbyEnemies) {
                    if (ri.type == RobotType.DELIVERY_DRONE) {
                        if (ri.location.isAdjacentTo(adjLoc)) {
                            isDirMoveable[i] = false;
                            isDirDanger[i] = true;
                            continue outer;
                        }
                    }
                }
            }
            isDirDanger[i] = false;
        }
    }
}
