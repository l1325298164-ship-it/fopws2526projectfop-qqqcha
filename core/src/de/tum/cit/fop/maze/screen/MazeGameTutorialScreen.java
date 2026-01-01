package de.tum.cit.fop.maze.screen;

import com.badlogic.gdx.Screen;
import de.tum.cit.fop.maze.MazeRunnerGame;
import de.tum.cit.fop.maze.game.DifficultyConfig;
import de.tum.cit.fop.maze.game.GameManager;
import de.tum.cit.fop.maze.input.PlayerInputHandler;

public class MazeGameTutorialScreen implements Screen {
    private final MazeRunnerGame game;
    private final DifficultyConfig config;

    private GameManager gm;
    private boolean tutorialCompleted = false;
    private boolean finished = false;
    enum TutorialStep {
        MOVE,
        ATTACK,
        DASH,
        OPEN_DOOR,
        ESCAPE
    }
    private TutorialStep step = TutorialStep.MOVE;

    enum MazeGameTutorialResult {
        SUCCESS,          // 正常完成
        FAILURE_DEAD,     // 死亡
        EXIT_BY_PLAYER    // 玩家点了「退出冒险」
    }

    private boolean movedUp, movedDown, movedLeft, movedRight;
    private boolean reachedTarget = false;

    // 示例：目标点（世界坐标 or tile 坐标）
    private static final float TARGET_X = 10f;
    private static final float TARGET_Y = 5f;
    private static final float TARGET_RADIUS = 0.5f;


    public MazeGameTutorialScreen(MazeRunnerGame game, DifficultyConfig config) {
        this.game = game;
        this.config = config;



    }
    @Override
    public void show() {
        gm = new GameManager(config);

        // 🔒 教程专属限制
        gm.setTutorialMode(true);

    }

    @Override
    public void render(float delta) {
        update(delta);
        renderGame(delta);


    }
    private void update(float delta) {
        // ================= 教程用移动标记 =================
        movedUp = false;
        movedDown = false;
        movedLeft = false;
        movedRight = false;
        if (finished) return;

        gm.update(delta);
        // ① 检测方向输入（一次即可）
        PlayerInputHandler input = gm.getInputHandler();

        if (input.hasMovedUp()
                && input.hasMovedDown()
                && input.hasMovedLeft()
                && input.hasMovedRight()) {
            // 上下左右已完成
        }
        // ② 检查是否到达目标点
        float px = gm.getPlayer().getX();
        float py = gm.getPlayer().getY();

        if (Math.abs(px - TARGET_X) < TARGET_RADIUS &&
                Math.abs(py - TARGET_Y) < TARGET_RADIUS) {
            reachedTarget = true;
        }

        // ③ 教程完成条件
        if (movedUp && movedDown && movedLeft && movedRight && reachedTarget) {
            finished = true;
            game.onTutorialFinished(this);
        }



        if (checkTutorialComplete()) {
            finished = true;
            game.onTutorialFinished(this);
        }
        if (gm.isPlayerDead()) {
            finished = true;
            game.onTutorialFailed(this, MazeGameTutorialResult.FAILURE_DEAD);
        }

        switch (step) {
            case MOVE -> {
                if (playerMovedOnce) {
                    step = TutorialStep.ATTACK;
                    showTip("按 空格 攻击敌人");
                }
            }
            case ATTACK -> {
                if (enemyKilled) {
                    step = TutorialStep.DASH;
                }
            }
            case DASH -> {
                if (dashUsed) {
                    step = TutorialStep.OPEN_DOOR;
                }
            }
            case OPEN_DOOR -> {
                if (doorOpened) {
                    step = TutorialStep.ESCAPE;
                }
            }
            case ESCAPE -> {
                tutorialCompleted = true;
            }
        }
        if (exitButtonPressed) {
            finished = true;
            game.onTutorialFailed(this, MazeGameTutorialResult.EXIT_BY_PLAYER);
        }
    }
    public void onTutorialFinished(MazeGameTutorialScreen screen) {
        // ✅ 教程完成 → 推进剧情
        setScreen(new PV4Screen(this, /* fromTutorial = true */));
    }

    public void onTutorialFailed(
            MazeGameTutorialScreen screen,
            MazeGameTutorialResult result
    ) {
        switch (result) {
            case FAILURE_DEAD, EXIT_BY_PLAYER -> {
                // ❌ 不推进剧情
                setScreen(new ChapterSelectScreen(this));
            }
        }
    }

    private boolean checkTutorialComplete() {
        // 👇 你以后所有教程判定都写在这里
        return tutorialCompleted;
    }

    private void renderGame(float delta) {
        // 复用 GameScreen 的渲染逻辑（你可以 copy 或抽工具类）
    }
    @Override
    public void resize(int w, int h) {

    }

    @Override
    public void pause() {

    }

    @Override
    public void resume() {

    }

    @Override
    public void hide() {

    }

    @Override
    public void dispose() {

    }
}
