package de.tum.cit.fop.maze.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import de.tum.cit.fop.maze.MazeRunnerGame;
import de.tum.cit.fop.maze.tools.ButtonFactory;

public class SettingsScreen implements Screen {
    private final MazeRunnerGame game;
    private final SettingsSource source;
    private final Screen previousScreen;
    private Stage stage;
    // SettingsScreen.java 里
    public enum SettingsSource {
        MAIN_MENU,
        PAUSE_MENU
    }


    public SettingsScreen(MazeRunnerGame game, SettingsSource source, Screen previousScreen) {
        this.game = game;
        this.source = source;
        this.previousScreen = previousScreen;
    }
    private void goBack() {
        switch (source) {
            case MAIN_MENU -> {
                game.setScreen(new MenuScreen(game));
            }
            case PAUSE_MENU -> {
                if (previousScreen != null) {
                    game.setScreen(previousScreen);
                } else {
                    game.resumeGame(); // 兜底
                }
            }
        }
    }

    @Override
    public void show() {
        stage = new Stage(new ScreenViewport());
        Gdx.input.setInputProcessor(stage);

        Table root = new Table();
        root.setFillParent(true);
        stage.addActor(root);

        // ===== 标题 =====
        root.add(new Label("SETTINGS", game.getSkin(), "title"))
                .padBottom(40)
                .row();

        ButtonFactory bf = new ButtonFactory(game.getSkin());
// ===== 占位按钮（后面逐个替换）=====
        root.add(bf.create("Audio Settings (TODO)", () -> {}))
                .width(400).height(70)
                .padBottom(20)
                .row();

        root.add(bf.create("Display Settings (TODO)", () -> {}))
                .width(400).height(70)
                .padBottom(20)
                .row();

        root.add(bf.create("Two Player Mode (TODO)", () -> {}))
                .width(400).height(70)
                .padBottom(40)
                .row();

        // ===== 返回 =====
        root.add(bf.create("BACK", () -> {
                    game.goToMenu(); // 暂时统一回主菜单
                }))
                .width(400).height(80);
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0f, 0f, 0f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        stage.act(delta);
        stage.draw();
    }

    @Override public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
    }
    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() {}
    @Override public void dispose() {
        if (stage != null) stage.dispose();
    }
}
