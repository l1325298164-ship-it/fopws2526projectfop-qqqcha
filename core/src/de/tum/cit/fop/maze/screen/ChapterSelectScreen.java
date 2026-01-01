package de.tum.cit.fop.maze.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.ScreenUtils;
import de.tum.cit.fop.maze.MazeRunnerGame;

/**
 * Mode Choice Menu（临时占位版）
 * 后续可以换成真正 UI
 */
public class ChapterSelectScreen implements Screen {
    //TODO难度切换界面 （开启新篇章）

    private final MazeRunnerGame game;
    private SpriteBatch batch;

    public ChapterSelectScreen(MazeRunnerGame game) {
        this.game = game;
    }

    @Override
    public void show() {
        batch = new SpriteBatch();
    }

    @Override
    public void render(float delta) {
        ScreenUtils.clear(0, 0, 0.2f, 1);

        batch.begin();
        // 现在什么都不画，先跑流程
        batch.end();
//start the chapter
        onChapterSelected(chapter) {
            game.startChapter(chapter);
        }
    }
    public void onChapterSelected(){}

    @Override public void resize(int width, int height) {}
    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() {}

    @Override
    public void dispose() {
        batch.dispose();
    }
}
