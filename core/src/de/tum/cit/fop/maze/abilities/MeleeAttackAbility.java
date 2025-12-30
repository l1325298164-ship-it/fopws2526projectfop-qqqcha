// MeleeAttackAbility.java
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
import java.util.List;

public class MeleeAttackAbility extends Ability {

    /* ===== 数值 ===== */
    private int baseDamage = 2;
    private int damagePerLevel = 1;

    /* ===== 攻击区域 ===== */
    private final List<int[]> attackTiles = new ArrayList<>();

    /* ===== 视觉调试 ===== */
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

    /* ================================================= */
    /* ================= 生命周期 ====================== */
    /* ================================================= */

    @Override
    protected void onActivate(Player player, GameManager gameManager) {

        // 1️⃣ 计算攻击区域
        calculateAttackTiles(player);

        // 2️⃣ 造成伤害
        dealDamage(gameManager);

        // 3️⃣ 播放音效
        AudioManager.getInstance().play(AudioType.SWORD_SWING);

        Logger.debug("Melee activated, tiles=" + attackTiles.size());
    }

    @Override
    protected void updateActive(float delta) {
        // 这里暂时不需要做事
        // 如果以后加动画，可以放这里
    }

    @Override
    protected void onDeactivate() {
        attackTiles.clear();
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

    private void dealDamage(GameManager gameManager) {
        int damage = baseDamage + (level - 1) * damagePerLevel;
        int hitCount = 0;

        for (int[] tile : attackTiles) {
            List<Enemy> enemies = gameManager.getEnemiesAt(tile[0], tile[1]);
            for (Enemy enemy : enemies) {
                if (enemy != null && !enemy.isDead()) {
                    enemy.takeDamage(damage);
                    hitCount++;
                }
            }
        }

        if (hitCount > 0) {
            Logger.gameEvent("Melee hit " + hitCount + " enemies");
        }
    }

    /* ================================================= */
    /* ================= 绘制 ========================== */
    /* ================================================= */

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

    /* ================================================= */
    /* ================= 升级 ========================== */
    /* ================================================= */

    @Override
    protected void onUpgrade() {

        // 2级：冷却减少
        if (level == 2) {
            Logger.debug("Melee cooldown reduced");
        }

        // 3级：范围已经生效（前方第二格）
        if (level == 3) {
            Logger.debug("Melee range increased");
        }

        // 4级：基础伤害提高
        if (level == 4) {
            baseDamage += 1;
        }

        // 5级：你可以在这里解锁新形态
        if (level == 5) {
            Logger.debug("Melee reached max level");
        }
    }
}
