package de.tum.cit.fop.maze.qte;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import de.tum.cit.fop.maze.game.GameConstants;
import de.tum.cit.fop.maze.utils.TextureManager;

public class QTEMazeRenderer {

    private final Texture floor;
    private final Texture wall;

    // QTE 专用视觉参数
    private static final float WALL_HEIGHT_MULTIPLIER = 1.8f;
    private static final int WALL_OVERLAP = 6;

    public QTEMazeRenderer() {
        TextureManager tm = TextureManager.getInstance();
        floor = tm.getFloorTexture();
        wall  = tm.getWallTexture();
    }

    /* =========================
       地板（固定迷宫）
       ========================= */
    public void renderFloor(SpriteBatch batch, int[][] maze) {
        for (int y = 0; y < maze.length; y++) {
            for (int x = 0; x < maze[y].length; x++) {
                if (maze[y][x] == 1) {
                    float px = x * GameConstants.CELL_SIZE;
                    float py = y * GameConstants.CELL_SIZE;
                    batch.draw(
                            floor,
                            px,
                            py,
                            GameConstants.CELL_SIZE,
                            GameConstants.CELL_SIZE
                    );
                }
            }
        }
    }

    /* =========================
       墙（高墙，宽度不变）
       ========================= */
    public void renderWall(SpriteBatch batch, int x, int y) {
        float px = x * GameConstants.CELL_SIZE;
        float py = y * GameConstants.CELL_SIZE;

        float wallHeight =
                GameConstants.CELL_SIZE * WALL_HEIGHT_MULTIPLIER;

        // 底部对齐格子，只向上长
        batch.draw(
                wall,
                px,
                py - WALL_OVERLAP,
                GameConstants.CELL_SIZE,
                wallHeight
        );
    }
}
