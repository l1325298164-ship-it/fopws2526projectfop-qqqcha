package de.tum.cit.fop.maze.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import de.tum.cit.fop.maze.MazeRunnerGame;
import de.tum.cit.fop.maze.game.GameSaveData;
import de.tum.cit.fop.maze.game.achievement.AchievementType;
import de.tum.cit.fop.maze.game.score.LevelResult;
import de.tum.cit.fop.maze.tools.ButtonFactory;
import de.tum.cit.fop.maze.utils.LeaderboardManager;
import de.tum.cit.fop.maze.utils.Logger;
import de.tum.cit.fop.maze.utils.StorageManager;

/**
 * 结算界面 (Settlement Screen)
 * <p>
 * 职责：
 * 1. 展示关卡评分详情。
 * 2. 展示评级印章。
 * 3. 排行榜交互（打破纪录时输入名字）。
 * 4. 展示新解锁的成就。
 * 5. 下一关/返回菜单。
 */
public class SettlementScreen implements Screen {

    private final MazeRunnerGame game;
    private final LevelResult result;
    private final GameSaveData saveData;
    private Stage stage;
    private final LeaderboardManager leaderboardManager;

    // ✨ [新增] 控制排行榜输入的标志位
    private boolean isHighScore = false;
    private boolean scoreSubmitted = false;

    public SettlementScreen(MazeRunnerGame game, LevelResult result, GameSaveData saveData) {
        this.game = game;
        this.result = result;
        this.saveData = saveData;
        this.leaderboardManager = new LeaderboardManager();

        // 🛠️ 累加分数
        this.saveData.score += result.finalScore;

        // 🛠️ [修改] 移除自动提交，改为检查是否破纪录
        this.isHighScore = leaderboardManager.isHighScore(this.saveData.score);

        Logger.info("Settlement: Level Score=" + result.finalScore +
                ", Total Score=" + saveData.score +
                ", HighScore? " + isHighScore);
    }

    @Override
    public void show() {
        stage = new Stage(new ScreenViewport());
        Gdx.input.setInputProcessor(stage);
        setupUI();
    }

    private void setupUI() {
        Table root = new Table();
        root.setFillParent(true);
        // root.setDebug(true); // 调试布局时可开启
        stage.addActor(root);

        // ==========================================
        // 1. 标题 (LEVEL COMPLETED)
        // ==========================================
        Label titleLabel = new Label("LEVEL COMPLETED", game.getSkin(), "title");
        titleLabel.setColor(Color.GOLD);
        root.add(titleLabel).padBottom(30).colspan(2).row();

        // ==========================================
        // 2. 核心布局 (左侧分数，右侧评级与输入框)
        // ==========================================
        Table leftPanel = new Table();
        Table rightPanel = new Table();

        // --- 左侧：评分详情表 ---
        Table scoreTable = new Table();
        scoreTable.setBackground(game.getSkin().getDrawable("window-c"));
        scoreTable.pad(20);

        addScoreRow(scoreTable, "Base Score", "+" + formatScore(result.baseScore), Color.WHITE);
        addScoreRow(scoreTable, "Penalty", "-" + formatScore(result.penaltyScore), Color.SCARLET);
        
        // 改进倍率显示：显示难度信息
        String multiplierText = getMultiplierText(result.scoreMultiplier);
        addScoreRow(scoreTable, "Multiplier", multiplierText, Color.CYAN);

        // 分割线
        scoreTable.add(new Label("----------", game.getSkin())).colspan(2).pad(5).row();

        addScoreRow(scoreTable, "LEVEL SCORE", String.valueOf(result.finalScore), Color.GOLD);
        // 显示当前总分
        addScoreRow(scoreTable, "TOTAL SCORE", String.valueOf(saveData.score), Color.ORANGE);

        leftPanel.add(scoreTable).width(400);

        // --- 右侧：评级印章 ---
        Label rankTitle = new Label("RANK", game.getSkin());
        rightPanel.add(rankTitle).row();

        // 巨大的评级字母
        Label rankLabel = new Label(result.rank, game.getSkin(), "title");
        rankLabel.setFontScale(4.0f); // 放大字体
        setRankColor(rankLabel, result.rank);
        rightPanel.add(rankLabel).pad(10).row();

        if ("S".equals(result.rank)) {
            Label praise = new Label("EXCELLENT!", game.getSkin());
            praise.setColor(Color.GOLD);
            rightPanel.add(praise).row();
        }

        // --- 右侧：✨ 排行榜输入逻辑 ---
        if (isHighScore && !scoreSubmitted) {
            Table inputTable = new Table();
            inputTable.setBackground(game.getSkin().getDrawable("window-c"));
            inputTable.pad(15);

            Label newRecordLabel = new Label("NEW HIGH SCORE!", game.getSkin());
            newRecordLabel.setColor(Color.YELLOW);
            newRecordLabel.setFontScale(0.8f);

            // 名字输入框 (需 Skin 支持 TextField)
            TextField nameField = new TextField("Traveler", game.getSkin());
            nameField.setMessageText("Enter Name");
            nameField.setAlignment(Align.center);

            ButtonFactory bf = new ButtonFactory(game.getSkin());

            inputTable.add(newRecordLabel).padBottom(5).row();
            inputTable.add(nameField).width(200).padBottom(10).row();
            inputTable.add(bf.create("SUBMIT", () -> {
                String name = nameField.getText();
                if (name == null || name.trim().isEmpty()) name = "Unknown";

                // 提交分数
                leaderboardManager.addScore(name, saveData.score);
                scoreSubmitted = true;

                // 刷新 UI
                inputTable.clear();
                Label submittedLabel = new Label("Score Submitted!", game.getSkin());
                submittedLabel.setColor(Color.GREEN);
                inputTable.add(submittedLabel);

            })).width(120).height(40);

            rightPanel.add(inputTable).padTop(20);

        } else if (scoreSubmitted) {
            Label submittedLabel = new Label("Score Submitted!", game.getSkin());
            submittedLabel.setColor(Color.GREEN);
            rightPanel.add(submittedLabel).padTop(20);
        }

        // 将左右面板加入根布局
        root.add(leftPanel).padRight(30);
        root.add(rightPanel).padLeft(30);
        root.row();

        // ==========================================
        // 3. 本局统计 & 新成就 (下方)
        // ==========================================
        Table statsTable = new Table();
        statsTable.defaults().pad(10);

        // 统计信息
        int totalKills = saveData.sessionKills.values().stream().mapToInt(Integer::intValue).sum();
        statsTable.add(new Label("Session Kills: " + totalKills, game.getSkin()));
        statsTable.add(new Label("Damage Taken: " + saveData.sessionDamageTaken, game.getSkin()));
        statsTable.row();

        // 新解锁成就
        if (!saveData.newAchievements.isEmpty()) {
            Label achievementTitle = new Label("NEW ACHIEVEMENTS UNLOCKED!", game.getSkin());
            achievementTitle.setColor(Color.YELLOW);
            statsTable.add(achievementTitle).colspan(2).padTop(20).row();

            for (String achId : saveData.newAchievements) {
                // 尝试查找成就名称
                String name = achId;
                for (AchievementType t : AchievementType.values()) {
                    if (t.id.equals(achId)) {
                        name = t.displayName;
                        break;
                    }
                }
                Label achLabel = new Label("🏆 " + name, game.getSkin());
                achLabel.setColor(Color.GREEN);
                statsTable.add(achLabel).colspan(2).row();
            }
        }

        root.add(statsTable).colspan(2).padTop(30).row();

        // ==========================================
        // 4. 按钮栏 (底部)
        // ==========================================
        Table buttonTable = new Table();
        ButtonFactory bf = new ButtonFactory(game.getSkin());

        // NEXT LEVEL 按钮
        buttonTable.add(bf.create("NEXT LEVEL", () -> {
            performSaveAndExit(true);
        })).width(300).pad(20);

        // MENU 按钮
        buttonTable.add(bf.create("MENU", () -> {
            performSaveAndExit(false);
        })).width(300).pad(20);

        root.add(buttonTable).colspan(2).padTop(40);
    }

