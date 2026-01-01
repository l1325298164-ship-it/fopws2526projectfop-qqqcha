package de.tum.cit.fop.maze.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import de.tum.cit.fop.maze.MazeRunnerGame;
import de.tum.cit.fop.maze.game.Difficulty;
import de.tum.cit.fop.maze.tools.ButtonFactory;

public class DifficultySelectScreen implements Screen {

    private final MazeRunnerGame game;
    private final Screen backScreen;
    private Stage stage;


    public DifficultySelectScreen(MazeRunnerGame game, Screen backScreen) {
        this.game = game;
        this.backScreen = backScreen;

        stage = new Stage(new ScreenViewport());
        Gdx.input.setInputProcessor(stage);

        Table root = new Table();
        root.setFillParent(true);
        stage.addActor(root);

        Label title = new Label("SELECT DIFFICULTY", game.getSkin(), "title");
        title.setAlignment(Align.center);
        root.add(title).padBottom(60).row();

        ButtonFactory bf = new ButtonFactory(game.getSkin());

        root.add(bf.create("EASY", () -> {
                    game.startNewGame(Difficulty.EASY);
                    game.goToGame();
                }))
                .padBottom(20)
                .width(550)
                .height(100)
                .row();

        root.add(bf.create("NORMAL", () -> {
                    game.startNewGame(Difficulty.NORMAL);
                    game.goToGame();
                }))
                .padBottom(20)
                .width(550)
                .height(100)
                .row();

        root.add(bf.create("HARD", () -> {
                    game.startNewGame(Difficulty.HARD);
                    game.goToGame();
                }))
                .padBottom(40)
                .width(550)
                .height(100)
                .row();


        root.add(bf.create("BACK", () ->
                game.setScreen(backScreen)
        ))
                .width(550)
                .height(100);


        root.layout();
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        stage.act(delta);
        stage.draw();

        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            game.setScreen(backScreen);
        }
    }

    @Override public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
    }

    @Override public void dispose() {
        stage.dispose();
    }

    @Override public void show() {
        Gdx.input.setInputProcessor(stage);
    }
    @Override public void hide() {}
    @Override public void pause() {}
    @Override public void resume() {}
}
