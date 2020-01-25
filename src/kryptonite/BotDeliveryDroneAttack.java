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

        if (!isAttacking) {
            if (rc.isCurrentlyHoldingUnit()) {
                for (Direction dir: directions) {
                    if (rc.canDropUnit(dir)) {
                        Actions.doDropUnit(dir);
                    }
                }
            }
        }

        // if in attack mode, ignore dangerous directions
        if (isAttacking) {
            updateIsDirMoveable();
        }

        boolean result = chaseEnemies(isAttacking);
        if (result) {
            return;
        }

        if (isAttacking) {
            if (!rc.isCurrentlyHoldingUnit()) {
                for (RobotInfo ri: adjacentAllies) {
                    if (ri.type == RobotType.LANDSCAPER) {
                        Actions.doPickUpUnit(ri.ID);
                    }
                }
            }
        }

        moveLog(getSymmetryLoc());
    }
}
