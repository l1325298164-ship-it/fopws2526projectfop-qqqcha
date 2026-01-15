package de.tum.cit.fop.maze.entities.boss;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import de.tum.cit.fop.maze.MazeRunnerGame;

public class BossStoryScreen implements Screen {

    private final MazeRunnerGame game;
    private SpriteBatch batch;
    private BitmapFont font;
    private OrthographicCamera camera;

    private final String[] storyLines = {
            "The tea has cooled.",
            "The maze falls silent.",
            "",
            "But some echoes remain..."
    };

    public BossStoryScreen(MazeRunnerGame game) {
        this.game = game;
    }

    @Override
    public void show() {
        batch = new SpriteBatch();
        font = game.getSkin().get("default-font", BitmapFont.class);

        camera = new OrthographicCamera();
        camera.setToOrtho(false,
                Gdx.graphics.getWidth(),
                Gdx.graphics.getHeight());
        camera.update();
    }

    @Override
    public void render(float delta) {
        if (Gdx.input.justTouched() || Gdx.input.isKeyJustPressed(Input.Keys.ENTER)) {
            game.setScreen(new CreditsScreen(game));
            return;
        }

        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        batch.setProjectionMatrix(camera.combined);
        batch.begin();

        font.getData().setScale(0.45f);
        float y = Gdx.graphics.getHeight() * 0.65f;

        for (String line : storyLines) {
            font.draw(batch, line, 120, y);
            y -= 40;
        }

        font.getData().setScale(0.3f);
        font.draw(batch,
                "[Click / ENTER to continue]",
                120,
                120);

        batch.end();
    }

    @Override public void resize(int w, int h) {}
    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() {}
    @Override public void dispose() {
        batch.dispose();
    }
}

