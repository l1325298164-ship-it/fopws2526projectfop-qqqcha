// Player.java - 更新版本
package de.tum.cit.fop.maze.entities;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
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
//朝向
    public enum Direction {
        UP, DOWN, LEFT, RIGHT
    }
    private Direction direction = Direction.DOWN;
//动画调整
    float cellX = x * GameConstants.CELL_SIZE;
    float cellY = y * GameConstants.CELL_SIZE;

    float cellCenterX = cellX + GameConstants.CELL_SIZE / 2f;
    float footY = cellY; // 脚踩在格子底边
    float drawWidth;
    float drawHeight; // = CELL_SIZE
    float drawX = cellCenterX - drawWidth / 2f;
    float drawY = footY;




    //ani相关
    private TextureAtlas frontAtlas, backAtlas, leftAtlas, rightAtlas;
    private Animation<TextureRegion> frontAnim, backAnim, leftAnim, rightAnim;

    private float stateTime = 0f;
    private boolean isMoving = false;



    //效果
    private float slowTimer = 0f;
    private boolean slowed = false;


    // 分数
    private int score = 0;

    public Player(int x, int y) {
        super(x, y);
        this.lives = GameConstants.INITIAL_PLAYER_LIVES;

        frontAtlas = new TextureAtlas("player/front.atlas");
        backAtlas  = new TextureAtlas("player/back.atlas");
        leftAtlas  = new TextureAtlas("player/left.atlas");
        rightAtlas = new TextureAtlas("player/right.atlas");
//帧率自己调整
        frontAnim = new Animation<>(0.4f, frontAtlas.getRegions(), Animation.PlayMode.LOOP);
        backAnim  = new Animation<>(0.4f, backAtlas.getRegions(), Animation.PlayMode.LOOP);
        leftAnim  = new Animation<>(0.4f, leftAtlas.getRegions(), Animation.PlayMode.LOOP);
        rightAnim = new Animation<>(0.4f, rightAtlas.getRegions(), Animation.PlayMode.LOOP);

        Logger.gameEvent("Player spawned at " + getPositionString());
    }

    @Override
    public void drawShape(ShapeRenderer shapeRenderer) {

    }

    /**
     * 更新纹理
     */


    /**
     * 响应纹理模式切换,已停用
     */
//    @Override
//    public void drawShape(ShapeRenderer shapeRenderer) {
//        if (!active || isDead || playerTexture != null) return;
//
//        // 备用：使用颜色绘制
//        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
//
//        // 无敌状态闪烁效果
//        if (isInvincible && invincibleTimer % 0.2f > 0.1f) {
//            shapeRenderer.setColor(Color.WHITE);
//        } else {
//            shapeRenderer.setColor(color);
//        }
//
//        shapeRenderer.rect(
//            x * GameConstants.CELL_SIZE + 2,
//            y * GameConstants.CELL_SIZE + 2,
//            GameConstants.CELL_SIZE - 4,
//            GameConstants.CELL_SIZE - 4
//        );
//        shapeRenderer.end();
//    }

    @Override
    public void drawSprite(SpriteBatch batch) {
        if (!active || isDead) return;

        Animation<TextureRegion> currentAnim;

        switch (direction) {
            case UP:    currentAnim = backAnim; break;
            case LEFT:  currentAnim = leftAnim; break;
            case RIGHT: currentAnim = rightAnim; break;
            case DOWN:
            default:    currentAnim = frontAnim; break;
        }

        TextureRegion frame = currentAnim.getKeyFrame(stateTime, true);
// === 缩放：高度占一格，宽度按比例 ===
        float scale = (float) GameConstants.CELL_SIZE / frame.getRegionHeight();
        float drawWidth  = frame.getRegionWidth() * scale;
        float drawHeight = GameConstants.CELL_SIZE;

// === 脚底对齐 ===
        float cellX = x * GameConstants.CELL_SIZE;
        float cellY = y * GameConstants.CELL_SIZE;

        float drawX = cellX + GameConstants.CELL_SIZE / 2f - drawWidth / 2f;
        float drawY = cellY;

// === 无敌闪烁 ===
        if (isInvincible && invincibleTimer % 0.2f > 0.1f) {
            batch.setColor(1, 1, 1, 0.7f);
        } else {
            batch.setColor(1, 1, 1, 1f);
        }

        batch.draw(
                frame,
                drawX,
                drawY,
                drawWidth,
                drawHeight
        );

        batch.setColor(1, 1, 1, 1f);
    }

    @Override
    public RenderType getRenderType() {
        return RenderType.SPRITE;
    }


    public void update(float deltaTime) {

        // ===== 动画时间（与移动速度同步）=====
        float animationSpeed = 1f / getMoveDelayMultiplier();
        stateTime += deltaTime * animationSpeed;

        if (!isMoving) {
            stateTime = 0f;
        }
        isMoving = false;

        // ===== 无敌 =====
        if (isInvincible) {
            invincibleTimer += deltaTime;
            if (invincibleTimer >= GameConstants.INVINCIBLE_TIME) {
                isInvincible = false;
                invincibleTimer = 0;
            }
        }

        // ===== 减速 =====
        if (slowed) {
            slowTimer -= deltaTime;
            if (slowTimer <= 0f) {
                slowed = false;
                slowTimer = 0f;
            }
        }
    }
    //减速倍率
    public float getMoveDelayMultiplier() {
        return slowed ? 2.0f : 1.0f;
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

        if (dx > 0) direction = Direction.RIGHT;
        else if (dx < 0) direction = Direction.LEFT;
        else if (dy > 0) direction = Direction.UP;
        else if (dy < 0) direction = Direction.DOWN;

        isMoving = true;

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
        //避免极端情况下「重开关卡还在减速」。
        this.slowed = false;
        this.slowTimer = 0f;
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

    public void applySlow(float slowDuration) {
        // 不可叠加：只刷新持续时间
        slowed = true;
        slowTimer = Math.max(slowTimer, slowDuration);

        Logger.debug("Player slowed for " + slowTimer + " seconds");
    }


}