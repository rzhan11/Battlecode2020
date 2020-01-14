package kryptonite;

import battlecode.common.*;

import static kryptonite.Communication.*;
import static kryptonite.Constants.*;
import static kryptonite.Debug.*;
import static kryptonite.Map.*;

public class BotNetGun extends Globals {

	public static void loop() throws GameActionException {
		while (true) {
			try {
				Globals.update();
				if (firstTurn) {

				}
			    turn();
			} catch (Exception e) {
				e.printStackTrace();
			}
			Globals.endTurn(false);
		}
	}

	public static void turn() throws GameActionException {

		// try to shoot the closest visible enemy units
		if (rc.isReady()) {
			int closestDist = P_INF;
			int id = -1;
			//MapLocation here = rc.getLocation();

			for (RobotInfo ri: visibleEnemies) {
				if (ri.type == RobotType.DELIVERY_DRONE) {
					int dist = here.distanceSquaredTo(ri.location);
					if(dist < closestDist){
						closestDist = dist;
						id = ri.ID;
					}
				}
			}

			// shoot radius less than sensor radius
			if(id != -1 && closestDist <= GameConstants.NET_GUN_SHOOT_RADIUS_SQUARED) {
				Actions.doShootUnit(id);
			}
		}
	}
}
