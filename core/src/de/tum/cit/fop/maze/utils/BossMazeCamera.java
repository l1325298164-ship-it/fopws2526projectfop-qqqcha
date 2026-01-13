package de.tum.cit.fop.maze.utils;

import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.MathUtils;
import de.tum.cit.fop.maze.entities.Player;
import de.tum.cit.fop.maze.game.DifficultyConfig;
import de.tum.cit.fop.maze.game.GameConstants;

public class BossMazeCamera {

    private final OrthographicCamera camera;

    // Arena 边界（世界坐标）
    private final float minX, maxX, minY, maxY;

    // zoom 控制
    private float targetZoom = 1.3f;

    public BossMazeCamera(OrthographicCamera camera, DifficultyConfig dc) {
        this.camera = camera;

        // Arena 边界（用迷宫尺寸即可）
        this.minX = 0;
        this.minY = 0;
        this.maxX = dc.mazeWidth * GameConstants.CELL_SIZE;
        this.maxY = dc.mazeHeight * GameConstants.CELL_SIZE;
    }

    public void setTargetZoom(float zoom) {
        this.targetZoom = zoom;
    }

    public void update(float delta, Player player) {
        // ===== 平滑跟随 =====
        camera.position.x = MathUtils.lerp(
                camera.position.x,
                player.getX(),
                0.08f
        );

        camera.position.y = MathUtils.lerp(
                camera.position.y,
                player.getY(),
                0.04f
        );

        // ===== zoom 插值 =====
        camera.zoom = MathUtils.lerp(
                camera.zoom,
                targetZoom,
                0.05f
        );

        // ===== Arena Clamp =====
        float halfW = camera.viewportWidth * camera.zoom * 0.5f;
        float halfH = camera.viewportHeight * camera.zoom * 0.5f;

        camera.position.x = MathUtils.clamp(
                camera.position.x,
                minX + halfW,
                maxX - halfW
        );

        camera.position.y = MathUtils.clamp(
                camera.position.y,
                minY + halfH,
                maxY - halfH
        );

        camera.update();
    }
}
