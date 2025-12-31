package de.tum.cit.fop.maze.entities;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import de.tum.cit.fop.maze.effects.portal.PortalEffectManager;
import de.tum.cit.fop.maze.game.GameConstants;
import de.tum.cit.fop.maze.game.GameManager;
import de.tum.cit.fop.maze.utils.Logger;

public class ExitDoor extends GameObject {
    private final PortalEffectManager portalEffect = new PortalEffectManager();
    private Texture lockedTexture;
    private Texture unlockedTexture;
    private boolean locked = true;
    private boolean triggered = false;

    public ExitDoor(int x, int y, int index) {
        super(x, y);
        this.active = true;

        lockedTexture = new Texture(Gdx.files.internal("Items/locked-door.png"));
        unlockedTexture = new Texture(Gdx.files.internal("Items/door.png"));

        Logger.debug("ExitDoor created at " + getPositionString());
    }

    public boolean isLocked() {
        return locked;
    }

    public void unlock() {
        locked = false;
        Logger.gameEvent("Exit unlocked at " + getPositionString());
    }

    public void update(float delta, GameManager gm) {
        portalEffect.update(delta);

        // 🔥 关键：不要在 update 中调用 gm.nextLevel()
        // 让 GameManager 控制重置时机
    }

    @Override
    public boolean isPassable() {
        return !locked;
    }

    public void onPlayerStep(Player player) {
        if (locked || triggered) return;

        triggered = true;
        portalEffect.startExitAnimation(
                x * GameConstants.CELL_SIZE,
                y * GameConstants.CELL_SIZE
        );
    }

    @Override
    public boolean isInteractable() {
        return false;
    }

    @Override
    public void onInteract(Player player) {
        // 不用
    }

    @Override
    public void drawSprite(SpriteBatch batch) {
        float px = x * GameConstants.CELL_SIZE;
        float py = y * GameConstants.CELL_SIZE;

        // 门后呼吸灯
        portalEffect.renderBack(batch, px, py);

        Texture tex = locked ? lockedTexture : unlockedTexture;
        if (tex == null) return;

        // 门体 + 悬浮
        batch.draw(
                tex,
                px,
                py + portalEffect.getDoorFloatOffset(),
                GameConstants.CELL_SIZE,
                GameConstants.CELL_SIZE * 1.5f
        );
    }

    @Override
    public void drawShape(ShapeRenderer shapeRenderer) {
        // 不需要 shape
    }

    @Override
    public RenderType getRenderType() {
        return RenderType.SPRITE;
    }

    public void renderPortalFront(SpriteBatch batch) {
        portalEffect.renderFront(batch);
    }

    // 🔥 新增：检查动画是否正在播放
    public boolean isAnimationPlaying() {
        return portalEffect.isActive();
    }

    // 🔥 新增：重置门状态
    public void resetDoor() {
        triggered = false;
        locked = true; // 重置为锁定状态
        portalEffect.reset(); // 重置特效
    }

    public void dispose() {
        if (lockedTexture != null) lockedTexture.dispose();
        if (unlockedTexture != null) unlockedTexture.dispose();
        portalEffect.dispose();
    }

    // ===== 给 GameScreen 用的简化版本 =====
    public void renderPortalBack(SpriteBatch batch) {
        portalEffect.renderBack(
                batch,
                x * GameConstants.CELL_SIZE,
                y * GameConstants.CELL_SIZE
        );
    }
}