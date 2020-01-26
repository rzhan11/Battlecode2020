package kryptonite;

import battlecode.common.*;

import static kryptonite.Communication.*;
import static kryptonite.Debug.*;
import static kryptonite.Map.*;
import static kryptonite.Wall.*;
import static kryptonite.Utils.*;
import static kryptonite.Zones.*;

public class BotDeliveryDroneAttack extends BotDeliveryDrone {

    public static boolean initializedDroneAttack = false;
    public static boolean isAttacking = false;
    public static boolean isCarryingLandscaper = false;

    public static void initDroneAttack() throws GameActionException {

        isAttacking = false;


        initializedDroneAttack = true;

        Globals.endTurn();
        Globals.update();
    }

    public static void turn() throws GameActionException {
        log("ATTACK DRONE ");

        if (!initializedDroneAttack) {
            initDroneAttack();
        }

        if (roundNum >= 1500) {
            isAttacking = true;
        }

        // if in attack mode, ignore dangerous directions

        if (isAttacking) {
            updateIsDirMoveable();

            if (isCarryingEnemy) {
                for (Direction dir: directions) {
                    MapLocation loc = rc.adjacentLocation(dir);
                    if (!rc.onTheMap(loc)) {
                        continue;
                    }
                    if (isLocWetEmpty(loc)) {
                        Actions.doDropUnit(dir);
                        return;
                    }
                }
                if (isLocWet(here)) {
                    log("Disintegrating to drop landscaper");
                    rc.disintegrate();
                    return;
                }
            }

            if (isCarryingLandscaper) {
                for (Direction dir: directions) {
                    MapLocation loc = rc.adjacentLocation(dir);
                    if (!rc.onTheMap(loc)) {
                        continue;
                    }
                    if (isLocDryEmpty(loc) && loc.isAdjacentTo(getSymmetryLoc())) {
                        Actions.doDropUnit(dir);
                        return;
                    }
                }
                if (here.isAdjacentTo(getSymmetryLoc()) && isLocDry(here)) {
                    log("Disintegrating to drop landscaper");
                    rc.disintegrate();
                    return;
                }
            }

            if (!rc.isCurrentlyHoldingUnit()) {
                for (RobotInfo ri: adjacentAllies) {
                    if (ri.type == RobotType.LANDSCAPER && !ri.location.isAdjacentTo(getSymmetryLoc())) {
                        Actions.doPickUpUnit(ri.ID);
                        isCarryingLandscaper = true;
                        return;
                    }
                }
            }
            if (!rc.isCurrentlyHoldingUnit()) {
                for (RobotInfo ri: adjacentEnemies) {
                    if (ri.type.canBePickedUp()) {
                        Actions.doPickUpUnit(ri.ID);
                        isCarryingEnemy = true;
                        return;
                    }
                }
            }
        } else {
            boolean result = chaseEnemies(isAttacking);
            if (result) {
                if (rc.isCurrentlyHoldingUnit()) {
                    isCarryingEnemy = true;
                }
                return;
            }
        }

        moveLog(getSymmetryLoc());
    }
}
