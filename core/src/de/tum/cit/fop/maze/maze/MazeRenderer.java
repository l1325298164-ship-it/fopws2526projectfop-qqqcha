// MazeRenderer.java —— 仅供 GameScreen 使用
package de.tum.cit.fop.maze.maze;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import de.tum.cit.fop.maze.game.GameConstants;
import de.tum.cit.fop.maze.game.GameManager;
import de.tum.cit.fop.maze.utils.Logger;
import de.tum.cit.fop.maze.utils.TextureManager;

public class MazeRenderer {

    private GameManager gameManager;
    private TextureManager textureManager;

    private Texture floorTexture;
    private Texture wallTexture;
    private boolean texturesReady = false;

    // 仅 GameScreen 用的墙参数
    private float wallHeightMultiplier = 1.5f;
    private int wallOverlap = 6;

    public MazeRenderer(GameManager gameManager) {
        this.gameManager = gameManager;
        this.textureManager = TextureManager.getInstance();
    }

    /* =========================
       内部：纹理初始化
       ========================= */
    private void ensureTextures() {
        if (!texturesReady) {
            floorTexture = textureManager.getFloorTexture();
            wallTexture  = textureManager.getWallTexture();
            texturesReady = true;
            Logger.debug("MazeRenderer textures loaded: " +
                    textureManager.getCurrentMode());
        }
    }

    /* =========================
       地板（整张迷宫）
       ========================= */
    public void renderFloor(SpriteBatch batch) {
        ensureTextures();

        int[][] maze = gameManager.getMazeForRendering();
        int size = GameConstants.CELL_SIZE;

        for (int y = 0; y < maze.length; y++) {
            for (int x = 0; x < maze[y].length; x++) {
                if (maze[y][x] == 1) {
                    batch.draw(
                            floorTexture,
                            x * size,
                            y * size,
                            size,
                            size
                    );
                }
            }
        }
    }

    /* =========================
       单个墙（由 GameScreen 决定何时画）
       ========================= */
    public void renderWallAtPosition(SpriteBatch batch, int x, int y) {
        ensureTextures();

        int[][] maze = gameManager.getMazeForRendering();
        if (maze[y][x] != 0) return;

        float size = GameConstants.CELL_SIZE;
        float px = x * size;
        float py = y * size;

        float wallHeight = size * wallHeightMultiplier;

        // 只向上长，保证遮挡关系稳定
        batch.draw(
                wallTexture,
                px,
                py - wallOverlap,
                size,
                wallHeight
        );
    }

    /* =========================
       纹理模式切换
       ========================= */
    public void onTextureModeChanged() {
        texturesReady = false;
        floorTexture = null;
        wallTexture = null;
        Logger.debug("MazeRenderer texture cache cleared");
    }

    public void setGameManager(GameManager gameManager) {
        this.gameManager = gameManager;
        this.texturesReady = false;
    }
}
