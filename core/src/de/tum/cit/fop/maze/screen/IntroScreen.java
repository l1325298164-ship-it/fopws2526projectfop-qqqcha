package de.tum.cit.fop.maze.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import de.tum.cit.fop.maze.MazeRunnerGame;
import de.tum.cit.fop.maze.audio.AudioManager;
import de.tum.cit.fop.maze.audio.AudioType;
import de.tum.cit.fop.maze.game.story.StoryProgress;
import de.tum.cit.fop.maze.tools.ButtonFactory;

public class IntroScreen implements Screen {

    private final MazeRunnerGame game;
    private final Animation<TextureRegion> pvAnim;
    private final PVExit exitType;
    private final AudioType musicType;
    private final PVFinishedListener finishedListener;

    private float stateTime = 0f;
    private boolean exited = false;
    private boolean animationFinished = false;

    private final SpriteBatch batch;
    private final Viewport viewport;

    // ===== PV4 UI =====
    private Stage stage;
    private ButtonFactory buttonFactory;
    private boolean showPV4Buttons = false;

    // 世界尺寸
    private static final float WORLD_WIDTH = 2784f;
    private static final float WORLD_HEIGHT = 1536f;



    //esc
    private float skipTimer = 0f; // 记录长按时间
    private static final float SKIP_THRESHOLD = 2.0f; // 设定为 2 秒
    private boolean isSkipping = false;
    private TextButton escButton; // 新增按钮引用
    private static final float PROGRESS_BAR_WIDTH = 230f;
    private static final float PROGRESS_BAR_HEIGHT = 15f;

    public enum PVExit {
        NEXT_STAGE,
        TO_MENU,
        PV4_CHOICE
    }

    public interface PVFinishedListener {
        void onPVFinished();
    }

    public IntroScreen(
            MazeRunnerGame game,
            Animation<TextureRegion> animation,
            PVExit exit,
            AudioType audio,
            PVFinishedListener listener
    ) {
        this.game = game;
        this.pvAnim = animation;
        this.exitType = exit;
        this.musicType = audio;
        this.finishedListener = listener;

        this.batch = game.getSpriteBatch();
        this.viewport = new FitViewport(WORLD_WIDTH, WORLD_HEIGHT);
    }

    @Override
    public void show() {
        Gdx.app.debug("IntroScreen", "PV started");

        stateTime = 0f;
        exited = false;
        animationFinished = false;
        showPV4Buttons = false;

        if (musicType != null) {
            game.getSoundManager().playMusic(musicType);
        }

        if (exitType == PVExit.PV4_CHOICE) {
            stage = new Stage(viewport, batch);
            buttonFactory = new ButtonFactory(game.getSkin());
            createPV4Buttons();
            stage.getActors().forEach(actor -> actor.setVisible(false));
        }

        // 初始化 Stage（如果不为 PV4_CHOICE 也要初始化，因为我们要放跳过按钮）
        if (stage == null) {
            stage = new Stage(viewport, batch);
            buttonFactory = new ButtonFactory(game.getSkin());
        }

        createSkipUI();
        Gdx.input.setInputProcessor(stage);
    }

    private void createSkipUI() {
        buttonFactory.setAnimationParams(0.11f, 0.10f, 0.10f, 1.0f, 1.0f, 1.0f);
        escButton = buttonFactory.create("ESC SKIP", this::skipAnimation);
        buttonFactory.setAnimationParams(0.12f, 0.08f, 0.10f, 1.05f, 0.95f, 1.08f);

        escButton.setSize(240, 60);
        // ⭐ 关键：强制设置原点为按钮的几何中心
        escButton.setOrigin(Align.center);
        escButton.setPosition(40, WORLD_HEIGHT - 120);

        stage.addActor(escButton);
    }
    private void createPV4Buttons() {
        if (stage == null) return;

        TextButton startButton = buttonFactory.createNavigationButton(
                "Start Chapter",
                () -> { // 🔒 永久剧情存档点①：PV4 已观看
                        StoryProgress progress = StoryProgress.load();
        progress.markPvWatched(1);
        progress.save();

        // 👉 继续原有流程
        game.onPV4Choice(MazeRunnerGame.PV4Result.START);
    }
        );

        float buttonWidth = 600f;
        float buttonHeight = 110f;

        startButton.setSize(buttonWidth, buttonHeight);
        startButton.setPosition(
                (WORLD_WIDTH - buttonWidth) / 2f,
                WORLD_HEIGHT * 0.28f
        );

        stage.addActor(startButton);
    }

    @Override
    public void render(float delta) {
        // 1. 逻辑更新
        stateTime += Math.min(delta, 1f / 24f);
        checkSkipInput(delta);

        // 2. 清屏
        ScreenUtils.clear(0, 0, 0, 1);

        // 3. 应用视口
        viewport.apply();
        batch.setProjectionMatrix(viewport.getCamera().combined);

        // 4. 渲染动画和进度条
        batch.begin();
        if (!pvAnim.isAnimationFinished(stateTime)) {
            TextureRegion frame = pvAnim.getKeyFrame(stateTime, false);
            batch.draw(frame, 0, 0, WORLD_WIDTH, WORLD_HEIGHT);

            // 绘制左上角进度条 (一定要在 batch 内)
            renderProgressBar();
        } else {
            // 播放结束显示最后一帧
            animationFinished = true;
            TextureRegion[] frames = pvAnim.getKeyFrames();
            TextureRegion lastFrame = frames[frames.length - 1];
            batch.draw(lastFrame, 0, 0, WORLD_WIDTH, WORLD_HEIGHT);

            // PV4 逻辑
            if (exitType == PVExit.PV4_CHOICE && !showPV4Buttons) {
                showPV4Buttons = true;
                Gdx.input.setInputProcessor(stage);
                AudioManager.getInstance().stopMusic();
                // ⭐ PV4 按钮出现时，隐藏 ESC
                if (escButton != null) {
                    escButton.setVisible(false);
                }
                // 显示所有 PV4 按钮
                stage.getActors().forEach(actor -> {
                    if (actor != escButton) actor.setVisible(true);
                });
            }
        }
        batch.end();

        // 5. 渲染 UI Stage (包含 ESC 按钮)
        if (stage != null) {
            // 确保 Stage 使用的矩阵也是 Viewport 的
            stage.getViewport().apply();
            stage.act(delta);
            stage.draw();
        }

        // 6. 退出检查逻辑
        if (exitType != PVExit.PV4_CHOICE
                && pvAnim.isAnimationFinished(stateTime)
                && stateTime > pvAnim.getAnimationDuration() + 2f) {
            handleExit();
        }
    }

