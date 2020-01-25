package kryptonite;

import battlecode.common.*;

import static kryptonite.Communication.*;
import static kryptonite.Debug.*;
import static kryptonite.Map.*;
import static kryptonite.Wall.*;
import static kryptonite.Utils.*;
import static kryptonite.Zones.*;

public class BotDeliveryDrone extends Globals {

	final public static int
			DRONE_SUPPORT_ROLE = 1,
			DRONE_HARASS_ROLE = 2,
			DRONE_ATTACK_ROLE = 3;

	public static int myRole = -1;

	public static boolean isCarryingEnemy = false;
	public static int avoidDangerResult;

	public static void loop() throws GameActionException {
		while (true) {
			try {
				Globals.update();

				if (!initalizedDrone) {
					initDeliveryDrone();
				}

				turn();
			} catch (Exception e) {
				e.printStackTrace();
			}
			Globals.endTurn();
		}
	}

	public static boolean initalizedDrone = false;

	public static void initDeliveryDrone() throws GameActionException {

		// if close to the HQ, be a support drone and move landscapers out of the 5x5 to the 7x7
//		if (wallCompleted && here.distanceSquaredTo(HQLoc) <= myType.sensorRadiusSquared)

		RobotInfo[] localDrones = rc.senseNearbyRobots(HQLoc, 18, us);
		int count = 0;
		for (RobotInfo ri: localDrones) {
			if (ri.type == RobotType.DELIVERY_DRONE) {
				count++;
			}
		}

		if (count < 1) {
			myRole = DRONE_SUPPORT_ROLE;
		} else if (myID % 2 == 0) {
			myRole = DRONE_SUPPORT_ROLE;
		} else {
			myRole = DRONE_HARASS_ROLE;
		}

		initalizedDrone = true;

		Globals.endTurn();
		Globals.update();
	}

	public static void turn() throws GameActionException {

		if (!rc.isReady()) {
			log("Not ready");
			return;
		}

		if (roundNum > 1000) {
			myRole = DRONE_ATTACK_ROLE;
		}

		avoidDangerResult = Nav.avoidDanger();
		if (avoidDangerResult == 1) {
			return;
		}

		switch (myRole) {
			case DRONE_SUPPORT_ROLE:
				BotDeliveryDroneSupport.turn();
				break;
			case DRONE_HARASS_ROLE:
				BotDeliveryDroneHarass.turn();
				break;
			case DRONE_ATTACK_ROLE:
				BotDeliveryDroneAttack.turn();
				break;
		}
	}

	public static boolean chaseEnemies (boolean moveDiagonal) throws GameActionException {
		if (!moveDiagonal) {
			for (int i = 1; i < directions.length; i+=2) {
				isDirMoveable[i] = false;
			}
		}

		boolean sawEnemy = tryKillRobots(visibleEnemies, them);
		if (sawEnemy) {
			return true;
		}
		boolean sawCow = tryKillRobots(visibleCows, cowTeam);
		if (sawCow) {
			return true;
		}

		return false;
	}

	/*
	targetRobots should contain the robots that the drone is checking for
	In most cases, it will be either visibleEnemies or visibleCows
	*/
	public static boolean tryKillRobots (RobotInfo[] targetRobots, Team killTeam) throws GameActionException {
		if (isCarryingEnemy) {
			// check for adjacent empty water
			for (Direction dir: directions) {
				MapLocation loc = rc.adjacentLocation(dir);
				if (!rc.onTheMap(loc)) {
					continue;
				}
				if (rc.senseFlooding(loc) && rc.senseRobotAtLocation(loc) == null) {
					log("Dropped unit into water at " + loc);
					Actions.doDropUnit(dir);
					isCarryingEnemy = false;
					return true;
				}
			}

			// go towards water
			if (floodingMemory != null) {
				// if on the flooded tile, tries to move off of it
				if (here.equals(floodingMemory)) {
					log("Moving to get off of floodingMemory");
					for (Direction dir: directions) {
						Direction move = Nav.tryMoveInDirection(dir);
					}
					return true;
				}

				log("Moving to drop into water " + floodingMemory);
				moveLog(floodingMemory);
				return true;
			}

			// explore
			MapLocation loc = findClosestSoupAndUnexploredZone()[1];
			log("Moving to unexplored zones to find water");
			moveLog(loc);
			return true;
		} else {
			// checks for adjacent enemies that can be picked up
			for (Direction dir: directions) {
				MapLocation loc = rc.adjacentLocation(dir);
				if (!rc.onTheMap(loc)) {
					continue;
				}
				RobotInfo ri = rc.senseRobotAtLocation(loc);
				if (ri != null && ri.team == killTeam && ri.type.canBePickedUp()) {
					Actions.doPickUpUnit(ri.ID);
					isCarryingEnemy = true;
					log("Picked up unit at " + loc);
					return true;
				}
			}

			// checks for nearby enemy that can be picked up
			int closestEnemyDist = P_INF;
			int closestEnemyIndex = -1;
			int index = 0;
			for (RobotInfo ri: targetRobots) {
				if (ri.type.canBePickedUp()) {
					int dist = here.distanceSquaredTo(ri.location);
					if (dist < closestEnemyDist) {
						closestEnemyDist = dist;
						closestEnemyIndex = index;
					}
				}
			}

			// if there is a nearby enemy that can be picked up, try to chase it
			if (closestEnemyIndex != -1) {
				MapLocation loc = targetRobots[closestEnemyIndex].location;
				moveLog(loc);
				return true;
			}
		}
		return false;
	}

	/*
	Tries to pick up the robot that corresponds to the given ID
	If adjacent to the unit, pick it up
	Else, bug navigate towards it
	Returns 1 if we picked up the unit
	Returns 0 otherwise (including if we moved towards it)
	 */
	public static int tryPickUpUnit (int targetID) throws GameActionException {
		RobotInfo ri = rc.senseRobot(targetID);
		if (here.isAdjacentTo(ri.location)) {
			Actions.doPickUpUnit(targetID);
			return 1;
		} else {
			moveLog(ri.location);
			return 0;
		}
	}
}
