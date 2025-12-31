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

        baseTexture = new Texture(Gdx.files.internal("compass_cat_base.png"));
        needleTexture = new Texture(Gdx.files.internal("compass_needle.png"));

        baseSprite = new Sprite(baseTexture);
        needleSprite = new Sprite(needleTexture);

        // UI 大小
        baseSprite.setSize(120, 120);
        needleSprite.setSize(60, 60);

        needleSprite.setOriginCenter();

        Logger.debug("Compass initialized");
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
        if (!active || nearestExit == null) return;

        float x = Gdx.graphics.getWidth() - 140;
        float y = Gdx.graphics.getHeight() - 140;

        float dx = nearestExit.getX() - player.getX();
        float dy = nearestExit.getY() - player.getY();
        float angle =
                MathUtils.atan2(dy, dx) * MathUtils.radiansToDegrees - 90f;

        baseSprite.setPosition(x, y);
        baseSprite.draw(batch);

        needleSprite.setPosition(
                x + baseSprite.getWidth() / 2f - needleSprite.getWidth() / 2f,
                y + baseSprite.getHeight() / 2f - needleSprite.getHeight() / 2f
        );
        needleSprite.setRotation(angle);
        needleSprite.setColor(
                nearestExit.isLocked() ? Color.YELLOW : Color.GREEN
        );

        needleSprite.draw(batch);
    }

    public void dispose() {
        baseTexture.dispose();
        needleTexture.dispose();
    }
}
