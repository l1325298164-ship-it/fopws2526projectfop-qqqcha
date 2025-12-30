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
        super("Dash", "Quick dash forward", 0f, 0.8f);
    }

    @Override
    public boolean canActivate(Player player) {
        return charges > 0;
    }

    @Override
    protected void onActivate(Player player, GameManager gameManager) {
        charges--;
        player.startDash();
    }

    @Override
    protected void updateActive(float delta) {
        // Dash 本身不需要逐帧逻辑
    }

    @Override
    protected void onDeactivate() {
        // Dash 结束
    }

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

    @Override
    public void draw(SpriteBatch batch, ShapeRenderer shapeRenderer, Player player) {}

    @Override
    protected void onUpgrade() {}

    public int getCharges() { return charges; }

    public int getMaxCharges() { return MAX_CHARGES; }

    public float getChargeProgress() {
        return chargeTimer / CHARGE_COOLDOWN;
    }
}
