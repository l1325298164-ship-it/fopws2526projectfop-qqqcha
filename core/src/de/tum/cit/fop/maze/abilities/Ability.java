// Ability.java
package de.tum.cit.fop.maze.abilities;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import de.tum.cit.fop.maze.entities.Player;
import de.tum.cit.fop.maze.game.GameManager;

public abstract class Ability {

    protected final String name;
    protected final String description;

    protected final float cooldown;
    protected final float duration;

    protected boolean active = false;
    protected boolean ready = true;

    protected float cooldownTimer = 0f;
    protected float durationTimer = 0f;

    protected int manaCost = 0;
    protected int level = 1;
    protected int maxLevel = 5;

    protected Ability(String name, String description, float cooldown, float duration) {
        this.name = name;
        this.description = description;
        this.cooldown = cooldown;
        this.duration = duration;
    }

    /* =================== 核心生命周期 =================== */

    public final boolean tryActivate(Player player, GameManager gameManager) {
        if (!canActivate(player)) return false;

        ready = false;
        active = true;
        cooldownTimer = 0f;
        durationTimer = 0f;

        if (manaCost > 0) {
            player.useMana(manaCost);
        }

        onActivate(player, gameManager);
        return true;
    }

    public void update(float delta) {

        // Active 阶段
        if (active) {
            durationTimer += delta;
            updateActive(delta);

            if (durationTimer >= duration) {
                active = false;
                durationTimer = 0f;
                onDeactivate();
            }
        }

        // 冷却阶段
        if (!ready) {
            cooldownTimer += delta;
            if (cooldownTimer >= cooldown) {
                ready = true;
                cooldownTimer = cooldown;
            }
        }
    }

    /* =================== 子类实现 =================== */

    protected abstract void onActivate(Player player, GameManager gameManager);

    protected void updateActive(float delta) {}

    protected void onDeactivate() {}

    protected abstract void onUpgrade();

    public abstract void draw(SpriteBatch batch, ShapeRenderer shapeRenderer, Player player);

    /* =================== 状态 =================== */

    public boolean canActivate(Player player) {
        return ready && player.getMana() >= manaCost;
    }

    public boolean isActive() { return active; }

    public boolean isReady() { return ready; }

    public float getCooldownProgress() {
        return cooldown <= 0 ? 1f : cooldownTimer / cooldown;
    }

    public float getDurationProgress() {
        return duration <= 0 ? 0f : durationTimer / duration;
    }

    public String getName() { return name; }

    public int getLevel() { return level; }

    public void upgrade() {
        if (level < maxLevel) {
            level++;
            onUpgrade();
        }
    }

    public void forceReset() {
        active = false;
        ready = true;
        cooldownTimer = 0f;
        durationTimer = 0f;
    }
}
