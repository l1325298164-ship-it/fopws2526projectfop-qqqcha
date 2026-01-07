package de.tum.cit.fop.maze.abilities;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import de.tum.cit.fop.maze.entities.Player;
import de.tum.cit.fop.maze.entities.enemy.Enemy;
import de.tum.cit.fop.maze.game.GameConstants;
import de.tum.cit.fop.maze.game.GameManager;

import java.util.List;

public class MagicAbility extends Ability {

    /* ================= 阶段 ================= */

    private enum Phase {
        READY,          // 可以第一次释放
        WAIT_SECOND,    // 已放 AOE，等第二次
        COOLDOWN        // 冷却中
    }

    private Phase phase = Phase.READY;

    private float timer = 0f;
    private int hitEnemyCount = 0;


    /* ================= 时间参数 ================= */

    // 二段输入窗口（1s 内可按第二次）
    private static final float WAIT_SECOND_TIME = 1.0f;

    // 冷却
    private static final float COOLDOWN_FAIL    = 1.5f; // 没二段
    private static final float COOLDOWN_SUCCESS = 5.0f; // 成功二段


    /* ================= AOE 参数 ================= */

    // 逻辑半径（格子）
    private int aoeTileRadius = 2;

    // 视觉半径（世界单位）
    private float aoeVisualRadius = 2.5f * GameConstants.CELL_SIZE;


    /* ================= 二段状态 ================= */

    private float phaseTimer = 0f;


    /* ================= 治疗参数 ================= */

    private float baseHealPercent = 0.10f;   // 每只敌人 10%
    private float extraPerEnemy  = 0.01f;    // 每多一只 +1%


    /* ================= 临时状态 ================= */

    private int aoeCenterX;
    private int aoeCenterY;


    public MagicAbility() {
        super(
                "Magic",
                "AOE damage, then heal both players based on enemies hit",
                0f,
                0f
        );
        this.manaCost = 20;
    }

    /* ================= 输入触发 ================= */

    @Override
    protected void onActivate(Player player, GameManager gm) {

        switch (phase) {

            case READY -> {
                castAOE(gm);
                phase = Phase.WAIT_SECOND;
                timer = 0f;
            }

            case WAIT_SECOND -> {
                castHeal(gm);
                startCooldown(COOLDOWN_SUCCESS);
            }

            case COOLDOWN -> {
                // 什么都不做
            }
        }
    }

    /* ================= 状态更新 ================= */

    @Override
    public void update(float delta) {

        // ⚠️ 不再依赖父类 cooldown
        if (phase == Phase.WAIT_SECOND) {
            timer += delta;

            if (timer >= WAIT_SECOND_TIME) {
                // 没按第二次
                startCooldown(COOLDOWN_FAIL);
            }
            return;
        }

        if (phase == Phase.COOLDOWN) {
            timer += delta;

            if (timer >= cooldownDuration) {
                // 冷却结束
                phase = Phase.READY;
                timer = 0f;
                ready = true;
            }
        }
    }

    private float cooldownDuration = 0f;

    private void startCooldown(float cd) {
        phase = Phase.COOLDOWN;
        cooldownDuration = cd;
        timer = 0f;

        // 锁死 Ability
        this.ready = false;
        this.active = false;
    }
    /* ================= 第一段：AOE ================= */

    private void castAOE(GameManager gm) {

        hitEnemyCount = 0;

        // 1️⃣ 鼠标屏幕坐标
        int screenX = Gdx.input.getX();
        int screenY = Gdx.input.getY();

        // 2️⃣ 转世界坐标（Y 反转）
        Vector3 world = new Vector3(
                screenX,
                Gdx.graphics.getHeight() - screenY,
                0
        );

        aoeCenterX = (int)(world.x / GameConstants.CELL_SIZE);
        aoeCenterY = (int)(world.y / GameConstants.CELL_SIZE);

        for (Enemy enemy : gm.getEnemies()) {
            if (enemy == null || enemy.isDead()) continue;

            int dx = enemy.getX() - aoeCenterX;
            int dy = enemy.getY() - aoeCenterY;

            if (dx * dx + dy * dy <= aoeTileRadius * aoeTileRadius) {
                enemy.takeDamage(1);
                hitEnemyCount++;
            }
        }
    }

    @Override
    public boolean canActivate(Player player) {
        // CD 中完全禁止
        if (phase == Phase.COOLDOWN) return false;

        return player.getMana() >= manaCost;
    }


    /* ================= 第二段：回血 ================= */

    private void castHeal(GameManager gm) {

        float healPercent =
                baseHealPercent + hitEnemyCount * extraPerEnemy;

        for (Player p : gm.getPlayers()) {
            if (p == null || p.isDead()) continue;

            int maxHp = p.getMaxLives();
            int heal = Math.max(1, Math.round(maxHp * healPercent));
            p.heal(heal);
        }
    }


    /* ================= Ability 接口占位 ================= */




    @Override
    public void draw(SpriteBatch batch, ShapeRenderer sr, Player player) {

        if (phase != Phase.WAIT_SECOND) return;

        sr.begin(ShapeRenderer.ShapeType.Line);
        sr.setColor(Color.PURPLE);

        sr.circle(
                (aoeCenterX + 0.5f) * GameConstants.CELL_SIZE,
                (aoeCenterY + 0.5f) * GameConstants.CELL_SIZE,
                aoeVisualRadius
        );

        sr.end();
    }

    @Override
    protected void onUpgrade() {
        if (level == 2) aoeTileRadius += 1;
        if (level == 3) baseHealPercent += 0.05f;
        if (level == 4) extraPerEnemy += 0.01f;
        if (level == 5) aoeTileRadius += 1;
    }
}
