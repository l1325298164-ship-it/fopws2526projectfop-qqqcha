// HUD.java - 更新版本
package de.tum.cit.fop.maze.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Matrix4;
import de.tum.cit.fop.maze.abilities.Ability;
import de.tum.cit.fop.maze.abilities.DashAbility;
import de.tum.cit.fop.maze.entities.Compass;
import de.tum.cit.fop.maze.game.GameConstants;
import de.tum.cit.fop.maze.game.GameManager;
import de.tum.cit.fop.maze.utils.Logger;
import de.tum.cit.fop.maze.utils.TextureManager;

import java.util.function.ToDoubleBiFunction;

public class HUD {
    private BitmapFont font;
    private GameManager gameManager;
    private TextureManager textureManager;
    private SpriteBatch uiBatchForCompass;
    // ❤ 生命值贴图
    private Texture heartFull;   // live_00
    private Texture heartHalf;   // live_01
    private static final int MAX_HEARTS_DISPLAY = 80; // 最多显示 50 颗
    private static final int HEARTS_PER_ROW = 40;     // 每行最多 10 颗
    private static final int HEART_SPACING = 70;      // 爱心之间的水平间距
    private static final int ROW_SPACING = 30;        // 行距
    // ❤ 抖动动画相关
    private int lastLives = -1;

    private boolean shaking = false;
    private float shakeTimer = 0f;

    private static final float SHAKE_DURATION = 0.2f; // 抖动 0.2 秒
    private static final float SHAKE_AMPLITUDE = 4f;  // 抖动幅度（像素）

    // ===== 技能图标：冲刺 =====
    private Texture dashIcon;

    private static final int DASH_MAX_CHARGES = 2;

    // ===== Mana UI =====
    private ShapeRenderer shapeRenderer;

    private static final int MANA_BAR_WIDTH = 180;
    private static final int MANA_BAR_HEIGHT = 18;

    private static final int MANA_UI_MARGIN_X = 20;
    private static final int MANA_UI_MARGIN_Y = 100; // 在 Dash 图标上方



    // UI 尺寸
    private static final int DASH_ICON_SIZE = 64;
    private static final int DASH_ICON_SPACING = 10;
    // ===== Dash UI 布局 =====

    private static final int DASH_UI_MARGIN_X = 20; // 距离左边
    private static final int DASH_UI_MARGIN_Y = 20; // 距离下边




    public HUD(GameManager gameManager) {
        this.gameManager = gameManager;
        this.font = new BitmapFont();
        this.font.getData().setScale(1.2f);
        this.textureManager = TextureManager.getInstance();
        Logger.debug("HUD initialized with compass support");
        this.uiBatchForCompass = new SpriteBatch();
        this.shapeRenderer = new ShapeRenderer();

        heartFull = new Texture("HUD/live_000.png");
        heartHalf = new Texture("HUD/live_001.png");

        dashIcon = new Texture("HUD/icon_dash.png");


        Logger.debug("HUD initialized with heart-based life bar");
    }

    /**
     * 渲染游戏进行中的UI
     */
    public void renderInGameUI(SpriteBatch uiBatch) {
        try {
            // 1. 钥匙状态
            if (gameManager.getPlayer().hasKey()) {
                font.setColor(Color.GREEN);
                font.draw(uiBatch, "key: get", 20, Gdx.graphics.getHeight() - 40);
            } else {
                font.setColor(Color.YELLOW);
                font.draw(uiBatch, "key: needed", 20, Gdx.graphics.getHeight() - 40);
            }

            // 2. 生命值（❤显示）
            renderLivesAsHearts(uiBatch);

            // 3. 关卡信息
            font.setColor(Color.CYAN);
            font.draw(uiBatch, "start: " + gameManager.getCurrentLevel(),
                20, Gdx.graphics.getHeight() - 120);

            // 4. 操作说明
            font.setColor(Color.WHITE);
            font.draw(uiBatch, "direction buttons to move，Shift to sprint",
                20, Gdx.graphics.getHeight() - 160);

            // 5. 纹理模式提示
            TextureManager.TextureMode currentMode = textureManager.getCurrentMode();
            if (currentMode != TextureManager.TextureMode.COLOR) {
                font.setColor(Color.GREEN);
                font.draw(uiBatch, "mode: " + currentMode + " (F1-F4 to switch)",
                    Gdx.graphics.getWidth() - 250,
                    Gdx.graphics.getHeight() - 20);
            }
            // 6. 指南针
            renderCompassAsUI();
            // 7. 技能图标
            renderDashIcon(uiBatch);


        } catch (Exception e) {
            Logger.error("Error rendering in-game UI", e);
        }
    }

