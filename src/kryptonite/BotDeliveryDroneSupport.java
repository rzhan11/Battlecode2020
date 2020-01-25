package kryptonite;

import battlecode.common.*;

import static kryptonite.Communication.*;
import static kryptonite.Debug.*;
import static kryptonite.Map.*;
import static kryptonite.Wall.*;
import static kryptonite.Utils.*;
import static kryptonite.Zones.*;


public class BotDeliveryDroneSupport extends BotDeliveryDrone {

    public static boolean initializedDroneSupport = false;
    public static int targetID = -1;
    public static MapLocation targetDropLoc = null;

    public static void initDroneSupport() throws GameActionException {

        myRole = DRONE_SUPPORT_ROLE;

        initializedDroneSupport = true;

        Globals.endTurn();
        Globals.update();
    }

    public static void turn() throws GameActionException {
        log("SUPPORT DRONE");

        if (!initializedDroneSupport) {
            initDroneSupport();
        }

        boolean moveDiagonal = false;
        if (maxXYDistance(HQLoc, here) <= wallRingRadius + 1) {
            moveDiagonal = true;
        }
        boolean result = chaseEnemies(moveDiagonal);
        if (result) {
            return;
        }

        // do not go into 3x3 plot, in order to avoid trapping the design school/fulfillment center
        if (wallCompleted) {
            int myRing = maxXYDistance(HQLoc, here);
            if (myRing > 1) {
                for (int i = 0; i < directions.length; i++) {
                    if (maxXYDistance(HQLoc, rc.adjacentLocation(directions[i])) <= 1) {
                        isDirMoveable[i] = false;
                    }
                }
            }
        }

        if (rc.isCurrentlyHoldingUnit()) {
            // if adjacent to valid tile, drop unit down
            for (Direction dir: directions) {
                MapLocation loc = rc.adjacentLocation(dir);
                if (!rc.onTheMap(loc)) {
                    continue;
                }
                if (isLocDryEmpty(loc) && !isDigLoc(loc) && maxXYDistance(HQLoc, loc) >= wallRingRadius) {
                    Actions.doDropUnit(dir);
                    return;
                }
            }

            if (targetDropLoc != null && rc.canSenseLocation(targetDropLoc)) {
                if (isLocDryEmpty(targetDropLoc)) {
                    moveLog(targetDropLoc);
                    return;
                } else {
                    targetDropLoc = null;
                }
            }

            // find a safe tile OUTSIDE of the 5x5 plot where we can drop unit
            if (maxXYDistance(HQLoc, here) < wallRingRadius) {
                for (int i = 0; i < wallLocsLength; i++) {
                    MapLocation loc = wallLocs[i];
                    if (rc.canSenseLocation(loc)) {
                        if (isLocDryEmpty(loc)) {
                            targetDropLoc = loc;
                            break;
                        }
                    }
                }
            } else {
                for (int[] dir : senseDirections) {
                    // ignore locs that are out of sensor range or within drop range (since not safe)
                    if (dir[2] <= 2 || actualSensorRadiusSquared < dir[2]) {
                        continue;
                    }
                    MapLocation loc = here.translate(dir[0], dir[1]);
                    if (!rc.onTheMap(loc)) {
                        continue;
                    }

                    if (isLocDryEmpty(loc) && !isDigLoc(loc) && maxXYDistance(HQLoc, loc) >= wallRingRadius) {
                        targetDropLoc = loc;
                        break;
                    }
                }
            }

            if (targetDropLoc == null) {
                moveLog(getSymmetryLoc());
            } else {
                moveLog(targetDropLoc);
            }
            return;

        } else {
            // STATE = not holding unit
            if (targetID != -1) {
                if (rc.canSenseRobot(targetID)) {
                    RobotInfo ri = rc.senseRobot(targetID);
                    // resets target if they are out of the 5x5 plot
                    // or is currently being carried by another drone
                    if (maxXYDistance(HQLoc, ri.location) >= wallRingRadius ||
                        !ri.equals(rc.senseRobotAtLocation(ri.location))) {
                        targetID = -1;
                    }
                } else {
                    // resets target since we cannot see them anymore
                    log("Lost track of " + targetID);
                    targetID = -1;
                }
            }

            if (targetID == -1) {
                // finds closest transportable robot in the 5x5 plot
                int closestDist = P_INF;
                for (RobotInfo ri: visibleAllies) {
                    if (ri.type == RobotType.LANDSCAPER ||
                            (ri.type == RobotType.MINER && wallCompleted)) {
                        if (maxXYDistance(HQLoc, ri.location) < wallRingRadius) {
                            int dist = here.distanceSquaredTo(ri.location);
                            if (dist < closestDist) {
                                closestDist = dist;
                                targetID = ri.ID;
                            }
                        }
                    }
                }
            }

            if (targetID != -1) {
                int res = tryPickUpUnit(targetID);
                if (res == 1) {
                    // successfully picked up unit
                    targetID = -1;
                }
            } else {
                // STATE == no valid visible, pick-up-able robots
                // Rotate around HQ and try to find one

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
            }
        }
    }
}
