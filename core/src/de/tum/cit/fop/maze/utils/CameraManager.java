// CameraManager.java
package de.tum.cit.fop.maze.utils;

import com.badlogic.gdx.graphics.OrthographicCamera;
import de.tum.cit.fop.maze.entities.Player;
import de.tum.cit.fop.maze.game.GameConstants;



public class CameraManager {
    private OrthographicCamera camera;
    private float targetX, targetY;
    private float smoothSpeed = 5.0f; // 相机跟随的平滑度
    // ===== QTE / 自由目标支持 =====
    private boolean useFreeTarget = false;
    private float freeTargetX;
    private float freeTargetY;



    public CameraManager() {
        camera = new OrthographicCamera();
        camera.setToOrtho(false, GameConstants.VIEWPORT_WIDTH, GameConstants.VIEWPORT_HEIGHT);
        Logger.debug("CameraManager initialized");
    }

    public void update(float deltaTime, Player player) {
        if (player == null) return;

        // 计算玩家在像素坐标中的位置
        float playerPixelX = player.getX() * GameConstants.CELL_SIZE + GameConstants.CELL_SIZE / 2;
        float playerPixelY = player.getY() * GameConstants.CELL_SIZE + GameConstants.CELL_SIZE / 2;

        // 设置相机目标位置为玩家位置
        targetX = playerPixelX;
        targetY = playerPixelY;

        // 限制相机范围，使其不超出地图边界
        targetX = Math.max(GameConstants.MIN_CAMERA_X, Math.min(GameConstants.MAX_CAMERA_X, targetX));
        targetY = Math.max(GameConstants.MIN_CAMERA_Y, Math.min(GameConstants.MAX_CAMERA_Y, targetY));

        // 平滑移动相机
        float currentX = camera.position.x;
        float currentY = camera.position.y;

        // 使用线性插值实现平滑跟随
        float newX = currentX + (targetX - currentX) * smoothSpeed * deltaTime;
        float newY = currentY + (targetY - currentY) * smoothSpeed * deltaTime;

        // 更新相机位置
        camera.position.set(newX, newY, 0);
        camera.update();

        // 调试日志（减少日志输出频率）
        if (Logger.isDebugEnabled()) {
            Logger.debug(String.format("Camera: (%.1f, %.1f) -> Player: (%.1f, %.1f)",
                newX, newY, playerPixelX, playerPixelY));
        }
    }

    public void centerOnPlayerImmediately(Player player) {
        if (player == null) return;

        float playerPixelX = player.getX() * GameConstants.CELL_SIZE + GameConstants.CELL_SIZE / 2;
        float playerPixelY = player.getY() * GameConstants.CELL_SIZE + GameConstants.CELL_SIZE / 2;

        // 限制相机范围
        playerPixelX = Math.max(GameConstants.MIN_CAMERA_X, Math.min(GameConstants.MAX_CAMERA_X, playerPixelX));
        playerPixelY = Math.max(GameConstants.MIN_CAMERA_Y, Math.min(GameConstants.MAX_CAMERA_Y, playerPixelY));

        camera.position.set(playerPixelX, playerPixelY, 0);
        camera.update();

        Logger.debug("Camera immediately centered on player");
    }

    public void setSmoothSpeed(float speed) {
        this.smoothSpeed = Math.max(1.0f, Math.min(20.0f, speed));
    }

    public OrthographicCamera getCamera() {
        return camera;
    }

    public void resize(int width, int height) {
        // 保持宽高比，根据窗口大小调整视口
        float aspectRatio = (float) width / height;

        if (aspectRatio > GameConstants.VIEWPORT_WIDTH / GameConstants.VIEWPORT_HEIGHT) {
            // 窗口比游戏宽，调整宽度
            camera.viewportWidth = GameConstants.VIEWPORT_HEIGHT * aspectRatio;
            camera.viewportHeight = GameConstants.VIEWPORT_HEIGHT;
        } else {
            // 窗口比游戏高，调整高度
            camera.viewportWidth = GameConstants.VIEWPORT_WIDTH;
            camera.viewportHeight = GameConstants.VIEWPORT_WIDTH / aspectRatio;
        }

        camera.update();
        Logger.debug(String.format("Camera resized to: %.0fx%.0f",
            camera.viewportWidth, camera.viewportHeight));
    }
    // 给 QTE 用：直接指定相机目标点
    public void setTarget(float x, float y) {
        this.freeTargetX = x;
        this.freeTargetY = y;
        this.useFreeTarget = true;
    }
    // QTE 用的 update（没有 Player）
    public void update(float deltaTime) {
        if (!useFreeTarget) return;

        targetX = freeTargetX;
        targetY = freeTargetY;

        // 限制相机范围
        targetX = Math.max(GameConstants.MIN_CAMERA_X,
                Math.min(GameConstants.MAX_CAMERA_X, targetX));
        targetY = Math.max(GameConstants.MIN_CAMERA_Y,
                Math.min(GameConstants.MAX_CAMERA_Y, targetY));

        float currentX = camera.position.x;
        float currentY = camera.position.y;

        float newX = currentX + (targetX - currentX) * smoothSpeed * deltaTime;
        float newY = currentY + (targetY - currentY) * smoothSpeed * deltaTime;

        camera.position.set(newX, newY, 0);
        camera.update();
    }
    public void disableFreeTarget() {
        useFreeTarget = false;
    }


}
