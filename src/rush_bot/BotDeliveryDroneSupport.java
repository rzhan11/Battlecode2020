package rush_bot;

import battlecode.common.*;

import static rush_bot.Debug.*;
import static rush_bot.Map.*;
import static rush_bot.Utils.*;
import static rush_bot.Wall.*;


public class BotDeliveryDroneSupport extends BotDeliveryDrone {

    public static boolean initializedDroneSupport = false;
    public static boolean isCarryingPlatformMiner = false;

    public static void initDroneSupport() throws GameActionException {

        myRole = DRONE_SUPPORT_ROLE;

        initializedDroneSupport = true;

        Globals.endTurn();
        Globals.update();
    }

    public static void turn() throws GameActionException {
        log("SUPPORT DRONE");
        log("wallFull " + wallFull);
        log("supportFull " + supportFull);

        if (!initializedDroneSupport) {
            initDroneSupport();
        }

        if (wallFull && supportFull) {
            myRole = DRONE_WALL_ROLE;
            return;
        }

        if (rc.isCurrentlyHoldingUnit()) {

            if (isCarryingPlatformMiner) {
                log("is carrying platform miner");
                MapLocation closestLoc = null;
                int closestDist = P_INF;
                for (MapLocation loc: platformLocs) {
                    if (here.isAdjacentTo(loc)) {
                        if (rc.canSenseLocation(loc) && isLocDryEmpty(loc)) {
                            Actions.doDropUnit(here.directionTo(loc));
                            isCarryingPlatformMiner = false;
                            return;
                        }
                    }
                    if (!rc.canSenseLocation(loc) || isLocDryEmpty(loc)) {
                        int dist = here.distanceSquaredTo(loc);
                        if (dist < closestDist) {
                            closestDist = dist;
                            closestLoc = loc;
                        }
                    }
                }
                if (closestLoc != null) {
                    moveLog(closestLoc);
                    return;
                } else {
                    moveLog(platformCornerLoc);
                    return;
                }
            }

            MapLocation[] targetLocs = null;
            int targetLocsLength = -1;
            if (!wallFull) {
                targetLocs = wallLocs;
                targetLocsLength = wallLocsLength;
            } else if (!supportFull) {
                targetLocs = supportWallLocs;
                targetLocsLength = supportWallLocsLength;
            }

            MapLocation closestLoc = null;
            int closestDist = P_INF;
            for (int i = 0; i < targetLocsLength; i++) {
                MapLocation loc = targetLocs[i];
                if (rc.canSenseLocation(loc) && isLocDryEmpty(loc)) {
                    if (here.isAdjacentTo(loc)) {
                        Actions.doDropUnit(here.directionTo(loc));
                        return;
                    }
                    int dist = getSymmetryLoc().distanceSquaredTo(loc);
                    if (dist < closestDist) {
                        closestDist = dist;
                        closestLoc = loc;
                    }
                }
            }
            if (closestLoc == null) {
                closestLoc = HQLoc;
            }
            moveLog(closestLoc);
        } else {
            // STATE == NOT HOLDING UNIT
            /*
            Pick up platform miner
             */
            for (RobotInfo ri: adjacentAllies) {
                if (ri.ID == platformMinerID && !inArray(platformLocs, ri.location, platformLocs.length)) {
                    Actions.doPickUpUnit(ri.ID);
                    isCarryingPlatformMiner = true;
                    return;
                }
            }
            for (RobotInfo ri: visibleAllies) {
                if (ri.ID == platformMinerID && !inArray(platformLocs, ri.location, platformLocs.length)) {
                    moveLog(ri.location);
                    return;
                }
            }

            /*
            pick up adjacent landscapers
             */
            for (RobotInfo ri: adjacentAllies) {
                if (ri.type == RobotType.LANDSCAPER) {
                    if (ri.ID == platformLandscaperID && !platformCompleted) {
                        continue;
                    }
                    if (ri.location.isAdjacentTo(HQLoc)) {
                        continue;
                    }
                    if (wallFull && maxXYDistance(HQLoc, ri.location) == 2 && !inArray(digLocs2x2, ri.location, digLocs2x2.length)) {
                        continue;
                    }
                    Actions.doPickUpUnit(ri.ID);
                    return;
                }
            }

            // move towards landscapers not on wall
            MapLocation closestLoc = null;
            int closestDist = P_INF;
            for (RobotInfo ri: visibleAllies) {
                if (ri.type == RobotType.LANDSCAPER) {
                    if (ri.ID == platformLandscaperID && !platformCompleted) {
                        continue;
                    }
                    if (ri.location.isAdjacentTo(HQLoc)) {
                        continue;
                    }
                    if (wallFull && maxXYDistance(HQLoc, ri.location) == 2 && !inArray(digLocs2x2, ri.location, digLocs2x2.length)) {
                        continue;
                    }
                    int dist = here.distanceSquaredTo(ri.location);
                    if (dist < closestDist) {
                        closestDist = dist;
                        closestLoc = ri.location;
                    }
                }
            }
            if (closestLoc == null) {
                // 90 clockwise
                Direction dirToHQ = HQLoc.directionTo(here);
                Direction targetDir = dirToHQ.rotateRight().rotateRight();
                MapLocation targetLoc = HQLoc.add(targetDir).add(targetDir).add(targetDir);
                if (!rc.onTheMap(targetLoc)) {
                    // 90 counterclockwise
                    targetDir = dirToHQ.rotateLeft().rotateLeft();
                    targetLoc = HQLoc.add(targetDir).add(targetDir).add(targetDir);
                    if (!rc.onTheMap(targetLoc)) {
                        // 45 clockwise
                        targetDir = dirToHQ.rotateRight();
                        targetLoc = HQLoc.add(targetDir).add(targetDir).add(targetDir);
                        if (!rc.onTheMap(targetLoc)) {
                            // 45 counterclockwise
                            targetDir = dirToHQ.rotateLeft();
                            targetLoc = HQLoc.add(targetDir).add(targetDir).add(targetDir);
                            if (!rc.onTheMap(targetLoc)) {
                                targetDir = HQLoc.directionTo(getSymmetryLoc());
                                targetLoc = HQLoc.add(targetDir).add(targetDir).add(targetDir);
                            }
                        }
                    }
                }
                log("Trying to rotate around ally HQ");
                moveLog(targetLoc);
                return;
            } else {
                moveLog(closestLoc);
                return;
            }
        }
    }
}
