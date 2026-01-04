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
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import de.tum.cit.fop.maze.MazeRunnerGame;
import de.tum.cit.fop.maze.audio.AudioManager;
import de.tum.cit.fop.maze.game.DifficultyConfig; // 需要默认难度
import de.tum.cit.fop.maze.game.GameSaveData;
import de.tum.cit.fop.maze.tools.ButtonFactory;
import de.tum.cit.fop.maze.tools.PerlinNoise;
import de.tum.cit.fop.maze.utils.SaveManager; // 🔥 导入

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

    // ===== 背景 =====
    private Texture bgCandyTex;
    private Texture bgHellTex;
    private TextureRegion bgCandy;
    private TextureRegion bgHell;

    // ===== UI =====
    private ImageButton musicButton;
    private TextureAtlas uiAtlas;
    private AudioManager audioManager;
    private boolean isMusicOn = true;
    private float BUTTON_WIDTH  = 800f;
    private float BUTTON_HEIGHT = 80f;

    public MenuScreen(MazeRunnerGame game) {
        this.game = game;
        noiseSeedX = MathUtils.random(0f, 1000f);
        noiseSeedY = MathUtils.random(0f, 1000f);
        batch = new SpriteBatch();

        // FBO
        fbo = new FrameBuffer(Pixmap.Format.RGBA8888, Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), false);

        // Stage
        OrthographicCamera camera = new OrthographicCamera();
        Viewport viewport = new ScreenViewport(camera);
        stage = new Stage(viewport, batch);
        Gdx.input.setInputProcessor(stage);

        // 资源
        uiAtlas = new TextureAtlas(Gdx.files.internal("ui/button.atlas"));
        audioManager = AudioManager.getInstance();
        isMusicOn = audioManager.isMusicEnabled();

        bgCandyTex = new Texture(Gdx.files.internal("menu_bg/bg_front.png"));
        bgHellTex  = new Texture(Gdx.files.internal("menu_bg/bg_hell.png"));
        bgCandy = new TextureRegion(bgCandyTex);
        bgHell  = new TextureRegion(bgHellTex);
        bgCandy.flip(false, true);
        bgHell.flip(false, true);

        setupUI();

        if (isMusicOn) {
            audioManager.playMusic(de.tum.cit.fop.maze.audio.AudioType.MUSIC_MENU);
        }
    }

    private void setupUI() {
        Table root = new Table();
        root.setFillParent(true);
        stage.addActor(root);

        Label title = new Label("QQ Cha", game.getSkin(), "title");
        title.setAlignment(Align.center);
        title.setFontScale(2.1f);
        root.add(title).padBottom(16).row();
        Label title2 = new Label("Reset to Origin", game.getSkin(), "title");
        title2.setAlignment(Align.center);
        title2.setFontScale(1.1f);
        root.add(title2).padBottom(80).row();

        ButtonFactory bf = new ButtonFactory(game.getSkin());

        // 🔥 1. 新增：继续游戏按钮 (只有存档存在时显示)
        if (SaveManager.hasSaveFile()) {
            root.add(bf.create("CONTINUE", () -> {
                        // 读取存档
                        GameSaveData data = SaveManager.loadGame();
                        if (data != null) {
                            // 创建游戏界面，并传入存档
                            // 注意：因为存档里还没存难度配置，这里暂时默认用 Normal 难度
                            // 实际效果：怪的数量是新的随机，但玩家属性会保留
                            //game.setScreen(new GameScreen(game, new DifficultyConfig(de.tum.cit.fop.maze.game.Difficulty.NORMAL), data));
                        }
                    }))
                    .width(BUTTON_WIDTH).height(BUTTON_HEIGHT).padBottom(18).row();
        }

        root.add(bf.create("START GAME", game::goToGame))
                .width(BUTTON_WIDTH).height(BUTTON_HEIGHT).padBottom(18).row();

        root.add(bf.create("RESET THE WORLD", game::goToPV))
                .width(BUTTON_WIDTH).height(BUTTON_HEIGHT).padBottom(20).row();

        root.add(bf.create("DIFFICULTY", () -> game.setScreen(new DifficultySelectScreen(game, this))))
                .width(BUTTON_WIDTH).height(BUTTON_HEIGHT).padBottom(20).row();

        root.add(bf.create("CONTROLS", () -> game.setScreen(new KeyMappingScreen(game, this))))
                .width(BUTTON_WIDTH).height(BUTTON_HEIGHT).padBottom(20).row();

        // 🔥 2. 新增：排行榜按钮
        root.add(bf.create("LEADERBOARD", () -> game.setScreen(new LeaderboardScreen(game, this))))
                .width(BUTTON_WIDTH).height(BUTTON_HEIGHT).padBottom(20).row();

        createMusicButton();

        Table bottomRight = new Table();
        bottomRight.setFillParent(true);
        bottomRight.bottom().right();
        bottomRight.add(musicButton).size(100).padRight(40).padBottom(20);
        stage.addActor(bottomRight);
    }

    // ================= 渲染 (保持不变) =================
    @Override
    public void render(float delta) {
        if (Gdx.input.isKeyJustPressed(Input.Keys.R)) {
            changeEnabled = !changeEnabled;
        }
        if(changeEnabled) {
            time += delta;
            corruption = Math.min(1f, corruption + delta * 0.15f);
            int w = Gdx.graphics.getWidth();
            int h = Gdx.graphics.getHeight();

            fbo.begin();
            Gdx.gl.glClearColor(0, 0, 0, 1);
            Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
            batch.begin();
            batch.draw(bgCandy, 0, 0, w, h);

            int step = 4;
            for (int x = 0; x < w; x += step) {
                for (int y = 0; y < h; y += step) {
                    float n = PerlinNoise.noise(x * 0.004f, y * 0.004f + time * 0.2f);
                    if (n < corruption) {
                        batch.draw(bgHell.getTexture(), x, y, step, step, x/(float)w, y/(float)h, (x+step)/(float)w, (y+step)/(float)h);
                    } else if (n < corruption + 0.08f) {
                        float drip = PerlinNoise.noise(x * 0.01f, y * 0.02f - time * 0.6f);
                        float alpha = MathUtils.clamp((corruption + 0.08f - n) / 0.08f, 0f, 1f);
                        batch.setColor(1f, 0.6f, 0.8f, alpha * 0.6f);
                        batch.draw(bgCandy, x, y - drip * 6f, step, step + drip * 8f);
                        batch.setColor(Color.WHITE);
                    }
                }
            }
            batch.end();
            fbo.end();

            Gdx.gl.glClearColor(0, 0, 0, 1);
            Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
            Texture fboTex = fbo.getColorBufferTexture();
            batch.begin();
            batch.draw(fboTex, 0, 0, w, h);
            batch.end();
        }
        else {
            batch.begin();
            batch.draw(bgCandyTex, 0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
            batch.end();
        }
        stage.act(delta);
        stage.draw();
    }

    private void createMusicButton() {
        TextureRegionDrawable on  = new TextureRegionDrawable(uiAtlas.findRegion("frame178"));
        TextureRegionDrawable off = new TextureRegionDrawable(uiAtlas.findRegion("frame180"));
        ImageButton.ImageButtonStyle style = new ImageButton.ImageButtonStyle();
        style.imageUp   = isMusicOn ? on : off;
        style.imageOver = isMusicOn ? on : off;
        style.imageDown = isMusicOn ? on : off;
        musicButton = new ImageButton(style);
        musicButton.setTransform(true);
        musicButton.setOrigin(Align.center);
        musicButton.addListener(new com.badlogic.gdx.scenes.scene2d.InputListener() {
            public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor) {
                musicButton.clearActions();
                updateOriginToCenter(musicButton);
                musicButton.addAction(Actions.scaleTo(1.1f, 1.1f, 0.12f));
            }
            public void exit(InputEvent event, float x, float y, int pointer, Actor toActor) {
                musicButton.clearActions();
                updateOriginToCenter(musicButton);
                musicButton.addAction(Actions.scaleTo(1f, 1f, 0.12f));
            }
            public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
                updateOriginToCenter(musicButton);
                musicButton.setScale(0.95f);
                return true;
            }
            public void touchUp(InputEvent event, float x, float y, int pointer, int button) {
                updateOriginToCenter(musicButton);
                musicButton.setScale(1.1f);
                toggleMusic();
            }
        });
    }

    private void updateOriginToCenter(Actor actor) { actor.setOrigin(actor.getWidth() * 0.5f, actor.getHeight() * 0.5f); }

    private void toggleMusic() {
        isMusicOn = !isMusicOn;
        audioManager.setMusicEnabled(isMusicOn);
        musicButton.getStyle().imageUp = new TextureRegionDrawable(uiAtlas.findRegion(isMusicOn ? "frame178" : "frame180"));
        if (isMusicOn) audioManager.playMusic(de.tum.cit.fop.maze.audio.AudioType.MUSIC_MENU);
        else audioManager.pauseMusic();
    }

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

    @Override public void show() { Gdx.input.setInputProcessor(stage); }
    @Override public void hide() {}
    @Override public void pause() {}
    @Override public void resume() {}
}