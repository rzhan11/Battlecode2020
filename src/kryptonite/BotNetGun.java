package kryptonite;

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
			Globals.endTurn();
		}
	}

	public static void turn() throws GameActionException {

		while(rc.isReady()) {
			RobotInfo[] robots = rc.senseNearbyRobots();
			int mind = Integer.MAX_VALUE;
			int id = -1;
			//MapLocation here = rc.getLocation();

			for (RobotInfo ri : robots) {
				if (ri.team == rc.getTeam().opponent() && ri.type == RobotType.DELIVERY_DRONE){
					int curdis = here.distanceSquaredTo(ri.location);
					if(curdis < mind){
						mind = curdis;
						id = ri.ID;
					}
				}
			}

			if(id!=-1){
				rc.shootUnit(id);
			}


		}


	}
}
