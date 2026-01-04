// CameraManager.java
package de.tum.cit.fop.maze.utils;

import com.badlogic.gdx.graphics.OrthographicCamera;
import de.tum.cit.fop.maze.entities.Player;
import de.tum.cit.fop.maze.game.DifficultyConfig;
import de.tum.cit.fop.maze.game.GameConstants;
import de.tum.cit.fop.maze.game.GameManager;



public class CameraManager {
    private OrthographicCamera camera;
    private float targetX, targetY;
    private float smoothSpeed = 5.0f; // 相机跟随的平滑度
    private float baseZoom = 1.0f;
    // ===== QTE / 自由目标支持 =====
    private boolean useFreeTarget = false;
    private float freeTargetX;
    private float freeTargetY;
    private final DifficultyConfig difficultyConfig;
    private boolean debugForceZoomEnabled = false;
    private float debugForcedZoom = 1.0f;

    public void setDebugZoom(float zoom) {
        debugForcedZoom = zoom;
        debugForceZoomEnabled = true;
    }

    public void clearDebugZoom() {
        debugForceZoomEnabled = false;
    }


    public CameraManager(DifficultyConfig difficultyConfig) {
        this.difficultyConfig = difficultyConfig;
        camera = new OrthographicCamera();
        Logger.debug("CameraManager initialized");
        this.baseZoom = camera.zoom;
    }
    //for tutorial
    private boolean clampToMap = true;
    private boolean tutorialMode = false;
    public void setClampToMap(boolean enabled) {
        this.clampToMap = enabled;
    }
    public void setTutorialMode(boolean tutorial) {
        this.tutorialMode = tutorial;
    }

    public void update(float deltaTime, Player player, GameManager gm) {
        if (player == null) return;

        // ==========================================
        // 🔥 [Console] 动态缩放逻辑
        // ==========================================
        float zoomMult = 1.0f;
        if (gm != null) {
            // 读取 "cam_zoom" 变量，如果没有设过默认是 1.0
            zoomMult = gm.getVariable("cam_zoom");
        }
        // 设置实际缩放 = 基础值 * 倍率
        if (debugForceZoomEnabled) {
            camera.zoom = debugForcedZoom;
        } else {
            camera.zoom = baseZoom * zoomMult;  // 原来的
        }


        // 计算玩家在像素坐标中的位置
        float playerPixelX = player.getX() * GameConstants.CELL_SIZE + GameConstants.CELL_SIZE / 2;
        float playerPixelY = player.getY() * GameConstants.CELL_SIZE + GameConstants.CELL_SIZE / 2;

        // 设置相机目标位置为玩家位置
        targetX = playerPixelX;
        targetY = playerPixelY;
        if (clampToMap) {
            // 限制相机范围，使其不超出地图边界
            targetX = Math.max(camera.viewportWidth / 2f, Math.min(difficultyConfig.mazeWidth * GameConstants.CELL_SIZE - camera.viewportWidth / 2f, targetX));
            targetY = Math.max(camera.viewportHeight / 2f, Math.min(difficultyConfig.mazeHeight * GameConstants.CELL_SIZE - camera.viewportHeight / 2f, targetY));
        }
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
        if (clampToMap) {
            // 限制相机范围
            playerPixelX = Math.max(camera.viewportWidth / 2f, Math.min(difficultyConfig.mazeWidth * GameConstants.CELL_SIZE - camera.viewportWidth / 2f, playerPixelX));
            playerPixelY = Math.max(camera.viewportHeight / 2f, Math.min(difficultyConfig.mazeHeight * GameConstants.CELL_SIZE - camera.viewportHeight / 2f, playerPixelY));
        }
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
        targetX = Math.max(camera.viewportWidth / 2f,
                Math.min(difficultyConfig.mazeWidth * GameConstants.CELL_SIZE - camera.viewportWidth / 2f, targetX));
        targetY = Math.max(camera.viewportHeight / 2f,
                Math.min(difficultyConfig.mazeHeight * GameConstants.CELL_SIZE - camera.viewportHeight / 2f, targetY));

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


    public void update(float delta, Player player) {
    }

    public boolean isDebugZoom() {
        return debugForceZoomEnabled;
    }

}
