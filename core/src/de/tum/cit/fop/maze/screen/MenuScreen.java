package de.tum.cit.fop.maze.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Dialog;
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import de.tum.cit.fop.maze.MazeRunnerGame;
import de.tum.cit.fop.maze.audio.AudioManager;
import de.tum.cit.fop.maze.audio.AudioType;
import de.tum.cit.fop.maze.entities.boss.BossLoadingScreen;
import de.tum.cit.fop.maze.game.Difficulty;
import de.tum.cit.fop.maze.game.save.SaveListPanel;
import de.tum.cit.fop.maze.game.story.StoryProgress;
import de.tum.cit.fop.maze.tools.ButtonFactory;
import de.tum.cit.fop.maze.tools.PerlinNoise;
import de.tum.cit.fop.maze.game.save.StorageManager;

public class MenuScreen implements Screen {
    private Table buttonTable;

    private final MazeRunnerGame game;
    private boolean changeEnabled = false;
    // ===== Story End State =====
    private boolean storyEnded = false;

    // 鼠标静止检测
    private float mouseIdleTimer = 0f;
    private int lastMouseX = -1;
    private int lastMouseY = -1;

    // ===== 渲染 =====
    private SpriteBatch batch;
    private Stage stage;
    private FrameBuffer fbo;

    private float time = 0f;
    private float corruption = 0f;

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

    private float getButtonWidth() {
        float screenWidth = Gdx.graphics.getWidth();
        return Math.min(800f, screenWidth * 0.6f);
    }

    private final float BUTTON_WIDTH  = 800f;
    private final float BUTTON_HEIGHT = 70f;

    private final StorageManager storage;

    public MenuScreen(MazeRunnerGame game) {
        this.game = game;
        this.storage = StorageManager.getInstance();

        batch = new SpriteBatch();
        stage = new Stage(new ScreenViewport(), batch);
        Gdx.input.setInputProcessor(stage);

        fbo = new FrameBuffer(Pixmap.Format.RGBA8888, Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), false);

        // ===== 资源加载 (带安全检查) =====
        try {
            uiAtlas = new TextureAtlas(Gdx.files.internal("ui/button.atlas"));
        } catch (Exception e) {
            Gdx.app.error("MenuScreen", "Failed to load button.atlas", e);
            throw e; // UI资源必须有，否则无法进行
        }

        audioManager = AudioManager.getInstance();
        isMusicOn = audioManager.isMusicEnabled();

        // 🔥 [修复] 背景图安全加载，防止文件丢失导致崩溃
        try {
            if (Gdx.files.internal("imgs/menu_bg/bg_front.png").exists()) {
                bgCandyTex = new Texture(Gdx.files.internal("imgs/menu_bg/bg_front.png"));
            } else {
                throw new Exception("bg_front.png not found");
            }

            if (Gdx.files.internal("imgs/menu_bg/bg_hell.png").exists()) {
                bgHellTex  = new Texture(Gdx.files.internal("imgs/menu_bg/bg_hell.png"));
            } else {
                throw new Exception("bg_hell.png not found");
            }
        } catch (Exception e) {
            Gdx.app.error("MenuScreen", "Background textures not found, using fallback color.", e);
            // 创建纯色背景作为 fallback
            Pixmap p = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
            p.setColor(Color.DARK_GRAY);
            p.fill();
            bgCandyTex = new Texture(p);
            bgHellTex = new Texture(p);
            p.dispose();
        }

        bgCandy = new TextureRegion(bgCandyTex);
        bgHell  = new TextureRegion(bgHellTex);
        bgCandy.flip(false, true);
        bgHell.flip(false, true);

        // ===== UI 布局 =====
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


        buttonTable = new Table();
        root.add(buttonTable).row();

        rebuildButtons();

        createMusicButton();

