package kryptonite;

import battlecode.common.*;

import static kryptonite.Constants.*;
import static kryptonite.Debug.*;
import static kryptonite.Map.*;

public class Debug extends Globals {

	public static boolean isDisplayLogs() {
		switch (myType) {
			case DELIVERY_DRONE: return true;
			case DESIGN_SCHOOL: return true;
			case FULFILLMENT_CENTER: return true;
			case HQ: return true;
			case LANDSCAPER: return false;
			case MINER:
				if (isBuilderMiner(myID)) {
					return true;
				}
				return true;
			case NET_GUN: return false;
			case REFINERY: return false;
			case VAPORATOR: return false;
			default:
				Debug.tlogi("ERROR: Sanity check failed - unknown class " + myType);
				return false;
		}
	}

	public static boolean isDisplayIndicators() {
		switch (myType) {
			case DELIVERY_DRONE: return false;
			case DESIGN_SCHOOL: return false;
			case FULFILLMENT_CENTER: return false;
			case HQ: return true;
			case LANDSCAPER: return false;
			case MINER:
				if (isBuilderMiner(myID)) {
					return false;
				}
				return true;
			case NET_GUN: return false;
			case REFINERY: return false;
			case VAPORATOR: return false;
			default:
				Debug.tlogi("ERROR: Sanity check failed - unknown class " + myType);
				return false;
		}
	}

	/*
	Prints a separator line, currently a single dash
	*/
	public static void log () {
		if (isDisplayLogs()) {
			System.out.println("\n");
		}
	}

	/*
	Prints message
	Can be turned off by setting 'DISPLAY_LOGS' to false
	*/
	public static void log (String str) {
		if (isDisplayLogs()) {
			System.out.println("\n" + str);
		}
	}

	/*
	Prints message with a single tab in front
	Can be turned off by setting 'DISPLAY_LOGS' to false
	*/
	public static void tlog (String str) {
		if (isDisplayLogs()) {
			System.out.println("\n- " + str);
		}
	}

	/*
	Prints message with a double tab in front
	Can be turned off by setting 'DISPLAY_LOGS' to true
	*/
	public static void ttlog (String str) {
		if (isDisplayLogs()) {
			System.out.println("\n-- " + str);
		}
	}

	/* (Log Important)
	Ignores the 'DISPLAY_LOGS' flag
	*/
	public static void logi () {
		System.out.println("\n");
	}

	public static void logi (String str) {
		System.out.println("\n" + str);
	}

	public static void tlogi (String str) {
		System.out.println("\n- " + str);
	}

	public static void ttlogi (String str) {
		System.out.println("\n-- " + str);
	}

	public static void drawLine(MapLocation loc1, MapLocation loc2, int[] color) {
		if (isDisplayIndicators()) {
			rc.setIndicatorLine(loc1, loc2, color[0], color[1], color[2]);
		}
	}

	public static void drawDot(MapLocation loc, int[] color) {
		if (isDisplayIndicators()) {
			rc.setIndicatorDot(loc, color[0], color[1], color[2]);
		}
	}
}
