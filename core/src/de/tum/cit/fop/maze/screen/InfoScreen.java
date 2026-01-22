package de.tum.cit.fop.maze.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion; // 导入防止报错
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import de.tum.cit.fop.maze.MazeRunnerGame;
import de.tum.cit.fop.maze.tools.ButtonFactory;
import de.tum.cit.fop.maze.utils.Logger;

/**
 * 游戏信息综合界面 (Info Screen) - 纯净悬浮版
 * <p>
 * 修改：
 * 1. 移除了内容容器的背景色 (setBackground(null))，按钮直接悬浮在背景图上。
 * 2. 保持了垂直排列和大按钮设计。
 */
public class InfoScreen implements Screen {

    private final MazeRunnerGame game;
    private final Screen previousScreen;
    private Stage stage;
    private Texture backgroundTexture;

    public InfoScreen(MazeRunnerGame game, Screen previousScreen) {
        this.game = game;
        this.previousScreen = previousScreen;

        // 尝试加载背景图 (复用主菜单背景)
        try {
            if (Gdx.files.internal("imgs/menu_bg/bg_front.png").exists()) {
                backgroundTexture = new Texture(Gdx.files.internal("imgs/menu_bg/bg_front.png"));
            }
        } catch (Exception e) {
            Logger.error("Failed to load background: " + e.getMessage());
        }

        stage = new Stage(new ScreenViewport());
        Gdx.input.setInputProcessor(stage);

        setupUI();
    }

    private void setupUI() {
        Table root = new Table();
        root.setFillParent(true);
        stage.addActor(root);

        // 容器 Table (用于组织布局，但不再有背景色)
        Table contentTable = new Table();
        // 🔥 关键修改：不需要背景色，完全透明
        // contentTable.setBackground(...) -> 已移除

        // 1. 标题
        Label title = new Label("GAME INFO", game.getSkin(), "title");
        title.setColor(Color.CYAN);
        title.setFontScale(1.3f); // 标题稍大
        contentTable.add(title).padBottom(80).row(); // 拉大标题与按钮的距离

        // 2. 按钮区域 (垂直排列)
        ButtonFactory bf = new ButtonFactory(game.getSkin());

        float btnWidth = 450f;
        float btnHeight = 80f;
        float spacing = 35f; // 增加按钮间距，显得更通透

        // 按钮 1: 成就
        contentTable.add(bf.create("ACHIEVEMENTS", () ->
                game.setScreen(new AchievementScreen(game, this))
        )).width(btnWidth).height(btnHeight).padBottom(spacing).row();

        // 按钮 2: 排行榜
        contentTable.add(bf.create("LEADERBOARD", () ->
                game.setScreen(new LeaderboardScreen(game, this))
        )).width(btnWidth).height(btnHeight).padBottom(spacing).row();

        // 按钮 3: 返回
        contentTable.add(bf.create("BACK", () -> game.setScreen(previousScreen)))
                .width(btnWidth).height(btnHeight).row();

        // 将透明的布局容器居中添加到舞台
        root.add(contentTable);
    }

    @Override
    public void render(float delta) {
        // 必须清屏，防止透视到主菜单
        Gdx.gl.glClearColor(0.05f, 0.05f, 0.08f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        stage.getBatch().begin();
        // 绘制背景图 (稍微变暗一点点，突出前景按钮)
        if (backgroundTexture != null) {
            stage.getBatch().setColor(0.5f, 0.5f, 0.5f, 1f); // 0.5 的亮度
            stage.getBatch().draw(backgroundTexture, 0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
            stage.getBatch().setColor(Color.WHITE);
        }
        stage.getBatch().end();

        stage.act(delta);
        stage.draw();
    }

    @Override public void resize(int w, int h) { stage.getViewport().update(w, h, true); }
    @Override public void show() { Gdx.input.setInputProcessor(stage); }
    @Override public void hide() {}
    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void dispose() {
        stage.dispose();
        if (backgroundTexture != null) backgroundTexture.dispose();
    }
}