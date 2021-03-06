package seeding;

import battlecode.common.*;

import static seeding.Communication.*;
import static seeding.Constants.*;
import static seeding.Debug.*;
import static seeding.Map.*;

public class BotNetGun extends Globals {

	public static int[] IDS = new int[5000];
	public static int dronesSeen = 0;

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

		if (!rc.isReady()) {
			log("Not ready");
			return;
		}


		// try to shoot the closest visible enemy units
		int closestDist = P_INF;
		int id = -1;
		//MapLocation here = rc.getLocation();

		for (RobotInfo ri: visibleEnemies) {
			if (ri.type == RobotType.DELIVERY_DRONE) {
				int dist = here.distanceSquaredTo(ri.location);
				if(dist < closestDist){
					closestDist = dist;
					id = ri.ID;
					IDS[dronesSeen++] = id;
					dronesSeen %= 5000;
				}
			}
		}

		// shoot radius less than sensor radius
		if(id != -1 && closestDist <= GameConstants.NET_GUN_SHOOT_RADIUS_SQUARED) {
			Actions.doShootUnit(id);
			log("Shooting unit");
			return;
		} else {
			for (int i : IDS) {
				if (rc.canShootUnit(i)) {
					if (Clock.getBytecodesLeft() > 0) {
						Actions.doShootUnit(i);
						log("Shooting unit");
					}
					return;
				}
			}
		}
	}

}
