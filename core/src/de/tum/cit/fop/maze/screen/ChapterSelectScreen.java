package de.tum.cit.fop.maze.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import de.tum.cit.fop.maze.MazeRunnerGame;
import de.tum.cit.fop.maze.game.Difficulty;
import de.tum.cit.fop.maze.game.DifficultyConfig;

/**
 * Chapter Select Screen
 */
public class ChapterSelectScreen implements Screen {

    private final MazeRunnerGame game;
    private Stage stage;

    public ChapterSelectScreen(MazeRunnerGame game) {
        this.game = game;
    }

    @Override
    public void show() {
        stage = new Stage(new ScreenViewport(), game.getSpriteBatch());
        Gdx.input.setInputProcessor(stage);

        Table table = new Table();
        table.setFillParent(true);
        table.center();

        // 重放序幕按钮
        TextButton introButton = new TextButton("Replay Prologue", game.getSkin());
        introButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                game.replayPrologue();

                DifficultyConfig config = DifficultyConfig.of(Difficulty.EASY);
                game.setScreen(new GameScreen(game, config)); // 确保跳转到游戏界面
            }
        });

        TextButton chapterOneButton = new TextButton("Chapter 1", game.getSkin());
        chapterOneButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {

                Difficulty difficulty = Difficulty.NORMAL;
                DifficultyConfig config = DifficultyConfig.of(difficulty);

                game.startNewGame(difficulty);
                game.advanceStory();
                game.setScreen(new GameScreen(game, config));
            }
        });


        // 布局
        table.add(introButton).width(320).height(64).padBottom(24);
        table.row();
        table.add(chapterOneButton).width(320).height(64);

        stage.addActor(table);
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0, 0, 0.2f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        stage.act(delta);
        stage.draw();
    }

    @Override
    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
    }

    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() {}

    @Override
    public void dispose() {
        stage.dispose();
    }
}