        Table bottomRight = new Table();
        bottomRight.setFillParent(true);
        bottomRight.bottom().right();
        bottomRight.add(musicButton).size(100).padRight(40).padBottom(20);
        stage.addActor(bottomRight);


    }
    private void rebuildButtons() {
        buttonTable.clear();

        ButtonFactory bf = new ButtonFactory(game.getSkin());
        boolean hasSave = storage.hasAnySave();

        float buttonWidth = getButtonWidth();
        float buttonPadding = Gdx.graphics.getWidth() > 1920 ? 18f : 15f;

        // ===== CONTINUE：只在有主存档时出现 =====
        if (hasSave) {
            buttonTable.add(
                            bf.create("CONTINUE", () -> {
                                SaveListPanel panel = new SaveListPanel(game, game.getSkin());
                                stage.addActor(panel);
                            })
                    ).width(buttonWidth).height(BUTTON_HEIGHT)
                    .padBottom(buttonPadding).row();
        }

        // ===== QUICK PLAY（临时测试按钮，不读主存档）=====
        buttonTable.add(
                        bf.create("DEBUG-STORY MODE", () -> {
                            game.startNewGame(Difficulty.NORMAL);
                        })
                ).width(buttonWidth).height(BUTTON_HEIGHT)
                .padBottom(buttonPadding).row();

        // ===== 剧情模式（独立系统）=====
        // ===== 剧情模式按钮（受 StoryProgress 控制）=====
        StoryProgress progress = StoryProgress.load();

// ❌ 如果第一章已经彻底完成 → 永久不显示
        if (!progress.isChapterFinished(1)) {

            buttonTable.add(
                            bf.create("RESET THE WORLD", () -> {

                                // ① 已经解锁 Boss（集齐三 relic，点过迎战）
                                if (progress.isBossUnlocked(1)) {

                                    // 👉 直接进 Boss Loading
                                    game.setScreen(new BossLoadingScreen(game));
                                    return;
                                }

                                // ② 看过 PV，但还没进 Boss
                                if (progress.isPvWatched(1)) {

                                    // 👉 直接进章节选择
                                    game.setScreen(new ChapterSelectScreen(game));
                                    return;
                                }

                                // ③ 什么都没发生过（第一次进剧情）
                                game.startStoryWithLoading();
                            })
                    ).width(buttonWidth).height(BUTTON_HEIGHT)
                    .padBottom(20).row();
        }


        // ===== 难度选择（仍然是主模式入口）=====
        buttonTable.add(
                        bf.create("DIFFICULTY", () ->
                                game.setScreen(new DifficultySelectScreen(game, this)))
                ).width(buttonWidth).height(BUTTON_HEIGHT)
                .padBottom(20).row();


        buttonTable.add(
                        bf.create("INFO", () -> game.setScreen(new InfoScreen(game, this)))
                ).width(buttonWidth).height(BUTTON_HEIGHT)
                .padBottom(buttonPadding).row();

        buttonTable.add(
                        bf.create("SETTINGS", () ->
                                game.setScreen(new SettingsScreen(
                                        game,
                                        SettingsScreen.SettingsSource.MAIN_MENU,
                                        this)))
                ).width(buttonWidth).height(BUTTON_HEIGHT)
                .padBottom(20).row();

        buttonTable.add(
                        bf.create("EXIT", game::exitGame)
                ).width(buttonWidth).height(BUTTON_HEIGHT)
                .row();
    }



    @Override
    public void render(float delta) {
        stage.getViewport().apply();
        batch.setProjectionMatrix(stage.getCamera().combined);

        if (storyEnded) {

            int mx = Gdx.input.getX();
            int my = Gdx.input.getY();

            if (mx == lastMouseX && my == lastMouseY) {
                mouseIdleTimer += delta;

                if (mouseIdleTimer >= 5f) {
                    changeEnabled = true;
                }
            } else {
                // 鼠标动了 → 重新计时
                mouseIdleTimer = 0f;
                lastMouseX = mx;
                lastMouseY = my;
                changeEnabled = false;
            }
        }





        if (Gdx.input.isKeyJustPressed(Input.Keys.T)) {
            game.debugEnterTutorial();
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.R)) {
            changeEnabled = !changeEnabled;
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.B)) {
            game.debugEnterBoss();
        }

        int w = Gdx.graphics.getWidth();
        int h = Gdx.graphics.getHeight();

        if (changeEnabled) {
            time += delta;
            corruption = Math.min(1f, corruption + delta * 0.15f);

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
                        batch.draw(bgHell.getTexture(), x, y, step, step, x / (float) w, y / (float) h, (x + step) / (float) w, (y + step) / (float) h);
                    }
                }
            }
            batch.end();
            fbo.end();

            Gdx.gl.glClearColor(0, 0, 0, 1);
            Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

            batch.begin();
            batch.draw(fbo.getColorBufferTexture(), 0, 0, w, h);
            batch.end();

        } else {
            batch.begin();
            batch.draw(bgCandyTex, 0, 0, w, h);
            batch.end();
        }

        stage.act(delta);
        stage.draw();
    }

    private void createMusicButton() {
        TextureRegionDrawable on  = new TextureRegionDrawable(uiAtlas.findRegion("frame178"));
        TextureRegionDrawable off = new TextureRegionDrawable(uiAtlas.findRegion("frame180"));

        ImageButton.ImageButtonStyle style = new ImageButton.ImageButtonStyle();
        style.imageUp = style.imageOver = style.imageDown = isMusicOn ? on : off;

        musicButton = new ImageButton(style);
        musicButton.setTransform(true);
        musicButton.setOrigin(Align.center);

        musicButton.addListener(new com.badlogic.gdx.scenes.scene2d.InputListener() {
            @Override
            public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor) {
                musicButton.clearActions();
                musicButton.setOrigin(Align.center);
                musicButton.addAction(Actions.scaleTo(1.05f, 1.05f, 0.15f));
            }
            @Override
            public void exit(InputEvent event, float x, float y, int pointer, Actor toActor) {
                musicButton.clearActions();
                musicButton.setOrigin(Align.center);
                musicButton.addAction(Actions.scaleTo(1f, 1f, 0.15f));
            }
            @Override
            public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
                musicButton.setOrigin(Align.center);
                musicButton.setScale(0.97f);
                return true;
            }
            @Override
            public void touchUp(InputEvent event, float x, float y, int pointer, int button) {
                musicButton.setOrigin(Align.center);
                musicButton.setScale(1.05f);
                toggleMusic();
            }
        });
    }

    private void toggleMusic() {
        isMusicOn = !isMusicOn;
        audioManager.setMusicEnabled(isMusicOn);
        musicButton.getStyle().imageUp = new TextureRegionDrawable(uiAtlas.findRegion(isMusicOn ? "frame178" : "frame180"));
        if (isMusicOn) audioManager.playMusic(AudioType.MUSIC_MENU);
        else audioManager.pauseMusic();
    }

    @Override public void resize(int w, int h) {
        stage.getViewport().update(w, h, true);
        if (fbo != null) fbo.dispose();
        fbo = new FrameBuffer(Pixmap.Format.RGBA8888, w, h, false);
        batch.setProjectionMatrix(stage.getCamera().combined);
    }

    @Override
    public void show() {
        Gdx.input.setInputProcessor(stage);
        rebuildButtons();

        StoryProgress progress = StoryProgress.load();

        storyEnded =
                progress.isPvWatched(1)
                        && progress.isBossUnlocked(1)
                        && progress.isChapterFinished(1);

        if (storyEnded) {
            // 🎵 播放“结局 Menu BGM”
            audioManager.playMusic(AudioType.MUSIC_MENU_END);

            // 重置 idle 计时
            mouseIdleTimer = 0f;
            lastMouseX = Gdx.input.getX();
            lastMouseY = Gdx.input.getY();
            changeEnabled = false;
        } else {
            audioManager.playMusic(AudioType.MUSIC_MENU);
        }
    }

    @Override public void hide() {}
    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void dispose() {
        stage.dispose();
        batch.dispose();
        fbo.dispose();
        uiAtlas.dispose();
        if (bgCandyTex != null) bgCandyTex.dispose();
        if (bgHellTex != null) bgHellTex.dispose();
    }
}