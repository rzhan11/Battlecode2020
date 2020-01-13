package kryptonite;

import battlecode.common.*;

public class BotOffenseDeliveryDrone extends BotDeliveryDrone {

    public static boolean initialized = false;
    public static int symmetryHQLocationsIndex;
    public static MapLocation exploreSymmetryLocation;
    public static MapLocation enemyHQLocation = null;

    public static void init() throws GameActionException {
        symmetryHQLocationsIndex = myID % symmetryHQLocations.length;
        exploreSymmetryLocation= symmetryHQLocations[symmetryHQLocationsIndex];

        initialized = true;
    }

    public static void turn() throws GameActionException {
        Debug.tlog("OFFENSE DRONE");

        if (!initialized) {
            init();
        }

        locateFlooding();
        Debug.tlog("floodingMemory: " + floodingMemory);

        // tries to determine enemyHQLocation if not already determined
        if (enemyHQLocation == null) {
            if (rc.canSenseLocation(exploreSymmetryLocation)) {
                RobotInfo ri = rc.senseRobotAtLocation(exploreSymmetryLocation);
                if (ri != null && ri.type == RobotType.HQ) {
                    enemyHQLocation = exploreSymmetryLocation;
                    Debug.tlog("Found enemy HQ at " + enemyHQLocation);
                    Communication.writeTransactionEnemyHQLocation(exploreSymmetryLocation);
                } else {
                    Debug.tlog("enemy HQ is not at " + exploreSymmetryLocation);
                    symmetryHQLocationsIndex++;
                    symmetryHQLocationsIndex %= symmetryHQLocations.length;
                    exploreSymmetryLocation = symmetryHQLocations[symmetryHQLocationsIndex];
                    Debug.tlog("Retargetting exploreSymmetryLocation to " + exploreSymmetryLocation);
                }
            }
        }

        // if enemyHQLocation not found, go to exploreSymmetryLocation
        if (enemyHQLocation == null) {
            Debug.tlog("Moving towards exploreSymmetryLocation at " + exploreSymmetryLocation);
            int[] color = Actions.WHITE;
            rc.setIndicatorLine(here, exploreSymmetryLocation, color[0], color[1], color[2]);
            moveLog(exploreSymmetryLocation);

        } else { // STATE == enemyHQLocation found (AKA not null)

            // chase enemies and drop them into water
            boolean sawEnemy = tryKillRobots(visibleEnemies, them);
            if (sawEnemy) {
                return;
            }
            boolean sawCow = tryKillRobots(visibleCows, cowTeam);
            if (sawCow) {
                return;
            }

            Debug.tlog("Moving towards enemyHQLocation at " + enemyHQLocation);
            int[] color = Actions.BLACK;
            rc.setIndicatorLine(here, enemyHQLocation, color[0], color[1], color[2]);
            moveLog(enemyHQLocation);
        }

        return;
    }

}
