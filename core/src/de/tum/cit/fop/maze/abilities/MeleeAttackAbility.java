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
import de.tum.cit.fop.maze.utils.Logger;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MeleeAttackAbility extends Ability {

    /* ===== 数值 ===== */
    private int baseDamage = 5;
    private int damagePerLevel = 1;
    private boolean damageDone = false;
    private static final float HIT_TIME = 0.12f; // 命中帧时间
    /* ===== 攻击区域 ===== */
    private final List<int[]> attackTiles = new ArrayList<>();
    // ===== 命中帧控制 =====
    private float attackTimer = 0f;
    /* ===== 视觉调试 ===== */
    private static final Color DEBUG_COLOR =
            new Color(1f, 0.2f, 0.2f, 0.3f);
    private GameManager gameManager;
    public MeleeAttackAbility() {
        super(
                "Sword Slash",
                "Slash enemies in front of you",
                0.8f,   // 冷却
                0.25f   // 持续（挥刀时间）
        );
        this.manaCost = 10;
    }

    /* ================================================= */
    /* ================= 生命周期 ====================== */
    /* ================================================= */

    @Override
    protected void onActivate(Player player, GameManager gameManager) {

        this.gameManager = gameManager;

        player.startAttack();
        // 初始化攻击计时
        attackTimer = 0f;
        damageDone = false;

        // 1️⃣ 计算攻击区域
        calculateAttackTiles(player);

        AudioManager.getInstance().play(AudioType.UI_CLICK);

    }

    @Override
    protected void updateActive(float delta) {
        attackTimer += delta;

        if (!damageDone && attackTimer >= HIT_TIME) {
            dealDamage(gameManager);
            damageDone = true;
        }
    }

    @Override
    protected void onDeactivate() {
        attackTiles.clear();
        attackTimer = 0f;
        damageDone = false;
        gameManager = null;
    }

    /* ================================================= */
    /* ================= 核心逻辑 ====================== */
    /* ================================================= */

    private void calculateAttackTiles(Player player) {
        attackTiles.clear();

        int px = player.getX();
        int py = player.getY();
        Player.Direction dir = player.getDirection();

        // 始终包含玩家所在格（防止贴脸敌人）
        attackTiles.add(new int[]{px, py});

        switch (dir) {
            case UP -> {
                attackTiles.add(new int[]{px, py + 1});
                attackTiles.add(new int[]{px - 1, py + 1});
                attackTiles.add(new int[]{px + 1, py + 1});
            }
            case DOWN -> {
                attackTiles.add(new int[]{px, py - 1});
                attackTiles.add(new int[]{px - 1, py - 1});
                attackTiles.add(new int[]{px + 1, py - 1});
            }
            case LEFT -> {
                attackTiles.add(new int[]{px - 1, py});
                attackTiles.add(new int[]{px - 1, py - 1});
                attackTiles.add(new int[]{px - 1, py + 1});
            }
            case RIGHT -> {
                attackTiles.add(new int[]{px + 1, py});
                attackTiles.add(new int[]{px + 1, py - 1});
                attackTiles.add(new int[]{px + 1, py + 1});
            }
        }

        // 等级扩展
        if (level >= 3) {
            extendRange(dir, px, py);
        }
    }

    private void extendRange(Player.Direction dir, int px, int py) {
        switch (dir) {
            case UP -> attackTiles.add(new int[]{px, py + 2});
            case DOWN -> attackTiles.add(new int[]{px, py - 2});
            case LEFT -> attackTiles.add(new int[]{px - 2, py});
            case RIGHT -> attackTiles.add(new int[]{px + 2, py});
        }
    }

    // 防止一次攻击多次伤害同一个对象
    private void dealDamage(GameManager gameManager) {
        int damage = baseDamage + (level - 1) * damagePerLevel;

        Set<Enemy> hitEnemies = new HashSet<>();

        for (int[] tile : attackTiles) {
            for (Enemy enemy : gameManager.getEnemiesAt(tile[0], tile[1])) {
                if (enemy != null && !enemy.isDead() && hitEnemies.add(enemy)) {
                    enemy.takeDamage(damage);
                }
            }
        }
    }

    /* ================================================= */
    /* ================= 绘制 ========================== */
    /* ================================================= */

    @Override
    public void draw(SpriteBatch batch, ShapeRenderer shapeRenderer, Player player) {
        if (!GameConstants.DEBUG_MODE) return;

        shapeRenderer.setColor(DEBUG_COLOR);

        for (int[] tile : attackTiles) {
            shapeRenderer.rect(
                    tile[0] * GameConstants.CELL_SIZE,
                    tile[1] * GameConstants.CELL_SIZE,
                    GameConstants.CELL_SIZE,
                    GameConstants.CELL_SIZE
            );
        }
    }

    /* ================================================= */
    /* ================= 升级 ========================== */
    /* ================================================= */

    @Override
    protected void onUpgrade() {
        // 2级：冷却减少
        if (level == 2) {
            // 冷却减少逻辑
        }

        // 3级：范围已经生效（前方第二格）
        if (level == 3) {
            // 范围增加逻辑
        }

        // 4级：基础伤害提高
        if (level == 4) {
            baseDamage += 1;
        }

        // 5级：你可以在这里解锁新形态
        if (level == 5) {
            // 最大等级效果
        }
    }
}