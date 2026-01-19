package de.tum.cit.fop.maze.abilities;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import de.tum.cit.fop.maze.audio.AudioManager;
import de.tum.cit.fop.maze.audio.AudioType;
import de.tum.cit.fop.maze.entities.Player;
import de.tum.cit.fop.maze.entities.enemy.Enemy;
import de.tum.cit.fop.maze.game.GameConstants;
import de.tum.cit.fop.maze.game.GameManager;
import de.tum.cit.fop.maze.utils.CameraManager; // <--- 必须导入这个
import de.tum.cit.fop.maze.utils.Logger;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MeleeAttackAbility extends Ability {

    /* ===== 数值 ===== */
    private int baseDamage = 5;
    private int damagePerLevel = 1;
    // 命中提前修正（升级用）
    private float hitTimeOffset = 0f;

    private static final float HIT_TIME = 0.12f;
    private GameManager gameManager;

    /* ===== 攻击区域 ===== */
    private final List<int[]> attackTiles = new ArrayList<>();

    /* ===== 命中帧控制 ===== */
    private float attackTimer = 0f;
    private boolean damageDone = false;

    /* ===== Debug ===== */
    private static final Color DEBUG_COLOR = new Color(1f, 0.9f, 0f, 0.5f);

    public MeleeAttackAbility() {
        super(
                "Sword Slash",
                "Slash enemies in front of you",
                0.8f,      // cooldown
                HIT_TIME   // duration = 命中窗口
        );
        this.manaCost = 10;
    }

    @Override
    protected void onActivate(Player player, GameManager gameManager) {
        this.gameManager = gameManager;
        attackTimer = 0f;
        damageDone = false;

        calculateAttackTiles(player);
        player.startAttack();

        // ✅ 1. 播放挥剑音效
        AudioManager.getInstance().play(AudioType.SKILL_SLASH);

        // ✅ 2. 播放挥剑特效
        if (gameManager.getCombatEffectManager() != null) {
            float angle = 0;
            switch (player.getDirection()) {
                case RIGHT -> angle = 0;
                case UP -> angle = 90;
                case LEFT -> angle = 180;
                case DOWN -> angle = 270;
            }

            // 🔥 修改重点：传入 this.level 而不是 0
            // 这样才能触发 SlashEffect 中写好的 Lv2(火焰) 和 Lv3(霓虹) 效果！
            gameManager.getCombatEffectManager().spawnSlash(
                    player.getWorldX() * GameConstants.CELL_SIZE,
                    player.getWorldY() * GameConstants.CELL_SIZE,
                    angle,
                    this.level // <--- 这里使用了当前技能等级
            );
        }
    }

    @Override
    protected boolean shouldStartCooldown() {
        return true;
    }

    @Override
    protected boolean shouldConsumeMana() {
        return manaCost > 0;
    }

    @Override
    public void update(float delta, Player player, GameManager gameManager) {
        super.update(delta, player, gameManager);

        if (gameManager == null) return;

        attackTimer += delta;

        if (!damageDone && attackTimer >= HIT_TIME - hitTimeOffset) {
            dealDamage(gameManager);
            damageDone = true;
        }
    }

    @Override
    protected boolean shouldBecomeActive() {
        return false;   // 瞬发技能，不保持 Active 状态
    }

    private void calculateAttackTiles(Player player) {
        attackTiles.clear();
        int px = player.getX();
        int py = player.getY();

        // 攻击面前两格
        switch (player.getDirection()) {
            case UP -> {
                attackTiles.add(new int[]{px, py + 1});
                attackTiles.add(new int[]{px, py + 2});
            }
            case DOWN -> {
                attackTiles.add(new int[]{px, py - 1});
                attackTiles.add(new int[]{px, py - 2});
            }
            case LEFT -> {
                attackTiles.add(new int[]{px - 1, py});
                attackTiles.add(new int[]{px - 2, py});
            }
            case RIGHT -> {
                attackTiles.add(new int[]{px + 1, py});
                attackTiles.add(new int[]{px + 2, py});
            }
        }
    }

    private void dealDamage(GameManager gameManager) {
        if (gameManager == null) return;
        int damage = (int)((baseDamage + (level - 1) * damagePerLevel));

        Set<Enemy> hitEnemies = new HashSet<>();

        // 遍历所有受影响的格子，对其中的敌人造成伤害
        for (int[] tile : attackTiles) {
            List<Enemy> enemies = gameManager.getEnemiesAt(tile[0], tile[1]);
            if (enemies != null) {
                for (Enemy enemy : enemies) {
                    if (enemy != null && !enemy.isDead() && hitEnemies.add(enemy)) {
                        enemy.takeDamage(damage);
                    }
                }
            }
        }

        // 🔥 新增：如果击中了任何敌人，触发屏幕震动 (Juice!)
        if (!hitEnemies.isEmpty()) {
            // 震动时间：0.15秒 (短促有力)
            // 震动强度：基础 2.0，每级增加 0.5 (Lv1=2.5, Lv5=4.5)
            float shakeStrength = 2.0f + (level * 0.5f);

            // 调用我们刚刚在 CameraManager 中修复的单例方法
            CameraManager.getInstance().shake(0.15f, shakeStrength);

            // 可以在这里加一点顿帧逻辑 (HitStop) 的预留位置
            // gameManager.triggerHitStop(0.05f);
        }
    }

    /* ================= 绘制 ================= */

    @Override
    public void draw(SpriteBatch batch, ShapeRenderer shapeRenderer, Player player) {
        if (!GameConstants.DEBUG_MODE) return;

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(DEBUG_COLOR);

        for (int[] tile : attackTiles) {
            shapeRenderer.rect(
                    tile[0] * GameConstants.CELL_SIZE,
                    tile[1] * GameConstants.CELL_SIZE,
                    GameConstants.CELL_SIZE,
                    GameConstants.CELL_SIZE
            );
        }

        shapeRenderer.end();
    }

    /* ================= 升级 ================= */

    @Override
    protected void onUpgrade() {
        switch (level) {
            case 2 -> {
                baseDamage += 2;
            }
            case 3 -> {
                damagePerLevel += 1;
            }
            case 4 -> {
                // ⭐ 出伤提前 0.03 秒
                hitTimeOffset += 0.03f;
            }
            case 5 -> {
                baseDamage += 5;
            }
        }
    }

    @Override
    public String getId() {
        return "melee";
    }
}