package de.tum.cit.fop.maze.utils;

import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.MathUtils;
import de.tum.cit.fop.maze.entities.Player;
import de.tum.cit.fop.maze.game.DifficultyConfig;
import de.tum.cit.fop.maze.game.GameConstants;

public class BossMazeCamera {

    // ===== Boss 战：禁止 padding（这是关键）=====
    // ❌ 不要 CAMERA_PADDING
    // ❌ 不要 CAMERA_PADDING_CELLS

    // ===== 视觉锚点修正（向上抬相机）=====
    private static final float VISUAL_Y_OFFSET =
            GameConstants.CELL_SIZE * 0.5f; // 0.5 ~ 0.7 都正常

    private final OrthographicCamera camera;
    private final DifficultyConfig dc;

    // 平滑度
    private static final float SMOOTH_SPEED = 5.0f;

    // 轻微视觉摇动（只影响表现）
    private static final float SHAKE_X = 6f;
    private static final float SHAKE_Y = 4f;

    public BossMazeCamera(OrthographicCamera camera, DifficultyConfig dc) {
        this.camera = camera;
        this.dc = dc;
    }

    public void update(float delta, Player player) {
        if (player == null) {
            camera.update();
            return;
        }

        // ===== 1️⃣ 玩家世界坐标（像素）=====
        float targetX =
                player.getX() * GameConstants.CELL_SIZE
                        + GameConstants.CELL_SIZE / 2f;

        float targetY =
                player.getY() * GameConstants.CELL_SIZE
                        + GameConstants.CELL_SIZE / 2f
                        + VISUAL_Y_OFFSET;

        // ===== 2️⃣ 计算相机可视半径 =====
        float halfW = camera.viewportWidth  * camera.zoom / 2f;
        float halfH = camera.viewportHeight * camera.zoom / 2f;

        float mazeWorldW = dc.mazeWidth  * GameConstants.CELL_SIZE;
        float mazeWorldH = dc.mazeHeight * GameConstants.CELL_SIZE;

        // ===== 3️⃣ Boss 战正确的 clamp（无 padding）=====
        targetX = MathUtils.clamp(
                targetX,
                halfW,
                mazeWorldW - halfW
        );

        targetY = MathUtils.clamp(
                targetY,
                halfH,
                mazeWorldH - halfH
        );

        // ===== 4️⃣ 平滑插值 =====
        float newX = MathUtils.lerp(camera.position.x, targetX, SMOOTH_SPEED * delta);
        float newY = MathUtils.lerp(camera.position.y, targetY, SMOOTH_SPEED * delta);

        // ===== 5️⃣ 轻微“假摇动”（只影响视觉）=====
        float t = (float) (System.currentTimeMillis() * 0.001);

        newX += MathUtils.sin(t * 1.3f) * SHAKE_X;
        newY += MathUtils.cos(t * 1.7f) * SHAKE_Y;

        camera.position.set(newX, newY, 0f);
        camera.update();
    }

    public OrthographicCamera getCamera() {
        return camera;
    }
}