    /**
     * 设置评级颜色
     */
    private void setRankColor(Label label, String rank) {
        switch (rank) {
            case "S" -> label.setColor(1f, 0.84f, 0f, 1f); // 金色
            case "A" -> label.setColor(0.75f, 0.75f, 0.75f, 1f); // 银色
            case "B" -> label.setColor(0.8f, 0.5f, 0.2f, 1f); // 铜色
            default  -> label.setColor(Color.WHITE);
        }
    }

    /**
     * 执行保存并跳转
     * @param toNextLevel true去下一关，false回菜单
     */
    private void performSaveAndExit(boolean toNextLevel) {
        // 1. 清理临时UI数据
        clearNewAchievements();

        // 2. 准备保存数据
        StorageManager storage = StorageManager.getInstance();
        
        if (toNextLevel) {
            // ✨ [修改] 进入下一关前，增加关卡数并重置本关临时统计
            saveData.currentLevel++;
            saveData.levelBaseScore = 0;
            saveData.levelPenalty = 0;
            // score 已经在构造函数中累加过了，保持不变
            
            // ✨ [新增] 同步分数到 ScoreManager（确保下一关时分数正确）
            if (game.getGameManager() != null && game.getGameManager().getScoreManager() != null) {
                // 通过 restoreState 更新 ScoreManager 的 accumulatedScore
                GameSaveData tempData = new GameSaveData();
                tempData.score = saveData.score;  // 使用累加后的总分
                tempData.levelBaseScore = 0;
                tempData.levelPenalty = 0;
                game.getGameManager().getScoreManager().restoreState(tempData);
            }
            
            // 保存进度（关键节点，使用同步保存）
            storage.saveGameSync(saveData);
            
            // 重新加载游戏（会从存档恢复状态）
            game.loadGame();
        } else {
            // 返回菜单时保存当前进度（关键节点，使用同步保存）
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

    /**
     * 格式化分数，添加千位分隔符
     */
    private String formatScore(int score) {
        return String.format("%,d", score);
    }
    
    /**
     * 获取倍率显示文本，包含难度信息
     */
    private String getMultiplierText(float multiplier) {
        // 根据倍率判断难度（如果可能的话）
        String difficultyHint = "";
        if (multiplier >= 1.5f) {
            difficultyHint = " (Hard)";
        } else if (multiplier >= 1.2f) {
            difficultyHint = " (Normal)";
        } else if (multiplier >= 1.0f) {
            difficultyHint = " (Easy)";
        } else if (multiplier >= 2.0f) {
            difficultyHint = " (Endless)";
        }
        return String.format("x%.1f%s", multiplier, difficultyHint);
    }
    
    private void clearNewAchievements() {
        // 离开界面时，清空"新解锁"列表
        if (saveData != null) {
            saveData.newAchievements.clear();
            // 同时清空单局统计
            saveData.sessionDamageTaken = 0;
            saveData.sessionKills.clear();
        }
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0.1f, 0.1f, 0.15f, 1f); // 深蓝背景
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        stage.act(delta);
        stage.draw();
    }

    @Override public void resize(int width, int height) { stage.getViewport().update(width, height, true); }
    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() {}
    @Override public void dispose() { stage.dispose(); }
}