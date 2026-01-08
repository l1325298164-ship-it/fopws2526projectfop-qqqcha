package de.tum.cit.fop.maze.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import de.tum.cit.fop.maze.MazeRunnerGame;
import de.tum.cit.fop.maze.game.achievement.Achievement;
import de.tum.cit.fop.maze.game.achievement.AchievementManager;
import de.tum.cit.fop.maze.game.achievement.CareerData;
import de.tum.cit.fop.maze.utils.Logger;
import de.tum.cit.fop.maze.utils.StorageManager;

/**
 * 成就列表界面 (奖杯室)
 * 显示所有成就的解锁状态和进度
 */
public class AchievementListScreen implements Screen {

    private final MazeRunnerGame game;
    private final Stage stage;
    private final SpriteBatch batch;
    private final BitmapFont titleFont;
    private final BitmapFont normalFont;
    private final GlyphLayout layout;

    private CareerData careerData;
    private ScrollPane scrollPane;
    private Table achievementTable;

    // 背景纹理（如果有的话）
    private Texture backgroundTexture;

    public AchievementListScreen(MazeRunnerGame game) {
        this.game = game;
        this.batch = new SpriteBatch();
        this.stage = new Stage(new ScreenViewport());

        // 字体设置
        this.titleFont = new BitmapFont();
        this.titleFont.getData().setScale(2.0f);
        this.titleFont.setColor(Color.GOLD);

        this.normalFont = new BitmapFont();
        this.normalFont.getData().setScale(1.2f);

        this.layout = new GlyphLayout();

        // 加载生涯数据
        loadCareerData();

        // 尝试加载背景纹理（可选）
        try {
            backgroundTexture = new Texture(Gdx.files.internal("background.png"));
        } catch (Exception e) {
            Logger.debug("No background texture found for AchievementListScreen");
            backgroundTexture = null;
        }

        setupUI();
    }

    /**
     * 加载玩家的生涯数据
     */
    private void loadCareerData() {
        try {
            careerData = StorageManager.getInstance().loadCareer();
            Logger.info("Career data loaded: " + careerData.unlockedAchievements.size() + " achievements unlocked");
        } catch (Exception e) {
            Logger.error("Failed to load career data: " + e.getMessage());
            careerData = new CareerData();
        }
    }

