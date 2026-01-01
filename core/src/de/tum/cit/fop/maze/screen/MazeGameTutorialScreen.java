package de.tum.cit.fop.maze.screen;

import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.ScreenUtils;
import de.tum.cit.fop.maze.MazeRunnerGame;
import de.tum.cit.fop.maze.game.DifficultyConfig;
import de.tum.cit.fop.maze.game.GameManager;
import de.tum.cit.fop.maze.input.PlayerInputHandler;
import de.tum.cit.fop.maze.utils.CameraManager;
import de.tum.cit.fop.maze.utils.TextureManager;

public class MazeGameTutorialScreen implements Screen {

    private final MazeRunnerGame game;
    private final DifficultyConfig config;

    private CameraManager cameraManager;
    private SpriteBatch batch;

    private GameManager gm;
    private boolean finished = false;

    public enum MazeGameTutorialResult {
        SUCCESS,
        FAILURE_DEAD,
        EXIT_BY_PLAYER
    }

    private boolean movedUp, movedDown, movedLeft, movedRight;
    private boolean reachedTarget = false;

    private static final float CELL_SIZE = 32f;
    private static final int TARGET_X = 10;
    private static final int TARGET_Y = 5;

    public MazeGameTutorialScreen(MazeRunnerGame game, DifficultyConfig config) {
        this.game = game;
        this.config = config;
    }

    @Override
    public void show() {
        gm = game.getGameManager();
        gm.setTutorialMode(true);

        batch = game.getSpriteBatch();

        cameraManager = new CameraManager(config);

        // 立刻把相机拉到玩家身上
        cameraManager.centerOnPlayerImmediately(gm.getPlayer());
    }

    @Override
    public void render(float delta) {
        update(delta);

        cameraManager.update(delta, gm.getPlayer());
        ScreenUtils.clear(0, 0, 0, 1);

        renderGame();
    }

    private void update(float delta) {
        if (finished) return;

        // === 输入 & 世界更新 ===
        gm.getInputHandler().update(delta,gm );
        gm.update(delta);

        // === 教程输入判定 ===
        PlayerInputHandler input = gm.getInputHandler();
        movedUp    |= input.hasMovedUp();
        movedDown  |= input.hasMovedDown();
        movedLeft  |= input.hasMovedLeft();
        movedRight |= input.hasMovedRight();

        // === 目标判定（格子级） ===
        int px = (int) gm.getPlayer().getX();
        int py = (int) gm.getPlayer().getY();

        if (px == TARGET_X && py == TARGET_Y) {
            reachedTarget = true;
        }

        // === 教程完成 ===
        if (movedUp && movedDown && movedLeft && movedRight && reachedTarget) {
            finished = true;
            game.onTutorialFinished(this);
        }

        // === 玩家死亡 ===
        if (gm.isPlayerDead()) {
            finished = true;
            game.onTutorialFailed(this, MazeGameTutorialResult.FAILURE_DEAD);
        }
    }

    private void renderGame() {
        OrthographicCamera cam = cameraManager.getCamera();
        batch.setProjectionMatrix(cam.combined);

        batch.begin();

        // ===== 迷宫（白色） =====
        batch.setColor(1, 1, 1, 1);
        int[][] maze = gm.getMaze();
        for (int y = 0; y < maze.length; y++) {
            for (int x = 0; x < maze[0].length; x++) {
                if (maze[y][x] == 1) {
                    batch.draw(
                            TextureManager.getInstance().getWhitePixel(),
                            x * CELL_SIZE,
                            y * CELL_SIZE,
                            CELL_SIZE,
                            CELL_SIZE
                    );
                }
            }
        }

        // ===== 目标点（绿色） =====
        batch.setColor(0, 1, 0, 1);
        batch.draw(
                TextureManager.getInstance().getWhitePixel(),
                TARGET_X * CELL_SIZE,
                TARGET_Y * CELL_SIZE,
                CELL_SIZE,
                CELL_SIZE
        );

        // ===== 玩家（白色） =====
        batch.setColor(0, 1, 1, 1);
        batch.draw(
                TextureManager.getInstance().getWhitePixel(),
                gm.getPlayer().getX() * CELL_SIZE,
                gm.getPlayer().getY() * CELL_SIZE,
                CELL_SIZE,
                CELL_SIZE
        );
        batch.setColor(1, 1, 1, 1);

        batch.end();
    }

    @Override
    public void resize(int width, int height) {
        if (cameraManager != null) {
            cameraManager.resize(width, height);
        }
    }

    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() {}

    @Override
    public void dispose() {
        // SpriteBatch 由 MazeRunnerGame 管
    }
}