    private void renderProgressBar() {
        if (skipTimer <= 0) return;

        TextureRegion white = game.getSkin().getRegion("white");
        if (white == null) return;

        float x = 40;
        float y = WORLD_HEIGHT - 50;
        float progress = Math.min(skipTimer / SKIP_THRESHOLD, 1.0f);
        float currentWidth = PROGRESS_BAR_WIDTH * progress;

        // 1. 绘制背景
        batch.setColor(0.1f, 0.1f, 0.1f, 0.6f);
        batch.draw(white, x, y, PROGRESS_BAR_WIDTH, PROGRESS_BAR_HEIGHT);

        // 2. 绘制渐变进度条
        // 创建一个临时的 Sprite (或者在类里创建一个成员变量重用它以提高性能)
        com.badlogic.gdx.graphics.g2d.Sprite gradientBar = new com.badlogic.gdx.graphics.g2d.Sprite(white);
        gradientBar.setPosition(x, y);
        gradientBar.setSize(currentWidth, PROGRESS_BAR_HEIGHT);

        // 定义颜色
        Color colorPink = new Color(1f, 0.4f, 0.7f, 1f);   // 左侧粉色
        Color colorYellow = new Color(1f, 1f, 0.2f, 1f); // 右侧黄色

        // 设置四个顶点的颜色：左下，左上，右上，右下
        gradientBar.getVertices()[com.badlogic.gdx.graphics.g2d.SpriteBatch.C1] = colorPink.toFloatBits();
        gradientBar.getVertices()[com.badlogic.gdx.graphics.g2d.SpriteBatch.C2] = colorPink.toFloatBits();
        gradientBar.getVertices()[com.badlogic.gdx.graphics.g2d.SpriteBatch.C3] = colorYellow.toFloatBits();
        gradientBar.getVertices()[com.badlogic.gdx.graphics.g2d.SpriteBatch.C4] = colorYellow.toFloatBits();

        // 注意：Sprite 绘制不需要显式传入颜色，因为它已经存在于顶点数据里了
        batch.setColor(Color.WHITE);
        gradientBar.draw(batch);
    }
    private void checkSkipInput(float delta) {
        if (Gdx.input.isKeyPressed(com.badlogic.gdx.Input.Keys.ESCAPE)) {
            skipTimer += delta;

            if (escButton != null) {
                // 确保原点始终在中心，防止缩放时位置偏移
                escButton.setOrigin(Align.center);
                escButton.setScale(0.95f); // 稍微缩小一点点即可，不要太多
                escButton.setColor(Color.LIGHT_GRAY);
            }

            if (skipTimer >= SKIP_THRESHOLD && !isSkipping) {
                isSkipping = true;
                skipAnimation();
            }
        } else {
            skipTimer = 0f;
            if (escButton != null && !escButton.isPressed()) {
                escButton.setScale(1.0f);
                escButton.setColor(Color.WHITE);
            }
        }
    }


    /**
     * 直接跳到动画结束状态
     */
    private void skipAnimation() {
        // 将 stateTime 设置为动画长度，使其进入“播放完成”状态
        stateTime = pvAnim.getAnimationDuration();

        // 如果是普通的 NEXT_STAGE，直接 handleExit
        if (exitType != PVExit.PV4_CHOICE) {
            handleExit();
        }
        // 如果是 PV4_CHOICE，render 里的逻辑会自动显示按钮
    }

    /**
     * 可选：在左下角或右下角渲染一个简单的“跳过中”进度
     */
    private void renderSkipPrompt() {
        if (skipTimer > 0) {
            // 这里可以画简单的文字或者进度条
            // 为了简单，我们只输出 Log，或者你可以用 game.getFont() 画一段文字
            // 例如：batch.draw(whitePixel, 100, 100, (skipTimer / SKIP_THRESHOLD) * 200, 10);
        }
    }
    private void handleExit() {
        if (exited) return;
        exited = true;

        switch (exitType) {
            case NEXT_STAGE -> {
                if (finishedListener != null) {
                    finishedListener.onPVFinished(); // ⭐ Pipeline 接管
                }
            }
            case TO_MENU -> game.goToMenu();
            case PV4_CHOICE -> {
                // PV4 等按钮
            }
        }
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);
    }

    @Override
    public void hide() {
        if (stage != null) {
            Gdx.input.setInputProcessor(null);
        }
    }

    @Override
    public void dispose() {
        if (stage != null) {
            stage.dispose();
            stage = null;
        }
    }

    @Override public void pause() {}
    @Override public void resume() {}
}
