package de.tum.cit.fop.maze.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import de.tum.cit.fop.maze.MazeRunnerGame;
import de.tum.cit.fop.maze.game.GameSaveData;
import de.tum.cit.fop.maze.tools.ButtonFactory;
import de.tum.cit.fop.maze.utils.LeaderboardManager;

public class VictoryScreen implements Screen {

    private final MazeRunnerGame game;
    private final Stage stage;
    private final GameSaveData finalData;
    private int totalScore;
    private TextField nameInput;

    public VictoryScreen(MazeRunnerGame game, GameSaveData data) {
        this.game = game;
        this.finalData = data;
        this.stage = new Stage(new ScreenViewport());
        Gdx.input.setInputProcessor(stage);

        calculateTotalScore();
        setupUI();
    }

    private void calculateTotalScore() {
        // 总分计算：基础分 (GameSaveData.score 已含战斗/拾取/扣分) + 额外奖励
        int baseScore = finalData.score;
        int lifeBonus = finalData.lives * 100;

        // 成就奖励：统计已解锁成就数量
        int achievementCount = 0;
        for (Boolean unlocked : finalData.unlockedAchievements.values()) {
            if (unlocked) achievementCount++;
        }
        int achievementBonus = achievementCount * 500; // 每个成就额外加 500 分展示分

        this.totalScore = baseScore + lifeBonus + achievementBonus;
    }

    private void setupUI() {
        Table root = new Table();
        root.setFillParent(true);
        stage.addActor(root);

        // 1. 标题 (淡入)
        Label title = new Label("MISSION ACCOMPLISHED", game.getSkin(), "title");
        title.setColor(Color.GOLD);
        title.setFontScale(1.5f);
        title.getColor().a = 0f;
        title.addAction(Actions.fadeIn(1f));
        root.add(title).padBottom(40).row();

        // 2. 清单表格 (带背景)
        Table list = new Table();
        list.setBackground(game.getSkin().getDrawable("window-c")); // 假设有此资源

        float delay = 0.5f;

        // 基础分
        addEntry(list, "Base Score", finalData.score, delay);
        delay += 0.3f;

        // 生命奖励
        addEntry(list, "Life Bonus (x" + finalData.lives + ")", finalData.lives * 100, delay);
        delay += 0.3f;

        // 成就加成
        int achCount = 0;
        for (Boolean b : finalData.unlockedAchievements.values()) if(b) achCount++;
        if (achCount > 0) {
            addEntry(list, "Achievements (x" + achCount + ")", achCount * 500, delay);
            delay += 0.3f;
        }

        // 分割线
        Label line = new Label("----------------", game.getSkin());
        list.add(line).colspan(2).pad(10).row();

        // 总分 (放大弹出特效)
        Label totalLabel = new Label("TOTAL SCORE", game.getSkin(), "title");
        Label totalValue = new Label(String.valueOf(totalScore), game.getSkin(), "title");
        totalValue.setColor(Color.CYAN);

        totalValue.getColor().a = 0f;
        totalValue.setScale(0f);
        totalValue.addAction(Actions.sequence(
                Actions.delay(delay + 0.2f),
                Actions.parallel(Actions.fadeIn(0.5f), Actions.scaleTo(1.2f, 1.2f, 0.5f, Interpolation.elasticOut))
        ));

        list.add(totalLabel).pad(10);
        list.add(totalValue).pad(10).row();

        root.add(list).width(600).padBottom(30).row();

        // 3. 名字输入框 (最后出现)
        Table inputTable = new Table();
        inputTable.add(new Label("Enter Name: ", game.getSkin())).padRight(10);
        nameInput = new TextField("Player", game.getSkin());
        inputTable.add(nameInput).width(200);

        inputTable.getColor().a = 0f;
        inputTable.addAction(Actions.sequence(Actions.delay(delay + 1f), Actions.fadeIn(0.5f)));
        root.add(inputTable).padBottom(20).row();

        // 4. 确认按钮
        ButtonFactory bf = new ButtonFactory(game.getSkin());
        TextButton btnConfirm = bf.create("CONFIRM", () -> {
            String name = nameInput.getText();
            if (name.isEmpty()) name = "Unknown";
            new LeaderboardManager().addScore(name, totalScore);
            game.setScreen(new LeaderboardScreen(game, new MenuScreen(game)));
        });

        btnConfirm.getColor().a = 0f;
        btnConfirm.addAction(Actions.sequence(Actions.delay(delay + 1.2f), Actions.fadeIn(0.5f)));
        root.add(btnConfirm).width(200).height(50);
    }

    // 辅助方法：添加一行带动画的清单项
    private void addEntry(Table table, String text, int value, float delay) {
        Label lText = new Label(text, game.getSkin());
        Label lValue = new Label(String.valueOf(value), game.getSkin());

        lText.getColor().a = 0f;
        lValue.getColor().a = 0f;

        // 文字左滑入，数字淡入
        lText.addAction(Actions.sequence(Actions.delay(delay), Actions.fadeIn(0.3f), Actions.moveBy(10, 0, 0.3f, Interpolation.pow2Out)));
        lValue.addAction(Actions.sequence(Actions.delay(delay + 0.1f), Actions.fadeIn(0.3f)));

        table.add(lText).align(Align.left).pad(5);
        table.add(lValue).align(Align.right).pad(5).row();
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0.1f, 0.1f, 0.15f, 1f); // 深蓝色背景
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