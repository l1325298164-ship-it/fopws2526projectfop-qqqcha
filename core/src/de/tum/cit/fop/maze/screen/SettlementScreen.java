package de.tum.cit.fop.maze.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import de.tum.cit.fop.maze.MazeRunnerGame;
import de.tum.cit.fop.maze.game.save.GameSaveData;
import de.tum.cit.fop.maze.game.achievement.AchievementType;
import de.tum.cit.fop.maze.game.score.LevelResult;
import de.tum.cit.fop.maze.tools.ButtonFactory;
import de.tum.cit.fop.maze.utils.LeaderboardManager;
import de.tum.cit.fop.maze.utils.Logger;
import de.tum.cit.fop.maze.game.save.StorageManager;

/**
 * 结算界面 (Settlement Screen) - 布局优化版
 * <p>
 * 修改：
 * 1. 紧凑布局：大幅减少了评级上下方的空白间距。
 * 2. 按钮加大：底部按钮尺寸调整为 420x90。
 */
public class SettlementScreen implements Screen {

    private final MazeRunnerGame game;
    private final LevelResult result;
    private final GameSaveData saveData;
    private Stage stage;
    private final LeaderboardManager leaderboardManager;

    private Texture backgroundTexture;

    // 交互状态
    private boolean isHighScore = false;
    private boolean scoreSubmitted = false;
    private TextField nameInput;

    // 分数滚动动画
    private float displayedTotalScore;
    private final float targetTotalScore;
    private boolean isScoreRolling = true;

    // UI 组件引用
    private Label labelTotalScore;

    public SettlementScreen(MazeRunnerGame game, LevelResult result, GameSaveData saveData) {
        if (game == null) throw new IllegalArgumentException("MazeRunnerGame cannot be null");
        if (result == null) result = new LevelResult(0, 0, 0, "D", 0, 1.0f);
        if (saveData == null) saveData = new GameSaveData();

        this.game = game;
        this.result = result;
        this.saveData = saveData;
        this.leaderboardManager = new LeaderboardManager();

        this.displayedTotalScore = saveData.score;
        this.saveData.score += result.finalScore;
        this.targetTotalScore = this.saveData.score;

        this.isHighScore = leaderboardManager.isHighScore(this.saveData.score);

        try {
            if (Gdx.files.internal("menu_bg/bg_front.png").exists()) {
                backgroundTexture = new Texture(Gdx.files.internal("menu_bg/bg_front.png"));
            }
        } catch (Exception e) {
            Logger.warning("Failed to load settlement background: " + e.getMessage());
        }
    }

    @Override
    public void show() {
        stage = new Stage(new ScreenViewport());
        Gdx.input.setInputProcessor(stage);
        setupUI();
    }

