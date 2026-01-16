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

        // 🔥 注意：如果你不想让每个宝箱都触发Relic逻辑（比如只在特定关卡触发），
        // 请把下面这行注释掉，或者加 if 判断。
        // player.requestChapter1RelicFromTreasure(this);

        // === 🎲 智能掉落逻辑 ===
        // 逻辑：只要玩家还没集齐3个Buff，就从缺少的Buff里随机给一个。
        // 等玩家集齐了，dropPool 就会变空，自然就走 else 给 20HP。

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
            // ✅ 情况 A: 玩家还没满Buff，随机给一个缺的
            int randomIndex = MathUtils.random(0, dropPool.size() - 1);
            int choice = dropPool.get(randomIndex);

            switch (choice) {
                case 0:
                    player.activateAttackBuff();
                    if (player.getGameManager() != null && player.getGameManager().getCombatEffectManager() != null) {
                        float tx = player.getWorldX() * GameConstants.CELL_SIZE;
                        float ty = player.getWorldY() * GameConstants.CELL_SIZE + 40;
                        player.getGameManager().getCombatEffectManager().spawnStatusText(tx, ty, "ATTACK UP", Color.RED);
                    }
                    Logger.gameEvent("宝箱奖励: 攻击力提升!");
                    break;
                case 1:
                    player.activateRegenBuff();
                    if (player.getGameManager() != null && player.getGameManager().getCombatEffectManager() != null) {
                        float tx = player.getWorldX() * GameConstants.CELL_SIZE;
                        float ty = player.getWorldY() * GameConstants.CELL_SIZE + 40;
                        player.getGameManager().getCombatEffectManager().spawnStatusText(tx, ty, "REGEN ON", Color.GREEN);
                    }
                    Logger.gameEvent("宝箱奖励: 自动回血!");
                    break;
                case 2:
                    player.activateManaBuff();
                    if (player.getGameManager() != null && player.getGameManager().getCombatEffectManager() != null) {
                        float tx = player.getWorldX() * GameConstants.CELL_SIZE;
                        float ty = player.getWorldY() * GameConstants.CELL_SIZE + 40;
                        player.getGameManager().getCombatEffectManager().spawnStatusText(tx, ty, "MANA UP", Color.CYAN);
                    }
                    Logger.gameEvent("宝箱奖励: 蓝耗减少!");
                    break;
            }
        } else {
            // ✅ 情况 B: 玩家Buff全满了，给保底奖励 (HP +20)

            player.heal(20);

            // 显示蓝色小字 POTION
            if (player.getGameManager() != null && player.getGameManager().getCombatEffectManager() != null) {
                player.getGameManager().getCombatEffectManager().spawnStatusText(
                        player.getWorldX() * GameConstants.CELL_SIZE,
                        player.getWorldY() * GameConstants.CELL_SIZE + 60,
                        "POTION +20",
                        Color.BLUE
                );
            }
            Logger.gameEvent("宝箱奖励: 生命药水");
        }
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
                // 确保你的路径是对的，如果有问题请检查 Assets 文件夹
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