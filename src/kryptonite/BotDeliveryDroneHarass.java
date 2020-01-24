package kryptonite;

import battlecode.common.*;

import static kryptonite.Communication.*;
import static kryptonite.Debug.*;
import static kryptonite.Map.*;
import static kryptonite.Wall.*;
import static kryptonite.Utils.*;
import static kryptonite.Zones.*;

public class BotDeliveryDroneHarass extends BotDeliveryDrone {

    public static boolean initializedDroneHarass = false;
    // 0 = soup zone
    // 1 = unexplored zone
    // 2 = symmetry loc
    // 3 = random loc
    final public static int NUM_HARASS_TYPES = 4;

    public static int harassTypeAssignRound = 0;
    public static int harassType;
    public static MapLocation targetLoc;

    public static void initDroneHarass() throws GameActionException {

        initializedDroneHarass = true;
        harassType = myID % NUM_HARASS_TYPES;

        Globals.endTurn();
        Globals.update();
    }

    public static void assignHarassType () {
        assignHarassType(rand.nextInt(NUM_HARASS_TYPES));
    }

    public static void assignHarassType (int type) {
        harassType = type;
        harassTypeAssignRound = roundNum;
        targetLoc = null;
    }

    public static void assignSoupExploreHarassType () {
        if (rand.nextInt(2) == 0) {
            assignHarassType(0);
        } else {
            assignHarassType(1);
        }
    }

    public static void assignSymmetryRandomHarassType () {
        if (rand.nextInt(2) == 0) {
            assignHarassType(2);
        } else {
            assignHarassType(3);
        }
    }

    public static void turn() throws GameActionException {
        log("HARASS DRONE ");

        if (!initializedDroneHarass) {
            initDroneHarass();
        }

        boolean result = chaseEnemies(false);
        if (result) {
            return;
        }

        // STATE = not carrying/seeing enemy units

        MapLocation[] locs = findClosestSoupAndUnexploredZone();
        MapLocation soupLoc = locs[0];
        MapLocation unexploredLoc = locs[1];
        if (unexploredLoc.equals(getSymmetryLoc())) {
            unexploredLoc = null;
        }

        if (roundNum - harassTypeAssignRound > 50) {
            assignHarassType();
        }

        switch (harassType) {
            case 0:
                log("SOUP HARASS");
                if (targetLoc != null && inSameZone(here, targetLoc)) {
                    targetLoc = null;
                }
                if (targetLoc == null) {;
                    if (soupLoc == null) {
                        assignSymmetryRandomHarassType();
                    } else {
                        int[] zone = locToZonePair(soupLoc);
                        // if the reflection of the closest known soup location is unexplored, target it
                        if (exploredZoneStatus[zone[0]][zone[1]] == 0) {
                            targetLoc = soupLoc;
                        } else {
                            assignSymmetryRandomHarassType();
                        }
                    }
                }
                break;
            case 1:
                log("EXPLORE HARASS");
                if (targetLoc != null && inSameZone(here, targetLoc)) {
                    targetLoc = null;
                }
                if (targetLoc == null) {
                    if (unexploredLoc == null) {
                        assignSymmetryRandomHarassType();
                    }
                }
                break;
            case 2:
                log("SYMMETRY HARASS");
                targetLoc = getSymmetryLoc();
                break;
            case 3:
                log("RANDOM HARASS");
                if (targetLoc != null && inSameZone(here, targetLoc)) {
                    targetLoc = null;
                }
                if (targetLoc == null) {
                    targetLoc = getRandomLoc();
                }
                break;
        }

        if (targetLoc != null) {
            moveLog(targetLoc);
        }
    }
}
