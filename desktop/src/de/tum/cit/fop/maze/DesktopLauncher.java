package de.tum.cit.fop.maze;

import com.badlogic.gdx.Graphics;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;

public class DesktopLauncher {
    public static void main(String[] arg) {
        Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
        config.setTitle("Maze Runner");

        // 获取屏幕信息
        Graphics.DisplayMode displayMode = Lwjgl3ApplicationConfiguration.getDisplayMode();
        int screenWidth = displayMode.width;
        int screenHeight = displayMode.height;

        // 计算初始窗口大小（屏幕的80%）
        int initialWidth = Math.round(0.8f * screenWidth);
        int initialHeight = Math.round(0.8f * screenHeight);

        // 保持16:9的宽高比
        float targetRatio = 16f / 9f;
        float currentRatio = (float) initialWidth / initialHeight;

        if (currentRatio > targetRatio) {
            // 太宽了，调整宽度
            initialWidth = Math.round(initialHeight * targetRatio);
        } else {
            // 太高了，调整高度
            initialHeight = Math.round(initialWidth / targetRatio);
        }

        // 设置窗口模式
        config.setWindowedMode(initialWidth, initialHeight);

        // 允许用户调整窗口大小
        config.setResizable(true);

        // 设置窗口最小尺寸（防止太小）
        config.setWindowSizeLimits(800, 600, -1, -1);

        // 窗口居中显示
        config.setWindowPosition(-1, -1);

        // 性能设置
        config.useVsync(true);
        config.setForegroundFPS(60);
        config.setIdleFPS(30); // 窗口失去焦点时降低FPS节省资源

        // 可选：设置窗口图标（如果有的话）
        // config.setWindowIcon("icon_128.png", "icon_64.png", "icon_32.png");

        // 启动游戏
        new Lwjgl3Application(new MazeRunnerGame(), config);
    }
}