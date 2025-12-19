// MazeRenderer.java - 修改版本，只负责绘制基础元素
package de.tum.cit.fop.maze.maze;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import de.tum.cit.fop.maze.game.GameConstants;
import de.tum.cit.fop.maze.game.GameManager;
import de.tum.cit.fop.maze.utils.Logger;
import de.tum.cit.fop.maze.utils.TextureManager;

public class MazeRenderer {
    private GameManager gameManager;
    private TextureManager textureManager;

    // 纹理缓存
    private com.badlogic.gdx.graphics.Texture floorTexture;
    private com.badlogic.gdx.graphics.Texture wallTexture;
    private boolean texturesInitialized = false;

    // 渲染尺寸控制
    private float wallScaleFactor = 1.2f;
    private float wallHeightMultiplier = 1.5f;
    private int wallOverlap = 5;

    public MazeRenderer(GameManager gameManager) {
        this.gameManager = gameManager;
        this.textureManager = TextureManager.getInstance();
        Logger.debug("MazeRenderer initialized");
    }

    /**
     * 初始化纹理
     */
    private void initTextures() {
        if (!texturesInitialized) {
            floorTexture = textureManager.getFloorTexture();
            wallTexture = textureManager.getWallTexture();
            texturesInitialized = true;
            Logger.debug("Maze textures initialized for mode: " +
                textureManager.getCurrentMode());
        }
    }

    /**
     * 检查并更新纹理
     */
    private void checkTextures() {
        if (!texturesInitialized) {
            initTextures();
        }
    }

    /**
     * 渲染迷宫地板层
     */
    public void renderFloor(SpriteBatch batch) {
        checkTextures();

        int[][] maze = gameManager.getMazeForRendering();

        // 渲染所有地板
        for (int y = 0; y < maze.length; y++) {
            for (int x = 0; x < maze[y].length; x++) {
                float pixelX = x * GameConstants.CELL_SIZE;
                float pixelY = y * GameConstants.CELL_SIZE;

                if (maze[y][x] == 1) { // 通路
                    // 地板铺满整个格子
                    batch.draw(floorTexture, pixelX, pixelY,
                        GameConstants.CELL_SIZE, GameConstants.CELL_SIZE);
                }
            }
        }
    }

    /**
     * 渲染墙壁层（基础部分）
     */
    public void renderWallBase(SpriteBatch batch) {
        checkTextures();

        int[][] maze = gameManager.getMazeForRendering();
        TextureManager.TextureMode currentMode = textureManager.getCurrentMode();

        if (currentMode == TextureManager.TextureMode.COLOR ||
            currentMode == TextureManager.TextureMode.MINIMAL) {
            renderWallBaseWithColors(batch, maze);
        } else {
            renderWallBaseWithTextures(batch, maze);
        }
    }

    /**
     * 渲染特定位置的墙壁（用于遮挡）
     */
    public void renderWallAtPosition(SpriteBatch batch, int x, int y) {
        checkTextures();

        int[][] maze = gameManager.getMazeForRendering();
        if (x < 0 || x >= maze[0].length || y < 0 || y >= maze.length) {
            return;
        }

        if (maze[y][x] == 0) { // 墙壁
            float pixelX = x * GameConstants.CELL_SIZE;
            float pixelY = y * GameConstants.CELL_SIZE;

            // 渲染墙壁基础部分
            batch.draw(wallTexture, pixelX, pixelY,
                GameConstants.CELL_SIZE, GameConstants.CELL_SIZE);

            // 渲染增大的立体墙壁部分
            if (wallScaleFactor > 1.0f) {
                float wallWidth = GameConstants.CELL_SIZE * wallScaleFactor;
                float wallHeight = GameConstants.CELL_SIZE * wallHeightMultiplier;
                float wallX = pixelX - (wallWidth - GameConstants.CELL_SIZE) / 2;
                float wallY = pixelY - (wallHeight - GameConstants.CELL_SIZE) / 2 - wallOverlap;

                // 使用稍深的颜色制造立体感
                batch.setColor(0.85f, 0.85f, 0.85f, 1f);
                batch.draw(wallTexture, wallX, wallY,
                    wallWidth, wallHeight);
                batch.setColor(Color.WHITE);
            }
        }
    }

