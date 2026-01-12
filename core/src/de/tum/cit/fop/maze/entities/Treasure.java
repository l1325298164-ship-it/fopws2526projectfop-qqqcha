package de.tum.cit.fop.maze.entities;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import de.tum.cit.fop.maze.game.GameConstants;
import de.tum.cit.fop.maze.utils.Logger;
import de.tum.cit.fop.maze.utils.TextureManager;

import java.util.ArrayList;
import java.util.List;

/**
 * 宝箱实体
 */
public class Treasure extends GameObject {

    private boolean isOpened = false;
    private Texture closedTexture;
    private Texture openTexture;
    private TextureManager textureManager;
    private boolean needsTextureUpdate = true;

    // 默认颜色
    private Color color = Color.GOLD;

    public Treasure(int x, int y) {
        super(x, y);
        this.textureManager = TextureManager.getInstance();
        updateTexture();
        Logger.debug("Treasure chest created at " + getPositionString());
    }

    @Override
    public void onInteract(Player player) {
        if (!isOpened) {
            open(player);
        }
    }

    private void open(Player player) {
        isOpened = true;

        // === 🎲 智能掉落逻辑 ===
        // 只掉落玩家还没有的 Buff

        List<Integer> dropPool = new ArrayList<>();

        // 0. 检查是否已有 攻击 Buff
        if (!player.hasBuffAttack()) {
            dropPool.add(0);
        }

        // 1. 检查是否已有 回血 Buff
        if (!player.hasBuffRegen()) {
            dropPool.add(1);
        }

        // 2. 检查是否已有 蓝耗减半 Buff
        if (!player.hasBuffManaEfficiency()) {
            dropPool.add(2);
        }

        // --- 抽取奖励 ---
        if (!dropPool.isEmpty()) {
            int randomIndex = MathUtils.random(0, dropPool.size() - 1);
            int choice = dropPool.get(randomIndex);

            switch (choice) {
                case 0:
                    // 对应：本关攻击力加 50%
                    player.activateAttackBuff();
                    break;
                case 1:
                    // 对应：本关每五秒自动回复五点 HP
                    player.activateRegenBuff();
                    break;
                case 2:
                    // 对应：本关内降低蓝耗 (50%)
                    player.activateManaBuff();
                    break;
            }
        } else {
            // 保底奖励 (如果全齐了)
            // 1. 回血 (自动飘绿色 +HP)
            player.heal(20);

            // 2. 🔥 修复：显示蓝色小字 POTION，代替原来的黄色乱码通知
            if (player.getGameManager() != null && player.getGameManager().getCombatEffectManager() != null) {
                player.getGameManager().getCombatEffectManager().spawnStatusText(
                        player.getWorldX() * GameConstants.CELL_SIZE,
                        player.getWorldY() * GameConstants.CELL_SIZE + 60, // 稍微高一点
                        "POTION",
                        Color.BLUE
                );
            }
        }

        Logger.gameEvent("宝箱打开了！获得了增幅！");
    }

    @Override
    public boolean isInteractable() {
        return !isOpened;
    }

    @Override
    public boolean isPassable() {
        return true;
    }

    // ================= 纹理与渲染 =================

    private void updateTexture() {
        if (closedTexture == null || openTexture == null) {
            try {
                closedTexture = new Texture(Gdx.files.internal("Items/chest_closed.png"));
                openTexture = new Texture(Gdx.files.internal("Items/chest_open.png"));
            } catch (Exception e) {
                Logger.error("Failed to load treasure textures: " + e.getMessage());
            }
        }
        needsTextureUpdate = false;
    }

    @Override
    public void onTextureModeChanged() {
        needsTextureUpdate = true;
    }

    @Override
    public void drawSprite(SpriteBatch batch) {
        if (needsTextureUpdate) updateTexture();

        Texture currentTexture = isOpened ? openTexture : closedTexture;

        if (currentTexture != null) {
            batch.draw(currentTexture,
                    x * GameConstants.CELL_SIZE,
                    y * GameConstants.CELL_SIZE,
                    GameConstants.CELL_SIZE,
                    GameConstants.CELL_SIZE
            );
        }
    }

    @Override
    public void drawShape(ShapeRenderer shapeRenderer) {
        if (closedTexture != null) return;

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(isOpened ? Color.GRAY : Color.GOLD);
        shapeRenderer.rect(
                x * GameConstants.CELL_SIZE + 4,
                y * GameConstants.CELL_SIZE + 4,
                GameConstants.CELL_SIZE - 8,
                GameConstants.CELL_SIZE - 8
        );
        shapeRenderer.end();
    }

    @Override
    public RenderType getRenderType() {
        if (textureManager.getCurrentMode() == TextureManager.TextureMode.COLOR ||
                textureManager.getCurrentMode() == TextureManager.TextureMode.MINIMAL ||
                closedTexture == null) {
            return RenderType.SHAPE;
        }
        return RenderType.SPRITE;
    }

    public void dispose() {
        if (closedTexture != null) closedTexture.dispose();
        if (openTexture != null) openTexture.dispose();
    }
}