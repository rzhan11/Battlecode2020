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

public class Map extends Globals {

    public static boolean isDigLoc(MapLocation ml) {
        return Math.abs(ml.x - HQLoc.x) % 2 == 0 &&  Math.abs(ml.y - HQLoc.y) % 2 == 0 && maxXYDistance(ml, HQLoc) > 2;
    }

    public static boolean isBuildLoc(MapLocation ml) {
        return Math.abs(ml.x - HQLoc.x) % 2 == 1 &&  Math.abs(ml.y - HQLoc.y) % 2 == 1;
    }

    /*
    Reflects ml1 over ml2
     */
    public static MapLocation reflect(MapLocation ml1, MapLocation ml2) {
        return new MapLocation(2 * ml2.x - ml1.x, 2 * ml2.y - ml1.y);
    }

    public static boolean inSameZone(MapLocation ml1, MapLocation ml2) throws GameActionException {
        return locToZonePair(ml1).equals(locToZonePair(ml2));
    }

    public static MapLocation getRandomLoc() throws GameActionException {
        return new MapLocation(rand.nextInt(mapWidth), rand.nextInt(mapHeight));
    }

    /*
    Reflects ml1 over ml2
     */
    public static MapLocation reflectLocOverSymmetry(MapLocation loc) {
        switch (symmetryHQLocsIndex) {
            case 0: // horizontal symmetry
                return new MapLocation(mapWidth - 1 - loc.x, loc.y);
            case 1:
                return new MapLocation(loc.x, mapWidth - 1 - loc.y);
            case 2:
                return new MapLocation(mapWidth - 1 - loc.x, mapWidth - 1 - loc.y);
        }
        return null;
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

    /*
    Assumes that we can sense this tile
    Returns true if this tile's elevation is within +/-3 of our tile's elevation
    Returns false otherwise
    */
    public static boolean isLocFlat(MapLocation loc) throws GameActionException {
        return Math.abs(rc.senseElevation(loc) - myElevation) <= GameConstants.MAX_DIRT_DIFFERENCE;
    }

    public static boolean isLocWet (MapLocation loc) throws GameActionException {
        return rc.senseFlooding(loc);
    }

    public static boolean isLocDry (MapLocation loc) throws GameActionException {
        return !rc.senseFlooding(loc);
    }

    public static boolean isAdjLocDry (MapLocation loc) throws GameActionException {
        for (Direction dir: directions) {
            MapLocation adjLoc = loc.add(dir);
            if (rc.canSenseLocation(adjLoc) && isLocDry(adjLoc)) {
                return true;
            }
        }
        return false;
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

    public static boolean isLocAlly(MapLocation loc) throws GameActionException {
        RobotInfo ri = rc.senseRobotAtLocation(loc);
        return ri != null && ri.team == us;
    }

    public static boolean isLocAllyBuilding(MapLocation loc) throws GameActionException {
        RobotInfo ri = rc.senseRobotAtLocation(loc);
        return ri != null && ri.type.isBuilding() && ri.team == us;
    }

    public static boolean isLocEnemy(MapLocation loc) throws GameActionException {
        RobotInfo ri = rc.senseRobotAtLocation(loc);
        return ri != null && ri.team == them;
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

            double locSensorRadiusSquared = myType.sensorRadiusSquared * GameConstants.getSensorRadiusPollutionCoefficient(rc.sensePollution(adjLoc));
            if (locSensorRadiusSquared < 2) {
                isDirMoveable[i] = false;
                continue;
            }

            if (myType == RobotType.DELIVERY_DRONE) {
                if (!isLocEmpty(adjLoc)) {
                    isDirMoveable[i] = false;
                    continue;
                }
            } else {
                // STATE == I am a miner/landscaper
                if (!isLocDryFlatEmpty(adjLoc)) {
                    isDirMoveable[i] = false;
                    continue;
                }
            }
            isDirMoveable[i] = true;
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
                if (ri.type.canShoot() && ri.cooldownTurns < 5) {
                    if (here.distanceSquaredTo(ri.location) <= GameConstants.NET_GUN_SHOOT_RADIUS_SQUARED) {
                        inDanger = true;
                    }
                }
            }
        } else {
            nearbyEnemies = rc.senseNearbyRobots(8, them);;
            for (RobotInfo ri : nearbyEnemies) {
                if (ri.type == RobotType.DELIVERY_DRONE && ri.cooldownTurns < 5) {
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
                for (RobotInfo ri : nearbyEnemies) {
                    if (ri.type.canShoot() && ri.cooldownTurns < 5) {
                        if (adjLoc.distanceSquaredTo(ri.location) <= GameConstants.NET_GUN_SHOOT_RADIUS_SQUARED) {
                            isDirMoveable[i] = false;
                            isDirDanger[i] = true;
                            continue outer;
                        }
                    }
                }
            } else {
                // STATE == I am a miner/landscaper
                // checks for dangerous drones
                for (RobotInfo ri : nearbyEnemies) {
                    if (ri.type == RobotType.DELIVERY_DRONE && ri.cooldownTurns < 5) {
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

        // when danger is unavoidable, reset isDirMoveable to ignore danger tiles
        if (inDanger) {
            boolean canAvoid = false;
            for (int i = 0; i < directions.length; i++) {
                if (isDirMoveable[i]) {
                    canAvoid = true;
                    break;
                }
            }
            if (!canAvoid) {
                updateIsDirMoveable();
            }
        }

    }
}
