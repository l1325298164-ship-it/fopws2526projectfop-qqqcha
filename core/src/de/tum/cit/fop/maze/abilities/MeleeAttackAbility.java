// MeleeAttackAbility.java - 完整更新版本
package de.tum.cit.fop.maze.abilities;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import de.tum.cit.fop.maze.audio.AudioManager;
import de.tum.cit.fop.maze.audio.AudioType;
import de.tum.cit.fop.maze.entities.enemy.Enemy;
import de.tum.cit.fop.maze.entities.Player;
import de.tum.cit.fop.maze.entities.enemy.EnemyBoba.EnemyCorruptedBoba;
import de.tum.cit.fop.maze.entities.enemy.EnemyE03_CaramelJuggernaut;
import de.tum.cit.fop.maze.game.GameConstants;
import de.tum.cit.fop.maze.game.GameManager;
import de.tum.cit.fop.maze.utils.Logger;

import java.util.ArrayList;
import java.util.List;

public class MeleeAttackAbility extends Ability {

    // 攻击属性
    private int baseDamage = 2; // 基础伤害
    private float attackRange = 1.0f;
    private float swingTime = 0.2f;
    private float currentSwingTime = 0;

    // 攻击动画
    private TextureAtlas attackAtlas;
    private Animation<TextureRegion> attackAnimation;
    private boolean isAttacking = false;

    // 攻击效果区域
    private List<int[]> attackTiles = new ArrayList<>();

    // 视觉效果
    private Color attackColor = new Color(1, 0.2f, 0.2f, 0.3f);

    // 升级数据
    private int upgradeDamage = 1;
    private float upgradeRange = 0.5f;

    public MeleeAttackAbility() {
        super("Sword Slash", "Basic melee attack in front and adjacent tiles",
                0.5f, 0.3f);
        this.cooldown = 0.8f;
        this.manaCost = 10;

        loadAnimations();
    }

    private void loadAnimations() {
        try {
            // 尝试加载攻击动画
            attackAtlas = new TextureAtlas(Gdx.files.internal("abilities/sword_swing.atlas"));
            attackAnimation = new Animation<>(0.05f, attackAtlas.getRegions(),
                    Animation.PlayMode.NORMAL);
            Logger.debug("Sword swing animation loaded successfully");
        } catch (Exception e) {
            Logger.error("Failed to load sword animation: " + e.getMessage());
            // 创建备用纹理
            Texture fallback = new Texture(Gdx.files.internal("abilities/fallback.png"));
            if (fallback.getTextureData().isPrepared()) {
                TextureRegion[] regions = new TextureRegion[1];
                regions[0] = new TextureRegion(fallback);
                attackAnimation = new Animation<>(0.1f, regions);
                Logger.debug("Using fallback texture for sword attack");
            } else {
                // 如果备用纹理也失败，创建一个纯色纹理
                Logger.debug("Using colored rectangle for sword attack");
            }
        }
    }

    @Override
    public void activate(Player player, GameManager gameManager) {
        if (!canActivate(player) || isAttacking) return;

        isActive = true;
        isReady = false;
        isAttacking = true;
        currentSwingTime = 0;
        currentDuration = duration;
        currentCooldown = cooldown;

        // 扣除魔法值
        player.useMana(manaCost);

        // 播放音效TODO
        AudioManager.getInstance().play(AudioType.SWORD_SWING);

        // 计算攻击区域
        calculateAttackArea(player);

        // 对区域内的敌人造成伤害
        performAttack(gameManager);

        Logger.debug("Player used melee attack (Level " + level + ")");
    }

