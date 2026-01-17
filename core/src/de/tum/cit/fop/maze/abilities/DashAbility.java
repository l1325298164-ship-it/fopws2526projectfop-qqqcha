package de.tum.cit.fop.maze.abilities;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import de.tum.cit.fop.maze.entities.Player;
import de.tum.cit.fop.maze.game.GameManager;
import de.tum.cit.fop.maze.audio.AudioManager;
import de.tum.cit.fop.maze.audio.AudioType;
import de.tum.cit.fop.maze.game.GameConstants;

import java.util.Map;

public class DashAbility extends Ability {

    private int maxCharges = 2;
    private float chargeCooldown = 2.0f;
    private float dashDuration = 0.8f;
    private float invincibleBonus = 0f;

    private int charges = maxCharges;
    private float chargeTimer = 0f;

    public DashAbility() {
        // cooldown = 0（不用），duration = Dash 持续时间
        super("Dash", "Quick dash forward", 0f, 0.8f);
        this.dashDuration = 0.8f;
    }

    /* ================= Ability Hooks ================= */

    @Override
    protected boolean shouldConsumeMana() {
        return false; // Dash 不耗蓝
    }

    @Override
    protected boolean shouldStartCooldown() {
        return false; // ❌ 不使用 Ability 的冷却系统
    }

    @Override
    protected boolean shouldBecomeActive() {
        return true; // Dash 是持续技能
    }

    @Override
    public boolean canActivate(Player player) {
        return charges > 0 && !player.isDashing();
    }


    /* ================= Activate ================= */

//    @Override
//    protected void onActivate(Player player, GameManager gameManager) {
//        charges--;
//        player.startDash(dashDuration, invincibleBonus);
//    }

    @Override
    protected void onActivate(Player player, GameManager gameManager) {
        charges--;

        // ✅ 1. 播放音效
        AudioManager.getInstance().play(AudioType.SKILL_DASH);

        // ✅ 2. 播放冲刺特效
        // 根据玩家朝向计算特效角度 (0=右, 90=上, 180=左, 270=下)
        float angle = 0f;
        switch (player.getDirection()) {
            case RIGHT -> angle = 0f;
            case UP    -> angle = 90f;
            case LEFT  -> angle = 180f;
            case DOWN  -> angle = 270f;
        }

        if (gameManager.getCombatEffectManager() != null) {
            gameManager.getCombatEffectManager().spawnDash(
                    player.getWorldX() * GameConstants.CELL_SIZE,
                    player.getWorldY() * GameConstants.CELL_SIZE,
                    angle
            );
        }

        // 3. 执行原有逻辑
        player.startDash(dashDuration, invincibleBonus);
    }
    /* ================= Active ================= */

    protected void updateActive(float delta) {
        // Dash 的位移 / 碰撞都在 Player 里处理
    }

    protected void onDeactivate() {
        // Dash 时间结束（如果你 Player 里需要回调，可以以后加）
    }

    /* ================= Update ================= */

    @Override
    public void update(float delta, Player player, GameManager gameManager) {
        super.update(delta, player, gameManager);

        if (charges < maxCharges) {
            chargeTimer += delta;
            if (chargeTimer >= chargeCooldown) {
                charges++;
                chargeTimer = 0f;
            }
        }
    }

    /* ================= Render ================= */

    @Override
    public void draw(SpriteBatch batch, ShapeRenderer shapeRenderer, Player player) {
        // Dash 不需要绘制额外效果
    }

    /* ================= Upgrade ================= */

    @Override
    protected void onUpgrade() {
        switch (level) {
            case 2 -> {
                // Lv2：回充更快
                chargeCooldown = 1.6f;
            }
            case 3 -> {
                // Lv3：+1 充能
                maxCharges = 3;
                charges = Math.min(charges + 1, maxCharges);
            }
            case 4 -> {
                // Lv4：Dash 更持久
                dashDuration = 1.0f;
            }
            case 5 -> {
                invincibleBonus = 0.2f;
            }
        }
    }


    /* ================= HUD Getter ================= */

    public int getCurrentCharges() {
        return charges;
    }

    public int getMaxCharges() {
        return maxCharges;
    }

    public float getChargeProgress() {
        if (charges >= maxCharges) return 1f;
        return chargeTimer / chargeCooldown;
    }
    @Override
    public String getId() {
        return "dash";
    }
    @Override
    public Map<String, Object> saveState() {
        Map<String, Object> m = super.saveState();
        m.put("charges", charges);
        m.put("chargeTimer", chargeTimer);
        return m;
    }

    @Override
    public void loadState(Map<String, Object> m) {
        super.loadState(m);
        charges = (int) m.getOrDefault("charges", maxCharges);
        chargeTimer = ((Number)m.getOrDefault("chargeTimer", 0f)).floatValue();
    }

}
