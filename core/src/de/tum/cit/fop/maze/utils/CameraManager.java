// CameraManager.java
package de.tum.cit.fop.maze.utils;

import com.badlogic.gdx.graphics.OrthographicCamera;
import de.tum.cit.fop.maze.entities.Player;
import de.tum.cit.fop.maze.game.GameConstants;

public class CameraManager {
    private OrthographicCamera camera;
    private float targetX, targetY;
    private float smoothSpeed = 5.0f;

    // 存储实际视口大小
    private float actualViewportWidth;
    private float actualViewportHeight;

    // 存储基础视口大小（游戏逻辑尺寸）
    private float baseViewportWidth = GameConstants.VIEWPORT_WIDTH;
    private float baseViewportHeight = GameConstants.VIEWPORT_HEIGHT;

    public CameraManager() {
        camera = new OrthographicCamera();
        // 在初始化时立即应用正确的视口计算
        initializeViewportWithProperAspectRatio();
        Logger.debug("CameraManager initialized");
    }

    /**
     * 在初始化时应用正确的视口计算
     * 使用与resize()相同的逻辑
     */
    private void initializeViewportWithProperAspectRatio() {
        // 假设初始宽高比为16:9（这是常见的游戏比例）
        float initialAspectRatio = baseViewportWidth / baseViewportHeight;

        // 使用与resize()完全相同的逻辑
        if (initialAspectRatio > baseViewportWidth / baseViewportHeight) {
            // 这个条件通常为真，因为我们使用相同的宽高比
            actualViewportWidth = baseViewportHeight * initialAspectRatio;
            actualViewportHeight = baseViewportHeight;
        } else {
            actualViewportWidth = baseViewportWidth;
            actualViewportHeight = baseViewportWidth / initialAspectRatio;
        }

        // 设置相机视口
        camera.setToOrtho(false, actualViewportWidth, actualViewportHeight);

        // 初始位置设置为地图中心
        float initialX = Math.max(
            actualViewportWidth / 2,
            Math.min(
                GameConstants.MAZE_WIDTH * GameConstants.CELL_SIZE - actualViewportWidth / 2,
                (GameConstants.MAZE_WIDTH * GameConstants.CELL_SIZE) / 2
            )
        );

        float initialY = Math.max(
            actualViewportHeight / 2,
            Math.min(
                GameConstants.MAZE_HEIGHT * GameConstants.CELL_SIZE - actualViewportHeight / 2,
                (GameConstants.MAZE_HEIGHT * GameConstants.CELL_SIZE) / 2
            )
        );

        camera.position.set(initialX, initialY, 0);
        camera.update();

        Logger.debug(String.format("Viewport initialized: %.0fx%.0f",
            actualViewportWidth, actualViewportHeight));
        Logger.debug(String.format("Camera initialized at: (%.1f, %.1f)",
            camera.position.x, camera.position.y));
    }

    public void update(float deltaTime, Player player) {
        if (player == null) return;

        // 计算玩家在像素坐标中的位置
        float playerPixelX = player.getX() * GameConstants.CELL_SIZE + GameConstants.CELL_SIZE / 2;
        float playerPixelY = player.getY() * GameConstants.CELL_SIZE + GameConstants.CELL_SIZE / 2;

        // 设置相机目标位置为玩家位置
        targetX = playerPixelX;
        targetY = playerPixelY;

        // 动态计算相机边界（基于实际视口大小）
        float minCameraX = actualViewportWidth / 2;
        float minCameraY = actualViewportHeight / 2;
        float maxCameraX = GameConstants.MAZE_WIDTH * GameConstants.CELL_SIZE - actualViewportWidth / 2;
        float maxCameraY = GameConstants.MAZE_HEIGHT * GameConstants.CELL_SIZE - actualViewportHeight / 2;

        // 限制相机范围
        targetX = Math.max(minCameraX, Math.min(maxCameraX, targetX));
        targetY = Math.max(minCameraY, Math.min(maxCameraY, targetY));

        // 平滑移动相机
        float currentX = camera.position.x;
        float currentY = camera.position.y;

        float newX = currentX + (targetX - currentX) * smoothSpeed * deltaTime;
        float newY = currentY + (targetY - currentY) * smoothSpeed * deltaTime;

        camera.position.set(newX, newY, 0);
        camera.update();
    }

    public void centerOnPlayerImmediately(Player player) {
        if (player == null) return;

        float playerPixelX = player.getX() * GameConstants.CELL_SIZE + GameConstants.CELL_SIZE / 2;
        float playerPixelY = player.getY() * GameConstants.CELL_SIZE + GameConstants.CELL_SIZE / 2;

        // 动态计算相机边界
        float minCameraX = actualViewportWidth / 2;
        float minCameraY = actualViewportHeight / 2;
        float maxCameraX = GameConstants.MAZE_WIDTH * GameConstants.CELL_SIZE - actualViewportWidth / 2;
        float maxCameraY = GameConstants.MAZE_HEIGHT * GameConstants.CELL_SIZE - actualViewportHeight / 2;

        playerPixelX = Math.max(minCameraX, Math.min(maxCameraX, playerPixelX));
        playerPixelY = Math.max(minCameraY, Math.min(maxCameraY, playerPixelY));

        camera.position.set(playerPixelX, playerPixelY, 0);
        camera.update();
    }

    public void resize(int width, int height) {
        // 保持宽高比
        float aspectRatio = (float) width / height;

        if (aspectRatio > baseViewportWidth / baseViewportHeight) {
            actualViewportWidth = baseViewportHeight * aspectRatio;
            actualViewportHeight = baseViewportHeight;
        } else {
            actualViewportWidth = baseViewportWidth;
            actualViewportHeight = baseViewportWidth / aspectRatio;
        }

        camera.viewportWidth = actualViewportWidth;
        camera.viewportHeight = actualViewportHeight;
        camera.update();

        Logger.debug(String.format("Camera resized: %.0fx%.0f",
            actualViewportWidth, actualViewportHeight));
    }

    public void setSmoothSpeed(float speed) {
        this.smoothSpeed = Math.max(1.0f, Math.min(20.0f, speed));
    }

    public OrthographicCamera getCamera() {
        return camera;
    }

    // 获取实际视口大小（调试用）
    public float getActualViewportWidth() {
        return actualViewportWidth;
    }

    public float getActualViewportHeight() {
        return actualViewportHeight;
    }
}
