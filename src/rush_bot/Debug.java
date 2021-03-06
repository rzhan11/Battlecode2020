package rush_bot;

import battlecode.common.*;

import static rush_bot.Actions.*;
import static rush_bot.Communication.*;
import static rush_bot.Debug.*;
import static rush_bot.Globals.*;
import static rush_bot.Map.*;
import static rush_bot.Nav.*;
import static rush_bot.Utils.*;
import static rush_bot.Wall.*;
import static rush_bot.Zones.*;

public class Debug extends Globals {

	public static boolean SILENCE_LOGS = false;
	public static boolean SILENCE_INDICATORS = false;

	/*
	Selectively turn off print logs for certain units
	NOTE: important messages will still be displayed
	 */
	public static boolean isDisplayLogs() {
		if (SILENCE_LOGS) {
			return false;
		}
		switch (myType) {
			case DELIVERY_DRONE: return true;
			case DESIGN_SCHOOL: return true;
			case FULFILLMENT_CENTER: return true;
			case HQ: return true;
			case LANDSCAPER: return true;
			case MINER:	return true;
			case NET_GUN: return true;
			case REFINERY: return true;
			case VAPORATOR: return true;
			default:
				logi("ERROR: Sanity check failed - unknown class " + myType);
				return false;
		}
	}

	/*
	Selectively turn off dots and lines for certain units
	 */
	public static boolean isDisplayIndicators() {
		if (SILENCE_INDICATORS) {
			return false;
		}
		switch (myType) {
			case DELIVERY_DRONE: return true;
			case DESIGN_SCHOOL: return true;
			case FULFILLMENT_CENTER: return true;
			case HQ: return true;
			case LANDSCAPER: return true;
			case MINER:	return true;
			case NET_GUN: return true;
			case REFINERY: return true;
			case VAPORATOR: return true;
			default:
				logi("ERROR: Sanity check failed - unknown class " + myType);
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

	public static void logByte (String tag) {
		if (isDisplayLogs()) {
			System.out.println("\nBYTECODE LEFT - " + tag + ": " + Clock.getBytecodesLeft());
		}
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
