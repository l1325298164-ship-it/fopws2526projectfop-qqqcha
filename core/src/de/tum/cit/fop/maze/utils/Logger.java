package de.tum.cit.fop.maze.utils;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Gdx;

public class Logger {
    private static final String TAG = "MazeGame";
    private static boolean forceDebug = true; // 强制开启调试模式

    public static void debug(String message) {
        if (forceDebug || Gdx.app.getLogLevel() >= Application.LOG_DEBUG) {
            System.out.println("[DEBUG] " + message); // 同时输出到控制台
            Gdx.app.debug(TAG, message);
        }
    }

    public static void info(String message) {
        Gdx.app.log(TAG, message);
    }

    public static void error(String message) {
        Gdx.app.error(TAG, message);
    }

    public static void error(String message, Throwable exception) {
        Gdx.app.error(TAG, message, exception);
    }

    public static void warning(String message) {
        Gdx.app.log(TAG, message);
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
        return forceDebug || Gdx.app.getLogLevel() >= Application.LOG_DEBUG;
    }

    // 专门用于无尽模式的调试
    public static void endless(String message) {
        debug("[ENDLESS] " + message);
    }
}