package de.tum.cit.fop.maze.entities;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import de.tum.cit.fop.maze.effects.portal.PortalEffectManager;
import de.tum.cit.fop.maze.game.GameConstants;
import de.tum.cit.fop.maze.game.GameManager;
import de.tum.cit.fop.maze.utils.Logger;

import java.util.EnumMap;

public class ExitDoor extends GameObject {

    // ===== 门方向枚举 =====
    public enum DoorDirection {
        UP, DOWN, LEFT, RIGHT
    }

    private final PortalEffectManager portalEffect = new PortalEffectManager();

    // ===== 四个方向的贴图 =====
    private final EnumMap<DoorDirection, Texture> lockedTextures = new EnumMap<>(DoorDirection.class);
    private final EnumMap<DoorDirection, Texture> unlockedTextures = new EnumMap<>(DoorDirection.class);

    // ===== 门状态 =====
    private final DoorDirection direction;
    private boolean locked = true;
    private boolean triggered = false;

    public ExitDoor(int x, int y, DoorDirection direction) {
        super(x, y);
        this.direction = direction;
        this.active = true;

        try {
            // 只加载四个方向的锁定门贴图
            lockedTextures.put(DoorDirection.UP,
                    new Texture(Gdx.files.internal("Items/door_up_locked.png")));
            lockedTextures.put(DoorDirection.DOWN,
                    new Texture(Gdx.files.internal("Items/door_down_locked.png")));
            lockedTextures.put(DoorDirection.LEFT,
                    new Texture(Gdx.files.internal("Items/door_left_locked.png")));
            lockedTextures.put(DoorDirection.RIGHT,
                    new Texture(Gdx.files.internal("Items/door_right_locked.png")));

            // 解锁门贴图（如果存在的话）
            unlockedTextures.put(DoorDirection.UP,
                    new Texture(Gdx.files.internal("Items/door_up_unlocked.png")));
            unlockedTextures.put(DoorDirection.DOWN,
                    new Texture(Gdx.files.internal("Items/door_down_unlocked.png")));
            unlockedTextures.put(DoorDirection.LEFT,
                    new Texture(Gdx.files.internal("Items/door_left_unlocked.png")));
            unlockedTextures.put(DoorDirection.RIGHT,
                    new Texture(Gdx.files.internal("Items/door_right_unlocked.png")));

            Logger.debug("ExitDoor created at (" + x + ", " + y + ") facing " + direction);
        } catch (Exception e) {
            Logger.error("Failed to load door textures: " + e.getMessage());
            // 如果解锁门贴图不存在，使用锁定门贴图作为fallback
            for (DoorDirection dir : DoorDirection.values()) {
                Texture lockedTex = lockedTextures.get(dir);
                if (lockedTex != null) {
                    unlockedTextures.put(dir, lockedTex);
                }
            }
        }
    }

//    // 🔥 重载：兼容旧代码的构造函数（默认向上）
//    public ExitDoor(int x, int y, int index) {
//        this(x, y, DoorDirection.UP);
//    }

    public DoorDirection getDirection() {
        return direction;
    }

    public boolean isLocked() {
        return locked;
    }

    public void unlock() {
        locked = false;
        Logger.gameEvent("Exit unlocked at " + getPositionString() + " (direction: " + direction + ")");
    }

    public void update(float delta, GameManager gm) {
        portalEffect.update(delta);
    }

    @Override
    public boolean isPassable() {
        return locked;
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

        // ===== 根据方向和锁状态选择贴图 =====
        Texture tex;
        if (locked) {
            tex = lockedTextures.get(direction);
        } else {
            tex = unlockedTextures.get(direction);
        }

        if (tex == null) {
            // 如果找不到贴图，使用默认
            Logger.warning("Texture not found for door direction: " + direction + ", locked: " + locked);
            return;
        }

        // 门体 + 悬浮效果
        float drawWidth = GameConstants.CELL_SIZE;
        float drawHeight = GameConstants.CELL_SIZE * 1.5f;

        // 🔥 根据不同方向调整绘制位置
        float offsetX = 0;
        float offsetY = portalEffect.getDoorFloatOffset();

        // 可以根据方向微调位置
        switch (direction) {
            case UP:
                // 向上门，正常绘制
                break;
            case DOWN:
                // 向下的门可能需要稍微调整位置
                offsetY -= GameConstants.CELL_SIZE * 0.5f;
                break;
            case LEFT:
                // 向左的门，旋转或调整位置
                offsetX = -GameConstants.CELL_SIZE * 0.25f;
                break;
            case RIGHT:
                // 向右的门
                offsetX = GameConstants.CELL_SIZE * 0.25f;
                break;
        }

        batch.draw(
                tex,
                px + offsetX,
                py + offsetY,
                drawWidth,
                drawHeight
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

    public boolean isAnimationPlaying() {
        return portalEffect.isActive();
    }

    public void resetDoor() {
        triggered = false;
        locked = true; // 重置为锁定状态
        portalEffect.reset(); // 重置特效
    }

    public void dispose() {
        // 释放所有贴图资源
        for (Texture tex : lockedTextures.values()) {
            if (tex != null) tex.dispose();
        }
        for (Texture tex : unlockedTextures.values()) {
            if (tex != null) tex.dispose();
        }
        portalEffect.dispose();
    }

    public void renderPortalBack(SpriteBatch batch) {
        portalEffect.renderBack(
                batch,
                x * GameConstants.CELL_SIZE,
                y * GameConstants.CELL_SIZE
        );
    }

    // 🔥 新增：获取门位置字符串（包含方向信息）
    @Override
    public String getPositionString() {
        return "(" + x + ", " + y + ", " + direction + ")";
    }
}