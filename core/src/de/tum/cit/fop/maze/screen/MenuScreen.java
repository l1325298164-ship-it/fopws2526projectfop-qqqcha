package de.tum.cit.fop.maze.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import de.tum.cit.fop.maze.MazeRunnerGame;
import de.tum.cit.fop.maze.tools.ButtonFactory;

public class MenuScreen implements Screen {

    private final Stage stage;

    public MenuScreen(MazeRunnerGame game) {

        // =====================================================
        // Camera & Stage
        // =====================================================
        OrthographicCamera camera = new OrthographicCamera();
        camera.zoom = 1.5f;

        Viewport viewport = new ScreenViewport(camera);
        stage = new Stage(viewport, game.getSpriteBatch());
        Gdx.input.setInputProcessor(stage);

        // =====================================================
        // Root Layout
        // =====================================================
        Table table = new Table();
        table.setFillParent(true);
        stage.addActor(table);

        // =====================================================
        // Title
        // =====================================================
        Label title = new Label(
                "Hello World from the Menu!",
                game.getSkin(),
                "title"
        );
        title.setAlignment(Align.center);
        title.setFontScale(1.1f);

        table.add(title)
                .padBottom(80)
                .row();

        // =====================================================
        // Buttons (via ButtonFactory)
        // =====================================================
        ButtonFactory bf = new ButtonFactory(game.getSkin());

        table.add(
                bf.create("START GAME", game::goToGame)
        ).padBottom(20).row();

        table.add(
                bf.create("RESET THE WORLD", game::goToPV)
        ).padBottom(20).row();

        table.add(
                bf.create("TEST", () -> {
                    // 这里可以以后接测试界面
                    System.out.println("TEST button clicked");
                })
        ).row();
    }

    // =====================================================
    // Render & lifecycle
    // =====================================================
    @Override
    public void render(float delta) {
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        stage.act(Math.min(Gdx.graphics.getDeltaTime(), 1 / 30f));
        stage.draw();
    }

    @Override
    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
    }

    @Override
    public void dispose() {
        stage.dispose();
    }

    @Override
    public void show() {
        Gdx.input.setInputProcessor(stage);
    }

    @Override public void hide() {}
    @Override public void pause() {}
    @Override public void resume() {}
}
