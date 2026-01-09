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

    // ===== Camera Shake =====
    private float shakeTime = 0f;
    private float shakeDuration = 0f;
    private float shakeStrength = 0f;

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

    public void update(float deltaTime, GameManager gm) {
        if (gm == null) return;

        var players = gm.getPlayers();
        if (players == null || players.isEmpty()) return;

        float sumX = 0f;
        float sumY = 0f;
        int count = 0;

        for (Player p : players) {
            if (p == null || p.isDead()) continue;

            sumX += (p.getX() + 0.5f) * GameConstants.CELL_SIZE;
            sumY += (p.getY() + 0.5f) * GameConstants.CELL_SIZE;
            count++;
        }

        if (count == 0) return;

        targetX = sumX / count;
        targetY = sumY / count;

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


    public void update(float delta, Player player) {
    }

    public boolean isDebugZoom() {
        return debugForceZoomEnabled;
    }

    public void shake(float duration, float strength) {
        shakeDuration = duration;
        shakeTime = duration;
        shakeStrength = strength;
    }


}
