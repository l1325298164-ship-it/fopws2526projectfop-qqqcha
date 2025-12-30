// Ability.java
package de.tum.cit.fop.maze.abilities;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import de.tum.cit.fop.maze.entities.Player;
import de.tum.cit.fop.maze.game.GameManager;

public abstract class Ability {

    /* ===== 基本信息 ===== */
    protected final String name;
    protected final String description;

    /* ===== 状态 ===== */
    protected boolean active = false;
    protected boolean ready = true;

    /* ===== 冷却 / 持续 ===== */
    protected final float cooldown;
    protected float cooldownTimer = 0f;

    protected final float duration;
    protected float durationTimer = 0f;

    /* ===== 成本 / 等级 ===== */
    protected int manaCost = 0;
    protected int level = 1;
    protected int maxLevel = 5;

    protected Ability(String name, String description, float cooldown, float duration) {
        this.name = name;
        this.description = description;
        this.cooldown = cooldown;
        this.duration = duration;
    }

    /* ===================================================== */
    /* ================== 生命周期模板 ===================== */
    /* ===================================================== */

    /** 外部统一调用 */
    public final void tryActivate(Player player, GameManager gameManager) {
        if (!canActivate(player)) return;

        ready = false;
        active = true;
        cooldownTimer = 0f;
        durationTimer = 0f;

        if (manaCost > 0) {
            player.useMana(manaCost);
        }

        onActivate(player, gameManager);
    }

    /** AbilityManager 每帧调用 */
    public void update(float delta) {

        // ===== Active 持续 =====
        if (active) {
            durationTimer += delta;
            updateActive(delta);

            if (durationTimer >= duration) {
                active = false;
                durationTimer = 0f;
                onDeactivate();
            }
        }

        // ===== 冷却 =====
        if (!ready) {
            cooldownTimer += delta;
            if (cooldownTimer >= cooldown) {
                ready = true;
                cooldownTimer = cooldown;
            }
        }
    }

    /* ===================================================== */
    /* ================== 子类只实现这些 =================== */
    /* ===================================================== */

    protected abstract void onActivate(Player player, GameManager gameManager);

    protected void updateActive(float delta) {
        // 默认什么都不做（Dash 可以覆盖）
    }

    protected void onDeactivate() {
        // 默认什么都不做（Dash 可以覆盖）
    }

    protected abstract void onUpgrade();

    public abstract void draw(SpriteBatch batch, ShapeRenderer shapeRenderer, Player player);

    /* ===================================================== */
    /* ================== 状态判断 ========================= */
    /* ===================================================== */

    public boolean canActivate(Player player) {
        return ready && player.getMana() >= manaCost;
    }

    public boolean isActive() {
        return active;
    }

    public boolean isReady() {
        return ready;
    }

    /* ===================================================== */
    /* ================== UI / HUD 读取 ==================== */
    /* ===================================================== */

    public float getCooldownProgress() {
        if (cooldown <= 0) return 1f;
        return Math.min(1f, cooldownTimer / cooldown);
    }

    public float getDurationProgress() {
        if (duration <= 0) return 0f;
        return Math.min(1f, durationTimer / duration);
    }

    /* ===================================================== */
    /* ================== 基本 Getter ====================== */
    /* ===================================================== */

    public String getName() { return name; }
    public String getDescription() { return description; }
    public int getLevel() { return level; }
    public int getMaxLevel() { return maxLevel; }

    public void upgrade() {
        if (level < maxLevel) {
            level++;
            onUpgrade();
        }
    }
}
