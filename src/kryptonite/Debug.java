package kryptonite;

import battlecode.common.*;

public class Debug extends Globals {

	final private static boolean DISPLAY_LOGS = true;

	/*
	Prints a separator line, currently a single dash
	*/
	public static void log () {
		if (DISPLAY_LOGS) {
			System.out.println("\n");
		}
	}

	/*
	Prints message
	Can be turned off by setting 'DISPLAY_LOGS' to false
	*/
	public static void log (String str) {
		if (DISPLAY_LOGS) {
			System.out.println("\n" + str);
		}
	}

	/*
	Prints message with a single tab in front
	Can be turned off by setting 'DISPLAY_LOGS' to false
	*/
	public static void tlog (String str) {
		if (DISPLAY_LOGS) {
			System.out.println("\n-" + str);
		}
	}

	/*
	Prints message with a double tab in front
	Can be turned off by setting 'DISPLAY_LOGS' to true
	*/
	public static void ttlog (String str) {
		if (DISPLAY_LOGS) {
			System.out.println("\n--" + str);
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
		System.out.println("\n-" + str);
	}

	public static void ttlogi (String str) {
		System.out.println("\n--" + str);
	}
}
