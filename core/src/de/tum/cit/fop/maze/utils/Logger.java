// Logger.java
package de.tum.cit.fop.maze.utils;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Gdx;

public class Logger {
    private static final String TAG = "MazeGame";

    public static void info(String message) {
        Gdx.app.log(TAG, "[INFO] " + message);
    }

    public static void debug(String message) {
        Gdx.app.debug(TAG, "[DEBUG] " + message);
    }

    public static void error(String message) {
        Gdx.app.error(TAG, "[ERROR] " + message);
    }

    public static void error(String message, Throwable exception) {
        Gdx.app.error(TAG, "[ERROR] " + message, exception);
    }

    public static void warning(String message) {
        Gdx.app.log(TAG, "[WARNING] " + message);
    }

    // 游戏特定日志
    public static void gameEvent(String event) {
        Gdx.app.log("GameEvent", event);
    }

    public static void performance(String message) {
        if (Gdx.app.getLogLevel() >= Application.LOG_DEBUG) {
            Gdx.app.debug("Performance", message);
        }
    }

    public static boolean isDebugEnabled() {
        return Gdx.app.getLogLevel() >= Application.LOG_DEBUG;
    }

    public static void debugFrame(String message) {
        // 只在特定帧数记录调试信息，避免日志过多
        if (Gdx.graphics.getFrameId() % 60 == 0 && isDebugEnabled()) {
            debug(message);
        }
    }
}