    /**
     * 使用纹理渲染墙壁基础层
     */
    private void renderWallBaseWithTextures(SpriteBatch batch, int[][] maze) {
        if (floorTexture == null || wallTexture == null) {
            renderWallBaseWithColors(batch, maze);
            return;
        }

        for (int y = 0; y < maze.length; y++) {
            for (int x = 0; x < maze[y].length; x++) {
                if (maze[y][x] == 0) { // 墙壁
                    float pixelX = x * GameConstants.CELL_SIZE;
                    float pixelY = y * GameConstants.CELL_SIZE;

                    // 只渲染基础墙壁，不渲染增大部分
                    batch.draw(wallTexture, pixelX, pixelY,
                        GameConstants.CELL_SIZE, GameConstants.CELL_SIZE);
                }
            }
        }
    }

    /**
     * 使用颜色渲染墙壁基础层
     */
    private void renderWallBaseWithColors(SpriteBatch batch, int[][] maze) {
        for (int y = 0; y < maze.length; y++) {
            for (int x = 0; x < maze[y].length; x++) {
                if (maze[y][x] == 0) { // 墙壁
                    float pixelX = x * GameConstants.CELL_SIZE;
                    float pixelY = y * GameConstants.CELL_SIZE;

                    batch.setColor(GameConstants.WALL_COLOR);
                    batch.draw(textureManager.getWallTexture(), pixelX, pixelY,
                        GameConstants.CELL_SIZE, GameConstants.CELL_SIZE);
                }
            }
        }

        batch.setColor(Color.WHITE);
    }

    /**
     * 获取墙壁的渲染参数
     */
    public float[] getWallRenderParameters() {
        return new float[] {
            wallScaleFactor,
            wallHeightMultiplier,
            wallOverlap
        };
    }

    /**
     * 检查坐标是否为墙壁
     */
    public boolean isWall(int x, int y) {
        int[][] maze = gameManager.getMazeForRendering();
        if (x < 0 || x >= maze[0].length || y < 0 || y >= maze.length) {
            return false;
        }
        return maze[y][x] == 0;
    }

    /**
     * 设置墙壁缩放因子
     */
    public void setWallScale(float scaleFactor) {
        this.wallScaleFactor = Math.max(1.0f, Math.min(2.0f, scaleFactor));
        Logger.debug("Wall scale factor set to: " + this.wallScaleFactor);
    }

    /**
     * 设置墙壁高度倍数
     */
    public void setWallHeightMultiplier(float multiplier) {
        this.wallHeightMultiplier = Math.max(1.0f, Math.min(2.5f, multiplier));
        Logger.debug("Wall height multiplier set to: " + this.wallHeightMultiplier);
    }

    /**
     * 设置墙壁重叠像素
     */
    public void setWallOverlap(int overlap) {
        this.wallOverlap = Math.max(0, Math.min(10, overlap));
        Logger.debug("Wall overlap set to: " + this.wallOverlap);
    }

    /**
     * 重置墙壁渲染参数为默认值
     */
    public void resetWallParameters() {
        this.wallScaleFactor = 1.2f;
        this.wallHeightMultiplier = 1.5f;
        this.wallOverlap = 5;
        Logger.debug("Wall parameters reset to default");
    }

    /**
     * 获取墙壁缩放因子
     */
    public float getWallScale() {
        return wallScaleFactor;
    }

    /**
     * 获取墙壁高度倍数
     */
    public float getWallHeightMultiplier() {
        return wallHeightMultiplier;
    }

    /**
     * 获取墙壁重叠像素
     */
    public int getWallOverlap() {
        return wallOverlap;
    }

    /**
     * 设置游戏管理器
     */
    public void setGameManager(GameManager gameManager) {
        this.gameManager = gameManager;
        this.texturesInitialized = false;
    }
}
