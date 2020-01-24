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

        if (roundNum >= 1500) {
            isAttacking = true;
        }

        if (!initializedDroneAttack) {
            initDroneAttack();
        }

        // if in attack mode, ignore dangerous directions
        if (isAttacking) {
            updateIsDirMoveable();
        }

        boolean result = chaseEnemies(isAttacking);
        if (result) {
            return;
        }

        moveLog(getSymmetryLoc());
    }
}
