package de.tum.cit.fop.maze.qte;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import de.tum.cit.fop.maze.game.GameConstants;
import de.tum.cit.fop.maze.utils.TextureManager;

public class QTEMazeRenderer {
    private int mazeHeight;
    private int mazeWidth;

    private final Texture floor;
    private final Texture wall;

    // QTE 专用视觉参数
    private static final float WALL_HEIGHT_MULTIPLIER = 1.2f;
    private static final int WALL_OVERLAP = 6;

    public QTEMazeRenderer() {
        TextureManager tm = TextureManager.getInstance();
        floor = tm.getFloorTexture();
        wall  = tm.getWallTexture();
    }

    public void setMazeDimensions(int width, int height) {
        this.mazeWidth = width;
        this.mazeHeight = height;
    }

    public int getMazeHeight() {
        return mazeHeight;
    }

    public int getMazeWidth() {
        return mazeWidth;
    }

    /* =========================
       y轴倒置辅助方法
       ========================= */
    public int invertY(int y) {
        if (mazeHeight <= 0) {
            return y; // 防止除零错误
        }
        return mazeHeight - 1 - y;
    }

    public float getInvertedWorldY(int gridY) {
        return invertY(gridY) * GameConstants.CELL_SIZE;
    }

    /* =========================
       地板（固定迷宫）
       ========================= */
    public void renderFloor(SpriteBatch batch, int[][] maze) {
        if (floor == null) return;

        for (int y = 0; y < maze.length; y++) {
            for (int x = 0; x < maze[y].length; x++) {
                if (maze[y][x] == 1) { // 假设1表示可通行区域（地板）
                    // 倒置y坐标
                    int invertedY = invertY(y);
                    float worldX = x * GameConstants.CELL_SIZE;
                    float worldY = invertedY * GameConstants.CELL_SIZE;

                    batch.draw(
                            floor,
                            worldX,
                            worldY,
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
    public void renderWall(SpriteBatch batch, int gridX, int gridY) {
        if (wall == null) return;

        // 倒置y坐标
        int invertedY = invertY(gridY);
        float worldX = gridX * GameConstants.CELL_SIZE;
        float worldY = invertedY * GameConstants.CELL_SIZE;

        float wallHeight = GameConstants.CELL_SIZE * WALL_HEIGHT_MULTIPLIER;

        // 底部对齐格子，只向上长
        batch.draw(
                wall,
                worldX,
                worldY - WALL_OVERLAP,
                GameConstants.CELL_SIZE,
                wallHeight
        );
    }

    /* =========================
       在指定位置渲染对象（通用方法）
       ========================= */
    public void renderObjectAt(SpriteBatch batch, Texture texture, int gridX, int gridY) {
        if (texture == null) return;

        // 倒置y坐标
        int invertedY = invertY(gridY);
        float worldX = gridX * GameConstants.CELL_SIZE;
        float worldY = invertedY * GameConstants.CELL_SIZE;

        batch.draw(
                texture,
                worldX,
                worldY,
                GameConstants.CELL_SIZE,
                GameConstants.CELL_SIZE
        );
    }

    /* =========================
       渲染指定区域（优化性能）
       ========================= */
    public void renderVisibleArea(SpriteBatch batch, int[][] maze,
                                  int cameraX, int cameraY,
                                  int viewWidth, int viewHeight) {
        if (floor == null || wall == null) return;

        int startX = Math.max(0, cameraX - viewWidth / 2);
        int endX = Math.min(mazeWidth, cameraX + viewWidth / 2 + 1);
        int startY = Math.max(0, cameraY - viewHeight / 2);
        int endY = Math.min(mazeHeight, cameraY + viewHeight / 2 + 1);

        // 先渲染地板
        for (int y = startY; y < endY; y++) {
            for (int x = startX; x < endX; x++) {
                if (maze[y][x] == 1) {
                    int invertedY = invertY(y);
                    float worldX = x * GameConstants.CELL_SIZE;
                    float worldY = invertedY * GameConstants.CELL_SIZE;

                    batch.draw(
                            floor,
                            worldX,
                            worldY,
                            GameConstants.CELL_SIZE,
                            GameConstants.CELL_SIZE
                    );
                }
            }
        }

        // 再渲染墙壁
        for (int y = startY; y < endY; y++) {
            for (int x = startX; x < endX; x++) {
                if (maze[y][x] == 0) { // 假设0表示墙壁
                    int invertedY = invertY(y);
                    float worldX = x * GameConstants.CELL_SIZE;
                    float worldY = invertedY * GameConstants.CELL_SIZE;

                    float wallHeight = GameConstants.CELL_SIZE * WALL_HEIGHT_MULTIPLIER;
                    batch.draw(
                            wall,
                            worldX,
                            worldY - WALL_OVERLAP,
                            GameConstants.CELL_SIZE,
                            wallHeight
                    );
                }
            }
        }
    }
}