    private void setupUI() {
        Table mainRoot = new Table();
        mainRoot.setFillParent(true);
        stage.addActor(mainRoot);

        // ==========================================
        // 1. 可滚动的内容区域
        // ==========================================
        Table scrollContent = new Table();
        scrollContent.pad(40);
        scrollContent.top();

        // --- 1. 标题 ---
        Label titleLabel = new Label("LEVEL COMPLETED", game.getSkin(), "title");
        titleLabel.setColor(Color.GOLD);
        titleLabel.setFontScale(1.2f);
        titleLabel.getColor().a = 0f;
        titleLabel.addAction(Actions.fadeIn(1.0f));
        // 🔥 修改：减小标题下间距 (30 -> 10)
        scrollContent.add(titleLabel).padBottom(10).row();

        // --- 2. Rank 印章 (居中大图标) ---
        Label rankLabel = new Label(result.rank, game.getSkin(), "title");
        rankLabel.setFontScale(6.0f);
        rankLabel.setAlignment(Align.center);
        setRankColor(rankLabel, result.rank);

        rankLabel.setOrigin(Align.center);
        rankLabel.setColor(rankLabel.getColor().r, rankLabel.getColor().g, rankLabel.getColor().b, 0f);
        rankLabel.setScale(3.0f);
        rankLabel.addAction(Actions.sequence(
                Actions.delay(0.3f),
                Actions.parallel(Actions.fadeIn(0.2f), Actions.scaleTo(1f, 1f, 0.5f, Interpolation.bounceOut))
        ));
        // 🔥 修改：减小 Rank 下间距 (10 -> 0)
        scrollContent.add(rankLabel).padBottom(0).row();

        if ("S".equals(result.rank)) {
            Label praise = new Label("PERFECT!", game.getSkin());
            praise.setColor(Color.GOLD);
            praise.setFontScale(1.2f);
            // 🔥 修改：S级评价间距 (30 -> 20)
            scrollContent.add(praise).padBottom(20).row();
        } else {
            // 🔥 修改：移除大占位符，仅留微小间隙 (10)
            scrollContent.add(new Label("", game.getSkin())).height(10).row();
        }

        // --- 3. 分数详情 (宽表格) ---
        Table scoreTable = new Table();
        float tableWidth = 500f;

        addScoreRow(scoreTable, "Base Score", "+" + formatScore(result.baseScore), Color.WHITE);
        addScoreRow(scoreTable, "Penalty", "-" + formatScore(result.penaltyScore), Color.SCARLET);
        addScoreRow(scoreTable, "Multiplier", getMultiplierText(result.scoreMultiplier), Color.CYAN);

        Label line = new Label("- - - - - - - - - -", game.getSkin());
        line.setColor(Color.GRAY);
        line.setAlignment(Align.center);
        scoreTable.add(line).colspan(2).pad(10).row(); // pad 15 -> 10

        addScoreRow(scoreTable, "LEVEL SCORE", String.valueOf(result.finalScore), Color.GOLD);

        scoreTable.add(new Label("TOTAL SCORE", game.getSkin())).align(Align.left).padTop(15); // pad 20 -> 15
        labelTotalScore = new Label(formatScore((int)displayedTotalScore), game.getSkin());
        labelTotalScore.setColor(Color.ORANGE);
        labelTotalScore.setFontScale(1.5f);
        scoreTable.add(labelTotalScore).align(Align.right).padTop(15);
        scoreTable.row();

        // 🔥 修改：表格下间距 (40 -> 30)
        scrollContent.add(scoreTable).width(tableWidth).padBottom(30).row();

        // --- 4. 统计与成就 ---
        Table statsTable = new Table();
        int totalKills = saveData.sessionKills.values().stream().mapToInt(Integer::intValue).sum();

        Label lKills = new Label("Kills: " + totalKills, game.getSkin());
        lKills.setColor(Color.LIGHT_GRAY);

        Label lDamage = new Label("Damage: " + saveData.sessionDamageTaken, game.getSkin());
        lDamage.setColor(Color.LIGHT_GRAY);

        statsTable.add(lKills).padRight(40);
        statsTable.add(lDamage);
        scrollContent.add(statsTable).padBottom(30).row();

        // 新解锁成就
        if (!saveData.newAchievements.isEmpty()) {
            Label achTitle = new Label("NEW UNLOCKS", game.getSkin());
            achTitle.setColor(Color.YELLOW);
            scrollContent.add(achTitle).padBottom(10).row();

            for (String achId : saveData.newAchievements) {
                String name = achId;
                for (AchievementType t : AchievementType.values()) {
                    if (t.id.equals(achId)) { name = t.displayName; break; }
                }
                Label achLabel = new Label("★ " + name, game.getSkin());
                achLabel.setColor(Color.GREEN);
                scrollContent.add(achLabel).padBottom(5).row();
            }
            scrollContent.add(new Label("", game.getSkin())).padBottom(20).row();
        }

        // --- 5. 新纪录输入框 ---
        if (isHighScore && !scoreSubmitted) {
            Table inputContainer = new Table();
            inputContainer.setBackground(createColorDrawable(new Color(1f, 1f, 1f, 0.1f)));
            inputContainer.pad(20);

            Label newRecLabel = new Label("NEW HIGH SCORE!", game.getSkin());
            newRecLabel.setColor(Color.YELLOW);
            inputContainer.add(newRecLabel).padBottom(15).row();

            Table inputRow = new Table();
            nameInput = new TextField("", createFallbackTextFieldStyle());
            nameInput.setMessageText("Enter Name...");
            nameInput.setMaxLength(10);
            nameInput.setAlignment(Align.center);
            inputRow.add(nameInput).width(200).height(50).padRight(15);

            ButtonFactory bf = new ButtonFactory(game.getSkin());
            inputRow.add(bf.create("SUBMIT", () -> {
                String name = nameInput.getText();
                if (name == null || name.trim().isEmpty()) name = "Traveler";
                leaderboardManager.addScore(name, saveData.score);
                scoreSubmitted = true;
                setupUI();
            })).width(120).height(50);

            inputContainer.add(inputRow);
            scrollContent.add(inputContainer).padBottom(30).row();
        }

        // 底部留白，防止内容被按钮挡住 (留出按钮高度 + 间隙)
        scrollContent.add(new Label("", game.getSkin())).height(150).row();

        // 放入 ScrollPane
        ScrollPane scrollPane = new ScrollPane(scrollContent, createScrollPaneStyle());
        scrollPane.setFadeScrollBars(false);
        scrollPane.setScrollingDisabled(true, false);

        mainRoot.add(scrollPane).expand().fill().row();

        // ==========================================
        // 2. 底部按钮 (透明背景)
        // ==========================================
        Table footer = new Table();
        footer.pad(20);

        ButtonFactory bf = new ButtonFactory(game.getSkin());

        // 🔥 修改：按钮尺寸加大至 420x90
        float btnWidth = 420f;
        float btnHeight = 90f;

        footer.add(bf.create("NEXT LEVEL", () -> performSaveAndExit(true))).width(btnWidth).height(btnHeight).padRight(40);
        footer.add(bf.create("MENU", () -> performSaveAndExit(false))).width(btnWidth).height(btnHeight);

        mainRoot.add(footer).fillX().bottom();
    }

