package turtle;

import battlecode.common.*;

import static turtle.Actions.*;
import static turtle.Communication.*;
import static turtle.Debug.*;
import static turtle.Globals.*;
import static turtle.Map.*;
import static turtle.Nav.*;
import static turtle.Utils.*;
import static turtle.Wall.*;
import static turtle.Zones.*;

public class Constants {
    /*
    Constants for general use
    */
    final public static int P_INF = 1000000000;
    final public static int N_INF = -1000000000;
    final public static int BIG_ARRAY_SIZE = 500;
    final public static int MAX_MAP_SIZE = 64;

    final public static int[] YELLOW = {255, 255, 0}; // moving

    final public static int[] MAGENTA = {255, 0, 255}; // mining soup
    final public static int[] BROWN = {101, 67, 33}; // depositing soup

    final public static int[] GRAY = {128, 128, 128}; // depositing soup

    final public static int[] CYAN = {0, 255, 255}; // building robots

    final public static int[] PINK = {255, 192, 203}; // picking up robot
    final public static int[] PURPLE = {128, 0, 128}; // dropping robot

    final public static int[] ORANGE = {256, 128, 0}; // shooting drone

    final public static int[] WHITE = {255, 255, 255}; // dig dirt
    final public static int[] BLACK = {0, 0, 0}; //  deposit dirt

    final public static int[] RED = {255, 0, 0};
    final public static int[] GREEN = {0, 255, 0};

}
