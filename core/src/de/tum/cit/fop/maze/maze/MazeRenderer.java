// MazeRenderer.java - 方案A：依赖GameManager
package de.tum.cit.fop.maze.maze;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import de.tum.cit.fop.maze.game.GameConstants;
import de.tum.cit.fop.maze.game.GameManager;
import de.tum.cit.fop.maze.utils.Logger;
import de.tum.cit.fop.maze.utils.TextureManager;

public class MazeRenderer {
    // ✅ 不再存储迷宫副本，改为依赖GameManager
    private GameManager gameManager;
    private TextureManager textureManager;
    private Texture floorTexture;
    private Texture wallTexture;

    /**
     * 构造函数 - 接收GameManager作为数据源
     * @param gameManager 游戏管理器，提供迷宫数据
     */
    public MazeRenderer(GameManager gameManager) {
        this.gameManager = gameManager;
        this.textureManager = TextureManager.getInstance();
        this.floorTexture = textureManager.getFloorTexture();
        this.wallTexture = textureManager.getWallTexture();
        Logger.debug("MazeRenderer initialized with GameManager dependency");
    }

    /**
     * 渲染迷宫
     * @param batch SpriteBatch用于绘制
     */
    public void render(SpriteBatch batch) {
        long startTime = System.currentTimeMillis();

        // ✅ 每次渲染时从GameManager获取最新迷宫数据
        int[][] currentMaze = gameManager.getMazeForRendering();

        // 检查迷宫数据是否有效
        if (currentMaze == null || currentMaze.length == 0) {
            Logger.error("Maze data is null or empty!");
            return;
        }

        int cellSize = GameConstants.CELL_SIZE;
        int mazeHeight = currentMaze.length;
        int mazeWidth = (mazeHeight > 0) ? currentMaze[0].length : 0;

        // 渲染迷宫网格
        for (int y = 0; y < mazeHeight; y++) {
            for (int x = 0; x < mazeWidth; x++) {
                Texture texture = currentMaze[y][x] == 1 ?
                    floorTexture : wallTexture;

                batch.draw(texture,
                    x * cellSize,
                    y * cellSize,
                    cellSize,
                    cellSize
                );
            }
        }

        long endTime = System.currentTimeMillis();
        long renderTime = endTime - startTime;
        if (renderTime > 16) { // 超过16ms可能有问题
            Logger.performance("Maze render took " + renderTime + "ms");
        }
    }

    /**
     * 更新GameManager引用（用于游戏重启）
     * @param gameManager 新的游戏管理器
     */
    public void setGameManager(GameManager gameManager) {
        if (gameManager == null) {
            Logger.error("Cannot set null GameManager to MazeRenderer");
            return;
        }

        this.gameManager = gameManager;
        Logger.debug("MazeRenderer updated with new GameManager");
    }

    /**
     * 检查渲染器是否已初始化
     */
    public boolean isInitialized() {
        return gameManager != null && floorTexture != null && wallTexture != null;
    }
}
