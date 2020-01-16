package kryptonite;

import battlecode.common.*;

import static kryptonite.Communication.*;
import static kryptonite.Constants.*;
import static kryptonite.Debug.*;
import static kryptonite.Map.*;

public class HardCode {

//	public static int[][][] netGunLU = new int[][][]{{{3,1},{3,2},{3,3},{2,3},{1,3}},{{-3,1},{-3,2},{-3,3},{-2,3},{-1,3}},{{-3,-1},{-3,-2},{-3,-3},{-2,-3},{-1,-3}},{{3,-1},{3,-2},{3,-3},{2,-3},{1,-3}}};
	public static int[][][] netGunLU = new int[][][]{{{3,2},{2,3}},{{-3,2},{-2,3}},{{-3,-2},{-2,-3}},{{3,-2},{2,-3}}};
	public static int[][] netGunBuildLocations = new int[][]{{2,2},{-2,2},{-2,-2},{2,-2}};

	/*
	Returns an array that contains all of the senseable dx/dy/mag positions for a given RobotType
	Sorted by distanceSquared
	*/
	public static int[][] getSenseDirections (RobotType rt) {
		switch (rt) {
			case DELIVERY_DRONE: // sensorRadiusSquared = 24
			case DESIGN_SCHOOL: // sensorRadiusSquared = 24
			case FULFILLMENT_CENTER: // sensorRadiusSquared = 24
			case LANDSCAPER: // sensorRadiusSquared = 24
			case NET_GUN: // sensorRadiusSquared = 24
			case REFINERY: // sensorRadiusSquared = 24
			case VAPORATOR: // sensorRadiusSquared = 24
				return new int[][] {{0,0,0},{-1,0,1},{0,-1,1},{0,1,1},{1,0,1},{-1,-1,2},{-1,1,2},{1,-1,2},{1,1,2},{-2,0,4},{0,-2,4},{0,2,4},{2,0,4},{-2,-1,5},{-2,1,5},{-1,-2,5},{-1,2,5},{1,-2,5},{1,2,5},{2,-1,5},{2,1,5},{-2,-2,8},{-2,2,8},{2,-2,8},{2,2,8},{-3,0,9},{0,-3,9},{0,3,9},{3,0,9},{-3,-1,10},{-3,1,10},{-1,-3,10},{-1,3,10},{1,-3,10},{1,3,10},{3,-1,10},{3,1,10},{-3,-2,13},{-3,2,13},{-2,-3,13},{-2,3,13},{2,-3,13},{2,3,13},{3,-2,13},{3,2,13},{-4,0,16},{0,-4,16},{0,4,16},{4,0,16},{-4,-1,17},{-4,1,17},{-1,-4,17},{-1,4,17},{1,-4,17},{1,4,17},{4,-1,17},{4,1,17},{-3,-3,18},{-3,3,18},{3,-3,18},{3,3,18},{-4,-2,20},{-4,2,20},{-2,-4,20},{-2,4,20},{2,-4,20},{2,4,20},{4,-2,20},{4,2,20}};
			case HQ: // sensorRadiusSquared = 48
				return new int[][] {{0,0,0},{-1,0,1},{0,-1,1},{0,1,1},{1,0,1},{-1,-1,2},{-1,1,2},{1,-1,2},{1,1,2},{-2,0,4},{0,-2,4},{0,2,4},{2,0,4},{-2,-1,5},{-2,1,5},{-1,-2,5},{-1,2,5},{1,-2,5},{1,2,5},{2,-1,5},{2,1,5},{-2,-2,8},{-2,2,8},{2,-2,8},{2,2,8},{-3,0,9},{0,-3,9},{0,3,9},{3,0,9},{-3,-1,10},{-3,1,10},{-1,-3,10},{-1,3,10},{1,-3,10},{1,3,10},{3,-1,10},{3,1,10},{-3,-2,13},{-3,2,13},{-2,-3,13},{-2,3,13},{2,-3,13},{2,3,13},{3,-2,13},{3,2,13},{-4,0,16},{0,-4,16},{0,4,16},{4,0,16},{-4,-1,17},{-4,1,17},{-1,-4,17},{-1,4,17},{1,-4,17},{1,4,17},{4,-1,17},{4,1,17},{-3,-3,18},{-3,3,18},{3,-3,18},{3,3,18},{-4,-2,20},{-4,2,20},{-2,-4,20},{-2,4,20},{2,-4,20},{2,4,20},{4,-2,20},{4,2,20},{-5,0,25},{-4,-3,25},{-4,3,25},{-3,-4,25},{-3,4,25},{0,-5,25},{0,5,25},{3,-4,25},{3,4,25},{4,-3,25},{4,3,25},{5,0,25},{-5,-1,26},{-5,1,26},{-1,-5,26},{-1,5,26},{1,-5,26},{1,5,26},{5,-1,26},{5,1,26},{-5,-2,29},{-5,2,29},{-2,-5,29},{-2,5,29},{2,-5,29},{2,5,29},{5,-2,29},{5,2,29},{-4,-4,32},{-4,4,32},{4,-4,32},{4,4,32},{-5,-3,34},{-5,3,34},{-3,-5,34},{-3,5,34},{3,-5,34},{3,5,34},{5,-3,34},{5,3,34},{-6,0,36},{0,-6,36},{0,6,36},{6,0,36},{-6,-1,37},{-6,1,37},{-1,-6,37},{-1,6,37},{1,-6,37},{1,6,37},{6,-1,37},{6,1,37},{-6,-2,40},{-6,2,40},{-2,-6,40},{-2,6,40},{2,-6,40},{2,6,40},{6,-2,40},{6,2,40},{-5,-4,41},{-5,4,41},{-4,-5,41},{-4,5,41},{4,-5,41},{4,5,41},{5,-4,41},{5,4,41},{-6,-3,45},{-6,3,45},{-3,-6,45},{-3,6,45},{3,-6,45},{3,6,45},{6,-3,45},{6,3,45}};
			case MINER: // sensorRadiusSquared = 35
				return new int[][] {{0,0,0},{-1,0,1},{0,-1,1},{0,1,1},{1,0,1},{-1,-1,2},{-1,1,2},{1,-1,2},{1,1,2},{-2,0,4},{0,-2,4},{0,2,4},{2,0,4},{-2,-1,5},{-2,1,5},{-1,-2,5},{-1,2,5},{1,-2,5},{1,2,5},{2,-1,5},{2,1,5},{-2,-2,8},{-2,2,8},{2,-2,8},{2,2,8},{-3,0,9},{0,-3,9},{0,3,9},{3,0,9},{-3,-1,10},{-3,1,10},{-1,-3,10},{-1,3,10},{1,-3,10},{1,3,10},{3,-1,10},{3,1,10},{-3,-2,13},{-3,2,13},{-2,-3,13},{-2,3,13},{2,-3,13},{2,3,13},{3,-2,13},{3,2,13},{-4,0,16},{0,-4,16},{0,4,16},{4,0,16},{-4,-1,17},{-4,1,17},{-1,-4,17},{-1,4,17},{1,-4,17},{1,4,17},{4,-1,17},{4,1,17},{-3,-3,18},{-3,3,18},{3,-3,18},{3,3,18},{-4,-2,20},{-4,2,20},{-2,-4,20},{-2,4,20},{2,-4,20},{2,4,20},{4,-2,20},{4,2,20},{-5,0,25},{-4,-3,25},{-4,3,25},{-3,-4,25},{-3,4,25},{0,-5,25},{0,5,25},{3,-4,25},{3,4,25},{4,-3,25},{4,3,25},{5,0,25},{-5,-1,26},{-5,1,26},{-1,-5,26},{-1,5,26},{1,-5,26},{1,5,26},{5,-1,26},{5,1,26},{-5,-2,29},{-5,2,29},{-2,-5,29},{-2,5,29},{2,-5,29},{2,5,29},{5,-2,29},{5,2,29},{-4,-4,32},{-4,4,32},{4,-4,32},{4,4,32},{-5,-3,34},{-5,3,34},{-3,-5,34},{-3,5,34},{3,-5,34},{3,5,34},{5,-3,34},{5,3,34}};
		}
		logi("ERROR: Failed sanity check - cannot find hardcoded senseDirections for RobotType " + rt);
		return null;
	}

	/*
	Returns an array that contains all of dx/dy/mag positions that a Net Gun/HQ can shoot
	Sorted by distanceSquared
	Current shoot radius = 15
	*/
	public static int[][] getShootDirections () {
		return new int[][] {{0,0,0},{-1,0,1},{0,-1,1},{0,1,1},{1,0,1},{-1,-1,2},{-1,1,2},{1,-1,2},{1,1,2},{-2,0,4},{0,-2,4},{0,2,4},{2,0,4},{-2,-1,5},{-2,1,5},{-1,-2,5},{-1,2,5},{1,-2,5},{1,2,5},{2,-1,5},{2,1,5},{-2,-2,8},{-2,2,8},{2,-2,8},{2,2,8},{-3,0,9},{0,-3,9},{0,3,9},{3,0,9},{-3,-1,10},{-3,1,10},{-1,-3,10},{-1,3,10},{1,-3,10},{1,3,10},{3,-1,10},{3,1,10},{-3,-2,13},{-3,2,13},{-2,-3,13},{-2,3,13},{2,-3,13},{2,3,13},{3,-2,13},{3,2,13}};
	}

}
