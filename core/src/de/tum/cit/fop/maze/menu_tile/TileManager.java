package de.tum.cit.fop.maze.menu_tile;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Array;

/**
 * 管理菜单背景碎片（壁画剥落系统）
 */
public class TileManager {

    /** 内部 Tile 结构 */
    public static class Tile {
        public TextureRegion region;
        public float x, y;
        public float alpha = 1f;
        public boolean removed = false;
    }

    // ===== 配置 =====
    private static final int STAGE_COUNT = 3;
    private static final float FADE_SPEED = 0.6f;

    // ===== 资源 =====
    private final TextureAtlas[] atlases = new TextureAtlas[STAGE_COUNT];
    private final Array<Tile> tiles = new Array<>();

    private int currentStage = -1;

    // ===== 构造 =====
    public TileManager() {
        atlases[1] = new TextureAtlas(Gdx.files.internal("menu_bg/tiles_stage_1.atlas"));
        atlases[2] = new TextureAtlas(Gdx.files.internal("menu_bg/tiles_stage_2.atlas"));
    }

    // ===== Stage 加载 =====
    public void loadStage(int stage) {
        if (stage < 0 || stage >= atlases.length) return;
        if (stage == currentStage) return;

        tiles.clear();

        TextureAtlas atlas = atlases[stage];
        if (atlas == null) {
            System.out.println("[TileManager] atlas NULL for stage " + stage);
            return;
        }

        Array<TextureAtlas.AtlasRegion> regions = atlas.getRegions();
        System.out.println("[TileManager] regions = " + regions.size);

        // ⚠️ 根据你的 tile 尺寸改
        int tileWidth = regions.first().getRegionWidth();
        int tileHeight = regions.first().getRegionHeight();

        // ⚠️ 一行多少块（按背景宽度）
        int cols = Gdx.graphics.getWidth() / tileWidth;

        for (int i = 0; i < regions.size; i++) {
            TextureAtlas.AtlasRegion region = regions.get(i);

            int col = i % cols;
            int row = i / cols;

            Tile tile = new Tile();
            tile.region = region;
            tile.x = col * tileWidth;
            tile.y = row * tileHeight;

            tiles.add(tile);
        }

        System.out.println("[TileManager] tiles created = " + tiles.size);
        currentStage = stage;
    }


    // ===== 更新逻辑（剥落）=====
    // ===== 更新逻辑（剥落）=====
    public void update(float delta) {
        System.out.println("[TileManager] update called, tiles=" + tiles.size);
        // stage 0 不会调用到这里
        if (tiles.size == 0){
            System.out.println("[TileManager] tiles is EMPTY");
            return;}

        for (Tile tile : tiles) {
            if (tile.removed) continue;

            tile.alpha -= delta * FADE_SPEED;
            /// //////////
            System.out.println("[TileManager] alpha=" + tile.alpha);
            if (tile.alpha <= 0f) {
                tile.alpha = 0f;
                tile.removed = true;
            }
            // 只打一个，别刷爆
            break;
        }
    }


    // ===== 渲染 =====
    public void render(SpriteBatch batch) {
        for (Tile tile : tiles) {
            if (tile.removed) continue;

            batch.setColor(1f, 1f, 1f, tile.alpha);
            batch.draw(tile.region, tile.x, tile.y);
        }
        batch.setColor(Color.WHITE);
    }

    // ===== 工具 =====
    public void reset() {
        for (Tile tile : tiles) {
            tile.alpha = 1f;
            tile.removed = false;
        }
    }

    public void dispose() {
        for (TextureAtlas atlas : atlases) {
            if (atlas != null) atlas.dispose();
        }
    }
}
