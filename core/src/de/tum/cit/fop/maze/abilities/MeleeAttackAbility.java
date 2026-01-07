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
    private static final Color DEBUG_COLOR =
            new Color(1f, 0.2f, 0.2f, 0.3f);

    public MeleeAttackAbility() {
        super(
                "Sword Slash",
                "Slash enemies in front of you",
                0.8f,   // 冷却
                0.25f   // 持续（挥刀时间）
        );
        this.manaCost = 10;
    }

    /* ================= 生命周期 ================= */

    @Override
    protected void onActivate(Player player, GameManager gameManager) {
        this.gameManager = gameManager;

        player.startAttack();

        attackTimer = 0f;
        damageDone = false;

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

    /* ================= 攻击逻辑 ================= */

    private void calculateAttackTiles(Player player) {
        attackTiles.clear();

        int px = player.getX();
        int py = player.getY();
        Player.Direction dir = player.getDirection();

        // 包含自身，防止贴脸
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

        if (level == 4) {
            baseDamage += 1;
        }

        // 2 / 3 / 5 级效果你以后再扩
    }
}
