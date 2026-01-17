package de.tum.cit.fop.maze.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion; // 🔥 关键修复：必须导入这个包
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

/**
 * 排行榜界面 - 宽屏半透明版 (修复 Import)
 */
public class LeaderboardScreen implements Screen {
    private final MazeRunnerGame game;
    private final Screen previousScreen;
    private Stage stage;
    private LeaderboardManager leaderboardManager;
    private Texture backgroundTexture;

    public LeaderboardScreen(MazeRunnerGame game, Screen previousScreen) {
        this.game = game;
        this.previousScreen = previousScreen;

        try {
            this.leaderboardManager = new LeaderboardManager();
            if (Gdx.files.internal("imgs/menu_bg/bg_front.png").exists()) {
                this.backgroundTexture = new Texture(Gdx.files.internal("imgs/menu_bg/bg_front.png"));
            }
        } catch (Exception e) {
            Logger.error("Failed to init LeaderboardScreen: " + e.getMessage());
        }

        stage = new Stage(new ScreenViewport());
        Gdx.input.setInputProcessor(stage);
        setupUI();
    }

    private void setupUI() {
        Table root = new Table();
        root.setFillParent(true);
        stage.addActor(root);

        // 1. 标题
        Label title = new Label("HALL OF FAME", game.getSkin(), "title");
        title.setColor(Color.GOLD);
        title.setFontScale(1.3f);
        root.add(title).padTop(50).padBottom(30).row();

        // 2. 数据表格容器 (大框)
        Table scoreContainer = new Table();
        // 半透明背景 (Alpha 0.5)
        scoreContainer.setBackground(createColorDrawable(new Color(0.05f, 0.05f, 0.08f, 0.5f)));
        scoreContainer.pad(40);

        // 表头
        scoreContainer.add(new Label("RANK", game.getSkin())).width(150).align(Align.center);
        // Name 这一列占据剩余空间并居中
        scoreContainer.add(new Label("NAME", game.getSkin())).expandX().align(Align.center);
        scoreContainer.add(new Label("SCORE", game.getSkin())).width(250).align(Align.right);
        scoreContainer.row();

        // 分割线
        Label separator = new Label("---------------------------------------------", game.getSkin());
        separator.setColor(new Color(1, 1, 1, 0.3f)); // 调淡
        scoreContainer.add(separator).colspan(3).pad(15).row();

        // 填充数据
        if (leaderboardManager.getScores().isEmpty()) {
            Label empty = new Label("Be the first legend!", game.getSkin());
            empty.setColor(Color.GRAY);
            empty.setFontScale(1.2f);
            scoreContainer.add(empty).colspan(3).pad(60);
        } else {
            int rank = 1;
            for (HighScore entry : leaderboardManager.getScores()) {
                // 排名颜色与字体大小
                Color rankColor = Color.WHITE;
                float fontScale = 1.1f;

                if (rank == 1) { rankColor = Color.GOLD; fontScale = 1.3f; }
                else if (rank == 2) { rankColor = new Color(0.8f, 0.8f, 0.8f, 1f); fontScale = 1.2f; }
                else if (rank == 3) { rankColor = new Color(0.8f, 0.5f, 0.2f, 1f); fontScale = 1.2f; }

                // Rank
                Label rankLabel = new Label("#" + rank, game.getSkin());
                rankLabel.setColor(rankColor);
                rankLabel.setFontScale(fontScale);
                rankLabel.setAlignment(Align.center);
                scoreContainer.add(rankLabel).pad(12);

                // Name
                Label nameLabel = new Label(entry.name, game.getSkin());
                nameLabel.setColor(rank == 1 ? Color.YELLOW : Color.WHITE);
                nameLabel.setFontScale(fontScale);
                nameLabel.setAlignment(Align.center); // 名字也居中
                scoreContainer.add(nameLabel).pad(12);

                // Score
                Label scoreLabel = new Label(String.format("%,d", entry.score), game.getSkin());
                scoreLabel.setColor(Color.CYAN);
                scoreLabel.setFontScale(fontScale);
                scoreLabel.setAlignment(Align.right);
                scoreContainer.add(scoreLabel).pad(12);

                scoreContainer.row();
                rank++;
            }
        }

        // 宽度设为屏幕的 85%
        root.add(scoreContainer).width(Gdx.graphics.getWidth() * 0.85f).padBottom(50).row();

        // 3. 返回按钮
        ButtonFactory bf = new ButtonFactory(game.getSkin());
        root.add(bf.create("BACK", () -> game.setScreen(previousScreen)))
                .width(350).height(70); // 按钮加大
    }

    private TextureRegionDrawable createColorDrawable(Color color) {
        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(color);
        pixmap.fill();
        // 这里的 TextureRegion 需要上面的 import
        TextureRegionDrawable drawable = new TextureRegionDrawable(new TextureRegion(new Texture(pixmap)));
        pixmap.dispose();
        return drawable;
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0.08f, 0.08f, 0.1f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        stage.getBatch().begin();
        if (backgroundTexture != null) {
            stage.getBatch().setColor(0.4f, 0.4f, 0.4f, 1f); // 背景稍暗
            stage.getBatch().draw(backgroundTexture, 0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
            stage.getBatch().setColor(Color.WHITE);
        }
        stage.getBatch().end();

        stage.act(delta);
        stage.draw();
    }

    @Override public void resize(int w, int h) { stage.getViewport().update(w, h, true); }
    @Override public void dispose() {
        stage.dispose();
        if (backgroundTexture != null) backgroundTexture.dispose();
    }
    @Override public void show() {}
    @Override public void hide() {}
    @Override public void pause() {}
    @Override public void resume() {}
}