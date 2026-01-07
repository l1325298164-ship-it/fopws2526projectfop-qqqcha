package de.tum.cit.fop.maze.utils;

public class Logger {

    private static boolean DEBUG_ENABLED = false;   // 默认关闭

    public static void toggleDebug() {
        DEBUG_ENABLED = !DEBUG_ENABLED;
        System.out.println("DEBUG MODE = " + DEBUG_ENABLED);
    }

    public static boolean isDebugEnabled() {
        return DEBUG_ENABLED;
    }

    public static void debug(String message) {
        if (DEBUG_ENABLED) {
            System.out.println("[DEBUG] " + message);
        }
    }

    public static void info(String message) {
        System.out.println("[INFO] " + message);
    }

    public static void warning(String message) {
        System.out.println("[WARNING] " + message);
    }

    public static void error(String message) {
        System.err.println("[ERROR] " + message);
    }

    public static void gameEvent(String event) {
        System.out.println("[GameEvent] " + event);
    }
}
