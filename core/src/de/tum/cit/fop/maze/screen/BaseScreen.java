package de.tum.cit.fop.maze.screen;

import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

public abstract class BaseScreen implements Screen {

    protected OrthographicCamera worldCamera;
    protected Viewport worldViewport;

    protected OrthographicCamera uiCamera;
    protected Viewport uiViewport;

    protected final float WORLD_WIDTH;
    protected final float WORLD_HEIGHT;

    protected BaseScreen(float worldWidth, float worldHeight) {
        this.WORLD_WIDTH = worldWidth;
        this.WORLD_HEIGHT = worldHeight;

        // World
        worldCamera = new OrthographicCamera();
        worldViewport = new FitViewport(
                WORLD_WIDTH,
                WORLD_HEIGHT,
                worldCamera
        );

        worldCamera.position.set(
                WORLD_WIDTH / 2f,
                WORLD_HEIGHT / 2f,
                0
        );
        worldCamera.update();

        // UI
        uiCamera = new OrthographicCamera();
        uiViewport = new com.badlogic.gdx.utils.viewport.ScreenViewport(uiCamera);
    }

    @Override
    public void resize(int width, int height) {
        worldViewport.update(width, height, true);
        uiViewport.update(width, height, true);
    }

    protected void applyWorld() {
        worldViewport.apply();
    }

    protected void applyUI() {
        uiViewport.apply();
    }

    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() {}
    @Override public void dispose() {}
}
