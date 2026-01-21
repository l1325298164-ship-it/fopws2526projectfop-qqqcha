package de.tum.cit.fop.maze.abilities;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import de.tum.cit.fop.maze.entities.Player;
import de.tum.cit.fop.maze.entities.enemy.Enemy;
import de.tum.cit.fop.maze.game.GameConstants;
import de.tum.cit.fop.maze.game.GameManager;
import com.badlogic.gdx.graphics.Color;
import de.tum.cit.fop.maze.utils.Logger; // 导入 Logger

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

    public Phase getPhase() { return phase; }
    public float getPhaseTime() { return phaseTimer; }

    @Override
    protected boolean shouldConsumeMana() { return phase == Phase.AIMING; }

    @Override
    protected boolean shouldStartCooldown() { return phase == Phase.COOLDOWN; }

    @Override
    protected float getCooldownDuration() { return currentCooldown; }

    @Override
    public boolean canActivate(Player player) {
        if (phase == Phase.COOLDOWN || phase == Phase.EXECUTED) return false;
        return player.getMana() >= manaCost;
    }

    @Override
    protected void onActivate(Player player, GameManager gm) {
        // 🔥 [修复] 移除这里的 isUIConsumingMouse 强硬检查
        // 防止因为 UI 遮挡导致无法进入任何状态

        if (inputConsumedThisFrame) return;
        inputConsumedThisFrame = true;
        this.gameManager = gm;

        switch (phase) {
            case IDLE -> {
                // 🔥 [修复] 智能瞄准：如果鼠标无效(如键盘玩家)，默认瞄准自己脚下
                int mx = gm.getMouseTileX();
                int my = gm.getMouseTileY();

                if (mx < 0 || my < 0) {
                    aoeCenterX = player.getX();
                    aoeCenterY = player.getY();
                } else {
                    aoeCenterX = mx;
                    aoeCenterY = my;
                }

                aimingTimer = 0f;
                setPhase(Phase.AIMING);
                Logger.debug("MagicAbility: Aiming Started at " + aoeCenterX + "," + aoeCenterY);
            }
            case AIMING -> {
                if (phaseTimer < 0.1f) return; // 防抖动

                // 再次确认是否有致命 UI 遮挡 (可选，这里放宽限制以确保能放出来)
                if (gm.isUIConsumingMouse()) {
                    Logger.debug("MagicAbility: Fire blocked by UI");
                    return;
                }

                castAOE(gm);
                effectWaitTimer = 0f;
                setPhase(Phase.EXECUTED);
                Logger.debug("MagicAbility: FIRED!");
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

                // 🔥 [修复] 只在鼠标有效且未被 UI 遮挡时更新瞄准位置
                // 这样如果不动鼠标，光标会停留在上一次有效位置，而不是跳到 0,0
                if (!gm.isUIConsumingMouse()) {
                    int mx = gm.getMouseTileX();
                    int my = gm.getMouseTileY();
                    if (mx >= 0 && my >= 0) {
                        aoeCenterX = mx;
                        aoeCenterY = my;
                    }
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

        // 🔥 [Debug] 确保特效管理器存在
        if (gm.getCombatEffectManager() != null) {
            float cx = (aoeCenterX + 0.5f) * GameConstants.CELL_SIZE;
            float cy = (aoeCenterY + 0.5f) * GameConstants.CELL_SIZE;

            gm.getCombatEffectManager().spawnMagicCircle(cx, cy, aoeVisualRadius, 0.5f);
            gm.getCombatEffectManager().spawnMagicPillar(cx, cy, aoeVisualRadius);

            if (de.tum.cit.fop.maze.utils.CameraManager.getInstance() != null) {
                de.tum.cit.fop.maze.utils.CameraManager.getInstance().shake(0.2f, 3.0f);
            }
        } else {
            Logger.error("MagicAbility: CombatEffectManager is NULL!");
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
        // 绘制瞄准圈
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