package de.tum.cit.fop.maze.tools;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.ui.Label.LabelStyle;
import com.badlogic.gdx.scenes.scene2d.ui.TextField.TextFieldStyle;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import de.tum.cit.fop.maze.game.GameManager;
import de.tum.cit.fop.maze.entities.Player;
import de.tum.cit.fop.maze.entities.enemy.Enemy;

/**
 * 开发者控制台 (独立样式版)
 * 不依赖 uiskin.json，使用自带的字体和纹理生成 UI。
 * 确保 100% 能显示出来。
 */
public class DeveloperConsole {

    private boolean isVisible = false;
    private final Stage stage;
    private final GameManager gameManager;

    private TextField inputField;
    private Label logLabel;
    private ScrollPane scrollPane;
    private final StringBuilder logHistory = new StringBuilder();

    // 手动管理的资源
    private Texture bgTexture;
    private Texture cursorTexture;
    private BitmapFont font;

    public DeveloperConsole(GameManager gameManager, Skin skin) {
        this.gameManager = gameManager;
        this.stage = new Stage(new ScreenViewport());

        // 忽略传入的 skin，使用自己生成的样式
        setupSelfContainedUI();
    }

    private void setupSelfContainedUI() {
        // 1. 准备基础资源 (字体 & 纯色纹理)
        font = new BitmapFont(); // 使用 LibGDX 默认字体 (Arial)

        // 生成半透明黑色背景
        Pixmap p = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        p.setColor(0, 0, 0, 0.7f);
        p.fill();
        bgTexture = new Texture(p);

        // 生成白色光标
        p.setColor(Color.WHITE);
        p.fill();
        cursorTexture = new Texture(p);
        p.dispose(); // 释放 Pixmap

        TextureRegionDrawable bgDrawable = new TextureRegionDrawable(new TextureRegion(bgTexture));
        TextureRegionDrawable cursorDrawable = new TextureRegionDrawable(new TextureRegion(cursorTexture));

        // 2. 定义样式 (Styles)
        LabelStyle labelStyle = new LabelStyle(font, Color.WHITE);

        TextFieldStyle tfStyle = new TextFieldStyle();
        tfStyle.font = font;
        tfStyle.fontColor = Color.WHITE;
        tfStyle.cursor = cursorDrawable;
        tfStyle.selection = cursorDrawable;
        tfStyle.background = bgDrawable; // 输入框背景

        // 3. 组装 UI
        Table rootTable = new Table();
        rootTable.setFillParent(true);
        rootTable.top().left();

        // 控制台主容器
        Table consoleTable = new Table();
        consoleTable.setBackground(bgDrawable); // 整个控制台背景

        // 日志区域
        logLabel = new Label("Console initialized. Commands: help, heal, give_key, kill_all\n", labelStyle);
        logLabel.setWrap(true);
        logLabel.setAlignment(Align.topLeft);

        scrollPane = new ScrollPane(logLabel); // 默认 ScrollPane 样式即可
        scrollPane.setFadeScrollBars(false);

        // 输入框
        inputField = new TextField("", tfStyle);
        inputField.setMessageText("Enter command...");

        // 布局
        consoleTable.add(scrollPane).expandX().fillX().height(300).pad(5).row();
        consoleTable.add(inputField).expandX().fillX().height(30).pad(5).row();

        rootTable.add(consoleTable).growX().top();
        stage.addActor(rootTable);

        // 监听回车
        inputField.setTextFieldListener((textField, c) -> {
            if (c == '\r' || c == '\n') {
                String cmd = textField.getText().trim();
                if (!cmd.isEmpty()) {
                    processCommand(cmd);
                    textField.setText("");
                }
            }
        });
    }

    private void processCommand(String rawCommand) {
        log("> " + rawCommand);

        String[] parts = rawCommand.split(" ");
        String command = parts[0].toLowerCase();

        try {
            switch (command) {
                case "help":
                    log("Available: heal [amt], give_key, kill_all, clear");
                    break;
                case "clear":
                    logHistory.setLength(0);
                    logLabel.setText("");
                    break;
                case "heal":
                    Player p = gameManager.getPlayer();
                    int amt = parts.length > 1 ? Integer.parseInt(parts[1]) : 100;
                    p.heal(amt);
                    log("Healed player by " + amt);
                    break;
                case "give_key":
                    gameManager.getPlayer().setHasKey(true);
                    log("Key added to inventory.");
                    break;
                case "kill_all":
                    int count = 0;
                    for (Enemy e : gameManager.getEnemies()) {
                        e.takeDamage(9999);
                        count++;
                    }
                    log("Killed " + count + " enemies.");
                    break;
                default:
                    log("Unknown command.");
                    break;
            }
        } catch (Exception e) {
            log("Error: " + e.getMessage());
        }
    }

    private void log(String message) {
        logHistory.append(message).append("\n");
        if (logLabel != null) {
            logLabel.setText(logHistory.toString());
        }
        if (scrollPane != null) {
            // 自动滚动到底部
            Gdx.app.postRunnable(() -> scrollPane.setScrollY(scrollPane.getMaxY()));
        }
    }

    public void toggle() {
        isVisible = !isVisible;
        if (isVisible) {
            Gdx.input.setInputProcessor(stage);
            stage.setKeyboardFocus(inputField);
        } else {
            // 这里不重置 InputProcessor，依靠 GameScreen 每帧判断来切回游戏控制
            stage.setKeyboardFocus(null);
            stage.unfocusAll();
        }
    }

    public void render() {
        if (isVisible) {
            stage.act();
            stage.draw();
        }
    }

    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
    }

    public void dispose() {
        if (stage != null) stage.dispose();
        if (bgTexture != null) bgTexture.dispose();
        if (cursorTexture != null) cursorTexture.dispose();
        if (font != null) font.dispose();
    }

    public boolean isVisible() {
        return isVisible;
    }
}