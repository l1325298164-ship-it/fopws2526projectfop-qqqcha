package de.tum.cit.fop.maze.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import de.tum.cit.fop.maze.MazeRunnerGame;
import de.tum.cit.fop.maze.tools.ButtonFactory;
import de.tum.cit.fop.maze.utils.LeaderboardManager;
import de.tum.cit.fop.maze.utils.Logger;
import de.tum.cit.fop.maze.utils.LeaderboardManager.HighScore;

public class LeaderboardScreen implements Screen {
    private final MazeRunnerGame game;
    private final Screen previousScreen;
    private Stage stage;
    private LeaderboardManager leaderboardManager;

    public LeaderboardScreen(MazeRunnerGame game, Screen previousScreen) {
        if (game == null) {
            throw new IllegalArgumentException("game cannot be null");
        }
        if (game.getSkin() == null) {
            throw new IllegalArgumentException("game.getSkin() cannot be null");
        }
        
        this.game = game;
        this.previousScreen = previousScreen;
        
        try {
            this.leaderboardManager = new LeaderboardManager(); // 加载数据
        } catch (Exception e) {
            Logger.error("Failed to initialize LeaderboardManager: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to initialize LeaderboardManager", e);
        }

        stage = new Stage(new ScreenViewport());
        Gdx.input.setInputProcessor(stage);

        try {
            setupUI();
        } catch (Exception e) {
            Logger.error("Failed to setup LeaderboardScreen UI: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to setup LeaderboardScreen UI", e);
        }
    }

    private void setupUI() {
        Table root = new Table();
        root.setFillParent(true);
        // 如果有背景图，可以在这里 setBackground
        // root.setBackground(new TextureRegionDrawable(new TextureRegion(new Texture("menu_bg/bg_front.png"))));
        stage.addActor(root);

        // 1. 标题
        Label title = new Label("HALL OF FAME", game.getSkin(), "title");
        root.add(title).padBottom(40).row();

        // 2. 数据表格
        Table scoreTable = new Table();
        // 🔥 安全使用white drawable作为背景
        try {
            if (game != null && game.getSkin() != null && 
                game.getSkin().has("white", com.badlogic.gdx.scenes.scene2d.utils.Drawable.class)) {
                scoreTable.setBackground(game.getSkin().getDrawable("white"));
                scoreTable.setColor(0.2f, 0.2f, 0.2f, 0.8f); // 深色半透明背景
            }
        } catch (Exception e) {
            Logger.warning("Failed to set leaderboard scoreTable background: " + e.getMessage());
            // 继续执行，不设置背景
        }

        // 表头
        scoreTable.add(new Label("RANK", game.getSkin())).pad(10);
        scoreTable.add(new Label("NAME", game.getSkin())).pad(10).width(300);
        scoreTable.add(new Label("SCORE", game.getSkin())).pad(10);
        scoreTable.row();

        // 填充数据
        int rank = 1;
        if (leaderboardManager.getScores().isEmpty()) {
            scoreTable.add(new Label("-", game.getSkin()));
            scoreTable.add(new Label("No Records Yet", game.getSkin()));
            scoreTable.add(new Label("-", game.getSkin()));
            scoreTable.row();
        } else {
            for (HighScore entry : leaderboardManager.getScores()) {
                // 排名
                Label rankLabel = new Label("#" + rank, game.getSkin());
                if (rank == 1) rankLabel.setColor(1f, 0.84f, 0f, 1f); // 金色
                else if (rank == 2) rankLabel.setColor(0.75f, 0.75f, 0.75f, 1f); // 银色
                else if (rank == 3) rankLabel.setColor(0.8f, 0.5f, 0.2f, 1f); // 铜色

                scoreTable.add(rankLabel).pad(5);
                scoreTable.add(new Label(entry.name, game.getSkin())).pad(5).align(Align.left);
                scoreTable.add(new Label(String.valueOf(entry.score), game.getSkin())).pad(5);
                scoreTable.row();
                rank++;
            }
        }

        root.add(scoreTable).width(600).height(400).padBottom(30).row();

        // 3. 返回按钮
        ButtonFactory bf = new ButtonFactory(game.getSkin());
        root.add(bf.create("BACK", () -> game.setScreen(previousScreen)))
                .width(300).height(60);
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0.1f, 0.1f, 0.1f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        stage.act(delta);
        stage.draw();
    }

    @Override public void resize(int w, int h) { stage.getViewport().update(w, h, true); }
    @Override public void dispose() { stage.dispose(); }
    @Override public void show() {}
    @Override public void hide() {}
    @Override public void pause() {}
    @Override public void resume() {}
}