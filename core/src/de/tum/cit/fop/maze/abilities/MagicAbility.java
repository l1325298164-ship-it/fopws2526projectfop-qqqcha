package de.tum.cit.fop.maze.abilities;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import de.tum.cit.fop.maze.entities.Player;
import de.tum.cit.fop.maze.entities.enemy.Enemy;
import de.tum.cit.fop.maze.game.GameConstants;
import de.tum.cit.fop.maze.game.GameManager;

public class MagicAbility extends Ability {

    private GameManager gameManager;

    /* ================= Phase ================= */

    // ✅ 必须是 public，HUD 才能读
    public enum Phase {
        IDLE,
        AIMING,      // AOE 预警
        EXECUTED,    // AOE 已释放，等待二段
        COOLDOWN
    }

    private Phase phase = Phase.IDLE;
    private float phaseTimer = 0f;

    /* ================= Timers ================= */

    private float aimingTimer = 0f;
    private float waitTimer   = 0f;

    private static final float AIMING_TIMEOUT    = 2.0f;
    private static final float WAIT_SECOND_TIME  = 1.0f;
    private static final float COOLDOWN_FAIL     = 0.5f;
    private static final float COOLDOWN_SUCCESS  = 5.0f;

    /* ================= AOE ================= */

    private int aoeTileRadius = 2;
    private float aoeVisualRadius = 2.5f * GameConstants.CELL_SIZE;

    private int aoeCenterX;
    private int aoeCenterY;

    private int hitEnemyCount = 0;

    /* ================= Heal ================= */

    private float baseHealPercent = 0.10f;
    private float extraPerEnemy   = 0.01f;

    /* ================= Cooldown ================= */

    private float currentCooldown = 0f;

    public MagicAbility() {
        super(
                "Magic",
                "AOE damage, then heal based on enemies hit",
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
        return phase == Phase.COOLDOWN;
    }

    @Override
    protected float getCooldownDuration() {
        return currentCooldown;
    }

    @Override
    public boolean canActivate(Player player) {
        return phase != Phase.COOLDOWN && player.getMana() >= manaCost;
    }

    /* ================= Activate ================= */

    @Override
    protected void onActivate(Player player, GameManager gm) {
        this.gameManager = gm;

        switch (phase) {

            case IDLE -> {
                player.startCasting();
                aoeCenterX = gm.getMouseTileX();
                aoeCenterY = gm.getMouseTileY();

                aimingTimer = 0f;
                setPhase(Phase.AIMING);
            }

            case AIMING -> {
                castAOE(gm);
                waitTimer = 0f;
                setPhase(Phase.EXECUTED);
            }

            case EXECUTED -> {
                player.startCasting();
                castHeal(gm);
                startInternalCooldown(COOLDOWN_SUCCESS);
            }
        }
    }

    @Override
    protected void onUpgrade() {

        switch (level) {

            case 2 -> {
                // AOE 半径 +1
                aoeTileRadius += 1;
                aoeVisualRadius = (aoeTileRadius + 0.5f) * GameConstants.CELL_SIZE;
            }

            case 3 -> {
                // 基础治疗提升
                baseHealPercent += 0.05f;
            }

            case 4 -> {
                // 每命中一个敌人的额外治疗
                extraPerEnemy += 0.01f;
            }

            case 5 -> {
                // 再一次扩大范围（后期爽点）
                aoeTileRadius += 1;
                aoeVisualRadius = (aoeTileRadius + 0.5f) * GameConstants.CELL_SIZE;
            }

            case 6 -> {
                // 成功施法冷却略微降低
                currentCooldown = Math.max(3.5f, COOLDOWN_SUCCESS - 0.5f);
            }
        }
    }


    /* ================= Update ================= */

    @Override
    public void update(float delta) {
        super.update(delta);

        // ⭐ 核心：统一累加
        phaseTimer += delta;

        if (phase == Phase.COOLDOWN && ready) {
            setPhase(Phase.IDLE);
        }

        if (phase == Phase.AIMING && gameManager != null) {
            aimingTimer += delta;
            aoeCenterX = gameManager.getMouseTileX();
            aoeCenterY = gameManager.getMouseTileY();

            if (aimingTimer >= AIMING_TIMEOUT) {
                setPhase(Phase.IDLE);
            }
        }

        if (phase == Phase.EXECUTED) {
            waitTimer += delta;
            if (waitTimer >= WAIT_SECOND_TIME) {
                startInternalCooldown(COOLDOWN_FAIL);
            }
        }
    }

    /* ================= Phase Helper ================= */

    // ✅ 所有 phase 切换都走这里
    private void setPhase(Phase newPhase) {
        if (phase != newPhase) {
            phase = newPhase;
            phaseTimer = 0f;
            aimingTimer = 0f;
            waitTimer = 0f;
        }
    }

    private void startInternalCooldown(float cd) {
        currentCooldown = cd;
        ready = false;
        cooldownTimer = 0f;
        setPhase(Phase.COOLDOWN);
    }

    /* ================= AOE ================= */

    private void castAOE(GameManager gm) {
        hitEnemyCount = 0;

        for (Enemy enemy : gm.getEnemies()) {
            if (enemy == null || enemy.isDead()) continue;

            int dx = enemy.getX() - aoeCenterX;
            int dy = enemy.getY() - aoeCenterY;

            if (dx * dx + dy * dy <= aoeTileRadius * aoeTileRadius) {
                enemy.takeDamage(20);
                hitEnemyCount++;
            }
        }
    }

    /* ================= Heal ================= */

    private void castHeal(GameManager gm) {
        float healPercent = baseHealPercent + hitEnemyCount * extraPerEnemy;

        for (Player p : gm.getPlayers()) {
            if (p == null || p.isDead()) continue;

            int heal = Math.max(1, Math.round(p.getMaxLives() * healPercent));
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

    /* ================= HUD Getters ================= */

    public Phase getPhase() {
        return phase;
    }

    public float getPhaseTime() {
        return phaseTimer;
    }

    @Override
    public AbilityInputType getInputType() {
        return AbilityInputType.CONTINUOUS;
    }
}
