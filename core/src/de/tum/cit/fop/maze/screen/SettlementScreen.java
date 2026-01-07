package de.tum.cit.fop.maze.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import de.tum.cit.fop.maze.MazeRunnerGame;
import de.tum.cit.fop.maze.game.GameSaveData;
import de.tum.cit.fop.maze.game.achievement.AchievementType;
import de.tum.cit.fop.maze.game.score.LevelResult;
import de.tum.cit.fop.maze.tools.ButtonFactory;
import de.tum.cit.fop.maze.utils.Logger;

/**
 * 结算界面 (Settlement Screen)
 * <p>
 * 职责：
 * 1. 展示关卡评分详情 (基础分、扣分、倍率、最终得分)。
 * 2. 展示评级印章 (S/A/B/C/D)。
 * 3. 展示本局游戏统计 (Session Stats)。
 * 4. 展示新解锁的成就。
 * 5. 提供 "下一关" 或 "返回菜单" 的入口。
 */
public class SettlementScreen implements Screen {

    private final MazeRunnerGame game;
    private final LevelResult result;
    private final GameSaveData saveData;
    private Stage stage;

    public SettlementScreen(MazeRunnerGame game, LevelResult result, GameSaveData saveData) {
        this.game = game;
        this.result = result;
        this.saveData = saveData;
    }

    @Override
    public void show() {
        stage = new Stage(new ScreenViewport());
        Gdx.input.setInputProcessor(stage);

        Table root = new Table();
        root.setFillParent(true);
        // root.setDebug(true); // 调试布局时可开启
        stage.addActor(root);

        // ==========================================
        // 1. 标题 (LEVEL CLEARED)
        // ==========================================
        Label titleLabel = new Label("LEVEL COMPLETED", game.getSkin(), "title");
        titleLabel.setColor(Color.GOLD);
        root.add(titleLabel).padBottom(40).colspan(2).row();

        // ==========================================
        // 2. 评分详情表 (左侧)
        // ==========================================
        Table scoreTable = new Table();
        scoreTable.setBackground(game.getSkin().getDrawable("window-c")); // 假设有窗口背景
        scoreTable.pad(20);

        addScoreRow(scoreTable, "Base Score", "+" + result.baseScore, Color.WHITE);
        addScoreRow(scoreTable, "Penalty", "-" + result.penaltyScore, Color.SCARLET);
        addScoreRow(scoreTable, "Multiplier", "x" + result.scoreMultiplier, Color.CYAN);

        // 分割线
        scoreTable.add(new Label("----------", game.getSkin())).colspan(2).pad(5).row();

        addScoreRow(scoreTable, "FINAL SCORE", String.valueOf(result.finalScore), Color.GOLD);

        // 评分表放在左边
        root.add(scoreTable).width(400).padRight(50);

        // ==========================================
        // 3. 评级印章 (右侧)
        // ==========================================
        Table rankTable = new Table();

        Label rankTitle = new Label("RANK", game.getSkin());
        rankTable.add(rankTitle).row();

        // 巨大的评级字母
        Label rankLabel = new Label(result.rank, game.getSkin(), "title");
        rankLabel.setFontScale(4.0f); // 放大字体

        // 根据评级设置颜色
        switch (result.rank) {
            case "S" -> rankLabel.setColor(1f, 0.84f, 0f, 1f); // 金色
            case "A" -> rankLabel.setColor(0.75f, 0.75f, 0.75f, 1f); // 银色
            case "B" -> rankLabel.setColor(0.8f, 0.5f, 0.2f, 1f); // 铜色
            default  -> rankLabel.setColor(Color.WHITE);
        }
        rankTable.add(rankLabel).pad(20).row();

        // 如果是S级，可以加一句评语
        if ("S".equals(result.rank)) {
            Label praise = new Label("EXCELLENT!", game.getSkin());
            praise.setColor(Color.GOLD);
            rankTable.add(praise);
        }

        root.add(rankTable).row();

        // ==========================================
        // 4. 本局统计 & 新成就 (下方)
        // ==========================================
        Table statsTable = new Table();
        statsTable.defaults().pad(10);

        // 4.1 统计信息
        int totalKills = saveData.sessionKills.values().stream().mapToInt(Integer::intValue).sum();
        statsTable.add(new Label("Session Kills: " + totalKills, game.getSkin()));
        statsTable.add(new Label("Damage Taken: " + saveData.sessionDamageTaken, game.getSkin()));
        statsTable.row();

        // 4.2 新解锁成就
        if (!saveData.newAchievements.isEmpty()) {
            statsTable.add(new Label("NEW ACHIEVEMENTS UNLOCKED!", game.getSkin())).colspan(2).padTop(20).color(Color.YELLOW).row();

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

            // 展示完毕后，可以在离开屏幕时清空，或者在这里就不清空留给StorageManager处理
            // 建议：saveData.newAchievements.clear() 放在点击按钮离开时
        }

        root.add(statsTable).colspan(2).padTop(30).row();

        // ==========================================
        // 5. 按钮栏 (底部)
        // ==========================================
        Table buttonTable = new Table();
        ButtonFactory bf = new ButtonFactory(game.getSkin());

        // NEXT LEVEL 按钮
        buttonTable.add(bf.create("NEXT LEVEL", () -> {
            clearNewAchievements();
            // 逻辑: 继续游戏 (GameManager 应该已经处理好关卡递增，或者在这里处理)
            // 这里假设调用 goToGame 会重新加载 GameScreen
            game.goToGame();
        })).width(300).pad(20);

        // MENU 按钮
        buttonTable.add(bf.create("MENU", () -> {
            clearNewAchievements();
            game.goToMenu();
        })).width(300).pad(20);

        root.add(buttonTable).colspan(2).padTop(40);
    }

    private void addScoreRow(Table table, String name, String value, Color valueColor) {
        table.add(new Label(name, game.getSkin())).align(Align.left).expandX();
        Label valLabel = new Label(value, game.getSkin());
        valLabel.setColor(valueColor);
        table.add(valLabel).align(Align.right);
        table.row();
    }

    private void clearNewAchievements() {
        // 离开界面时，清空"新解锁"列表，以免下次结算重复显示
        if (saveData != null) {
            saveData.newAchievements.clear();
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
