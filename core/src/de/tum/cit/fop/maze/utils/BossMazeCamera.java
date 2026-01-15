package de.tum.cit.fop.maze.utils;

import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.MathUtils;
import de.tum.cit.fop.maze.entities.Player;
import de.tum.cit.fop.maze.game.DifficultyConfig;
import de.tum.cit.fop.maze.game.GameConstants;

public class BossMazeCamera {

    private static final float CAMERA_PADDING_CELLS = 12f; // ← 已调整
    private static final float CAMERA_PADDING =
            CAMERA_PADDING_CELLS * GameConstants.CELL_SIZE;
    // ===== 视觉锚点修正（向上抬相机）=====
    private static final float VISUAL_Y_OFFSET = GameConstants.CELL_SIZE * 0.5f;
// 如果还不够：0.6f / 0.7f 都很正常

    private final OrthographicCamera camera;
    private final DifficultyConfig dc;

    // 平滑度（和 CameraManager 风格一致）
    private float smoothSpeed = 5.0f;

    // 茶杯摇动
    private float shakeStrengthX = 6f;
    private float shakeStrengthY = 4f;

    public BossMazeCamera(OrthographicCamera camera, DifficultyConfig dc) {
        this.camera = camera;
        this.dc = dc;
    }

    public void update(float delta, Player player) {


        if (player == null) {
            camera.update();
            return;
        }

        // ===== 1️⃣ 玩家「像素坐标」（关键修复）=====
        float targetX =
                player.getX() * GameConstants.CELL_SIZE
                        + GameConstants.CELL_SIZE / 2f;
        float targetY =
                player.getY() * GameConstants.CELL_SIZE
                        + GameConstants.CELL_SIZE / 2f
                        + VISUAL_Y_OFFSET;

        // ===== 2️⃣ clamp 到迷宫边界 =====
        float halfW = camera.viewportWidth * camera.zoom / 2f;
        float halfH = camera.viewportHeight * camera.zoom / 2f;

        float mazeWorldW = dc.mazeWidth * GameConstants.CELL_SIZE;
        float mazeWorldH = dc.mazeHeight * GameConstants.CELL_SIZE;

        float minX = halfW - CAMERA_PADDING;
        float maxX = mazeWorldW - halfW + CAMERA_PADDING;

        float minY = halfH - CAMERA_PADDING;
        float maxY = mazeWorldH - halfH + CAMERA_PADDING;

        targetX = MathUtils.clamp(targetX, minX, maxX);
        targetY = MathUtils.clamp(targetY, minY, maxY);

        // ===== 3️⃣ 平滑跟随（和 CameraManager 同模型）=====
        float newX = camera.position.x + (targetX - camera.position.x) * smoothSpeed * delta;
        float newY = camera.position.y + (targetY - camera.position.y) * smoothSpeed * delta;

        // ===== 4️⃣ 茶杯“假摇动”（只影响视觉）=====
        float t = (float)(System.currentTimeMillis() * 0.001);
        newX += MathUtils.sin(t * 1.3f) * shakeStrengthX;
        newY += MathUtils.cos(t * 1.7f) * shakeStrengthY;

        camera.position.set(newX, newY, 0);
        camera.update();
    }

    public OrthographicCamera getCamera() {
        return camera;
    }
}