    // --- Helpers ---

    private ScrollPane.ScrollPaneStyle createScrollPaneStyle() {
        ScrollPane.ScrollPaneStyle style = new ScrollPane.ScrollPaneStyle();
        style.vScrollKnob = createColorDrawable(new Color(1f, 1f, 1f, 0.3f));
        return style;
    }

    private TextField.TextFieldStyle createFallbackTextFieldStyle() {
        TextField.TextFieldStyle style = new TextField.TextFieldStyle();
        style.font = game.getSkin().getFont("default-font");
        if (style.font == null) style.font = new BitmapFont();
        style.fontColor = Color.WHITE;
        style.cursor = createColorDrawable(Color.WHITE);
        style.cursor.setMinWidth(2);
        style.selection = createColorDrawable(new Color(0, 0, 1, 0.5f));
        style.background = createColorDrawable(new Color(0.2f, 0.2f, 0.2f, 1f));
        return style;
    }

    private TextureRegionDrawable createColorDrawable(Color color) {
        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(color);
        pixmap.fill();
        TextureRegionDrawable drawable = new TextureRegionDrawable(new TextureRegion(new Texture(pixmap)));
        pixmap.dispose();
        return drawable;
    }

    private void performSaveAndExit(boolean toNextLevel) {
        clearNewAchievements();
        StorageManager storage = StorageManager.getInstance();

        if (toNextLevel) {
            saveData.currentLevel++;
            saveData.levelBaseScore = 0;
            saveData.levelPenalty = 0;

            if (game.getGameManager() != null && game.getGameManager().getScoreManager() != null) {
                GameSaveData tempData = new GameSaveData();
                tempData.score = saveData.score;
                tempData.levelBaseScore = 0;
                tempData.levelPenalty = 0;
                game.getGameManager().getScoreManager().restoreState(tempData);
            }
            storage.saveGameSync(saveData);
            game.loadGame();
        } else {
            storage.saveGameSync(saveData);
            game.goToMenu();
        }
    }

    private void addScoreRow(Table table, String name, String value, Color valueColor) {
        table.add(new Label(name, game.getSkin())).align(Align.left).expandX();
        Label valLabel = new Label(value, game.getSkin());
        valLabel.setColor(valueColor);
        table.add(valLabel).align(Align.right);
        table.row();
    }

    private String formatScore(int score) {
        return String.format("%,d", score);
    }

    private String getMultiplierText(float multiplier) {
        String difficultyHint = "";
        if (multiplier >= 1.5f) difficultyHint = " (Hard)";
        else if (multiplier >= 1.2f) difficultyHint = " (Normal)";
        else if (multiplier >= 1.0f) difficultyHint = " (Easy)";
        else if (multiplier >= 2.0f) difficultyHint = " (Endless)";
        return String.format("x%.1f%s", multiplier, difficultyHint);
    }

    private void setRankColor(Label label, String rank) {
        switch (rank) {
            case "S" -> label.setColor(1f, 0.84f, 0f, 1f);
            case "A" -> label.setColor(0.75f, 0.75f, 0.75f, 1f);
            case "B" -> label.setColor(0.8f, 0.5f, 0.2f, 1f);
            default  -> label.setColor(Color.WHITE);
        }
    }

    private void clearNewAchievements() {
        if (saveData != null) {
            saveData.newAchievements.clear();
            saveData.sessionDamageTaken = 0;
            saveData.sessionKills.clear();
        }
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0.08f, 0.08f, 0.12f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        stage.getBatch().begin();
        if (backgroundTexture != null) {
            stage.getBatch().setColor(0.4f, 0.4f, 0.4f, 1f);
            stage.getBatch().draw(backgroundTexture, 0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
            stage.getBatch().setColor(Color.WHITE);
        }
        stage.getBatch().end();

        if (isScoreRolling && labelTotalScore != null) {
            float diff = targetTotalScore - displayedTotalScore;
            if (Math.abs(diff) < 5) {
                displayedTotalScore = targetTotalScore;
                isScoreRolling = false;
            } else {
                displayedTotalScore += diff * 2.0f * delta;
            }
            labelTotalScore.setText(formatScore((int)displayedTotalScore));
        }

        stage.act(delta);
        stage.draw();
    }

    @Override public void resize(int w, int h) { stage.getViewport().update(w, h, true); }
    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() {}
    @Override public void dispose() {
        stage.dispose();
        if(backgroundTexture != null) backgroundTexture.dispose();
    }
}