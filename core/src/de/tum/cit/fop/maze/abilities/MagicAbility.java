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

    private enum Phase {
        IDLE,
        AIMING,      // AOE 预警
        EXECUTED,    // AOE 已释放，等待二段
        COOLDOWN
    }

    private Phase phase = Phase.IDLE;

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
    protected boolean shouldBecomeActive() {
        return false;
    }

    @Override
    public boolean canActivate(Player player) {
        if (phase == Phase.COOLDOWN) return false;
        return player.getMana() >= manaCost;
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
                phase = Phase.AIMING;
            }

            case AIMING -> {
                castAOE(gm);
                waitTimer = 0f;
                phase = Phase.EXECUTED;
            }

            case EXECUTED -> {
                player.startCasting();
                castHeal(gm);
                startInternalCooldown(COOLDOWN_SUCCESS);
            }
        }
    }

    /* ================= Update ================= */

    @Override
    public void update(float delta) {
        super.update(delta);
        if (phase == Phase.COOLDOWN && ready) {
            phase = Phase.IDLE;
        }
        if (phase == Phase.AIMING && gameManager != null) {
            aimingTimer += delta;
            aoeCenterX = gameManager.getMouseTileX();
            aoeCenterY = gameManager.getMouseTileY();

            // 2s 内未再次按键 → 自动取消，无 CD
            if (aimingTimer >= AIMING_TIMEOUT) {
                phase = Phase.IDLE;
                aimingTimer = 0f;
            }
        }

        if (phase == Phase.EXECUTED) {
            waitTimer += delta;
            if (waitTimer >= WAIT_SECOND_TIME) {
                startInternalCooldown(COOLDOWN_FAIL);
                // 🔥 关键：清理状态，防止重复进入
                waitTimer = 0f;
            }
        }
    }

    private void startInternalCooldown(float cd) {
        phase = Phase.COOLDOWN;
        currentCooldown = cd;
        ready = false;
        cooldownTimer = 0f;

        // 🔒 清理所有阶段计时
        aimingTimer = 0f;
        waitTimer = 0f;
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

            int heal = Math.max(
                    1,
                    Math.round(p.getMaxLives() * healPercent)
            );
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
}
