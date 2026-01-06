package de.tum.cit.fop.maze.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.Align;
import de.tum.cit.fop.maze.entities.Player;
import de.tum.cit.fop.maze.game.GameConstants;

public class HUD {
    private final Player player;
    private final BitmapFont font;
    private final SpriteBatch uiBatch;
    private final Texture heartTexture;
    private final Texture keyTexture;
    private final Texture hudBg; // 假设有个背景条

    // 🔥 成就弹窗变量
    private String currentNotification = null;
    private float notificationTimer = 0f;
    private final float NOTIFICATION_DURATION = 3.0f;

    public HUD(SpriteBatch batch, Player player) {
        this.player = player;
        this.uiBatch = batch;
        this.font = new BitmapFont(Gdx.files.internal("ui/font.fnt"));
        this.font.setColor(Color.WHITE);

        this.heartTexture = new Texture("items/heart.png");
        this.keyTexture = new Texture("items/key.png");
        this.hudBg = new Texture("ui/hud_bg.png"); // 如果没有就注释掉
    }

    public void render(float delta) {
        // 1. 轮询通知
        if (currentNotification == null && player.hasNotifications()) {
            currentNotification = player.pollNotification();
            notificationTimer = NOTIFICATION_DURATION;
        }

        uiBatch.begin();

        // ... (原有的绘制生命值、分数逻辑) ...
        font.draw(uiBatch, "SCORE: " + player.getScore(), 20, Gdx.graphics.getHeight() - 20);

        // 绘制生命值
        for (int i = 0; i < player.getLives(); i++) {
            uiBatch.draw(heartTexture, 20 + i * 40, Gdx.graphics.getHeight() - 60, 32, 32);
        }

        // 🔥 绘制成就弹窗 (Banner)
        if (currentNotification != null) {
            notificationTimer -= delta;

            // 简单的滑入滑出效果
            float yOffset = 0;
            if (notificationTimer > NOTIFICATION_DURATION - 0.5f) { // Slide In
                yOffset = 100 * (1 - (NOTIFICATION_DURATION - notificationTimer) / 0.5f);
            } else if (notificationTimer < 0.5f) { // Slide Out
                yOffset = 100 * (1 - notificationTimer / 0.5f);
            }

            float drawY = Gdx.graphics.getHeight() - 100 + yOffset;
            float centerX = Gdx.graphics.getWidth() / 2f;

            // 画背景框 (可选)
            // uiBatch.draw(hudBg, centerX - 200, drawY - 40, 400, 80);

            // 画文字
            font.setColor(1f, 0.84f, 0f, 1f); // 金色
            font.getData().setScale(1.2f);
            // 居中绘制
            // GlypthLayout layout = new GlyphLayout(font, currentNotification);
            // font.draw(uiBatch, currentNotification, centerX - layout.width / 2, drawY);
            font.draw(uiBatch, currentNotification, centerX - 150, drawY); // 简化居中

            // 重置设置
            font.getData().setScale(1f);
            font.setColor(Color.WHITE);

            if (notificationTimer <= 0) {
                currentNotification = null;
            }
        }

        uiBatch.end();
    }

    public void dispose() {
        font.dispose();
        heartTexture.dispose();
        keyTexture.dispose();
        if (hudBg != null) hudBg.dispose();
    }
}