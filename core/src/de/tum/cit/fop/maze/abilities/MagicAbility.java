package de.tum.cit.fop.maze.abilities;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import de.tum.cit.fop.maze.entities.Player;
import de.tum.cit.fop.maze.entities.enemy.Enemy;
import de.tum.cit.fop.maze.game.GameConstants;
import de.tum.cit.fop.maze.game.GameManager;
import com.badlogic.gdx.graphics.Color;

import java.util.Map;

public class MagicAbility extends Ability {

    private GameManager gameManager;

    public enum Phase {
        IDLE,
        AIMING,
        EXECUTED,
        COOLDOWN
    }

    private Phase phase = Phase.IDLE;
    private float phaseTimer = 0f;

    // ... (Variables omitted for brevity) ...
    private float aimingTimer = 0f;
    private float effectWaitTimer = 0f;
    private static final float AIMING_TIMEOUT    = 5.0f;
    private static final float EFFECT_DURATION   = 0.6f;
    private static final float COOLDOWN_DURATION = 5.0f;
    private int aoeTileRadius = 2;
    private float aoeVisualRadius = 2.5f * GameConstants.CELL_SIZE;
    private boolean inputConsumedThisFrame = false;
    private int aoeCenterX;
    private int aoeCenterY;
    private int hitEnemyCount = 0;
    private float baseHealPercent = 0.10f;
    private float extraPerEnemy   = 0.02f;
    private float currentCooldown = COOLDOWN_DURATION;

    public MagicAbility() {
        super("Magic Strike", "Aim -> Pillar Damage -> Absorb Essence", 0f, 0f);
        this.manaCost = 20;
    }

    // 🔥 [修复] 添加 Getter 供 HUD 使用
    public Phase getPhase() {
        return phase;
    }

    public float getPhaseTime() {
        return phaseTimer;
    }

    @Override
    protected boolean shouldConsumeMana() {
        return phase == Phase.AIMING;
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
        if (phase == Phase.COOLDOWN || phase == Phase.EXECUTED) return false;
        return player.getMana() >= manaCost;
    }

    // ... (rest of the file content remains the same as provided in prompt) ...
    @Override
    protected void onActivate(Player player, GameManager gm) {
        if (gm.isUIConsumingMouse()) return;
        if (inputConsumedThisFrame) return;
        inputConsumedThisFrame = true;
        this.gameManager = gm;

        switch (phase) {
            case IDLE -> {
                aoeCenterX = gm.getMouseTileX();
                aoeCenterY = gm.getMouseTileY();
                aimingTimer = 0f;
                setPhase(Phase.AIMING);
            }
            case AIMING -> {
                if (phaseTimer < 0.1f) return;
                castAOE(gm);
                effectWaitTimer = 0f;
                setPhase(Phase.EXECUTED);
            }
        }
    }

    @Override
    public void update(float delta, Player player, GameManager gm) {
        super.update(delta, player, gm);
        inputConsumedThisFrame = false;
        phaseTimer += delta;

        switch (phase) {
            case COOLDOWN -> {
                if (ready) setPhase(Phase.IDLE);
            }
            case AIMING -> {
                aimingTimer += delta;
                if (!gm.isUIConsumingMouse()) {
                    aoeCenterX = gm.getMouseTileX();
                    aoeCenterY = gm.getMouseTileY();
                }
                if (aimingTimer >= AIMING_TIMEOUT) {
                    setPhase(Phase.IDLE);
                }
            }
            case EXECUTED -> {
                effectWaitTimer += delta;
                if (effectWaitTimer >= EFFECT_DURATION) {
                    castHeal(gm);
                    startInternalCooldown(currentCooldown);
                }
            }
        }
    }

    private void setPhase(Phase newPhase) {
        phase = newPhase;
        phaseTimer = 0f;
    }

    private void startInternalCooldown(float cd) {
        currentCooldown = cd;
        ready = false;
        cooldownTimer = 0f;
        setPhase(Phase.COOLDOWN);
    }

