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
        // 使用正确的冷却时间（原版是0.4f）
        super(
                "Sword Slash",
                "Slash enemies in front of you",
                0.8f,      // cooldown
                HIT_TIME   // duration = 命中窗口
        );

        this.manaCost = 10; // 如果没有法力消耗，设为0
    }

    @Override
    protected void onActivate(Player player, GameManager gameManager) {

        this.gameManager = gameManager;

        attackTimer = 0f;
        damageDone = false;

        calculateAttackTiles(player);
        player.startAttack();

        AudioManager.getInstance().play(AudioType.UI_CLICK);

    }
    @Override
    protected boolean shouldStartCooldown() {
        return true;
    }


    @Override
    protected boolean shouldConsumeMana() {
        return manaCost > 0; // 只有有法力消耗时才消耗法力
    }



    @Override
    public void update(float delta, Player player, GameManager gameManager) {
        super.update(delta, player, gameManager);

        if (gameManager == null) return;

        attackTimer += delta;

        if (!damageDone && attackTimer >= HIT_TIME) {
            dealDamage(gameManager);
            damageDone = true;
        }
    }




    @Override
    protected boolean shouldBecomeActive() {
        return false;   // ❗ 不进入 active 生命周期
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
        // 升级逻辑通过 getLevel() 动态计算 damage 实现
    }
    @Override
    public String getId() {
        return "melee";
    }
}