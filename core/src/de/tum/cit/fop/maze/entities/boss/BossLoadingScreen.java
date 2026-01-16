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
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.TimeUtils;
import de.tum.cit.fop.maze.MazeRunnerGame;
import de.tum.cit.fop.maze.audio.AudioManager;
import de.tum.cit.fop.maze.audio.AudioType;
import de.tum.cit.fop.maze.entities.boss.config.BossDifficultyFactory;
import de.tum.cit.fop.maze.entities.boss.config.BossMazeConfig;
import de.tum.cit.fop.maze.entities.boss.config.BossMazeConfigLoader;
import de.tum.cit.fop.maze.game.DifficultyConfig;
import de.tum.cit.fop.maze.maze.MazeGenerator;
import de.tum.cit.fop.maze.screen.MenuScreen;
import de.tum.cit.fop.maze.utils.BlockingInputProcessor;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

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
        preloadAllPhasesAsync();
        Gdx.input.setInputProcessor(new BlockingInputProcessor());
    }

    private void queueBossAssets() {

        // ===== Atlas =====
        assets.load("story_file/boss/bossFight/BOSS_PV.atlas", TextureAtlas.class);

        // ===== Textures =====
        assets.load("story_file/boss/teacup_top.png", Texture.class);

        assets.load("effects/aoe_fill.png", Texture.class);
        assets.load("effects/aoe_ring.png", Texture.class);

        // ===== BGM =====
        assets.load("sounds_file/BGM/boss_bgm.mp3", Music.class);

        // ===== Voice =====
        assets.load("story_file/boss/voice/boss_1.ogg", Sound.class);
    }

    @Override
    public void render(float delta) {

        // ⭐ 关键：重置为屏幕坐标
        batch.setProjectionMatrix(
                new Matrix4().setToOrtho2D(
                        0, 0,
                        Gdx.graphics.getWidth(),
                        Gdx.graphics.getHeight()
                )
        );

        font.getData().setScale(1f);
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

// 🔹 左上角 ESC 提示
        font.setColor(1f, 1f, 1f, 0.65f);
        font.getData().setScale(0.5f);
        font.draw(
                batch,
                "ESC  —  Return to Menu",
                18f,
                Gdx.graphics.getHeight() - 18f
        );

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
        Gdx.app.log("FONT", "scale=" + font.getData().scaleX);

        batch.end();
        font.getData().setScale(1f);
    }
    private final Map<Integer, BossPhasePreloadData> phaseCache =
            Collections.synchronizedMap(new HashMap<>());


    private void preloadAllPhasesAsync() {

        new Thread(() -> {

            BossMazeConfig config =
                    BossMazeConfigLoader.loadOne("boss/boss_phases.json");

            // ⭐ 每个线程一个独立 generator（安全）
            MazeGenerator generator = new MazeGenerator();

            for (int i = 0; i < 3; i++) {

                BossMazeConfig.Phase phase = config.phases.get(i);

                DifficultyConfig dc =
                        BossDifficultyFactory.create(config.base, phase);

                // ✅ 正确方法名（与你工程一致）
                int[][] maze = generator.generateMaze(dc);

                BossPhasePreloadData data = new BossPhasePreloadData();
                data.maze = maze;
                data.phase = phase;

                phaseCache.put(i, data);

                Gdx.app.log(
                        "BOSS_PRELOAD",
                        "Phase " + i + " maze ready (" +
                                dc.mazeWidth + "x" + dc.mazeHeight + ")"
                );
            }

        }, "BossPhasePreloader").start();

    }

    @Override public void resize(int w, int h) {}
    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() {}
    @Override public void dispose() {}
}
