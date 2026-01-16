package de.tum.cit.fop.maze.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.badlogic.gdx.scenes.scene2d.InputEvent;

import de.tum.cit.fop.maze.MazeRunnerGame;
import de.tum.cit.fop.maze.entities.chapter.ChapterContext;
import de.tum.cit.fop.maze.game.Difficulty;
import de.tum.cit.fop.maze.tools.ButtonFactory;

/**
 * Chapter Select Screen
 */
public class ChapterSelectScreen implements Screen {

    private static final String BG_PATH =
            "menu_bg/bg_front.png";

    private final MazeRunnerGame game;
    private Stage stage;
    private Texture bgTexture;

    public ChapterSelectScreen(MazeRunnerGame game) {
        this.game = game;
    }

    @Override
    public void show() {
        ButtonFactory buttonFactory =
                new ButtonFactory(game.getSkin());
        stage = new Stage(new ScreenViewport(), game.getSpriteBatch());
        Gdx.input.setInputProcessor(stage);

        /* =====================
           背景图
           ===================== */
        bgTexture = new Texture(Gdx.files.internal(BG_PATH));
        Image bgImage = new Image(bgTexture);
        bgImage.setFillParent(true);
        stage.addActor(bgImage);

        /* =====================
           白色“虚化”遮罩
           ===================== */
        Image blurMask =
                new Image(game.getSkin().newDrawable("white", Color.WHITE));
        blurMask.setFillParent(true);
        blurMask.getColor().a = 0.35f; // ⭐ 虚化强度（0.25~0.45 推荐）
        stage.addActor(blurMask);

        /* =====================
           UI
           ===================== */
        Table table = new Table();
        table.setFillParent(true);
        table.center();

        TextButton chapterOneButton =
                buttonFactory.createNavigationButton(
                        "Chapter 1",
                        () -> {
                            Difficulty difficulty = Difficulty.NORMAL;
                            ChapterContext chapterContext = ChapterContext.chapter1();
                            game.startChapterGame(difficulty, chapterContext);
                        }
                );


        table.add(chapterOneButton)
                .width(520)
                .height(64);

        stage.addActor(table);
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0, 0, 0, 1);
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
        if (bgTexture != null) bgTexture.dispose();
    }
}
