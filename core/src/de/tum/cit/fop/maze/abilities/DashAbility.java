package de.tum.cit.fop.maze.abilities;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import de.tum.cit.fop.maze.abilities.interfaces.ChargeStatus;
import de.tum.cit.fop.maze.abilities.interfaces.CooldownStatus;
import de.tum.cit.fop.maze.entities.Player;
import de.tum.cit.fop.maze.game.GameManager;

public class DashAbility extends Ability
        implements CooldownStatus, ChargeStatus {

    private static final int MAX_CHARGES = 2;
    private static final float COOLDOWN = 2f;

    private int charges = MAX_CHARGES;
    private float cooldownTimer = 0f;
    private float activeTimer = 0f;

    public DashAbility() {
        super("Dash", "Quick dash forward", COOLDOWN, 0.8f);
    }

    @Override
    public boolean canActivate(Player player) {
        return charges > 0;
    }


    public void activate(Player player, GameManager gameManager) {
        if (charges <= 0) return;

        charges--;
        cooldownTimer = 0f;
        activeTimer = 0f;

        // 👉 真正的效果给 Player
        player.startDash();

        setActive(true);
    }

    @Override
    public void update(float delta) {

        // ===== Dash 持续 =====
        if (isActive()) {
            activeTimer += delta;
            if (activeTimer >= getDuration()) {
                activeTimer = 0f;
                setActive(false);
            }
        }

        // ===== 冷却 / 充能 =====
        if (charges < MAX_CHARGES) {
            cooldownTimer += delta;
            if (cooldownTimer >= COOLDOWN) {
                charges++;
                cooldownTimer = 0f;
            }
        }
    }

    @Override
    protected void onActivate(Player player, GameManager gameManager) {

    }


    @Override
    public void draw(SpriteBatch batch, ShapeRenderer shapeRenderer, Player player) {
        // Dash 本体不需要单独绘制
    }

    @Override
    protected void onUpgrade() {
        // TODO：升级 dash（比如更多 charge / 更短 cooldown）
    }

    /* ===== 接口实现 ===== */

    @Override
    public int getCurrentCharges() {
        return charges;
    }

    @Override
    public int getMaxCharges() {
        return MAX_CHARGES;
    }

    @Override
    public float getCooldownProgress() {
        return cooldownTimer / COOLDOWN;
    }
}
