package de.tum.cit.fop.maze.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import de.tum.cit.fop.maze.MazeRunnerGame;
import de.tum.cit.fop.maze.tools.ButtonFactory;

public class IntroScreen implements Screen {

    private final MazeRunnerGame game;
    private final PVExit exitType;
    private final String atlasPath;
    private final String regionName;

    private TextureAtlas pvAtlas;
    private Animation<TextureRegion> pvAnim;
    private Texture backgroundTexture;

    private float stateTime = 0f;
    private SpriteBatch batch;

    // ===== PV4 UI =====
    private Stage stage;
    private ButtonFactory buttonFactory;
    private boolean showPV4Buttons = false;
    private boolean animationFinished = false;

    // 定义世界坐标尺寸
    private static final float WORLD_WIDTH = 2784f;
    private static final float WORLD_HEIGHT = 1536f;
    private static final float CONTENT_SCALE = 0.85f;
    private Viewport viewport;
    private static final float FRAME_DURATION = 1.0f;
    private boolean exited = false;

    public enum PVExit {
        NEXT_STAGE,   // PV1–PV3
        TO_MENU,      // 失败
        PV4_CHOICE    // 最终确认
    }

    public IntroScreen(MazeRunnerGame game, String atlasPath, String regionName, PVExit exitType) {
        this.game = game;
        this.atlasPath = atlasPath;
        this.regionName = regionName;
        this.exitType = exitType;
        this.batch = game.getSpriteBatch();
        this.viewport = new FitViewport(WORLD_WIDTH, WORLD_HEIGHT);
    }

    @Override
    public void show() {
        Gdx.app.debug("IntroScreen", "Loading PV: " + regionName);

        // 重置状态
        stateTime = 0f;
        animationFinished = false;
        showPV4Buttons = false;
        exited = false;

        try {
            // 加载背景图
            backgroundTexture = new Texture(Gdx.files.internal("pv/background.PNG"));

            // 加载漫画 Atlas
            pvAtlas = new TextureAtlas(Gdx.files.internal(atlasPath));

            Array<TextureAtlas.AtlasRegion> frames = pvAtlas.findRegions(regionName);
            if (frames.isEmpty()) {
                Gdx.app.error("IntroScreen", "❌ 找不到图片: " + regionName);
                // 如果加载失败，直接退出
                handleExit();
                return;
            }

            pvAnim = new Animation<>(FRAME_DURATION, frames, Animation.PlayMode.NORMAL);

            if (exitType == PVExit.PV4_CHOICE) {
                stage = new Stage(viewport, batch);
                buttonFactory = new ButtonFactory(game.getSkin());
                createPV4Buttons();
                // 初始时不显示按钮
                for (com.badlogic.gdx.scenes.scene2d.Actor actor : stage.getActors()) {
                    actor.setVisible(false);
                }
            }

        } catch (Exception e) {
            Gdx.app.error("IntroScreen", "❌ 资源加载错误: " + e.getMessage());
            e.printStackTrace();
            handleExit();
        }
    }

    private void createPV4Buttons() {
        if (stage == null) return;

        TextButton startButton = buttonFactory.createNavigationButton(
                "Start Chapter",
                () -> game.onPV4Choice(MazeRunnerGame.PV4Result.START)
        );

        TextButton exitButton = buttonFactory.createSilent(
                "Back to Menu",
                () -> game.onPV4Choice(MazeRunnerGame.PV4Result.EXIT)
        );

        float buttonWidth = 420f;
        float buttonHeight = 110f;

        startButton.setSize(buttonWidth, buttonHeight);
        exitButton.setSize(buttonWidth, buttonHeight);

        startButton.setPosition(
                (WORLD_WIDTH - buttonWidth) / 2,
                WORLD_HEIGHT * 0.28f
        );

        exitButton.setPosition(
                (WORLD_WIDTH - buttonWidth) / 2,
                WORLD_HEIGHT * 0.18f
        );

        stage.addActor(startButton);
        stage.addActor(exitButton);
    }

