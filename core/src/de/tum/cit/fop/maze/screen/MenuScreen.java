package de.tum.cit.fop.maze.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
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

public class MenuScreen implements Screen {

    private final Stage stage;
    private ImageButton musicButton;
    private TextureAtlas atlas;
    private AudioManager audioManager;
    private boolean isMusicOn = true;

    public MenuScreen(MazeRunnerGame game) {
        // =====================================================
        // Camera & Stage
        // =====================================================
        OrthographicCamera camera = new OrthographicCamera();
        camera.zoom = 1.5f;

        Viewport viewport = new ScreenViewport(camera);
        stage = new Stage(viewport, game.getSpriteBatch());
        Gdx.input.setInputProcessor(stage);

        // =====================================================
        // 获取资源
        // =====================================================
        // 🔧 请修改这个路径为您的实际atlas路径
        atlas = new TextureAtlas(Gdx.files.internal("ui/button.atlas"));
        audioManager = AudioManager.getInstance();
        isMusicOn = audioManager.isMusicEnabled();

        // =====================================================
        // Root Layout
        // =====================================================
        Table mainTable = new Table();
        mainTable.setFillParent(true);
        stage.addActor(mainTable);

        // =====================================================
        // Title
        // =====================================================
        Label title = new Label(
                "Hello World from the Menu!",
                game.getSkin(),
                "title"
        );
        title.setAlignment(Align.center);
        title.setFontScale(1.1f);

        mainTable.add(title)
                .padBottom(80)
                .row();

        // =====================================================
        // Buttons (via ButtonFactory)
        // =====================================================
        ButtonFactory bf = new ButtonFactory(game.getSkin());

        mainTable.add(
                bf.create("START GAME", game::goToGame)
        ).padBottom(20).row();

        mainTable.add(
                bf.create("RESET THE WORLD", game::goToPV)
        ).padBottom(20).row();

        mainTable.add(
                bf.create("TEST", () -> {
                    System.out.println("TEST button clicked");
                })
        ).row();

        // =====================================================
// 音乐开关按钮（右下角）
// =====================================================
        createMusicButton();

        Table bottomRightTable = new Table();
        bottomRightTable.setFillParent(true);
        bottomRightTable.bottom().right();

// 获取屏幕尺寸
        float screenWidth = Gdx.graphics.getWidth();
        float screenHeight = Gdx.graphics.getHeight();

// 设置按钮位置（从屏幕边缘往回减）
        float buttonSize = 180f;
        float padding = -130f;

// 创建位置Table，先设置到右下角，然后往左上方移动
        bottomRightTable.add(musicButton)
                .size(buttonSize, buttonSize)
                .padRight(padding)    // 距离右边30像素
                .padBottom(padding);  // 距离底部30像素

        stage.addActor(bottomRightTable);

        // 初始播放音乐
        if (isMusicOn) {
            audioManager.playMusic(de.tum.cit.fop.maze.audio.AudioType.MUSIC_MENU);
        }
    }

    /**
     * 创建音乐按钮
     */
    private void createMusicButton() {
        // 创建图标
        TextureRegionDrawable musicOnIcon = new TextureRegionDrawable(atlas.findRegion("frame178"));
        TextureRegionDrawable musicOffIcon = new TextureRegionDrawable(atlas.findRegion("frame180"));

        ImageButton.ImageButtonStyle style = new ImageButton.ImageButtonStyle();
        style.imageUp = isMusicOn ? musicOnIcon : musicOffIcon;

        musicButton = new ImageButton(style);
        musicButton.setTransform(true);

        // 🔧 关键：设置缩放原点为按钮中心
        musicButton.setOrigin(Align.center);

        // 🔧 可选：也可以显式设置原点位置
        // musicButton.setOrigin(buttonSize/2, buttonSize/2);

        // 简单动画效果
        musicButton.addListener(new com.badlogic.gdx.scenes.scene2d.InputListener() {
            @Override
            public void enter(com.badlogic.gdx.scenes.scene2d.InputEvent event,
                              float x, float y, int pointer,
                              com.badlogic.gdx.scenes.scene2d.Actor fromActor) {
                musicButton.clearActions();
                musicButton.addAction(
                        com.badlogic.gdx.scenes.scene2d.actions.Actions.scaleTo(1.01f, 1.01f, 0.1f)
                );
            }

            @Override
            public void exit(com.badlogic.gdx.scenes.scene2d.InputEvent event,
                             float x, float y, int pointer,
                             com.badlogic.gdx.scenes.scene2d.Actor toActor) {
                musicButton.clearActions();
                musicButton.addAction(
                        com.badlogic.gdx.scenes.scene2d.actions.Actions.scaleTo(1f, 1f, 0.1f)
                );
            }

            @Override
            public boolean touchDown(com.badlogic.gdx.scenes.scene2d.InputEvent event,
                                     float x, float y, int pointer, int buttonCode) {
                musicButton.clearActions();
                musicButton.addAction(
                        com.badlogic.gdx.scenes.scene2d.actions.Actions.scaleTo(0.9f, 0.9f, 0.05f)
                );
                return true;
            }

            @Override
            public void touchUp(com.badlogic.gdx.scenes.scene2d.InputEvent event,
                                float x, float y, int pointer, int buttonCode) {
                musicButton.clearActions();
                musicButton.addAction(
                        com.badlogic.gdx.scenes.scene2d.actions.Actions.scaleTo(1f, 1f, 0.1f)
                );

                if (x >= 0 && x <= musicButton.getWidth() && y >= 0 && y <= musicButton.getHeight()) {
                    toggleMusic();
                }
            }
        });
    }

    /**
     * 切换音乐
     */
    private void toggleMusic() {
        isMusicOn = !isMusicOn;
        audioManager.setMusicEnabled(isMusicOn);

        // 更新图标
        TextureRegionDrawable newIcon;
        if (isMusicOn) {
            newIcon = new TextureRegionDrawable(atlas.findRegion("frame178"));
            audioManager.playMusic(de.tum.cit.fop.maze.audio.AudioType.MUSIC_MENU);
        } else {
            newIcon = new TextureRegionDrawable(atlas.findRegion("frame180"));
            audioManager.pauseMusic();
        }

        musicButton.getStyle().imageUp = newIcon;

        // 播放点击音效
        audioManager.playUIClick();
    }

    // =====================================================
    // Render & lifecycle
    // =====================================================
    @Override
    public void render(float delta) {
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        stage.act(Math.min(Gdx.graphics.getDeltaTime(), 1 / 30f));
        stage.draw();
    }

    @Override
    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
    }

    @Override
    public void dispose() {
        stage.dispose();
        if (atlas != null) {
            atlas.dispose();
        }
    }

    @Override
    public void show() {
        Gdx.input.setInputProcessor(stage);
    }

    @Override public void hide() {}
    @Override public void pause() {}
    @Override public void resume() {}
}