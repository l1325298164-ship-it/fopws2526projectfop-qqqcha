package de.tum.cit.fop.maze.utils;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.MathUtils;
import de.tum.cit.fop.maze.entities.Player;
import de.tum.cit.fop.maze.game.DifficultyConfig;
import de.tum.cit.fop.maze.game.GameConstants;

import java.util.List;

public class BossMazeCamera {

    // ===== Boss 战：禁止 padding =====
    private static final float VISUAL_Y_OFFSET =
            GameConstants.CELL_SIZE * 0.5f;

    private final OrthographicCamera camera;
    private final DifficultyConfig dc;

    // 平滑度
    private static final float SMOOTH_SPEED = 5.0f;

    // 轻微视觉摇动
    private static final float SHAKE_X = 6f;
    private static final float SHAKE_Y = 4f;

    public BossMazeCamera(OrthographicCamera camera, DifficultyConfig dc) {
        this.camera = camera;
        this.dc = dc;
    }

    /**
     * ⭐ Boss 战：跟随【所有存活玩家的中心点】
     */
    public void update(float delta, List<Player> players) {

        if (players == null || players.isEmpty()) {
            camera.update();
            return;
        }

        float sumX = 0f;
        float sumY = 0f;
        int count = 0;

        // ===== 1️⃣ 计算存活玩家中心 =====
        for (Player p : players) {
            if (p == null || p.isDead()) continue;

            sumX += p.getX() * GameConstants.CELL_SIZE
                    + GameConstants.CELL_SIZE / 2f;

            sumY += p.getY() * GameConstants.CELL_SIZE
                    + GameConstants.CELL_SIZE / 2f;

            count++;
        }

        if (count == 0) {
            camera.update();
            return;
        }

        float targetX = sumX / count;
        float targetY = sumY / count + VISUAL_Y_OFFSET;

        // ===== 2️⃣ 相机可视范围 =====
        float halfW = camera.viewportWidth  * camera.zoom / 2f;
        float halfH = camera.viewportHeight * camera.zoom / 2f;

        float mazeWorldW = dc.mazeWidth  * GameConstants.CELL_SIZE;
        float mazeWorldH = dc.mazeHeight * GameConstants.CELL_SIZE;

        // ===== 3️⃣ Boss 战 clamp（无 padding）=====
        targetX = MathUtils.clamp(targetX, halfW, mazeWorldW - halfW);
        targetY = MathUtils.clamp(targetY, halfH, mazeWorldH - halfH);

        // ===== 4️⃣ 平滑插值 =====
        float newX = MathUtils.lerp(camera.position.x, targetX, SMOOTH_SPEED * delta);
        float newY = MathUtils.lerp(camera.position.y, targetY, SMOOTH_SPEED * delta);

        // ===== 5️⃣ 轻微假摇动（纯表现）=====
        float t = (float) (System.currentTimeMillis() * 0.001);
        newX += MathUtils.sin(t * 1.3f) * SHAKE_X;
        newY += MathUtils.cos(t * 1.7f) * SHAKE_Y;

        camera.position.set(newX, newY, 0f);
        camera.update();
    }
    public void snapToPlayers(List<Player> players) {

        if (players == null || players.isEmpty()) return;

        float sumX = 0f;
        float sumY = 0f;
        int count = 0;

        for (Player p : players) {
            if (p == null || p.isDead()) continue;

            sumX += p.getX() * GameConstants.CELL_SIZE
                    + GameConstants.CELL_SIZE / 2f;
            sumY += p.getY() * GameConstants.CELL_SIZE
                    + GameConstants.CELL_SIZE / 2f;
            count++;
        }

        if (count == 0) return;

        float targetX = sumX / count;
        float targetY = sumY / count + VISUAL_Y_OFFSET;
        Gdx.app.log(
                "BOSS_CAM",
                "snap to " + targetX + "," + targetY
        );
        float halfW = camera.viewportWidth * camera.zoom / 2f;
        float halfH = camera.viewportHeight * camera.zoom / 2f;

        float mazeWorldW = dc.mazeWidth * GameConstants.CELL_SIZE;
        float mazeWorldH = dc.mazeHeight * GameConstants.CELL_SIZE;

        targetX = MathUtils.clamp(targetX, halfW, mazeWorldW - halfW);
        targetY = MathUtils.clamp(targetY, halfH, mazeWorldH - halfH);

        camera.position.set(targetX, targetY, 0f);
        camera.update();
    }

    public OrthographicCamera getCamera() {
        return camera;
    }
}
