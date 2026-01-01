package de.tum.cit.fop.maze.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.ScreenUtils;
import de.tum.cit.fop.maze.MazeRunnerGame;
import de.tum.cit.fop.maze.game.Difficulty;
import de.tum.cit.fop.maze.game.DifficultyConfig;

/**
 * Chapter / Difficulty Select Screen（流程版）
 */
public class ChapterSelectScreen implements Screen {

    private final MazeRunnerGame game;
    private SpriteBatch batch;

    public ChapterSelectScreen(MazeRunnerGame game) {
        this.game = game;
    }

    @Override
    public void show() {
        // ⚠️ 建议：用 game 的 batch，而不是 new
        batch = game.getSpriteBatch();
    }

    @Override
    public void render(float delta) {
        ScreenUtils.clear(0, 0, 0.2f, 1);

        handleInput();

        batch.begin();
        // TODO: 后续在这里画 UI 文本
        batch.end();
    }

    private void handleInput() {

        if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_1)) {
            startChapter(Difficulty.EASY);
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_2)) {
            startChapter(Difficulty.NORMAL);
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_3)) {
            startChapter(Difficulty.HARD);
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            game.goToMenu();
        }
    }


    private void startChapter(Difficulty difficulty) {
        game.startNewGame(difficulty);  // ⭐ 关键：在这里更新 difficultyConfig + GameManager
        game.advanceStory();            // 推进到 MAZE_GAME
    }

    @Override public void resize(int width, int height) {}
    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() {}

    @Override
    public void dispose() {
        // SpriteBatch 由 MazeRunnerGame 管
    }
}