    private void calculateAttackArea(Player player) {
        attackTiles.clear();

        int playerX = player.getX();
        int playerY = player.getY();
        Player.Direction dir = player.getDirection();

        // 总是包含玩家自己的格子（防止敌人站在玩家格子上）
        attackTiles.add(new int[]{playerX, playerY});

        // 根据方向添加攻击格子
        switch (dir) {
            case UP:
                // 前方三格
                attackTiles.add(new int[]{playerX, playerY + 1});
                attackTiles.add(new int[]{playerX - 1, playerY + 1}); // 左上
                attackTiles.add(new int[]{playerX + 1, playerY + 1}); // 右上
                break;
            case DOWN:
                attackTiles.add(new int[]{playerX, playerY - 1});
                attackTiles.add(new int[]{playerX - 1, playerY - 1});
                attackTiles.add(new int[]{playerX + 1, playerY - 1});
                break;
            case LEFT:
                attackTiles.add(new int[]{playerX - 1, playerY});
                attackTiles.add(new int[]{playerX - 1, playerY - 1});
                attackTiles.add(new int[]{playerX - 1, playerY + 1});
                break;
            case RIGHT:
                attackTiles.add(new int[]{playerX + 1, playerY});
                attackTiles.add(new int[]{playerX + 1, playerY - 1});
                attackTiles.add(new int[]{playerX + 1, playerY + 1});
                break;
        }

        // 升级后增加范围
        if (level >= 3) {
            switch (dir) {
                case UP:
                    attackTiles.add(new int[]{playerX, playerY + 2});
                    if (level >= 5) {
                        attackTiles.add(new int[]{playerX - 1, playerY + 2});
                        attackTiles.add(new int[]{playerX + 1, playerY + 2});
                    }
                    break;
                case DOWN:
                    attackTiles.add(new int[]{playerX, playerY - 2});
                    if (level >= 5) {
                        attackTiles.add(new int[]{playerX - 1, playerY - 2});
                        attackTiles.add(new int[]{playerX + 1, playerY - 2});
                    }
                    break;
                case LEFT:
                    attackTiles.add(new int[]{playerX - 2, playerY});
                    if (level >= 5) {
                        attackTiles.add(new int[]{playerX - 2, playerY - 1});
                        attackTiles.add(new int[]{playerX - 2, playerY + 1});
                    }
                    break;
                case RIGHT:
                    attackTiles.add(new int[]{playerX + 2, playerY});
                    if (level >= 5) {
                        attackTiles.add(new int[]{playerX + 2, playerY - 1});
                        attackTiles.add(new int[]{playerX + 2, playerY + 1});
                    }
                    break;
            }
        }

        Logger.debug("Attack area calculated: " + attackTiles.size() + " tiles");
    }

    private void performAttack(GameManager gameManager) {
        int totalDamage = calculateDamage();
        int enemiesHit = 0;

        for (int[] tile : attackTiles) {
            List<Enemy> enemies = gameManager.getEnemiesAt(tile[0], tile[1]);

            for (Enemy enemy : enemies) {
                if (enemy != null && !enemy.isDead()) {
                    // 获取敌人类型
                    String enemyType = enemy.getClass().getSimpleName();

                    // 应用伤害
                    enemy.takeDamage(totalDamage);
                    enemiesHit++;

                    // 记录详细日志
                    Logger.debug("Hit " + enemyType +
                            " at (" + tile[0] + "," + tile[1] +
                            ") for " + totalDamage + " damage");

                    // 添加攻击特效
                    spawnAttackEffect(tile[0], tile[1], gameManager);

                    // 根据敌人类型可能有不同效果
                    handleSpecialEnemyEffects(enemy, gameManager);
                }
            }
        }

        if (enemiesHit > 0) {
            Logger.gameEvent("近战攻击击中了 " + enemiesHit + " 个敌人");

            // 可以在这里添加连击奖励
            if (enemiesHit >= 3) {
                Logger.gameEvent("三重击！额外奖励");
                // 给玩家加分或恢复魔法值
            }
        } else {
            Logger.debug("攻击未命中任何敌人");
        }
    }
    // 添加攻击特效
    private void spawnAttackEffect(int x, int y, GameManager gameManager) {
        // 这里可以创建视觉特效，比如火花、闪光等
        // 暂时用日志代替
        Logger.debug("Spawn attack effect at (" + x + ", " + y + ")");
    }

    // 处理特殊敌人效果
    private void handleSpecialEnemyEffects(Enemy enemy, GameManager gameManager) {
        // 根据敌人类型添加特殊效果

//        if (enemy instanceof EnemyE03_CaramelJuggernaut) {
//            // 焦糖重装兵被攻击时可能触发反击
//            handleCaramelSpecialEffect((EnemyE03_CaramelJuggernaut) enemy);
//        }
    }


