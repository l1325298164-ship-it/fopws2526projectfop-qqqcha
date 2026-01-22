package de.tum.cit.fop.maze.utils;

import com.badlogic.gdx.graphics.OrthographicCamera;
import de.tum.cit.fop.maze.entities.Player;
import de.tum.cit.fop.maze.game.DifficultyConfig;
import de.tum.cit.fop.maze.game.GameConstants;
import de.tum.cit.fop.maze.game.GameManager;

public class CameraManager {
    // 🔥 新增：单例实例
    private static CameraManager instance;

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

    // ===== Camera Shake =====
    private float shakeTime = 0f;
    private float shakeDuration = 0f;
    private float shakeStrength = 0f;

    // 🔥 新增：获取单例的方法
    public static CameraManager getInstance() {
        return instance;
    }

    public CameraManager(DifficultyConfig difficultyConfig) {
        // 🔥 新增：赋值单例
        instance = this;

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

    public void update(float deltaTime, GameManager gm) {
        if (gm == null) return;

        var players = gm.getPlayers();
        if (players == null || players.isEmpty()) return;

        float sumX = 0f;
        float sumY = 0f;
        int count = 0;

        for (Player p : players) {
            if (p == null || p.isDead()) continue;

            float px = (p.getX() + 0.5f) * GameConstants.CELL_SIZE;
            float py = (p.getY() + 0.5f) * GameConstants.CELL_SIZE;

            sumX += px;
            sumY += py;
            count++;
        }

        if (count == 0) return;

        targetX = sumX / count;
        targetY = sumY / count;

        // ===== clamp 到地图 =====
        if (clampToMap) {
            targetX = Math.max(
                    camera.viewportWidth / 2f,
                    Math.min(
                            difficultyConfig.mazeWidth * GameConstants.CELL_SIZE - camera.viewportWidth / 2f,
                            targetX
                    )
            );
            targetY = Math.max(
                    camera.viewportHeight / 2f,
                    Math.min(
                            difficultyConfig.mazeHeight * GameConstants.CELL_SIZE - camera.viewportHeight / 2f,
                            targetY
                    )
            );
        }

        // ===== 平滑跟随 =====
        float currentX = camera.position.x;
        float currentY = camera.position.y;

        float newX = currentX + (targetX - currentX) * smoothSpeed * deltaTime;
        float newY = currentY + (targetY - currentY) * smoothSpeed * deltaTime;

        // 🔥 新增：应用震动偏移 (从 QTE update 方法中移植过来的)
        if (shakeTime > 0f) {
            shakeTime -= deltaTime;
            // 计算震动衰减 (progress 1.0 -> 0.0)
            float progress = shakeTime / shakeDuration;

            // 生成随机偏移
            float offsetX = (float)(Math.random() * 2 - 1) * shakeStrength * progress;
            float offsetY = (float)(Math.random() * 2 - 1) * shakeStrength * progress;

            newX += offsetX;
            newY += offsetY;
        }

        camera.position.set(newX, newY, 0);
        camera.update();
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

    public void setDebugZoom(float zoom) {
        debugForcedZoom = zoom;
        debugForceZoomEnabled = true;
    }

    public void clearDebugZoom() {
        debugForceZoomEnabled = false;
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

        if (shakeTime > 0f) {
            shakeTime -= deltaTime;
            float progress = shakeTime / shakeDuration;

            float offsetX = (float)(Math.random() * 2 - 1) * shakeStrength * progress;
            float offsetY = (float)(Math.random() * 2 - 1) * shakeStrength * progress;

            newX += offsetX;
            newY += offsetY;
        }

        camera.position.set(newX, newY, 0);
        camera.update();
    }
    public void disableFreeTarget() {
        useFreeTarget = false;
    }

    public boolean isDebugZoom() {
        return debugForceZoomEnabled;
    }

    /**
     * 触发屏幕震动
     * @param duration 持续时间 (秒)
     * @param strength 震动强度 (像素)
     */
    public void shake(float duration, float strength) {
        shakeDuration = duration;
        shakeTime = duration;
        shakeStrength = strength;
    }
}