    @Override
    public void render(float delta) {
        stateTime += delta;

        // 清屏
        ScreenUtils.clear(0, 0, 0, 1);

        // 应用视口
        viewport.apply();
        batch.setProjectionMatrix(viewport.getCamera().combined);

        // 检查资源是否加载成功
        if (pvAnim == null || backgroundTexture == null) {
            // 资源加载失败，直接退出
            handleExit();
            return;
        }

        batch.begin();

        // 绘制背景
        batch.draw(backgroundTexture, 0, 0, WORLD_WIDTH, WORLD_HEIGHT);

        // 检查动画是否还在播放
        if (!pvAnim.isAnimationFinished(stateTime)) {
            // 绘制当前帧
            int currentFrameIndex = pvAnim.getKeyFrameIndex(stateTime);
            TextureRegion[] frames = pvAnim.getKeyFrames();

            float scaledWidth = WORLD_WIDTH * CONTENT_SCALE;
            float scaledHeight = WORLD_HEIGHT * CONTENT_SCALE;
            float offsetX = (WORLD_WIDTH - scaledWidth) / 2;
            float offsetY = (WORLD_HEIGHT - scaledHeight) / 2;

            for (int i = 0; i <= currentFrameIndex && i < frames.length; i++) {
                TextureRegion region = frames[i];
                batch.draw(region, offsetX, offsetY, scaledWidth, scaledHeight);
            }
        } else {
            // 动画播放完成
            if (!animationFinished) {
                animationFinished = true;

                // 绘制最后一帧
                TextureRegion[] frames = pvAnim.getKeyFrames();
                if (frames.length > 0) {
                    float scaledWidth = WORLD_WIDTH * CONTENT_SCALE;
                    float scaledHeight = WORLD_HEIGHT * CONTENT_SCALE;
                    float offsetX = (WORLD_WIDTH - scaledWidth) / 2;
                    float offsetY = (WORLD_HEIGHT - scaledHeight) / 2;

                    batch.draw(frames[frames.length - 1], offsetX, offsetY, scaledWidth, scaledHeight);
                }
            }

            // PV4需要显示按钮
            if (exitType == PVExit.PV4_CHOICE && !showPV4Buttons) {
                showPV4Buttons = true;
                // 设置输入处理器并显示按钮
                Gdx.input.setInputProcessor(stage);
                for (com.badlogic.gdx.scenes.scene2d.Actor actor : stage.getActors()) {
                    actor.setVisible(true);
                }
            }
        }

        batch.end();

        // 绘制舞台（按钮）
        if (stage != null && showPV4Buttons) {
            stage.act(delta);
            stage.draw();
        }

        // 处理非PV4的自动跳转
        if (exitType != PVExit.PV4_CHOICE && pvAnim.isAnimationFinished(stateTime)) {
            // 动画播放完等待2秒
            if (stateTime > pvAnim.getAnimationDuration() + 2.0f) {
                handleExit();
            }
        }
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);
    }

    private void handleExit() {
        if (exited) return;
        exited = true;

        switch (exitType) {
            case NEXT_STAGE:
                game.advanceStory();
                break;
            case TO_MENU:
                game.goToMenu();
                break;
            case PV4_CHOICE:
                // PV4等待用户选择，不自动退出
                break;
        }
    }

    @Override
    public void dispose() {
        if (pvAtlas != null) {
            pvAtlas.dispose();
            pvAtlas = null;
        }
        if (backgroundTexture != null) {
            backgroundTexture.dispose();
            backgroundTexture = null;
        }
        if (stage != null) {
            stage.dispose();
            stage = null;
        }
        pvAnim = null;
    }

    @Override
    public void pause() {}

    @Override
    public void resume() {}

    @Override
    public void hide() {
        // 隐藏时重置输入处理器
        if (stage != null) {
            Gdx.input.setInputProcessor(null);
        }
    }
}