    private void castAOE(GameManager gm) {
        hitEnemyCount = 0;
        if (gm.getCombatEffectManager() != null) {
            float cx = (aoeCenterX + 0.5f) * GameConstants.CELL_SIZE;
            float cy = (aoeCenterY + 0.5f) * GameConstants.CELL_SIZE;
            gm.getCombatEffectManager().spawnMagicCircle(cx, cy, aoeVisualRadius, 0.5f);
            gm.getCombatEffectManager().spawnMagicPillar(cx, cy, aoeVisualRadius);
            de.tum.cit.fop.maze.utils.CameraManager.getInstance().shake(0.2f, 3.0f);
        }
        for (Enemy enemy : gm.getEnemies()) {
            if (enemy == null || enemy.isDead()) continue;
            int dx = enemy.getX() - aoeCenterX;
            int dy = enemy.getY() - aoeCenterY;
            if (dx * dx + dy * dy <= aoeTileRadius * aoeTileRadius) {
                enemy.takeDamage(20 + (level * 5));
                hitEnemyCount++;
                if (gm.getCombatEffectManager() != null) {
                    gm.getCombatEffectManager().spawnHitSpark(
                            (enemy.getX() + 0.5f) * GameConstants.CELL_SIZE,
                            (enemy.getY() + 0.5f) * GameConstants.CELL_SIZE
                    );
                }
            }
        }
    }

    private void castHeal(GameManager gm) {
        float healPercent = baseHealPercent + hitEnemyCount * extraPerEnemy;
        healPercent = Math.min(healPercent, 0.5f);
        for (Player p : gm.getPlayers()) {
            if (p == null || p.isDead()) continue;
            if (gm.getCombatEffectManager() != null) {
                float px = (p.getX() + 0.5f) * GameConstants.CELL_SIZE;
                float py = (p.getY() + 0.5f) * GameConstants.CELL_SIZE;
                gm.getCombatEffectManager().spawnMagicEssence(px - 50, py, px, py);
                gm.getCombatEffectManager().spawnMagicEssence(px + 50, py, px, py);
                if (hitEnemyCount > 0) {
                    gm.getCombatEffectManager().spawnStatusText(px, py + 40, "ABSORB", Color.CYAN);
                }
            }
            int heal = Math.max(1, Math.round(p.getMaxLives() * healPercent));
            p.heal(heal);
        }
    }

    @Override
    public void draw(SpriteBatch batch, ShapeRenderer sr, Player player) {
        if (phase != Phase.AIMING) return;
        sr.begin(ShapeRenderer.ShapeType.Line);
        float alpha = 0.5f + 0.5f * (float) Math.sin(phaseTimer * 5f);
        sr.setColor(0.5f, 0f, 1f, alpha);
        sr.circle((aoeCenterX + 0.5f) * GameConstants.CELL_SIZE, (aoeCenterY + 0.5f) * GameConstants.CELL_SIZE, aoeVisualRadius);
        sr.end();
    }

    @Override
    protected void onUpgrade() {
        switch (level) {
            case 2 -> {
                aoeTileRadius += 1;
                aoeVisualRadius = (aoeTileRadius + 0.5f) * GameConstants.CELL_SIZE;
            }
            case 3 -> baseHealPercent += 0.05f;
            case 4 -> extraPerEnemy += 0.02f;
            case 5 -> {
                aoeTileRadius += 1;
                aoeVisualRadius = (aoeTileRadius + 0.5f) * GameConstants.CELL_SIZE;
                currentCooldown = 3.0f;
            }
        }
    }

    @Override
    public String getId() { return "magic"; }

    @Override
    public AbilityInputType getInputType() { return AbilityInputType.INSTANT; }

    @Override
    public Map<String, Object> saveState() {
        Map<String, Object> m = super.saveState();
        m.put("phase", phase.name());
        return m;
    }

    @Override
    public void loadState(Map<String, Object> m) {
        super.loadState(m);
        phase = Phase.valueOf((String) m.getOrDefault("phase", Phase.IDLE.name()));
    }
}