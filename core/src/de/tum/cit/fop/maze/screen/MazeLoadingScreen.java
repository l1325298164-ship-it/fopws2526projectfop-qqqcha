package de.tum.cit.fop.maze.screen;

import com.badlogic.gdx.Screen;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.utils.TimeUtils;
import de.tum.cit.fop.maze.MazeRunnerGame;

public class MazeLoadingScreen implements Screen {

    private final MazeRunnerGame game;
    private final AssetManager assets;
    private boolean finished = false;
    private long showTime;

    public MazeLoadingScreen(MazeRunnerGame game) {
        this.game = game;
        this.assets = game.getAssets();
    }

    @Override
    public void show() {
        showTime = TimeUtils.millis();

        assets.load("player/back.atlas", TextureAtlas.class);
        assets.load("player/front.atlas", TextureAtlas.class);
        assets.load("player/left.atlas", TextureAtlas.class);
        assets.load("player/right.atlas", TextureAtlas.class);

    }

    @Override
    public void render(float delta) {

    }

    @Override
    public void resize(int width, int height) {

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
