package de.tum.cit.fop.maze.abilities;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import de.tum.cit.fop.maze.entities.Player;
import de.tum.cit.fop.maze.game.GameManager;

public class DashAbility extends Ability {

    private static final int MAX_CHARGES = 2;
    private static final float CHARGE_COOLDOWN = 2f;

    private int charges = MAX_CHARGES;
    private float chargeTimer = 0f;

    public DashAbility() {
        // cooldown = 0（不用），duration = Dash 持续时间
        super("Dash", "Quick dash forward", 0f, 0.8f);
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

    @Override
    protected void onActivate(Player player, GameManager gameManager) {
        charges--;
        player.startDash();
    }

    /* ================= Active ================= */

    @Override
    protected void updateActive(float delta) {
        // Dash 的位移 / 碰撞都在 Player 里处理
    }

    @Override
    protected void onDeactivate() {
        // Dash 时间结束（如果你 Player 里需要回调，可以以后加）
    }

    /* ================= Update ================= */

    @Override
    public void update(float delta) {
        super.update(delta);

        // 充能恢复
        if (charges < MAX_CHARGES) {
            chargeTimer += delta;
            if (chargeTimer >= CHARGE_COOLDOWN) {
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
        // 以后可以做：+1 charge / cooldown -x
    }

    /* ================= HUD Getter ================= */

    public int getCurrentCharges() {
        return charges;
    }

    public int getMaxCharges() {
        return MAX_CHARGES;
    }

    public float getChargeProgress() {
        if (charges >= MAX_CHARGES) return 1f;
        return chargeTimer / CHARGE_COOLDOWN;
    }
}
