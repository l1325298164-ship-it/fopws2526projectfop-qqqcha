package de.tum.cit.fop.maze.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import de.tum.cit.fop.maze.MazeRunnerGame;
import de.tum.cit.fop.maze.audio.AudioManager;
import de.tum.cit.fop.maze.tools.ButtonFactory;
import de.tum.cit.fop.maze.tools.PerlinNoise;

public class MenuScreen implements Screen {

    private final MazeRunnerGame game;

    // ===== 渲染 =====
    private SpriteBatch batch;
    private Stage stage;

    private float time = 0f;
    private float corruption = 0f;

    // ===== 资源 =====
    private Texture bgCandy;
    private Texture bgHell;

    // ===== UI =====
    private ImageButton musicButton;
    private TextureAtlas uiAtlas;
    private AudioManager audioManager;
    private boolean isMusicOn = true;

    public MenuScreen(MazeRunnerGame game) {
        this.game = game;

        batch = new SpriteBatch();

        // ===== Stage =====
        OrthographicCamera camera = new OrthographicCamera();
        Viewport viewport = new ScreenViewport(camera);
        stage = new Stage(viewport, batch);
        Gdx.input.setInputProcessor(stage);

        // ===== 资源 =====
        uiAtlas = new TextureAtlas(Gdx.files.internal("ui/button.atlas"));
        audioManager = AudioManager.getInstance();
        isMusicOn = audioManager.isMusicEnabled();

        bgCandy = new Texture(Gdx.files.internal("menu_bg/bg_front.png"));
        bgHell  = new Texture(Gdx.files.internal("menu_bg/bg_hell.png"));

        // ===== UI =====
        Table root = new Table();
        root.setFillParent(true);
        stage.addActor(root);

        Label title = new Label("HELLO WORLD", game.getSkin(), "title");
        title.setAlignment(Align.center);
        title.setFontScale(1.1f);
        root.add(title).padBottom(80).row();

        ButtonFactory bf = new ButtonFactory(game.getSkin());
        root.add(bf.create("START GAME", game::goToGame)).padBottom(20).row();
        root.add(bf.create("RESET THE WORLD", game::goToPV)).padBottom(20).row();
        root.add(bf.create("CONTROLS", () ->
                game.setScreen(new KeyMappingScreen(game, this))
        )).padBottom(20).row();
        root.add(bf.create("TEST", () -> {})).row();

        createMusicButton();

        Table bottomRight = new Table();
        bottomRight.setFillParent(true);
        bottomRight.bottom().right();
        bottomRight.add(musicButton).size(160).pad(20);
        stage.addActor(bottomRight);

        if (isMusicOn) {
            audioManager.playMusic(de.tum.cit.fop.maze.audio.AudioType.MUSIC_MENU);
        }
    }

    // ================= 渲染 =================

    @Override
    public void render(float delta) {

        time += delta;
        corruption = Math.min(1f, corruption + delta * 0.15f);

        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        batch.begin();

        // ===== ① 先画糖果世界（整张，清楚）=====
        batch.draw(
                bgCandy,
                0, 0,
                Gdx.graphics.getWidth(),
                Gdx.graphics.getHeight()
        );

        // ===== ② 再在腐化区域画地狱（按区域采样）=====
        int cell = 32; // 可调：32 / 48 / 64
        int cols = Gdx.graphics.getWidth()  / cell + 1;
        int rows = Gdx.graphics.getHeight() / cell + 1;

        int screenW = Gdx.graphics.getWidth();
        int screenH = Gdx.graphics.getHeight();

        for (int x = 0; x < cols; x++) {
            for (int y = 0; y < rows; y++) {

                float n = PerlinNoise.noise(
                        x * 0.15f,
                        y * 0.15f + time * 0.2f
                );

                if (n < corruption) {

                    // ⭐ 实际绘制尺寸（处理最后一列 / 行）
                    float drawW = Math.min(cell, screenW - x * cell);
                    float drawH = Math.min(cell, screenH - y * cell);

                    if (drawW <= 0 || drawH <= 0) continue;

                    // ⭐ 对应 UV（Y 轴已翻转）
                    int texW = bgHell.getWidth();
                    int texH = bgHell.getHeight();

                    float u1 = (x * cell) / (float) screenW;
                    float u2 = (x * cell + drawW) / (float) screenW;

// ⭐ 用“纹理高度”来算 V（并翻转）
                    float v2 = 1f - (y * cell) / (float) texH;
                    float v1 = 1f - (y * cell + drawH) / (float) texH;

                    batch.draw(
                            bgHell,
                            x * cell,
                            y * cell,
                            drawW,
                            drawH,
                            u1, v1,
                            u2, v2
                    );
                }
            }
        }


        batch.end();

        // ===== UI =====
        stage.act(delta);
        stage.draw();
    }


    // ================= 音乐按钮 =================

    private void createMusicButton() {
        TextureRegionDrawable on = new TextureRegionDrawable(uiAtlas.findRegion("frame178"));
        TextureRegionDrawable off = new TextureRegionDrawable(uiAtlas.findRegion("frame180"));

        ImageButton.ImageButtonStyle style = new ImageButton.ImageButtonStyle();
        style.imageUp = isMusicOn ? on : off;

        musicButton = new ImageButton(style);
        musicButton.setTransform(true);
        musicButton.setOrigin(Align.center);

        musicButton.addListener(new com.badlogic.gdx.scenes.scene2d.InputListener() {
            @Override
            public boolean touchDown(com.badlogic.gdx.scenes.scene2d.InputEvent e,
                                     float x, float y, int p, int b) {
                return true;
            }

            @Override
            public void touchUp(com.badlogic.gdx.scenes.scene2d.InputEvent e,
                                float x, float y, int p, int b) {
                toggleMusic();
            }
        });
    }

    private void toggleMusic() {
        isMusicOn = !isMusicOn;
        audioManager.setMusicEnabled(isMusicOn);

        musicButton.getStyle().imageUp =
                new TextureRegionDrawable(uiAtlas.findRegion(
                        isMusicOn ? "frame178" : "frame180"
                ));

        if (isMusicOn) {
            audioManager.playMusic(de.tum.cit.fop.maze.audio.AudioType.MUSIC_MENU);
        } else {
            audioManager.pauseMusic();
        }
    }

    // ================= 生命周期 =================

    @Override
    public void resize(int w, int h) {
        stage.getViewport().update(w, h, true);
    }

    @Override
    public void dispose() {
        stage.dispose();
        batch.dispose();
        uiAtlas.dispose();
        bgCandy.dispose();
        bgHell.dispose();
    }

    @Override public void show() {}
    @Override public void hide() {}
    @Override public void pause() {}
    @Override public void resume() {}
}
