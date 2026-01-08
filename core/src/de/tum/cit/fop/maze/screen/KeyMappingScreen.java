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
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;

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

        skin = game.getSkin();

        // 1. 主表格
        Table rootTable = new Table();
        rootTable.setFillParent(true);
        stage.addActor(rootTable);

        // --- 标题 ---
        Label titleLabel = new Label("CONTROLS SETTINGS", skin);
        titleLabel.setFontScale(1.5f);
        rootTable.add(titleLabel).padBottom(50).row(); // 标题下方的间距加大到 50

        // 2. 内容表格 (放按键列表)
        Table contentTable = new Table();

        for (GameAction action : GameAction.values()) {
            String actionName = action.name().replace("_", " ");
            Label nameLabel = new Label(actionName, skin);

            String keyName = KeyBindingManager.getInstance().getKeyName(action);
            TextButton keyButton = new TextButton(keyName, skin);

            keyButton.addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    if (!isWaitingForKey) {
                        startRebinding(action, keyButton);
                    }
                }
            });

            // 🔥 修改点 1：文字和按钮中间的空隙，从 20 改成 50
            contentTable.add(nameLabel).left().padRight(500);

            // 🔥 修改点 2：每一行的上下间距，从 10 改成 25
            contentTable.add(keyButton).width(150).height(40).padBottom(10).row();
        }

        // 3. 滚动窗格
        com.badlogic.gdx.scenes.scene2d.ui.ScrollPane.ScrollPaneStyle scrollStyle = new com.badlogic.gdx.scenes.scene2d.ui.ScrollPane.ScrollPaneStyle();
        if (skin.has("white", com.badlogic.gdx.graphics.g2d.TextureRegion.class)) {
            com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable knob = new com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable(skin.getRegion("white"));
            knob.setMinWidth(10);
            scrollStyle.vScrollKnob = knob;
        }

        com.badlogic.gdx.scenes.scene2d.ui.ScrollPane scrollPane = new com.badlogic.gdx.scenes.scene2d.ui.ScrollPane(contentTable, scrollStyle);
        scrollPane.setScrollingDisabled(true, false);
        scrollPane.setFadeScrollBars(false);

        // 把滚动窗格加进去
        rootTable.add(scrollPane).expand().fill().row();

        // 4. 底部按钮区
        Table bottomTable = new Table();

        TextButton resetButton = new TextButton("Default", skin);
        resetButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                KeyBindingManager.getInstance().resetToDefaults();
                game.setScreen(new KeyMappingScreen(game, previousScreen));
            }
        });

        TextButton backButton = new TextButton("Back", skin);
        backButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                game.setScreen(previousScreen);
                dispose();
            }
        });

        // 🔥 修改点 3：底部两个按钮中间的间距，从 20 改成 60
        bottomTable.add(resetButton).width(150).height(50).padRight(300);
        bottomTable.add(backButton).width(150).height(50);

        rootTable.add(bottomTable).padTop(40);
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
