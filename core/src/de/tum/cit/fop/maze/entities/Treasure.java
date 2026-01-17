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

        // 暂时注释掉剧情逻辑
        // player.requestChapter1RelicFromTreasure(this);

        // === 🎲 智能掉落逻辑 ===
        List<Integer> dropPool = new ArrayList<>();

        if (!player.hasBuffAttack()) dropPool.add(0);
        if (!player.hasBuffRegen()) dropPool.add(1);
        if (!player.hasBuffManaEfficiency()) dropPool.add(2);

        float tx = player.getWorldX() * GameConstants.CELL_SIZE;
        float ty = player.getWorldY() * GameConstants.CELL_SIZE + 40;

        // --- 抽取奖励 ---
        if (!dropPool.isEmpty()) {
            int randomIndex = MathUtils.random(0, dropPool.size() - 1);
            int choice = dropPool.get(randomIndex);

            switch (choice) {
                case 0:
                    player.activateAttackBuff();
                    Logger.gameEvent("宝箱掉落: 攻击 Buff");
                    spawnFloatingText(player, "ATTACK UP", Color.RED, tx, ty);
                    break;
                case 1:
                    player.activateRegenBuff();
                    Logger.gameEvent("宝箱掉落: 回血 Buff");
                    spawnFloatingText(player, "REGEN ON", Color.GREEN, tx, ty);
                    break;
                case 2:
                    player.activateManaBuff();
                    Logger.gameEvent("宝箱掉落: 蓝耗减少 Buff");
                    spawnFloatingText(player, "MANA UP", Color.CYAN, tx, ty);
                    break;
            }
        } else {
            // 🔥 修改点：Buff 满后，掉落 HP +20
            player.heal(20);

            Logger.gameEvent("宝箱掉落: 生命药水 (Buff已满)");

            // 把 "POTION" 改成了 "HP +20"，颜色设为绿色 (Color.GREEN)
            spawnFloatingText(player, "HP +20", Color.GREEN, tx, ty);
        }
    }

    private void spawnFloatingText(Player player, String text, Color color, float x, float y) {
        if (player.getGameManager() != null && player.getGameManager().getCombatEffectManager() != null) {
            player.getGameManager().getCombatEffectManager().spawnStatusText(x, y, text, color);
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

    private void updateTexture() {
        if (closedTexture == null || openTexture == null) {
            try {
                closedTexture = new Texture(Gdx.files.internal("imgs/Items/chest_closed.png"));
                openTexture = new Texture(Gdx.files.internal("imgs/Items/chest_open.png"));
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
            batch.draw(currentTexture, x * GameConstants.CELL_SIZE, y * GameConstants.CELL_SIZE, GameConstants.CELL_SIZE, GameConstants.CELL_SIZE);
        }
    }

    @Override
    public void drawShape(ShapeRenderer shapeRenderer) {
        if (closedTexture != null) return;
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(isOpened ? Color.GRAY : Color.GOLD);
        shapeRenderer.rect(x * GameConstants.CELL_SIZE + 4, y * GameConstants.CELL_SIZE + 4, GameConstants.CELL_SIZE - 8, GameConstants.CELL_SIZE - 8);
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