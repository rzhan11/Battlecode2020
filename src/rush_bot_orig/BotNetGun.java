package rush_bot_orig;

import battlecode.common.*;

import static rush_bot_orig.Actions.*;
import static rush_bot_orig.Communication.*;
import static rush_bot_orig.Debug.*;
import static rush_bot_orig.Globals.*;
import static rush_bot_orig.Map.*;
import static rush_bot_orig.Nav.*;
import static rush_bot_orig.Utils.*;
import static rush_bot_orig.Wall.*;
import static rush_bot_orig.Zones.*;

public class BotNetGun extends Globals {

	public static void loop() throws GameActionException {
		while (true) {
			try {
				Globals.update();

				turn();
			} catch (Exception e) {
				e.printStackTrace();
			}
			Globals.endTurn();
		}
	}

	public static void turn() throws GameActionException {

		if (!rc.isReady()) {
			log("Not ready");
			return;
		}

		int shotID = tryShoot();

	}

	public static int tryShoot() throws GameActionException {

		// try to shoot the closest visible enemy units
		int closestDist = P_INF;
		RobotInfo closestDrone = null;

		for (RobotInfo ri: visibleEnemies) {
			if (ri.type.canBeShot()) {
				int dist = here.distanceSquaredTo(ri.location);
				if(dist < closestDist){
					closestDist = dist;
					closestDrone = ri;
				}
			}
		}

		// shoot radius less than sensor radius
		if(closestDrone != null && closestDist <= GameConstants.NET_GUN_SHOOT_RADIUS_SQUARED) {
			log("Shooting unit " + closestDrone.ID);
			Actions.doShootUnit(closestDrone.ID);
			return closestDrone.ID;
		}

		return -1;
	}
}
