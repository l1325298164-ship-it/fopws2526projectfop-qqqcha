package de.tum.cit.fop.maze.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.math.MathUtils;
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
import de.tum.cit.fop.maze.input.PlayerInputHandler;
import de.tum.cit.fop.maze.tools.ButtonFactory;
import de.tum.cit.fop.maze.tools.PerlinNoise;

public class MenuScreen implements Screen {

    private final MazeRunnerGame game;
    private boolean changeEnabled = false;

    // ===== 渲染 =====
    private SpriteBatch batch;
    private Stage stage;
    private FrameBuffer fbo;

    private float time = 0f;
    private float corruption = 0f;
    private float noiseSeedX;
    private float noiseSeedY;

    // ===== 背景（用 TextureRegion）=====
    private Texture bgCandyTex;
    private Texture bgHellTex;
    private TextureRegion bgCandy;
    private TextureRegion bgHell;

    // ===== UI =====
    private ImageButton musicButton;
    private TextureAtlas uiAtlas;
    private AudioManager audioManager;
    private boolean isMusicOn = true;

    public MenuScreen(MazeRunnerGame game) {
        this.game = game;
        noiseSeedX = MathUtils.random(0f, 1000f);
        noiseSeedY = MathUtils.random(0f, 1000f);
        batch = new SpriteBatch();

        // ===== FBO =====
        fbo = new FrameBuffer(
                Pixmap.Format.RGBA8888,
                Gdx.graphics.getWidth(),
                Gdx.graphics.getHeight(),
                false
        );

        // ===== Stage =====
        OrthographicCamera camera = new OrthographicCamera();
        Viewport viewport = new ScreenViewport(camera);
        stage = new Stage(viewport, batch);
        Gdx.input.setInputProcessor(stage);

        // ===== 资源 =====
        uiAtlas = new TextureAtlas(Gdx.files.internal("ui/button.atlas"));
        audioManager = AudioManager.getInstance();
        isMusicOn = audioManager.isMusicEnabled();

        bgCandyTex = new Texture(Gdx.files.internal("menu_bg/bg_front.png"));
        bgHellTex  = new Texture(Gdx.files.internal("menu_bg/bg_hell.png"));

        // ⭐ 关键：在“源头”翻转一次
        bgCandy = new TextureRegion(bgCandyTex);
        bgHell  = new TextureRegion(bgHellTex);
        bgCandy.flip(false, true);
        bgHell.flip(false, true);

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
        //临时调试R按下看情况
        if (Gdx.input.isKeyJustPressed(Input.Keys.R)) {
            changeEnabled=!changeEnabled;
        }
        if(changeEnabled) {


            time += delta;
            corruption = Math.min(1f, corruption + delta * 0.15f);

            int w = Gdx.graphics.getWidth();
            int h = Gdx.graphics.getHeight();

            // ===== ① 渲染到 FBO =====
            fbo.begin();
            Gdx.gl.glClearColor(0, 0, 0, 1);
            Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

            batch.begin();

            // 糖果世界（整张）
            batch.draw(bgCandy, 0, 0, w, h);

            // 地狱侵蚀（连续空间）
            int step = 4;
            for (int x = 0; x < w; x += step) {
                for (int y = 0; y < h; y += step) {

                    float n = PerlinNoise.noise(
                            x * 0.004f,
                            y * 0.004f + time * 0.2f
                    );

                    if (n < corruption) {
                        batch.draw(
                                bgHell.getTexture(),
                                x, y,
                                step, step,
                                x / (float) w,
                                y / (float) h,
                                (x + step) / (float) w,
                                (y + step) / (float) h
                        );
                    } else if (n < corruption + 0.08f) {

                        float drip = PerlinNoise.noise(
                                x * 0.01f,
                                y * 0.02f - time * 0.6f
                        );

                        float alpha = MathUtils.clamp((corruption + 0.08f - n) / 0.08f, 0f, 1f);

                        batch.setColor(1f, 0.6f, 0.8f, alpha * 0.6f);

                        batch.draw(
                                bgCandy,          // ⭐ 用糖果图“拉伸”
                                x,
                                y - drip * 6f,    // ⭐ 微微往下流
                                step,
                                step + drip * 8f
                        );

                        batch.setColor(Color.WHITE);
                    }


                }
            }

            batch.end();
            fbo.end();

            // ===== ② FBO → 屏幕（不再翻转）=====
            Gdx.gl.glClearColor(0, 0, 0, 1);
            Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

            Texture fboTex = fbo.getColorBufferTexture();

            batch.begin();
            batch.draw(fboTex, 0, 0, w, h);
            batch.end();
        }
        else if(!changeEnabled) {

            batch.begin();
            batch.draw(
                    bgCandyTex,
                    0, 0,
                    Gdx.graphics.getWidth(),
                    Gdx.graphics.getHeight()
            );
            batch.end();
        }
        // ===== ③ UI =====
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

        if (fbo != null) fbo.dispose();
        fbo = new FrameBuffer(Pixmap.Format.RGBA8888, w, h, false);
    }

    @Override
    public void dispose() {
        stage.dispose();
        batch.dispose();
        fbo.dispose();
        uiAtlas.dispose();
        bgCandyTex.dispose();
        bgHellTex.dispose();
    }

    @Override public void show() {}
    @Override public void hide() {}
    @Override public void pause() {}
    @Override public void resume() {}
}
