package de.tum.cit.fop.maze.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
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
import de.tum.cit.fop.maze.menu_tile.CorruptionManager;
import de.tum.cit.fop.maze.menu_tile.TileManager;
import de.tum.cit.fop.maze.tools.ButtonFactory;

public class MenuScreen implements Screen {

    private final MazeRunnerGame game;
    private final Stage stage;

    // UI
    private ImageButton musicButton;
    private TextureAtlas uiAtlas;
    private AudioManager audioManager;
    private boolean isMusicOn = true;

    // 世界状态
    private CorruptionManager corruptionManager;
    private TileManager tileManager;
    private int currentStage = 0;
    private float idleTimer = 0f;
    private int loadedTileStage = -1;


    // 背景
    private Texture bgCandy;
    private Texture bgHell;

    public MenuScreen(MazeRunnerGame game) {
        this.game = game;

        // ================= Camera & Stage =================
        OrthographicCamera camera = new OrthographicCamera();
        Viewport viewport = new ScreenViewport(camera);
        stage = new Stage(viewport, game.getSpriteBatch());
        Gdx.input.setInputProcessor(stage);

        // ================= 资源 =================
        uiAtlas = new TextureAtlas(Gdx.files.internal("ui/button.atlas"));
        audioManager = AudioManager.getInstance();
        isMusicOn = audioManager.isMusicEnabled();

        bgCandy = new Texture(Gdx.files.internal("menu_bg/bg_front.png"));
        bgHell  = new Texture(Gdx.files.internal("menu_bg/bg_hell.png"));

        corruptionManager = new CorruptionManager();
        tileManager = new TileManager(); // 只管 stage 1 / 2

        // ================= UI =================
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
        root.add(bf.create("CONTROLS", () -> {
            idleTimer = 0f;
            game.setScreen(new KeyMappingScreen(game, MenuScreen.this));
        })).padBottom(20).row();

        root.add(bf.create("TEST", () -> {
            corruptionManager.onUselessClick();
            idleTimer = 0f;
        })).row();

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
        corruptionManager.onUselessClick();
        idleTimer = 0f;

        TextureRegionDrawable icon = new TextureRegionDrawable(
                uiAtlas.findRegion(isMusicOn ? "frame178" : "frame180")
        );
        musicButton.getStyle().imageUp = icon;

        if (isMusicOn) {
            audioManager.playMusic(de.tum.cit.fop.maze.audio.AudioType.MUSIC_MENU);
        } else {
            audioManager.pauseMusic();
        }
    }

    @Override
    public void render(float delta) {
        /// /////////////////////////////////////////////
        System.out.println("[MenuScreen] render delta=" + delta);
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        // ===== 腐化更新 =====
        idleTimer += delta;
        boolean idle = idleTimer > 3f;

        corruptionManager.update(delta, idle);

        int newStage = corruptionManager.getStage();

        if (newStage != currentStage) {
/// /////////////////////////////
            System.out.println("[MenuScreen] Stage change: "
                    + currentStage + " -> " + newStage);
            currentStage = newStage;

            if (currentStage > 0 && currentStage != loadedTileStage) {
                /// //////////
                System.out.println("[MenuScreen] loadStage(" + currentStage + ")");
                tileManager.loadStage(currentStage);
                loadedTileStage = currentStage;
            }
        }
        if (currentStage > 0) {
            /// ///////////////////////
            System.out.println("[MenuScreen] calling tileManager.update()");
            tileManager.update(delta);
        }

        // ===== 画背景 =====
        game.getSpriteBatch().begin();

        if (currentStage == 0) {
            game.getSpriteBatch().draw(
                    bgCandy, 0, 0,
                    Gdx.graphics.getWidth(),
                    Gdx.graphics.getHeight()
            );
        } else {
            game.getSpriteBatch().draw(
                    bgHell, 0, 0,
                    Gdx.graphics.getWidth(),
                    Gdx.graphics.getHeight()
            );
            tileManager.render(game.getSpriteBatch());
        }

        game.getSpriteBatch().end();

        // ===== UI =====
        stage.act(delta);
        stage.draw();
    }

    @Override
    public void resize(int w, int h) {
        stage.getViewport().update(w, h, true);
    }

    @Override
    public void dispose() {
        stage.dispose();
        uiAtlas.dispose();
        bgCandy.dispose();
        bgHell.dispose();
        tileManager.dispose();
    }

    @Override public void show() {}
    @Override public void hide() {}
    @Override public void pause() {}
    @Override public void resume() {}
}