    public void renderManaBar() {
        if (gameManager == null || gameManager.getPlayer() == null) return;

        var player = gameManager.getPlayer();

        float mana = player.getMana();
        float maxMana = 100f; // 如果以后做升级，可以改成 getter
        float percent = mana / maxMana;

        float x = MANA_UI_MARGIN_X;
        float y = MANA_UI_MARGIN_Y;

        shapeRenderer.setProjectionMatrix(
                new Matrix4().setToOrtho2D(
                        0, 0,
                        Gdx.graphics.getWidth(),
                        Gdx.graphics.getHeight()
                )
        );

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        // 背景
        shapeRenderer.setColor(0f, 0f, 0f, 0.6f);
        shapeRenderer.rect(x, y, MANA_BAR_WIDTH, MANA_BAR_HEIGHT);

        // Mana 填充
        shapeRenderer.setColor(0.2f, 0.5f, 1f, 0.9f);
        shapeRenderer.rect(
                x,
                y,
                MANA_BAR_WIDTH * percent,
                MANA_BAR_HEIGHT
        );

        shapeRenderer.end();

        // 边框
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        shapeRenderer.setColor(Color.WHITE);
        shapeRenderer.rect(x, y, MANA_BAR_WIDTH, MANA_BAR_HEIGHT);
        shapeRenderer.end();


    }


    private void renderDashIcon(SpriteBatch uiBatch) {

        DashAbility dash = null;
        for (Ability a : gameManager.getPlayer()
                .getAbilityManager()
                .getAbilities()
                .values()) {
            if (a instanceof DashAbility d) {
                dash = d;
                break;
            }
        }
        if (dash == null) return;

        int dashCharges = dash.getCurrentCharges();
        int maxCharges = dash.getMaxCharges();
        float progress = dash.getCooldownProgress();

        int startX = DASH_UI_MARGIN_X;
        int startY = DASH_UI_MARGIN_Y;


        // 从右往左画充能
        for (int i = 0; i < DASH_MAX_CHARGES; i++) {
            float x = startX + i * (DASH_ICON_SIZE + DASH_ICON_SPACING);
            float y = startY;

            boolean available = i < dashCharges;

            uiBatch.setColor(
                    available ? 1f : 0.3f,
                    available ? 1f : 0.3f,
                    available ? 1f : 0.3f,
                    1f
            );

            uiBatch.draw(dashIcon, x, y, DASH_ICON_SIZE, DASH_ICON_SIZE);

            // 冷却遮罩（当前充能槽）
            if (!available && i == dashCharges) {
                float maskHeight = DASH_ICON_SIZE * (1f - progress);

                uiBatch.setColor(0f, 0f, 0f, 0.6f);
                uiBatch.draw(
                        TextureManager.getInstance().getWhitePixel(),
                        x,
                        y,
                        DASH_ICON_SIZE,
                        maskHeight
                );
            }
        }

        uiBatch.setColor(1f, 1f, 1f, 1f);
    }


