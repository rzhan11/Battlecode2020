package kryptonite;

import battlecode.common.*;

public class BotDeliveryDrone extends Globals {

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

	public static boolean canPickUp (RobotType rt) {
		return rt == RobotType.MINER || rt == RobotType.LANDSCAPER || rt == RobotType.COW;
	}

	public static void turn() throws GameActionException {
		if (rc.isCurrentlyHoldingUnit()) {
			for (Direction dir: directions) {
				MapLocation loc = rc.adjacentLocation(dir);
				if (inMap(loc) && rc.senseFlooding(loc) && rc.canDropUnit(dir)) {
					rc.dropUnit(dir);
					Debug.tlog("Dropped unit into water at " + loc);
					return;
				}
			}
		} else {
			// checks for adjacent enemies that can be picked upup
			for (Direction dir: directions) {
				MapLocation loc = rc.adjacentLocation(dir);
				if (inMap(loc)) {
					RobotInfo ri = rc.senseRobotAtLocation(loc);
					if (ri != null && rc.canPickUpUnit(ri.ID)) {
						rc.pickUpUnit(ri.ID);
						Debug.tlog("Picked up unit at " + loc);
						return;
					}
				}
			}

			// checks for nearby enemy that can be picked up
			int closestEnemyDist = P_INF;
			int closestEnemyIndex = -1;
			int index = 0;
			for (RobotInfo ri: visibleEnemies) {
				if (canPickUp(ri.type)) {
					int dist = here.distanceSquaredTo(ri.location);
					if (dist < closestEnemyDist) {
						closestEnemyDist = dist;
						closestEnemyIndex = index;
					}
				}
			}

			// if there is a nearby enemy that can be picked up, try to chase it
			if (closestEnemyIndex != -1) {
				MapLocation loc = visibleEnemies[closestEnemyIndex].location;
				Direction dir = Nav.tryMoveInDirectionDrone(here.directionTo(loc));
				Debug.tlog("Chasing enemy at " + loc + ", moved " + dir);
			}
		}

		// moves away from HQLocation
		int curHQDist = here.distanceSquaredTo(HQLocation);
		for (Direction dir: directions) {
			MapLocation loc = rc.adjacentLocation(dir);
			if (rc.canMove(dir) && HQLocation.distanceSquaredTo(loc) > curHQDist) {
				Actions.doMove(dir);
			}
		}
	}
}
