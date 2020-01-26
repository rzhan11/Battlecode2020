package landscaper_rush_bot;

public class Constants {
    /*
    Constants for general use
    */
    final public static int P_INF = 1000000000;
    final public static int N_INF = -1000000000;
    final public static int BIG_ARRAY_SIZE = 500;
    final public static int MAX_MAP_SIZE = 64;

    public static int[] YELLOW = {255, 255, 0}; // moving
    public static int[] MAGENTA = {255, 0, 255}; // picking up robot
    public static int[] PURPLE = {128, 0, 128}; // dropping robot
    public static int[] ORANGE = {256, 128, 0}; // shooting drone
    public static int[] WHITE = {255, 255, 255}; // drone explore symmetry
    // white also used for dig dirt
    public static int[] BLACK = {0, 0, 0}; // drone targetting HQLocation
    // black also used for deposit dirt
    public static int[] CYAN = {0, 255, 255}; // drone explore symmetry
    public static int[] BROWN = {101, 67, 33}; // builderminer
}
