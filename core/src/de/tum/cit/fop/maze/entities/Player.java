// Player.java - 更新版本
package de.tum.cit.fop.maze.entities;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import de.tum.cit.fop.maze.audio.AudioManager;
import de.tum.cit.fop.maze.audio.AudioType;
import de.tum.cit.fop.maze.game.GameConstants;
import de.tum.cit.fop.maze.utils.Logger;
import de.tum.cit.fop.maze.utils.TextureManager;

public class Player extends GameObject {
    private Color color = GameConstants.PLAYER_COLOR;
    private boolean hasKey = false;
    private int lives;
    private float invincibleTimer = 0;
    private boolean isInvincible = false;
    private boolean isDead = false;

    // 纹理管理
    private TextureManager textureManager;
    private Texture playerTexture;

    // 状态标识
    private boolean needsTextureUpdate = true;

    // 分数
    private int score = 0;

    public Player(int x, int y) {
        super(x, y);
        this.lives = GameConstants.INITIAL_PLAYER_LIVES;
        this.textureManager = TextureManager.getInstance();

        // 初始加载纹理
        updateTexture();

        Logger.gameEvent("Player spawned at " + getPositionString() + " with " + lives + " lives");
    }

    /**
     * 更新纹理
     */
    private void updateTexture() {
        playerTexture = textureManager.getPlayerTexture();
        needsTextureUpdate = false;
    }

    /**
     * 响应纹理模式切换
     */
    public void onTextureModeChanged() {
        needsTextureUpdate = true;
        Logger.debug("Player texture needs update due to mode change");
    }
    @Override
    public void drawShape(ShapeRenderer shapeRenderer) {
        if (!active || isDead || playerTexture != null) return;

        // 备用：使用颜色绘制
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        // 无敌状态闪烁效果
        if (isInvincible && invincibleTimer % 0.2f > 0.1f) {
            shapeRenderer.setColor(Color.WHITE);
        } else {
            shapeRenderer.setColor(color);
        }

        shapeRenderer.rect(
            x * GameConstants.CELL_SIZE + 2,
            y * GameConstants.CELL_SIZE + 2,
            GameConstants.CELL_SIZE - 4,
            GameConstants.CELL_SIZE - 4
        );
        shapeRenderer.end();
    }

    @Override
    public void drawSprite(SpriteBatch batch) {
        if (!active || isDead || playerTexture == null) return;

        // 如果需要更新纹理
        if (needsTextureUpdate) {
            updateTexture();
        }

        // 无敌状态闪烁效果
        if (isInvincible && invincibleTimer % 0.2f > 0.1f) {
            batch.setColor(1, 1, 1, 0.7f); // 半透明闪烁
        } else {
            batch.setColor(1, 1, 1, 1);
        }

        float posX = x * GameConstants.CELL_SIZE;
        float posY = y * GameConstants.CELL_SIZE;
        batch.draw(playerTexture, posX, posY,
            GameConstants.CELL_SIZE, GameConstants.CELL_SIZE);

        // 重置颜色
        batch.setColor(1, 1, 1, 1);
    }

    @Override
    public RenderType getRenderType() {
        // 如果当前模式是COLOR或没有纹理，使用SHAPE
        if (textureManager.getCurrentMode() == TextureManager.TextureMode.COLOR ||
            textureManager.getCurrentMode() == TextureManager.TextureMode.MINIMAL ||
            playerTexture == null) {
            return RenderType.SHAPE;
        }
        return RenderType.SPRITE;
    }

    public void update(float deltaTime) {
        if (isInvincible) {
            invincibleTimer += deltaTime;
            if (invincibleTimer >= GameConstants.INVINCIBLE_TIME) {
                isInvincible = false;
                invincibleTimer = 0;
                Logger.debug("Player invincibility ended");
            }
        }
    }

    public boolean hasKey() { return hasKey; }
    public void setHasKey(boolean hasKey) {
        this.hasKey = hasKey;
        if (hasKey) {
            Logger.gameEvent("Player obtained the key!");
        }
    }

    public void move(int dx, int dy) {
        if (isDead) return;
        this.x += dx;
        this.y += dy;
        Logger.debug("Player moved to " + getPositionString());
    }

    public void takeDamage(int damage) {
        if (isDead || isInvincible) return;

        lives -= damage;

        // 🔊 玩家受伤音效（只播一次）
        AudioManager.getInstance().play(AudioType.PLAYER_ATTACKED);
        isInvincible = true;
        invincibleTimer = 0;

        Logger.gameEvent("Player took " + damage + " damage, lives left: " + lives);

        if (lives <= 0) {
            isDead = true;
            Logger.gameEvent("Player died");
        }
    }

    public int getLives() {
        return lives;
    }

    public boolean isDead() {
        return lives <= 0;
    }

    // 获取分数
    public int getScore() {
        return score;
    }

    // 增加分数
    public void addScore(int points) {
        score += points;
        Logger.debug("Player score increased by " + points + ", total: " + score);
    }

    /**
     * 重置玩家状态
     */
    public void reset() {
        // 重置位置到初始位置（需要在GameManager中设置）
        // 这里只重置状态，位置由GameManager负责设置

        // 重置生命值
        this.lives = GameConstants.INITIAL_PLAYER_LIVES;

        // 重置钥匙状态
        this.hasKey = false;

        // 重置无敌状态
        this.isInvincible = false;
        this.invincibleTimer = 0;

        // 重置死亡状态
        this.isDead = false;

        // 重置分数
        this.score = 0;

        // 重置纹理状态
        this.needsTextureUpdate = true;
        updateTexture();

        Logger.debug("Player状态已重置: 生命=" + lives + ", 分数=" + score + ", 有钥匙=" + hasKey);
    }

    /**
     * 设置玩家位置（用于重置时的重新定位）
     */
    public void setPosition(int x, int y) {
        this.x = x;
        this.y = y;
        Logger.debug("Player位置设置为: " + getPositionString());
    }

    // 其他辅助方法
    public String getPositionString() {
        return "(" + x + ", " + y + ")";
    }
}