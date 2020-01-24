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
			DRONE_ATTACK_ROLE = 2;

	public static int myRole = -1;

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

		myRole = DRONE_SUPPORT_ROLE;

		initalizedDrone = true;

		Globals.endTurn();
		Globals.update();
	}

	public static void turn() throws GameActionException {

		if (!rc.isReady()) {
			log("Not ready");
			return;
		}

		int avoidDangerResult = Nav.avoidDanger();
		if (avoidDangerResult == 1) {
			return;
		}
		if (avoidDangerResult == -1) {
			// when danger is unavoidable, reset isDirMoveable to ignore danger tiles
			updateIsDirMoveable();
		}

		switch (myRole) {
			case DRONE_SUPPORT_ROLE:
				BotDeliveryDroneSupport.turn();
				break;
			case DRONE_ATTACK_ROLE:
				break;
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
