package de.tum.cit.fop.maze.maze;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Array;
import de.tum.cit.fop.maze.entities.ExitDoor;
import de.tum.cit.fop.maze.game.GameConstants;
import de.tum.cit.fop.maze.game.GameManager;
import de.tum.cit.fop.maze.utils.TextureManager;

import java.util.ArrayList;
import java.util.List;

public class MazeRenderer {

    private final GameManager gameManager;
    private final TextureManager textureManager = TextureManager.getInstance();
    private int[][] lastMazeRef = null;


    /* ===== 地板 ===== */
    private Texture floorTexture;

    /* ===== 墙 ===== */
    private TextureAtlas wallAtlas;
    private TextureRegion[] wallRegions;

    private boolean analyzed = false;
    private final List<WallGroup> wallGroups = new ArrayList<>();

    /* ================================================= */

    public static class WallGroup {
        public int startX, startY, length, textureIndex;

        public WallGroup(int x, int y, int len, int tex) {
            startX = x;
            startY = y;
            length = len;
            textureIndex = tex;
        }
    }

    public MazeRenderer(GameManager gm) {
        this.gameManager = gm;
        loadTextures();
    }

    /* ================= 纹理加载 ================= */

    private void loadTextures() {
        floorTexture = textureManager.getFloorTexture();

        FileHandle fh = Gdx.files.internal("Wallpaper/Wallpaper.atlas");
        wallAtlas = new TextureAtlas(fh);

        Array<TextureAtlas.AtlasRegion> regions =
                wallAtlas.findRegions("Wallpaper");

        wallRegions = new TextureRegion[4];
        for (int i = 0; i < 4; i++) {
            wallRegions[i] = regions.get(i % regions.size);
        }
    }

    /* ================= 地板 ================= */

    public void renderFloor(SpriteBatch batch) {
        if (floorTexture == null) return;

        float w = GameConstants.MAZE_WIDTH * GameConstants.CELL_SIZE;
        float h = GameConstants.MAZE_HEIGHT * GameConstants.CELL_SIZE;

        batch.draw(floorTexture, 0, 0, w, h);
    }

    /* ================= 墙分析（核心修复在这里） ================= */

    private void analyze() {
        wallGroups.clear();

        int[][] maze = gameManager.getMaze();
        if (maze == null) return;

        for (int y = 0; y < maze.length; y++) {
            int x = 0;
            while (x < maze[y].length) {

                if (!isWallCellButNotExit(x, y)) {
                    x++;
                    continue;
                }

                int startX = x;
                int len = 0;

                while (x < maze[y].length && isWallCellButNotExit(x, y)) {
                    len++;
                    x++;
                }

                splitWall(startX, y, len);
            }
        }

        analyzed = true;
    }


    /**
     * ✅ 核心判断：
     * 是墙，并且不是出口门
     */
    private boolean isWallCellButNotExit(int x, int y) {
        return gameManager.getMaze()[y][x] == 0
                && !gameManager.isExitDoorAt(x, y);
    }

    private void splitWall(int x, int y, int len) {
        int cx = x;
        int remain = len;

        while (remain > 0) {
            if (remain >= 5) {
                wallGroups.add(new WallGroup(cx, y, 5, 3));
                cx += 5;
                remain -= 5;
            } else if (remain == 4) {
                wallGroups.add(new WallGroup(cx, y, 2, 1));
                wallGroups.add(new WallGroup(cx + 2, y, 2, 1));
                return;
            } else if (remain >= 3) {
                wallGroups.add(new WallGroup(cx, y, 3, 2));
                cx += 3;
                remain -= 3;
            } else if (remain == 2) {
                wallGroups.add(new WallGroup(cx, y, 2, 1));
                return;
            } else {
                wallGroups.add(new WallGroup(cx, y, 1, 0));
                return;
            }
        }
    }

    public List<WallGroup> getWallGroups() {
        int[][] currentMaze = gameManager.getMaze();

        // 🔥 迷宫引用变了 → 强制重新分析
        if (!analyzed || currentMaze != lastMazeRef) {
            analyze();
            lastMazeRef = currentMaze;
        }

        return wallGroups;
    }


    /* ================= 前后遮挡判断 ================= */

    public boolean isWallInFrontOfAnyEntity(int wx, int wy) {
        var p = gameManager.getPlayer();
        if (p != null && wy > p.getY()) return true;

        for (var e : gameManager.getEnemies()) {
            if (e.isActive() && wy > e.getY()) return true;
        }

        for (ExitDoor d : gameManager.getExitDoors()) {
            if (wy > d.getY()) return true;
        }

        return false;
    }

    /* ================= 墙绘制 ================= */

    public void renderWallGroup(SpriteBatch batch, WallGroup g) {
        float cs = GameConstants.CELL_SIZE;
        float h = cs * 1.5f;
        int overlap = 6;

        TextureRegion base = wallRegions[g.textureIndex];

        float u0 = base.getU();
        float u1 = base.getU2();
        float v0 = base.getV();

        float step = (u1 - u0) / g.length;

        for (int i = 0; i < g.length; i++) {
            TextureRegion slice = new TextureRegion(
                    base.getTexture(),
                    (int) ((u0 + i * step) * base.getTexture().getWidth()),
                    (int) (v0 * base.getTexture().getHeight()),
                    (int) (step * base.getTexture().getWidth()),
                    base.getRegionHeight()
            );

            batch.draw(
                    slice,
                    (g.startX + i) * cs,
                    g.startY * cs - overlap,
                    cs,
                    h
            );
        }
    }

    public void dispose() {
        if (wallAtlas != null) wallAtlas.dispose();
    }


}
