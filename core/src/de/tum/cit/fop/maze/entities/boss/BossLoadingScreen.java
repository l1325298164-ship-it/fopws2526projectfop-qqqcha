package de.tum.cit.fop.maze.entities.boss;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.TimeUtils;
import de.tum.cit.fop.maze.MazeRunnerGame;
import de.tum.cit.fop.maze.audio.AudioManager;
import de.tum.cit.fop.maze.audio.AudioType;
import de.tum.cit.fop.maze.screen.MenuScreen;

public class BossLoadingScreen implements Screen {

    private final MazeRunnerGame game;
    private final AssetManager assets;
    private final SpriteBatch batch;

    private BitmapFont font;

    private long showTime;
    private boolean finished = false;
    private float blinkTime = 0f;

    private static final long MIN_SHOW_TIME_MS = 1200;

    public BossLoadingScreen(MazeRunnerGame game) {
        this.game = game;
        this.assets = game.getAssets();
        this.batch = game.getSpriteBatch();
    }

    @Override
    public void show() {
        showTime = TimeUtils.millis();
        font = game.getSkin().getFont("default-font");
        queueBossAssets();
    }

    private void queueBossAssets() {

        // ===== Atlas =====
        assets.load("bossFight/BOSS_PV.atlas", TextureAtlas.class);

        // ===== Textures =====
        assets.load("debug/boss_bg.jpg", Texture.class);
        assets.load("debug/teacup_top.png", Texture.class);

        assets.load("effects/aoe_fill.png", Texture.class);
        assets.load("effects/aoe_ring.png", Texture.class);

        // ===== BGM =====
        assets.load("sounds/music/boss_bgm.mp3", Music.class);

        // ===== Voice =====
        assets.load("voice/boss/boss_1.ogg", Sound.class);
    }

    @Override
    public void render(float delta) {

        blinkTime += delta;
        assets.update();

        // ===== ESC：回菜单（不 clear AssetManager，避免影响全局）=====
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            game.setScreen(new MenuScreen(game));
            return;
        }

        boolean ready =
                assets.isFinished()
                        && TimeUtils.timeSinceMillis(showTime) > MIN_SHOW_TIME_MS;

        if (ready && !finished) {
            finished = true;
            AudioManager.getInstance().stopMusic();
            game.setScreen(new BossFightScreen(game));
            return;
        }

        // ===== 清屏 =====
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        // ===== 文本渲染 =====
        batch.begin();

        float w = Gdx.graphics.getWidth();
        float h = Gdx.graphics.getHeight();

        // --- Loading 百分比 ---
        int percent = MathUtils.floor(assets.getProgress() * 100f);
        font.setColor(Color.WHITE);
        font.getData().setScale(0.9f);
        font.draw(
                batch,
                "LOADING " + percent + "%",
                0,
                h * 0.55f,
                w,
                Align.center,
                false
        );

        // --- 闪烁提示文字 ---
        float alpha = 0.4f + 0.6f * MathUtils.sin(blinkTime * 1.2f);
        font.setColor(1f, 1f, 1f, alpha);
        font.getData().setScale(0.6f);
        font.draw(
                batch,
                "Next: 2 minutes of high-intensity boss combat.\nPrepare yourself.",
                0,
                h * 0.40f,
                w,
                Align.center,
                true
        );

        batch.end();
    }

    @Override public void resize(int w, int h) {}
    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() {}
    @Override public void dispose() {}
}
