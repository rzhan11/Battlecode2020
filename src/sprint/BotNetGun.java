package sprint;

import battlecode.common.*;

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

			if(id != -1){
				rc.shootUnit(id);
			}
		}
	}
}
