// HUD.java - 更新版本
package de.tum.cit.fop.maze.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Matrix4;
import de.tum.cit.fop.maze.entities.Compass;
import de.tum.cit.fop.maze.game.GameConstants;
import de.tum.cit.fop.maze.game.GameManager;
import de.tum.cit.fop.maze.utils.Logger;
import de.tum.cit.fop.maze.utils.TextureManager;

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


    public HUD(GameManager gameManager) {
        this.gameManager = gameManager;
        this.font = new BitmapFont();
        this.font.getData().setScale(1.2f);
        this.textureManager = TextureManager.getInstance();
        Logger.debug("HUD initialized with compass support");
        this.uiBatchForCompass = new SpriteBatch();
// TODO: 替换成你的贴图真实路径
        heartFull = new Texture("HUD/live_000.png");
        heartHalf = new Texture("HUD/live_001.png");

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

        } catch (Exception e) {
            Logger.error("Error rendering in-game UI", e);
        }
    }
    private void renderLivesAsHearts(SpriteBatch uiBatch) {
        int lives = gameManager.getPlayer().getLives();

        /* ---------- 1. 状态检测：是否触发抖动 ---------- */
        if (lastLives != -1) {
            boolean wasFull = lastLives % 10 == 0;
            boolean isHalfNow = lives % 10 > 0 && lives % 10 <= 5;

            if (wasFull && isHalfNow) {
                shaking = true;
                shakeTimer = 0f;
            }
        }
        lastLives = lives;

        /* ---------- 2. 更新时间 ---------- */
        float delta = Gdx.graphics.getDeltaTime();
        if (shaking) {
            shakeTimer += delta;
            if (shakeTimer >= SHAKE_DURATION) {
                shaking = false;
            }
        }

        /* ---------- 3. 生命值 → 爱心数量 ---------- */
        int fullHearts = lives / 10;
        int remainder = lives % 10;

        boolean drawHalf = false;
        boolean addExtraFull = false;

        if (remainder > 0 && remainder <= 5) {
            drawHalf = true;
        } else if (remainder > 5) {
            addExtraFull = true;
        }

        int totalHearts = fullHearts
                + (drawHalf ? 1 : 0)
                + (addExtraFull ? 1 : 0);

        totalHearts = Math.min(totalHearts, MAX_HEARTS_DISPLAY);

        /* ---------- 4. 布局参数 ---------- */
        int startX = 20;
        int startY = Gdx.graphics.getHeight() - 90;

        int drawnHearts = 0;

        float shakeOffsetX = 0f;
        if (shaking) {
            shakeOffsetX = (float) Math.sin(shakeTimer * 40f) * SHAKE_AMPLITUDE;
        }

        /* ---------- 5. 画完整爱心（最后一颗可抖动） ---------- */
        for (int i = 0; i < fullHearts && drawnHearts < totalHearts; i++) {
            int row = drawnHearts / HEARTS_PER_ROW;
            int col = drawnHearts % HEARTS_PER_ROW;

            boolean isShakingHeart =
                    shaking && (i == fullHearts - 1);

            float offsetX = isShakingHeart ? shakeOffsetX : 0f;

            uiBatch.draw(
                    heartFull,
                    startX + col * HEART_SPACING + offsetX,
                    startY - row * ROW_SPACING
            );
            drawnHearts++;
        }

        /* ---------- 6. 半颗爱心（抖动结束后才出现） ---------- */
        if (drawHalf && !shaking && drawnHearts < totalHearts) {
            int row = drawnHearts / HEARTS_PER_ROW;
            int col = drawnHearts % HEARTS_PER_ROW;

            uiBatch.draw(
                    heartHalf,
                    startX + col * HEART_SPACING,
                    startY - row * ROW_SPACING
            );
            drawnHearts++;
        }

        /* ---------- 7. 向上取补的完整爱心 ---------- */
        if (addExtraFull && drawnHearts < totalHearts) {
            int row = drawnHearts / HEARTS_PER_ROW;
            int col = drawnHearts % HEARTS_PER_ROW;

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

        Logger.debug("HUD disposed");
    }
}
