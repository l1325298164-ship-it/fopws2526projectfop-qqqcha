package de.tum.cit.fop.maze.entities;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import de.tum.cit.fop.maze.utils.Logger;

public class Compass {

    private final Player player;
    private ExitDoor nearestExit;

    private final Texture baseTexture;
    private final Texture needleTexture;

    private final Sprite baseSprite;
    private final Sprite needleSprite;

    private boolean active = true;   // ✅ 真正的 active 状态

    public Compass(Player player) {
        this.player = player;



        baseTexture = new Texture(Gdx.files.internal("ui/HUD/compass_base.png"));
        needleTexture = new Texture(Gdx.files.internal("ui/HUD/compass_needle.png"));

        baseSprite = new Sprite(baseTexture);
        needleSprite = new Sprite(needleTexture);

        // UI 大小（逻辑尺寸）
        baseSprite.setSize(120, 120);
        needleSprite.setSize(20, 60);

// 🔥 origin 一定都在中心
        baseSprite.setOriginCenter();
        needleSprite.setOriginCenter();

// 🔥 整体放大
        baseSprite.setScale(2.3f);
        needleSprite.setScale(2.3f);

    }

    /* ================= 状态 ================= */

    public void setActive(boolean active) {
        this.active = active;
    }

    public boolean isActive() {
        return active;
    }

    /* ================= 更新 ================= */

    public void update(ExitDoor exitDoor) {
        this.nearestExit = exitDoor;
    }

    /* ================= 渲染 ================= */

    public void drawAsUI(SpriteBatch batch) {
        float margin = 10f;

        float x = Gdx.graphics.getWidth()
                - getUIWidth()
                - margin - 60;

        float y = margin + 50;

        drawAsUIAt(batch, x, y);
    }


    public void dispose() {
        baseTexture.dispose();
        needleTexture.dispose();
    }
    public float getUIWidth() {
        return baseSprite.getWidth() * baseSprite.getScaleX();
    }

    public float getUIHeight() {
        return baseSprite.getHeight() * baseSprite.getScaleY();
    }

    public void drawAsUIAt(SpriteBatch batch, float x, float y) {
        if (!active || nearestExit == null) return;

        // ===== 方向计算（和原来完全一致）=====
        float dx = nearestExit.getX() - player.getX();
        float dy = nearestExit.getY() - player.getY();
        float angle =
                MathUtils.atan2(dy, dx) * MathUtils.radiansToDegrees - 90f;

        // ===== Base Shadow =====
        baseSprite.setPosition(x + 6f, y - 6f);
        baseSprite.setColor(0f, 0f, 0f, 0.25f);
        baseSprite.draw(batch);

        // ===== Base =====
        baseSprite.setPosition(x, y);
        baseSprite.setColor(1f, 1f, 1f, 1f);
        baseSprite.draw(batch);

        // ===== 正确中心点（你原来算的那套）=====
        float baseCenterX =
                x + baseSprite.getWidth() * baseSprite.getScaleX() / 2f;
        float baseCenterY =
                y + baseSprite.getHeight() * baseSprite.getScaleY() / 2f;

// 👉 魔法数字只作用在 needle
        float centerX = baseCenterX - 76f;
        float centerY = baseCenterY - 105f;

        // ===== Needle Shadow =====
        needleSprite.setCenter(centerX + 3f, centerY - 3f);
        needleSprite.setRotation(angle);
        needleSprite.setColor(0f, 0f, 0f, 0.35f);
        needleSprite.draw(batch);

        // ===== Needle =====
        needleSprite.setCenter(centerX, centerY);
        needleSprite.setRotation(angle);
        needleSprite.setColor(
                nearestExit.isLocked() ? Color.YELLOW : Color.GREEN
        );
        needleSprite.draw(batch);
    }


}
