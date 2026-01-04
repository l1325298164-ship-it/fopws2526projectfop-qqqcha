package de.tum.cit.fop.maze.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import de.tum.cit.fop.maze.MazeRunnerGame;
import de.tum.cit.fop.maze.audio.AudioType;
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
    }

    private void createPV4Buttons() {
        if (stage == null) return;

        TextButton startButton = buttonFactory.createNavigationButton(
                "Start Chapter",
                () -> game.onPV4Choice(MazeRunnerGame.PV4Result.START)
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
        // 1. 更新动画时间
        stateTime += Math.min(delta, 1f / 24f);

        // 2. 长按 ESC 跳过逻辑
        checkSkipInput(delta);

        // 3. 渲染背景和动画
        ScreenUtils.clear(0, 0, 0, 1);
        viewport.apply();
        batch.setProjectionMatrix(viewport.getCamera().combined);

        batch.begin();

        // 渲染动画帧 (如果没跳过且没播完)
        if (!pvAnim.isAnimationFinished(stateTime)) {
            TextureRegion frame = pvAnim.getKeyFrame(stateTime, false);
            batch.draw(frame, 0, 0, WORLD_WIDTH, WORLD_HEIGHT);

            // 可选：渲染跳过进度条提示 (让玩家知道长按有效)
            renderSkipPrompt();
        } else {
            // 播放结束逻辑 (保持原有代码不变)
            animationFinished = true;
            TextureRegion lastFrame = pvAnim.getKeyFrames()[pvAnim.getKeyFrames().length - 1];
            batch.draw(lastFrame, 0, 0, WORLD_WIDTH, WORLD_HEIGHT);

            if (exitType == PVExit.PV4_CHOICE && !showPV4Buttons) {
                showPV4Buttons = true;
                Gdx.input.setInputProcessor(stage);
                stage.getActors().forEach(actor -> actor.setVisible(true));
            }
        }
        batch.end();

        // UI 渲染 (PV4 按钮等)
        if (stage != null && showPV4Buttons) {
            stage.act(delta);
            stage.draw();
        }

        // 自动退出检查 (如果没跳过，等待2秒静止)
        if (exitType != PVExit.PV4_CHOICE
                && pvAnim.isAnimationFinished(stateTime)
                && stateTime > pvAnim.getAnimationDuration() + 2f) {
            handleExit();
        }



        stateTime += Math.min(delta, 1f / 24f);

        ScreenUtils.clear(0, 0, 0, 1);

        viewport.apply();
        batch.setProjectionMatrix(viewport.getCamera().combined);

        batch.begin();

        if (!pvAnim.isAnimationFinished(stateTime)) {
            TextureRegion frame = pvAnim.getKeyFrame(stateTime, false);
            batch.draw(frame, 0, 0, WORLD_WIDTH, WORLD_HEIGHT);
        } else {
            if (!animationFinished) {
                animationFinished = true;
                TextureRegion lastFrame =
                        pvAnim.getKeyFrames()[pvAnim.getKeyFrames().length - 1];
                batch.draw(lastFrame, 0, 0, WORLD_WIDTH, WORLD_HEIGHT);
            }

            if (exitType == PVExit.PV4_CHOICE && !showPV4Buttons) {
                showPV4Buttons = true;
                Gdx.input.setInputProcessor(stage);
                stage.getActors().forEach(actor -> actor.setVisible(true));
            }
        }

        batch.end();

        if (stage != null && showPV4Buttons) {
            stage.act(delta);
            stage.draw();
        }

        // ⭐ 非 PV4：播放结束后统一交给 Pipeline / Game
        if (exitType != PVExit.PV4_CHOICE
                && pvAnim.isAnimationFinished(stateTime)
                && stateTime > pvAnim.getAnimationDuration() + 2f) {
            handleExit();
        }
    }


    /**
     * 检测 ESC 长按输入
     */
    private void checkSkipInput(float delta) {
        if (Gdx.input.isKeyPressed(com.badlogic.gdx.Input.Keys.ESCAPE)) {
            skipTimer += delta;
            if (skipTimer >= SKIP_THRESHOLD && !isSkipping) {
                isSkipping = true;
                Gdx.app.log("IntroScreen", "Skipping PV...");
                skipAnimation();
            }
        } else {
            skipTimer = 0f; // 松开按键，重置计时器
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
