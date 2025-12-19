package de.tum.cit.fop.maze.entities;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import de.tum.cit.fop.maze.utils.Logger;

public class Compass {

    private Player player;
    private ExitDoor nearestExit;

    private Texture baseTexture;
    private Texture needleTexture;

    private Sprite baseSprite;
    private Sprite needleSprite;

    public Compass(Player player) {
        this.player = player;

        baseTexture = new Texture(Gdx.files.internal("compass_cat_base.png"));
        needleTexture = new Texture(Gdx.files.internal("compass_needle.png"));

        baseSprite = new Sprite(baseTexture);
        needleSprite = new Sprite(needleTexture);

        // 设置大小（UI）
        baseSprite.setSize(120, 120);
        needleSprite.setSize(60, 60);

        // 指针以中心旋转
        needleSprite.setOriginCenter();

        Logger.debug("Cat Compass initialized");
    }

    public void update(ExitDoor exitDoor) {
        this.nearestExit = exitDoor;
    }

    public void drawAsUI(SpriteBatch batch) {
        if (nearestExit == null) return;

        // 屏幕右上角
        float x = Gdx.graphics.getWidth() - 140;
        float y = Gdx.graphics.getHeight() - 140;

        // 计算角度
        float dx = nearestExit.getX() - player.getX();
        float dy = nearestExit.getY() - player.getY();
        float angle = MathUtils.atan2(dy, dx) * MathUtils.radiansToDegrees-90;

        // 画底图（猫猫 + 表盘）
        baseSprite.setPosition(x, y);
        baseSprite.draw(batch);

        // 画指针（旋转）
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

    public boolean isActive() {
        return true;
    }
}
