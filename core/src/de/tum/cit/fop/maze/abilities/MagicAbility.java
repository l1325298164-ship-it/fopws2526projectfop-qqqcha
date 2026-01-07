package de.tum.cit.fop.maze.abilities;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector3;
import de.tum.cit.fop.maze.entities.Player;
import de.tum.cit.fop.maze.entities.enemy.Enemy;
import de.tum.cit.fop.maze.game.GameConstants;
import de.tum.cit.fop.maze.game.GameManager;

public class MagicAbility extends Ability {
    private GameManager gameManager;

    /* ================= 阶段 ================= */
    enum Phase {
        IDLE,        // 什么都没发生
        AIMING,      // 按住：只画 AOE，不结算
        EXECUTED,    // 已结算 AOE，等二段
        COOLDOWN
    }

    private Phase phase = Phase.IDLE;

    /* ================= 计时 ================= */

    private float waitTimer = 0f;

    private static final float WAIT_SECOND_TIME = 1.0f;
    private static final float COOLDOWN_FAIL    = 1.5f;
    private static final float COOLDOWN_SUCCESS = 5.0f;

    /* ================= AOE ================= */

    private int aoeTileRadius = 2;
    private float aoeVisualRadius = 2.5f * GameConstants.CELL_SIZE;

    private int aoeCenterX;
    private int aoeCenterY;

    private int hitEnemyCount = 0;

    /* ================= 治疗 ================= */

    private float baseHealPercent = 0.10f;
    private float extraPerEnemy   = 0.01f;

    /* ================= 冷却 ================= */

    private float currentCooldown = 0f;

    public MagicAbility() {
        super(
                "Magic",
                "AOE damage, then heal both players based on enemies hit",
                0f,
                0f
        );
        this.manaCost = 20;
    }

    /* ================= Ability Hooks ================= */

    @Override
    protected boolean shouldConsumeMana() {
        return phase == Phase.IDLE;
    }

    @Override
    protected boolean shouldStartCooldown() {
        return phase == Phase.EXECUTED    || phase == Phase.COOLDOWN;
    }

    @Override
    protected float getCooldownDuration() {
        return currentCooldown;
    }

    @Override
    protected boolean shouldBecomeActive() {
        return false; // P2 不走 active/duration
    }

    @Override
    public boolean canActivate(Player player) {
        if (phase == Phase.COOLDOWN) return false;
        return player.getMana() >= manaCost;
    }

    /* ================= 激活 ================= */

    @Override
    protected void onActivate(Player player, GameManager gm) {
        this.gameManager = gm;
        switch (phase) {

            case IDLE -> {
                aoeCenterX = gm.getMouseTileX();
                aoeCenterY = gm.getMouseTileY();
                phase = Phase.AIMING;
            }

            case AIMING -> {
                // 第二次按下：确认释放 AOE
                castAOE(gm);
                phase = Phase.EXECUTED;
                waitTimer = 0f;
            }

            case EXECUTED -> {
                // 二段技能：治疗
                castHeal(gm);
                startInternalCooldown(COOLDOWN_SUCCESS);
            }
        }
    }


    /* ================= 更新 ================= */

    @Override
    public void update(float delta) {

        if (phase == Phase.AIMING && gameManager != null) {
            aoeCenterX = gameManager.getMouseTileX();
            aoeCenterY = gameManager.getMouseTileY();
        }


        if (phase == Phase.EXECUTED) {
            waitTimer += delta;
            if (waitTimer >= WAIT_SECOND_TIME) {
                startInternalCooldown(COOLDOWN_FAIL);
            }
        }
    }


    private void startInternalCooldown(float cd) {
        phase = Phase.COOLDOWN;
        currentCooldown = cd;
        ready = false;
        cooldownTimer = 0f;
    }

    /* ================= AOE ================= */

    private void castAOE(GameManager gm) {

        hitEnemyCount = 0;

        aoeCenterX = gm.getMouseTileX();
        aoeCenterY = gm.getMouseTileY();

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

    /* ================= Heal ================= */

    private void castHeal(GameManager gm) {

        float healPercent =
                baseHealPercent + hitEnemyCount * extraPerEnemy;

        for (Player p : gm.getPlayers()) {
            if (p == null || p.isDead()) continue;

            int heal = Math.max(1,
                    Math.round(p.getMaxLives() * healPercent));
            p.heal(heal);
        }
    }

    /* ================= Draw ================= */

    @Override
    public void draw(SpriteBatch batch, ShapeRenderer sr, Player player) {
        if (phase != Phase.AIMING) return;

        sr.begin(ShapeRenderer.ShapeType.Line);
        sr.setColor(Color.PURPLE);

        sr.circle(
                (aoeCenterX + 0.5f) * GameConstants.CELL_SIZE,
                (aoeCenterY + 0.5f) * GameConstants.CELL_SIZE,
                aoeVisualRadius
        );

        sr.end();
    }


    /* ================= Upgrade ================= */

    @Override
    protected void onUpgrade() {
        if (level == 2) aoeTileRadius += 1;
        if (level == 3) baseHealPercent += 0.05f;
        if (level == 4) extraPerEnemy += 0.01f;
        if (level == 5) aoeTileRadius += 1;
    }
    @Override
    public AbilityInputType getInputType() {
        return AbilityInputType.CONTINUOUS;
    }
    public void onMousePressed(GameManager gm) {
        if (phase != Phase.IDLE) return;
        if (!canActivate(gm.getPlayer())) return;

        // 扣蓝
        gm.getPlayer().useMana(manaCost);

        // 进入瞄准
        aoeCenterX = gm.getMouseTileX();
        aoeCenterY = gm.getMouseTileY();
        phase = Phase.AIMING;
    }

    public void onMouseHeld(GameManager gm) {
        if (phase != Phase.AIMING) return;

        // AOE 跟着鼠标
        aoeCenterX = gm.getMouseTileX();
        aoeCenterY = gm.getMouseTileY();
    }
    public void onMouseReleased(GameManager gm) {
        if (phase != Phase.AIMING) return;

        // 释放 AOE
        castAOE(gm);
        phase = Phase.EXECUTED;
        waitTimer = 0f;
    }



}
