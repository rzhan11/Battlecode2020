package orig_rush_bot;

import battlecode.common.*;

import static orig_rush_bot.Actions.*;
import static orig_rush_bot.Communication.*;
import static orig_rush_bot.Debug.*;
import static orig_rush_bot.Globals.*;
import static orig_rush_bot.Map.*;
import static orig_rush_bot.Nav.*;
import static orig_rush_bot.Utils.*;
import static orig_rush_bot.Wall.*;
import static orig_rush_bot.Zones.*;

public class BotDeliveryDrone extends Globals {

	final public static int
			DRONE_SUPPORT_ROLE = 1,
			DRONE_HARASS_ROLE = 2,
			DRONE_ATTACK_ROLE = 3,
			DRONE_RUSH_ROLE = 4,
			DRONE_WALL_ROLE = 5;

	public static int myRole = -1;

	public static boolean isCarryingEnemy = false;
	public static MapLocation floodingMemory;

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

		myRole = DRONE_SUPPORT_ROLE;

		for (RobotInfo ri: visibleAllies) {
			if (ri.ID == rushMinerID && rc.senseRobotAtLocation(ri.location).ID == rushMinerID) {
				myRole = DRONE_RUSH_ROLE;
			}
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

		locateFlooding();
		log("floodingMemory: " + floodingMemory);

		int avoidDangerResult = Nav.avoidDanger();
		log("avoid result " + avoidDangerResult);
		if (avoidDangerResult == 1) {
			return;
		}
		if (avoidDangerResult == -1) {
			// when danger is unavoidable, reset isDirMoveable to ignore danger tiles
			updateIsDirMoveable();
		}

		if (myRole == DRONE_RUSH_ROLE) {
			BotDeliveryDroneRush.turn();
			return;
		}

		switch (myRole) {
			case DRONE_SUPPORT_ROLE:
				BotDeliveryDroneSupport.turn();
				break;
			case DRONE_HARASS_ROLE:
				BotDeliveryDroneHarass.turn();
				break;
//			case DRONE_WALL_ROLE:
//				BotDeliveryDroneWall.turn();
//				break;
//			case DRONE_ATTACK_ROLE:
//				break;
		}
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
				if (rc.onTheMap(loc) && rc.senseFlooding(loc) && rc.senseRobotAtLocation(loc) == null) {
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
						if (move != null) {
							tlog("Moved " + move);
						} else {
							tlog("But no move found");
						}
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
				if (rc.onTheMap(loc)) {
					RobotInfo ri = rc.senseRobotAtLocation(loc);
					if (ri != null && ri.team == killTeam && ri.type.canBePickedUp()) {
						Actions.doPickUpUnit(ri.ID);
						isCarryingEnemy = true;
						log("Picked up unit at " + loc);
						return true;
					}
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
	If we do not already know a visible flooded tile
		Checks visible tiles for flooding
		Saves the flooded tile to memory
	 */
	public static void locateFlooding () throws GameActionException {
		// checks if floodingMemory still exists
		if (floodingMemory != null && rc.canSenseLocation(floodingMemory)) {
			if (rc.senseFlooding(floodingMemory)) {
				log("Confirmed that floodingMemory at " + floodingMemory + " is flooded");
				return;
			} else {
				log("Resetting floodingMemory at " + floodingMemory + " since it is dry");
				floodingMemory = null;
			}
		}

		// runs if floodingMemory is not visible or is null
		// searches for a flooded tile that is empty
		for (int[] dir: senseDirections) {
			if (actualSensorRadiusSquared < dir[2]) {
				break;
			}
			MapLocation loc = here.translate(dir[0], dir[1]);
			if (rc.onTheMap(loc) && rc.senseFlooding(loc) && rc.senseRobotAtLocation(loc) == null) {
				// floodingMemory[loc.x][loc.y] = rc.senseFlooding(loc);

				log("Found visible flooded tile at " + loc);

				// if floodingMemory is null, write a Transaction
				if (floodingMemory == null) {
					writeTransactionFloodingFound(loc);
				}

				floodingMemory = loc;
				return;
			}
		}
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
