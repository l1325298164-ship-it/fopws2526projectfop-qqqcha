// Ability.java
package de.tum.cit.fop.maze.abilities;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import de.tum.cit.fop.maze.entities.Player;
import de.tum.cit.fop.maze.game.GameManager;

public abstract class Ability {
    public enum AbilityInputType {
        INSTANT,    // 一按就触发（P1、Dash）
        CONTINUOUS  // 按/持/放（Magic）
    }
    public AbilityInputType getInputType() {
        return AbilityInputType.INSTANT;
    }
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

    public boolean tryActivate(Player player, GameManager gameManager) {
        if (!canActivate(player)) return false;

        if (shouldConsumeMana() && manaCost > 0) {
            player.useMana(manaCost);
        }

        onActivate(player, gameManager);
//立刻进冷却或者立刻按
        if (shouldStartCooldown()) {
            ready = false;
            cooldownTimer = 0f;
        }


        if (shouldBecomeActive()) {
            active = true;
            durationTimer = 0f;
        }

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

        if (!ready) {
            cooldownTimer += delta;
            if (cooldownTimer >= getCooldownDuration()) {
                ready = true;
                cooldownTimer = getCooldownDuration();
            }
        }
    }

    /* =================== 子类实现 =================== */

    protected boolean shouldBecomeActive() {
        return duration > 0;
    }
// 是否在 tryActivate 时消耗 mana
    protected boolean shouldConsumeMana() {
        return true;
    }

    // 是否在 tryActivate 后立刻进入冷却
    protected boolean shouldStartCooldown() {
        return true;
    }

    // 实际使用的冷却时间（允许动态）
    protected float getCooldownDuration() {
        return cooldown;
    }

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
        float cd = getCooldownDuration();
        return cd <= 0 ? 1f : cooldownTimer / cd;
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

    public int getManaCost() { return manaCost; }

    // 可能还需要这些
    public String getDescription() { return description; }
    public float getCooldown() { return cooldown; }
    public float getDuration() { return duration; }
}
