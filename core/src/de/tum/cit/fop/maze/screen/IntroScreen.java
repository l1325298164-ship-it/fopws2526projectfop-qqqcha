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
