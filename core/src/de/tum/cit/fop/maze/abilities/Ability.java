package de.tum.cit.fop.maze.abilities;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import de.tum.cit.fop.maze.entities.Player;
import de.tum.cit.fop.maze.game.GameManager;

public abstract class Ability {

    public enum AbilityInputType {
        INSTANT,    // 一按就触发（P1、Dash）
        CONTINUOUS  // 多阶段 / 状态机（Magic）
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

    /* =================== 对外统一入口（新增） =================== */

    /**
     * 👉 推荐：所有输入系统 / Screen / Controller 都只调用这个方法
     */
    public boolean activate(Player player, GameManager gameManager) {
        return tryActivate(player, gameManager);
    }

    /* =================== 核心生命周期 =================== */

    protected boolean tryActivate(Player player, GameManager gameManager) {
        if (!canActivate(player)) return false;

        if (shouldConsumeMana() && manaCost > 0) {
            player.useMana(manaCost);
        }

        onActivate(player, gameManager);

        // 是否立刻进入冷却
        if (shouldStartCooldown()) {
            ready = false;
            cooldownTimer = 0f;
        }

        // 是否进入 active/duration 阶段
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

        // 冷却
        if (!ready) {
            cooldownTimer += delta;
            if (cooldownTimer >= getCooldownDuration()) {
                ready = true;
                cooldownTimer = getCooldownDuration();
            }
        }
    }

    /* =================== 子类钩子 =================== */

    protected boolean shouldBecomeActive() {
        return duration > 0;
    }

    protected boolean shouldConsumeMana() {
        return true;
    }

    protected boolean shouldStartCooldown() {
        return true;
    }

    protected float getCooldownDuration() {
        return cooldown;
    }

    protected abstract void onActivate(Player player, GameManager gameManager);

    protected void updateActive(float delta) {}

    protected void onDeactivate() {}

    protected abstract void onUpgrade();

    public abstract void draw(SpriteBatch batch, ShapeRenderer shapeRenderer, Player player);

    /* =================== 状态查询 =================== */

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
    public void setLevel(int level) {
        this.level = level;
    }
    public void forceReset() {
        active = false;
        ready = true;
        cooldownTimer = 0f;
        durationTimer = 0f;
    }

    public int getManaCost() { return manaCost; }

    public String getDescription() { return description; }

    public float getCooldown() { return cooldown; }

    public float getDuration() { return duration; }
}
