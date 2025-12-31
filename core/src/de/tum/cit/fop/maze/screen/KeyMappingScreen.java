package de.tum.cit.fop.maze.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import de.tum.cit.fop.maze.MazeRunnerGame;
import de.tum.cit.fop.maze.input.KeyBindingManager;
import de.tum.cit.fop.maze.input.KeyBindingManager.GameAction;

/**
 * 按键设置菜单
 * 对应任务：Create a settings menu that allows players to remap controls.
 */
public class KeyMappingScreen implements Screen {

    private final MazeRunnerGame game;
    private final Screen previousScreen; // 记录上一个界面，方便返回
    private Stage stage;
    private Skin skin;

    // 状态标记：是否正在等待用户输入新按键
    private boolean isWaitingForKey = false;
    private GameAction actionRebinding = null; // 当前正在修改哪个动作
    private TextButton buttonRebinding = null; // 当前正在修改的按钮

    public KeyMappingScreen(MazeRunnerGame game, Screen previousScreen) {
        this.game = game;
        this.previousScreen = previousScreen;
    }

    @Override
    public void show() {
        stage = new Stage(new ScreenViewport());
        Gdx.input.setInputProcessor(stage);

        // 获取皮肤 (假设你在 GameManager 或 Game 类里加载了 skin，如果没有请替换为你的皮肤路径)
        // 这里假设 game.getSkin() 存在，如果不存在，你需要用 new Skin(Gdx.files.internal("ui/uiskin.json"))
        skin = game.getSkin();

        Table table = new Table();
        table.setFillParent(true);
        // table.setDebug(true); // 调试布局时可以打开
        stage.addActor(table);

        // 标题
        Label titleLabel = new Label("CONTROLS SETTINGS", skin);
        titleLabel.setFontScale(1.5f);
        table.add(titleLabel).colspan(2).padBottom(40).row();

        // 遍历所有动作，动态生成设置行
        for (GameAction action : GameAction.values()) {
            // 动作名称标签 (左边)
            String actionName = action.name().replace("_", " "); // 把 MOVE_UP 变成 MOVE UP 稍微好看点
            Label nameLabel = new Label(actionName, skin);

            // 当前按键按钮 (右边)
            String keyName = KeyBindingManager.getInstance().getKeyName(action);
            TextButton keyButton = new TextButton(keyName, skin);

            // 给按钮添加点击事件
            keyButton.addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    if (!isWaitingForKey) {
                        startRebinding(action, keyButton);
                    }
                }
            });

            table.add(nameLabel).left().padRight(20);
            // 把 .padBottom(10) 移到 .row() 之前
            table.add(keyButton).width(150).height(40).padBottom(10).row();
        }

        // 🔥 新增：恢复默认按钮
        TextButton resetButton = new TextButton("Default", skin);
        resetButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                // 1. 调用数据重置
                KeyBindingManager.getInstance().resetToDefaults();

                // 2. 刷新当前界面 (最简单的刷新方法就是重新 setScreen 一次自己)
                // 这样所有按钮上的文字就会自动更新回 "UP", "DOWN" 等
                game.setScreen(new KeyMappingScreen(game, previousScreen));
            }
        });

        // 返回按钮
        TextButton backButton = new TextButton("Back", skin);
        backButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                game.setScreen(previousScreen);
                dispose();
            }
        });

        // 将两个按钮并排放在底部
        // 先加 Reset 按钮
        table.add(resetButton).width(150).height(50).padTop(40).padRight(20);
        // 再加 Back 按钮
        table.add(backButton).width(150).height(50).padTop(40);
    }

    /**
     * 开始重新绑定流程
     */
    private void startRebinding(GameAction action, TextButton button) {
        isWaitingForKey = true;
        actionRebinding = action;
        buttonRebinding = button;

        // 更新按钮文字提示
        button.setText("Press any key...");

        // 临时切换输入处理器，监听键盘
        Gdx.input.setInputProcessor(new InputAdapter() {
            @Override
            public boolean keyDown(int keycode) {
                // 如果按了 ESC，取消修改
                if (keycode == Input.Keys.ESCAPE) {
                    finishRebinding(KeyBindingManager.getInstance().getKey(action)); // 恢复原状
                    return true;
                }

                // 保存新按键
                finishRebinding(keycode);
                return true;
            }
        });
    }

    /**
     * 完成绑定，保存并恢复 UI
     */
    private void finishRebinding(int keycode) {
        // 1. 保存数据
        KeyBindingManager.getInstance().setBinding(actionRebinding, keycode);

        // 2. 更新 UI 文字
        String newKeyName = Input.Keys.toString(keycode);
        buttonRebinding.setText(newKeyName);

        // 3. 重置状态
        isWaitingForKey = false;
        actionRebinding = null;
        buttonRebinding = null;

        // 4. 恢复 Stage 为输入处理器 (让按钮能再次被点击)
        Gdx.input.setInputProcessor(stage);
    }

    @Override
    public void render(float delta) {
        // 深灰色背景
        Gdx.gl.glClearColor(0.2f, 0.2f, 0.2f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        stage.act(Math.min(Gdx.graphics.getDeltaTime(), 1 / 30f));
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
    public void hide() {}

    @Override
    public void dispose() {
        if (stage != null) stage.dispose();
    }
}