    private void renderLivesAsHearts(SpriteBatch uiBatch) {

        // 🔴 关键：UI 颜色必须重置
        uiBatch.setColor(1f, 1f, 1f, 1f);

        int lives = gameManager.getPlayer().getLives();

        /* ================= 抖动触发 ================= */
        if (lastLives != -1 && lives < lastLives) {
            int oldSlot = (lastLives - 1) / 10;
            int newSlot = (lives - 1) / 10;

            int oldInSlot = lastLives - oldSlot * 10;
            int newInSlot = lives - newSlot * 10;

            boolean wasFull = oldInSlot > 5;
            boolean nowHalf = newInSlot <= 5;

            if (oldSlot == newSlot && wasFull && nowHalf) {
                shaking = true;
                shakeTimer = 0f;
            }
        }
        lastLives = lives;

        float delta = Gdx.graphics.getDeltaTime();
        if (shaking) {
            shakeTimer += delta;
            if (shakeTimer >= SHAKE_DURATION) {
                shaking = false;
            }
        }

        /* ================= 心数计算（你的规则） ================= */
        int fullHearts = lives / 10;
        int remainder = lives % 10;

        boolean hasHalf = remainder > 0 && remainder <= 5;
        boolean hasExtraFull = remainder > 5;

        int totalHearts = fullHearts
                + (hasHalf ? 1 : 0)
                + (hasExtraFull ? 1 : 0);

        totalHearts = Math.min(totalHearts, MAX_HEARTS_DISPLAY);

        /* ================= 布局 ================= */
        int startX = 20;
        int startY = Gdx.graphics.getHeight() - 90;

        float shakeOffsetX =
                shaking ? (float) Math.sin(shakeTimer * 40f) * SHAKE_AMPLITUDE : 0f;

        int drawn = 0;

        /* ================= 画满心 ================= */
        for (int i = 0; i < fullHearts && drawn < totalHearts; i++) {
            int row = drawn / HEARTS_PER_ROW;
            int col = drawn % HEARTS_PER_ROW;

            boolean shakeThis =
                    shaking && i == fullHearts - 1 && !hasExtraFull;

            uiBatch.draw(
                    heartFull,
                    startX + col * HEART_SPACING + (shakeThis ? shakeOffsetX : 0f),
                    startY - row * ROW_SPACING
            );
            drawn++;
        }

        /* ================= 半心 ================= */
        if (hasHalf && drawn < totalHearts) {
            int row = drawn / HEARTS_PER_ROW;
            int col = drawn % HEARTS_PER_ROW;

            uiBatch.draw(
                    heartHalf,
                    startX + col * HEART_SPACING,
                    startY - row * ROW_SPACING
            );
            drawn++;
        }

        /* ================= 6–10 的补满心 ================= */
        if (hasExtraFull && drawn < totalHearts) {
            int row = drawn / HEARTS_PER_ROW;
            int col = drawn % HEARTS_PER_ROW;

            uiBatch.draw(
                    heartFull,
                    startX + col * HEART_SPACING,
                    startY - row * ROW_SPACING
            );
        }
    }






    /**
     * 渲染指南针（UI模式）
     */
    public void renderCompassAsUI() {
        if (gameManager == null || gameManager.getCompass() == null) return;

        Compass compass = gameManager.getCompass();
        if (!compass.isActive()) return;

        Matrix4 uiMatrix = new Matrix4()
                .setToOrtho2D(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

        uiBatchForCompass.setProjectionMatrix(uiMatrix);

        uiBatchForCompass.begin();
        System.out.println("print the compass");
        compass.drawAsUI(uiBatchForCompass);
        uiBatchForCompass.end();
    }



    /**
     * 渲染游戏结束画面
     */
    public void renderGameComplete(SpriteBatch batch) {
        String message = "恭喜！你成功逃出了迷宫！";
        font.getData().setScale(2);
        font.setColor(Color.GREEN);

        com.badlogic.gdx.graphics.g2d.GlyphLayout layout = new com.badlogic.gdx.graphics.g2d.GlyphLayout();
        layout.setText(font, message);

        float x = (Gdx.graphics.getWidth() - layout.width) / 2;
        float y = Gdx.graphics.getHeight() / 2;

        font.draw(batch, message, x, y);

        // 显示重新开始提示
        font.getData().setScale(1);
        font.setColor(Color.WHITE);
        String restartMsg = "按R键重新开始游戏";
        layout.setText(font, restartMsg);

        float restartX = (Gdx.graphics.getWidth() - layout.width) / 2;
        font.draw(batch, restartMsg, restartX, y - 50);
    }

    /**
     * 渲染游戏结束画面
     */
    public void renderGameOver(SpriteBatch batch) {
        String message = "游戏结束！";
        font.getData().setScale(2);
        font.setColor(Color.RED);

        com.badlogic.gdx.graphics.g2d.GlyphLayout layout = new com.badlogic.gdx.graphics.g2d.GlyphLayout();
        layout.setText(font, message);

        float x = (Gdx.graphics.getWidth() - layout.width) / 2;
        float y = Gdx.graphics.getHeight() / 2;

        font.draw(batch, message, x, y);

        // 显示重新开始提示
        font.getData().setScale(1);
        font.setColor(Color.WHITE);
        String restartMsg = "按R键重新开始游戏";
        layout.setText(font, restartMsg);

        float restartX = (Gdx.graphics.getWidth() - layout.width) / 2;
        font.draw(batch, restartMsg, restartX, y - 50);
    }

    public BitmapFont getFont() {
        return font;
    }

    public void dispose() {
        if (font != null) font.dispose();
        if (uiBatchForCompass != null) uiBatchForCompass.dispose();
        if (heartFull != null) heartFull.dispose();
        if (heartHalf != null) heartHalf.dispose();
        if (shapeRenderer != null) shapeRenderer.dispose();

        Logger.debug("HUD disposed");
    }
}
