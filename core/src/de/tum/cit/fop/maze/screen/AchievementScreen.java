package de.tum.cit.fop.maze.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import de.tum.cit.fop.maze.MazeRunnerGame;
import de.tum.cit.fop.maze.game.achievement.AchievementType;
import de.tum.cit.fop.maze.game.achievement.CareerData;
import de.tum.cit.fop.maze.game.score.ScoreConstants;
import de.tum.cit.fop.maze.tools.ButtonFactory;
import de.tum.cit.fop.maze.utils.Logger;
import de.tum.cit.fop.maze.utils.StorageManager;

/**
 * 成就展示界面
 * <p>
 * 功能：
 * 1. 显示所有成就列表
 * 2. 已解锁成就高亮显示
 * 3. 显示成就进度（如"30/60"）
 */
public class AchievementScreen implements Screen {

    private final MazeRunnerGame game;
    private final Screen previousScreen;
    private Stage stage;
    private CareerData careerData;

    public AchievementScreen(MazeRunnerGame game, Screen previousScreen) {
        this.game = game;
        this.previousScreen = previousScreen;

        // 加载生涯数据
        this.careerData = StorageManager.getInstance().loadCareer();

        stage = new Stage(new ScreenViewport());
        Gdx.input.setInputProcessor(stage);

        setupUI();
    }

    private void setupUI() {
        Table root = new Table();
        root.setFillParent(true);
        stage.addActor(root);

        // ==========================================
        // 1. 标题
        // ==========================================
        Label title = new Label("ACHIEVEMENTS", game.getSkin(), "title");
        title.setColor(Color.GOLD);
        root.add(title).padTop(30).padBottom(20).row();

        // 统计已解锁数量
        int unlocked = careerData.unlockedAchievements.size();
        int total = AchievementType.values().length;
        Label statsLabel = new Label("Unlocked: " + unlocked + " / " + total, game.getSkin());
        statsLabel.setColor(Color.LIGHT_GRAY);
        root.add(statsLabel).padBottom(20).row();

        // ==========================================
        // 2. 成就列表（可滚动）
        // ==========================================
        Table achievementTable = new Table();
        achievementTable.defaults().pad(8).left();

        for (AchievementType type : AchievementType.values()) {
            boolean isUnlocked = careerData.unlockedAchievements.contains(type.id);
            Table row = createAchievementRow(type, isUnlocked);
            achievementTable.add(row).width(700).fillX().row();
        }

        ScrollPane scrollPane = new ScrollPane(achievementTable, game.getSkin());
        scrollPane.setFadeScrollBars(false);
        scrollPane.setScrollingDisabled(true, false);

        root.add(scrollPane).width(750).height(450).padBottom(20).row();

        // ==========================================
        // 3. 返回按钮
        // ==========================================
        ButtonFactory bf = new ButtonFactory(game.getSkin());
        root.add(bf.create("BACK", () -> game.setScreen(previousScreen)))
                .width(300).height(60).padBottom(30);
    }

    /**
     * 创建单个成就行
     */
    private Table createAchievementRow(AchievementType type, boolean isUnlocked) {
        Table row = new Table();
        // 🔥 安全使用white drawable作为背景
        try {
            if (game != null && game.getSkin() != null && 
                game.getSkin().has("white", com.badlogic.gdx.scenes.scene2d.utils.Drawable.class)) {
                row.setBackground(game.getSkin().getDrawable("white"));
                row.setColor(0.2f, 0.2f, 0.2f, 0.8f); // 深色半透明背景
            }
        } catch (Exception e) {
            Logger.warning("Failed to set achievement row background: " + e.getMessage());
            // 继续执行，不设置背景
        }
        row.pad(10);

        // 左侧：状态图标
        String statusIcon = isUnlocked ? "✓" : "○";
        Label iconLabel = new Label(statusIcon, game.getSkin());
        iconLabel.setFontScale(1.5f);
        iconLabel.setColor(isUnlocked ? Color.GOLD : Color.GRAY);
        row.add(iconLabel).width(40).padRight(10);

        // 中间：成就信息
        Table infoTable = new Table();
        infoTable.left();

        // 成就名称
        Label nameLabel = new Label(type.displayName, game.getSkin());
        nameLabel.setFontScale(1.1f);
        nameLabel.setColor(isUnlocked ? Color.WHITE : Color.GRAY);
        infoTable.add(nameLabel).left().row();

        // 成就描述
        String description = type.description;
        // 为未实现的成就添加标记
        boolean isNotImplemented = (type == AchievementType.ACH_12_MINE_EXPERT || type == AchievementType.ACH_13_TRUE_RECIPE);
        if (isNotImplemented) {
            description += " [未实装]";
        }
        Label descLabel = new Label(description, game.getSkin());
        descLabel.setFontScale(0.8f);
        // 未实现的成就使用特殊颜色
        if (isNotImplemented) {
            descLabel.setColor(Color.ORANGE); // 橙色标记未实现
        } else {
            descLabel.setColor(isUnlocked ? Color.LIGHT_GRAY : Color.DARK_GRAY);
        }
        descLabel.setWrap(true);
        infoTable.add(descLabel).width(500).left().row();

        // 进度条（仅对有进度的成就显示）
        String progress = getProgressText(type);
        if (progress != null && !isUnlocked) {
            Label progressLabel = new Label(progress, game.getSkin());
            progressLabel.setFontScale(0.75f);
            progressLabel.setColor(Color.CYAN);
            infoTable.add(progressLabel).left().padTop(3);
        }

        row.add(infoTable).expandX().fillX();

        return row;
    }

    /**
     * 获取成就进度文本
     */
    private String getProgressText(AchievementType type) {
        switch (type) {
            case ACH_04_PEARL_SWEEPER:
                return "Progress: " + careerData.totalKills_E01 + " / " + ScoreConstants.TARGET_KILLS_E01;
            case ACH_05_COFFEE_GRINDER:
                return "Progress: " + careerData.totalKills_E02 + " / " + ScoreConstants.TARGET_KILLS_E02;
            case ACH_06_CARAMEL_MELT:
                return "Progress: " + careerData.totalKills_E03 + " / " + ScoreConstants.TARGET_KILLS_E03;
            case ACH_07_SHELL_BREAKER:
                return "Progress: " + careerData.totalDashKills_E04 + " / " + ScoreConstants.TARGET_KILLS_E04_DASH;
            case ACH_08_BEST_SELLER:
                return "Progress: " + careerData.totalKills_Global + " / " + ScoreConstants.TARGET_KILLS_GLOBAL;
            case ACH_09_FREE_TOPPING:
                return "Progress: " + careerData.totalHeartsCollected + " / " + ScoreConstants.TARGET_HEARTS_COLLECTED;
            case ACH_10_TREASURE_MASTER:
                return "Progress: " + careerData.collectedBuffTypes.size() + " / " + ScoreConstants.TARGET_TREASURE_TYPES;
            case ACH_11_SEALED_TIGHT:
                // ACH_11是关卡级别的成就，无法显示累计进度
                // 显示提示信息
                return "Complete any level with ≤3 hits";
            default:
                return null; // 无进度的成就
        }
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0.08f, 0.08f, 0.12f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        stage.act(delta);
        stage.draw();
    }

    @Override
    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
    }

    @Override
    public void show() {
        Gdx.input.setInputProcessor(stage);
    }

    @Override
    public void hide() {}

    @Override
    public void pause() {}

    @Override
    public void resume() {}

    @Override
    public void dispose() {
        stage.dispose();
    }
}
