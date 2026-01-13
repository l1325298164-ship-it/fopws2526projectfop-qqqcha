package de.tum.cit.fop.maze;

import com.badlogic.gdx.Graphics;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;

public class DesktopLauncher {
    public static void main(String[] arg) {
        // 🔥 [修复] 添加全局 try-catch 以捕获启动时的资源加载错误
        try {
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
                initialWidth = Math.round(initialHeight * targetRatio);
            } else {
                initialHeight = Math.round(initialWidth / targetRatio);
            }

            // 设置窗口模式
            config.setWindowedMode(initialWidth, initialHeight);
            config.setResizable(true);
            config.setWindowSizeLimits(800, 600, -1, -1);
            config.setWindowPosition(-1, -1);

            // 性能设置
            config.useVsync(true);
            config.setForegroundFPS(60);
            config.setIdleFPS(30);

            // 启动游戏
            new Lwjgl3Application(new MazeRunnerGame(), config);
        } catch (Exception e) {
            System.err.println("❌ 游戏启动失败 (CRITICAL ERROR):");
            e.printStackTrace();
        }
    }
}