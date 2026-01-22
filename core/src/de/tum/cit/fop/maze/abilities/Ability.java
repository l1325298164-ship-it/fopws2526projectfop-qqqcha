package de.tum.cit.fop.maze.abilities;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import de.tum.cit.fop.maze.audio.AudioManager;
import de.tum.cit.fop.maze.audio.AudioType;
import de.tum.cit.fop.maze.entities.Player;
import de.tum.cit.fop.maze.game.GameManager;

import java.util.HashMap;
import java.util.Map;

public abstract class Ability {

    public enum AbilityInputType {
        INSTANT,
        CONTINUOUS
    }

    public abstract String getId();

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

    /* ================= 输入入口 ================= */

    public boolean activate(Player player, GameManager gameManager) {
        return tryActivate(player, gameManager);
    }

    /* ================= 生命周期 ================= */

    protected boolean tryActivate(Player player, GameManager gameManager) {
        if (!canActivate(player)) return false;

        if (shouldConsumeMana() && manaCost > 0) {
            player.useMana(manaCost);
        }

        onActivate(player, gameManager);

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

    public void update(float delta, Player player, GameManager gameManager) {

        if (active) {
            durationTimer += delta;
            updateActive(delta, player, gameManager);

            if (durationTimer >= duration) {
                active = false;
                durationTimer = 0f;
                onDeactivate(player, gameManager);
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

    /* ================= 子类钩子 ================= */

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

    protected void updateActive(float delta, Player player, GameManager gameManager) {}

    protected void onDeactivate(Player player, GameManager gameManager) {}

    protected abstract void onUpgrade();

    public abstract void draw(SpriteBatch batch, ShapeRenderer shapeRenderer, Player player);

    /* ================= 状态 ================= */

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
            playUpgradeSound();
        }
    }

    protected void playUpgradeSound() {
        AudioManager.getInstance().play(getUpgradeSoundForLevel(level));
    }

    protected AudioType getUpgradeSoundForLevel(int level) {
        // 默认：通用升级音效
        return AudioType.ABILITY_UPGRADE_COMMON;
    }


    public boolean canUpgrade() {
        return level < maxLevel;
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

    public Map<String, Object> saveState() {
        Map<String, Object> m = new HashMap<>();
        m.put("level", level);
        m.put("ready", ready);
        m.put("active", active);
        m.put("cooldownTimer", cooldownTimer);
        m.put("durationTimer", durationTimer);
        return m;
    }

    public void loadState(Map<String, Object> m) {
        if (m == null) return;

        // 安全的类型转换
        Object levelObj = m.get("level");
        if (levelObj instanceof Number) {
            level = ((Number) levelObj).intValue();
        }
        
        Object readyObj = m.get("ready");
        if (readyObj instanceof Boolean) {
            ready = (Boolean) readyObj;
        }
        
        Object activeObj = m.get("active");
        if (activeObj instanceof Boolean) {
            active = (Boolean) activeObj;
        }
        
        Object cooldownObj = m.get("cooldownTimer");
        if (cooldownObj instanceof Number) {
            cooldownTimer = ((Number) cooldownObj).floatValue();
        }
        
        Object durationObj = m.get("durationTimer");
        if (durationObj instanceof Number) {
            durationTimer = ((Number) durationObj).floatValue();
        }
    }
}
