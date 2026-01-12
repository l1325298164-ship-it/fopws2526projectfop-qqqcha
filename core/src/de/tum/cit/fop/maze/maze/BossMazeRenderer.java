package de.tum.cit.fop.maze.maze;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import de.tum.cit.fop.maze.game.GameConstants;
import de.tum.cit.fop.maze.game.DifficultyConfig;
import de.tum.cit.fop.maze.game.GameManager;

import java.util.Random;

public class BossMazeRenderer extends MazeRenderer {

    // ⭐ Boss 专用墙高度倍率（你要改的就在这）
    private static final float BOSS_WALL_HEIGHT_MULT = 1f;
    private static final int OVERLAP = 4;

    // ✅ 皮肤池：每个 textureIndex 对应一个池子
    // 0 -> (0,1,2)
    // 1 -> (3,4)
    // 2 -> (5,6)
    // 3 -> (7,8)
    private TextureRegion[][] wallSkinPools;

    public BossMazeRenderer(GameManager gm, DifficultyConfig difficultyConfig) {
        super(gm, difficultyConfig);
        loadBossWallTextures();
    }

    private void loadBossWallTextures() {
        FileHandle fh = Gdx.files.internal("Wallpaper/boss/wallpaper.atlas");
        TextureAtlas atlas = new TextureAtlas(fh);

        var regions = atlas.findRegions("wallpaper");

        // ⚠️ 你必须确保 atlas 里真的有 0..8 共 9 张
        // 不够的话会越界崩溃
        wallSkinPools = new TextureRegion[][] {
                { regions.get(0), regions.get(1), regions.get(2) }, // group 0
                { regions.get(3), regions.get(4) },                 // group 1
                { regions.get(5), regions.get(6) },                 // group 2
                { regions.get(7), regions.get(8) }                  // group 3
        };
    }

    // ✅ 关键：给每个 WallGroup 一个“稳定随机”的 texture
    // 不能用 Math.random() 每帧抽，否则墙会闪
    private TextureRegion pickStableTexture(WallGroup g) {
        TextureRegion[] pool = wallSkinPools[g.textureIndex];

        // 用坐标 + 长度做 seed（保证同一段墙在一个迷宫里一直是同一张）
        long seed = 1469598103934665603L;
        seed ^= g.startX; seed *= 1099511628211L;
        seed ^= g.startY; seed *= 1099511628211L;
        seed ^= g.length; seed *= 1099511628211L;
        seed ^= g.textureIndex; seed *= 1099511628211L;

        Random r = new Random(seed);
        return pool[r.nextInt(pool.length)];
    }

    @Override
    public void renderWallGroup(SpriteBatch batch, WallGroup g) {
        float cs = GameConstants.CELL_SIZE;
        float h  = cs * BOSS_WALL_HEIGHT_MULT;

        // ✅ 不再用 “bossWallRegions[g.textureIndex]”
        // ✅ 改成：从池子里稳定随机选一张
        TextureRegion base = pickStableTexture(g);

        float u0 = base.getU();
        float u1 = base.getU2();
        float v0 = base.getV();

        float step = (u1 - u0) / g.length;

        for (int i = 0; i < g.length; i++) {
            TextureRegion slice = new TextureRegion(
                    base.getTexture(),
                    (int)((u0 + i * step) * base.getTexture().getWidth()),
                    (int)(v0 * base.getTexture().getHeight()),
                    (int)(step * base.getTexture().getWidth()),
                    base.getRegionHeight()
            );

            batch.draw(
                    slice,
                    (g.startX + i) * cs,
                    g.startY * cs - OVERLAP,
                    cs,
                    h
            );
        }
    }
}
