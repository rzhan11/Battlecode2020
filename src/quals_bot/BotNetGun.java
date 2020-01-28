package quals_bot;

import battlecode.common.*;

import static quals_bot.Actions.*;
import static quals_bot.Communication.*;
import static quals_bot.Debug.*;
import static quals_bot.Globals.*;
import static quals_bot.Map.*;
import static quals_bot.Nav.*;
import static quals_bot.Utils.*;
import static quals_bot.Wall.*;
import static quals_bot.Zones.*;

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