    private void handleCaramelSpecialEffect(EnemyE03_CaramelJuggernaut caramel) {
        // 焦糖重装兵受到攻击时可能进入愤怒状态
//        if (!caramel.isEnraged() && caramel.getHP() <= caramel.getMaxHP() * 0.5f) {
//            Logger.debug("Caramel Juggernaut is enraged!");
//            caramel.setEnraged(true);
//            // 提高移动速度或攻击力
//        }
    }
    private int calculateDamage() {
        return baseDamage + (level - 1) * upgradeDamage;
    }

    @Override
    public void update(float deltaTime) {
        // 更新攻击动画
        if (isAttacking) {
            currentSwingTime += deltaTime;
            if (currentSwingTime >= swingTime) {
                isAttacking = false;
                Logger.debug("Melee attack animation finished");
            }
        }

        // 更新持续时间
        if (isActive) {
            currentDuration -= deltaTime;
            if (currentDuration <= 0) {
                isActive = false;
                currentDuration = 0;
                Logger.debug("Melee attack effect ended");
            }
        }

        // 更新冷却
        if (!isReady) {
            currentCooldown -= deltaTime;
            if (currentCooldown <= 0) {
                isReady = true;
                currentCooldown = 0;
            }
        }
    }

    @Override
    protected void onActivate(Player player, GameManager gameManager) {

    }

    @Override
    public void draw(SpriteBatch batch, ShapeRenderer shapeRenderer, Player player) {
        if (!isAttacking) return;

        // 绘制攻击动画
        if (attackAnimation != null) {
            TextureRegion frame = attackAnimation.getKeyFrame(currentSwingTime);
            if (frame != null) {
                float cellSize = GameConstants.CELL_SIZE;
                float drawX = player.getX() * cellSize;
                float drawY = player.getY() * cellSize;

                // 根据方向调整绘制位置
                Player.Direction dir = player.getDirection();
                float offsetX = 0, offsetY = 0;

                switch (dir) {
                    case UP:
                        offsetY = cellSize * 0.5f;
                        break;
                    case DOWN:
                        offsetY = -cellSize * 0.3f;
                        break;
                    case LEFT:
                        offsetX = -cellSize * 0.5f;
                        break;
                    case RIGHT:
                        offsetX = cellSize * 0.5f;
                        break;
                }

                // 根据攻击进度调整透明度
                float alpha = 1.0f - (currentSwingTime / swingTime);
                batch.setColor(1, 1, 1, alpha);

                // 绘制攻击特效
                batch.draw(frame,
                        drawX + offsetX,
                        drawY + offsetY,
                        cellSize * 1.5f,
                        cellSize * 1.5f);

                batch.setColor(1, 1, 1, 1); // 恢复颜色
            }
        }

        // 调试：显示攻击区域
        if (GameConstants.DEBUG_MODE) {
            shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
            shapeRenderer.setColor(attackColor);

            for (int[] tile : attackTiles) {
                float x = tile[0] * GameConstants.CELL_SIZE;
                float y = tile[1] * GameConstants.CELL_SIZE;
                shapeRenderer.rect(x, y, GameConstants.CELL_SIZE, GameConstants.CELL_SIZE);
            }

            shapeRenderer.end();
        }
    }

    @Override
    protected void onUpgrade() {
        // 每级增加伤害
        upgradeDamage += 1;

        // 第2级减少冷却
        if (level == 2) {
            cooldown *= 0.8f;
            Logger.debug("Melee attack cooldown reduced to: " + cooldown);
        }

        // 第3级增加范围
        if (level == 3) {
            attackRange += upgradeRange;
            Logger.debug("Melee attack range increased to: " + attackRange);
        }

        // 第4级增加额外伤害
        if (level == 4) {
            baseDamage += 1;
            Logger.debug("Melee attack base damage increased to: " + baseDamage);
        }

        // 第5级解锁额外攻击方向
        if (level == 5) {
            Logger.debug("Melee attack unlocked extra attack directions");
        }

        Logger.debug("Melee attack upgraded to level " + level +
                ", Total damage: " + calculateDamage());
    }

    // Getter 方法
    public int getBaseDamage() { return baseDamage; }
    public float getAttackRange() { return attackRange; }
    public float getSwingTime() { return swingTime; }
    public boolean isAttacking() { return isAttacking; }
    public List<int[]> getAttackTiles() { return attackTiles; }

    @Override
    public boolean canActivate(Player player) {
        // 检查是否准备好且有足够的魔法值
        return isReady && player.getMana() >= manaCost;
    }
}