    /**
     * 设置UI布局
     */
    private void setupUI() {
        Table mainTable = new Table();
        mainTable.setFillParent(true);

        // 标题
        Label titleLabel = new Label("ACHIEVEMENTS", new Label.LabelStyle(titleFont, Color.GOLD));
        titleLabel.setAlignment(Align.center);
        mainTable.add(titleLabel).padTop(30).padBottom(20).colspan(2).row();

        // 统计信息
        int totalAchievements = Achievement.values().length;
        int unlockedCount = careerData.unlockedAchievements.size();
        String statsText = String.format("Unlocked: %d / %d (%.1f%%)",
                unlockedCount, totalAchievements, (unlockedCount * 100.0 / totalAchievements));

        Label statsLabel = new Label(statsText, new Label.LabelStyle(normalFont, Color.WHITE));
        mainTable.add(statsLabel).padBottom(20).colspan(2).row();

        // 创建成就列表
        achievementTable = new Table();
        achievementTable.top();
        achievementTable.defaults().pad(10).width(700).height(100);

        // 添加所有成就
        for (Achievement achievement : Achievement.values()) {
            achievementTable.add(createAchievementRow(achievement)).row();
        }

        // 滚动面板
        scrollPane = new ScrollPane(achievementTable);
        scrollPane.setFadeScrollBars(false);
        scrollPane.setScrollingDisabled(true, false);

        mainTable.add(scrollPane).width(750).height(400).colspan(2).padBottom(20).row();

        // 返回按钮
        TextButton backButton = createStyledButton("Back to Menu");
        backButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                Logger.info("Returning to main menu from achievements");
                game.goToMenu();
            }
        });
        mainTable.add(backButton).width(200).height(50).padTop(20);

        stage.addActor(mainTable);
    }

    /**
     * 创建单个成就行
     */
    private Table createAchievementRow(Achievement achievement) {
        Table row = new Table();
        row.setBackground(createRowBackground());

        boolean isUnlocked = careerData.unlockedAchievements.contains(achievement.id);

        // 成就图标占位符（可以替换为真实图标纹理）
        Label iconLabel = new Label(isUnlocked ? "🏆" : "🔒",
                new Label.LabelStyle(titleFont, isUnlocked ? Color.GOLD : Color.GRAY));
        row.add(iconLabel).width(60).padLeft(10);

        // 成就信息容器
        Table infoTable = new Table();
        infoTable.left();

        // 成就名称
        String displayName = isUnlocked ? achievement.name : "???";
        Label nameLabel = new Label(displayName,
                new Label.LabelStyle(normalFont, isUnlocked ? Color.WHITE : Color.DARK_GRAY));
        nameLabel.setAlignment(Align.left);
        infoTable.add(nameLabel).left().row();

        // 成就描述
        String displayDesc = isUnlocked ? achievement.description : "Locked";
        Label descLabel = new Label(displayDesc,
                new Label.LabelStyle(normalFont, isUnlocked ? Color.LIGHT_GRAY : Color.DARK_GRAY));
        descLabel.setFontScale(0.8f);
        descLabel.setAlignment(Align.left);
        infoTable.add(descLabel).left().padTop(5).row();

        // 进度条（如果有进度追踪）
        if (!isUnlocked && hasProgress(achievement)) {
            float progress = getProgress(achievement);
            Label progressLabel = new Label(String.format("Progress: %.0f%%", progress * 100),
                    new Label.LabelStyle(normalFont, Color.YELLOW));
            progressLabel.setFontScale(0.7f);
            infoTable.add(progressLabel).left().padTop(5);
        }

        row.add(infoTable).expandX().fillX().padLeft(20);

        return row;
    }

    /**
     * 创建行背景
     */
    private com.badlogic.gdx.scenes.scene2d.utils.Drawable createRowBackground() {
        // 创建一个简单的半透明背景
        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(0.2f, 0.2f, 0.2f, 0.8f);
        pixmap.fill();
        Texture texture = new Texture(pixmap);
        pixmap.dispose();
        return new com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable(new com.badlogic.gdx.graphics.g2d.TextureRegion(texture));
    }

    /**
     * 检查成就是否有进度追踪
     */
    private boolean hasProgress(Achievement achievement) {
        // 根据成就类型判断是否显示进度
        switch (achievement) {
            case FIRST_KILL:
            case KILL_STREAK_5:
            case KILL_STREAK_10:
            case SPEEDRUN:
            case PERFECT_LEVEL:
                return false; // 这些是一次性触发的成就
            case KILL_50_ENEMIES:
            case KILL_100_ENEMIES:
            case TREASURE_HUNTER:
            case SURVIVOR:
                return true; // 这些有累计进度
            default:
                return false;
        }
    }

    /**
     * 获取成就进度（0.0 - 1.0）
     */
    private float getProgress(Achievement achievement) {
        switch (achievement) {
            case KILL_50_ENEMIES:
                return Math.min(1.0f, careerData.totalKills / 50.0f);
            case KILL_100_ENEMIES:
                return Math.min(1.0f, careerData.totalKills / 100.0f);
            case TREASURE_HUNTER:
                // 假设需要收集 20 个宝藏
                return Math.min(1.0f, careerData.totalTreasures / 20.0f);
            case SURVIVOR:
                // 假设需要通过 10 关不死
                return Math.min(1.0f, careerData.totalLevelsCompleted / 10.0f);
            default:
                return 0.0f;
        }
    }

    /**
     * 创建统一风格的按钮
     */
    private TextButton createStyledButton(String text) {
        TextButton.TextButtonStyle style = new TextButton.TextButtonStyle();
        style.font = normalFont;
        style.fontColor = Color.WHITE;
        style.overFontColor = Color.YELLOW;

        // 创建按钮背景
        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(0.3f, 0.3f, 0.5f, 0.9f);
        pixmap.fill();
        Texture texture = new Texture(pixmap);
        pixmap.dispose();

        style.up = new com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable(
                new com.badlogic.gdx.graphics.g2d.TextureRegion(texture));

        return new TextButton(text, style);
    }

    @Override
    public void show() {
        Gdx.input.setInputProcessor(stage);
        Logger.info("AchievementListScreen shown");
    }

    @Override
    public void render(float delta) {
        // 清屏
        Gdx.gl.glClearColor(0.1f, 0.1f, 0.15f, 1.0f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        // 绘制背景（如果有）
        if (backgroundTexture != null) {
            batch.begin();
            batch.draw(backgroundTexture, 0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
            batch.end();
        }

        // 更新和绘制 Stage
        stage.act(delta);
        stage.draw();
    }

    @Override
    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
    }

    @Override
    public void pause() {}

    @Override
    public void resume() {}

    @Override
    public void hide() {
        Gdx.input.setInputProcessor(null);
    }

    @Override
    public void dispose() {
        stage.dispose();
        batch.dispose();
        titleFont.dispose();
        normalFont.dispose();
        if (backgroundTexture != null) {
            backgroundTexture.dispose();
        }
        Logger.info("AchievementListScreen disposed");
    }
}