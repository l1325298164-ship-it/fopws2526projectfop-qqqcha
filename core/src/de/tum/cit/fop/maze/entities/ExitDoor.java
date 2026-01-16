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

    // 🔥 新增：解锁特效计时器
    private float unlockEffectTimer = 0f;
    private static final float UNLOCK_EFFECT_DURATION = 2f; // 解锁特效持续时间

    public ExitDoor(int x, int y, DoorDirection direction) {
        super(x, y);
        this.direction = direction;
        this.active = true;

        try {
            // 只加载四个方向的锁定门贴图
            lockedTextures.put(DoorDirection.UP,
                    new Texture(Gdx.files.internal("ani/Items/door_up_locked.png")));
            lockedTextures.put(DoorDirection.DOWN,
                    new Texture(Gdx.files.internal("ani/Items/door_down_locked.png")));
            lockedTextures.put(DoorDirection.LEFT,
                    new Texture(Gdx.files.internal("ani/Items/door_left_locked.png")));
            lockedTextures.put(DoorDirection.RIGHT,
                    new Texture(Gdx.files.internal("ani/Items/door_right_locked.png")));

            // 解锁门贴图（如果存在的话）
            unlockedTextures.put(DoorDirection.UP,
                    new Texture(Gdx.files.internal("ani/Items/door_up_locked.png")));
            unlockedTextures.put(DoorDirection.DOWN,
                    new Texture(Gdx.files.internal("ani/Items/door_down_locked.png")));
            unlockedTextures.put(DoorDirection.LEFT,
                    new Texture(Gdx.files.internal("ani/Items/door_left_locked.png")));
            unlockedTextures.put(DoorDirection.RIGHT,
                    new Texture(Gdx.files.internal("ani/Items/door_right_locked.png")));

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


    public boolean isLocked() {
        return locked;
    }

    public void unlock() {
        if (locked) {
            locked = false;
            unlockEffectTimer = 0f; // 🔥 重置解锁特效计时器
            Logger.gameEvent("Exit unlocked at " + getPositionString() + " (direction: " + direction + ")");
        }
    }

    public void update(float delta, GameManager gm) {
        portalEffect.update(delta);

        // 🔥 更新解锁特效计时器
        if (!locked && unlockEffectTimer < UNLOCK_EFFECT_DURATION) {
            unlockEffectTimer += delta;
        }
    }

    @Override
    public boolean isPassable() {
        // 🔥 修正：门永远不可"穿过"，但已解锁的门允许玩家站在上面触发传送
        // 实际通行性由 GameManager.canPlayerMoveTo() 决定
        return false;
    }

    public void onPlayerStep(Player player) {
        if (locked || triggered) return;

        triggered = true;
        portalEffect.startExitAnimation(
                (x + 0.5f) * GameConstants.CELL_SIZE,
                (y + 0.5f) * GameConstants.CELL_SIZE
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

        // 🔥 根据锁定状态设置不同的颜色效果
        if (locked) {
            // 锁定状态：颜色暗淡
            batch.setColor(0.7f, 0.7f, 0.7f, 1f);
        } else {
            // 解锁状态：颜色鲜艳 + 解锁特效
            if (unlockEffectTimer < UNLOCK_EFFECT_DURATION) {
                // 🔥 解锁特效：金色闪烁
                float alpha = (float) Math.sin(unlockEffectTimer * 10) * 0.3f + 0.7f;
                float goldR = 1.0f;
                float goldG = 0.8f;
                float goldB = 0.3f;
                batch.setColor(goldR, goldG, goldB, alpha);
            } else {
                // 解锁完成：正常亮色
                batch.setColor(1f, 1f, 1f, 1f);
            }
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

        // 🔥 恢复默认颜色
        batch.setColor(1f, 1f, 1f, 1f);
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
        unlockEffectTimer = 0f; // 🔥 重置解锁特效计时器
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

    // 🔥 新增：检查玩家是否在门附近（用于可能的未来扩展）
    public boolean isPlayerNearby(int playerX, int playerY, int range) {
        int dx = Math.abs(playerX - this.x);
        int dy = Math.abs(playerY - this.y);
        return dx <= range && dy <= range;
    }
}