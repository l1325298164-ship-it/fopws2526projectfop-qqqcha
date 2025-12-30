// Ability.java
package de.tum.cit.fop.maze.abilities;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import de.tum.cit.fop.maze.entities.Player;
import de.tum.cit.fop.maze.game.GameManager;

import java.util.List;

//使用方式
//        初始化：Player构造函数中初始化AbilityManager
//
//        更新：在Player.update()中调用abilityManager.update()
//
//        绘制：在游戏绘制阶段调用abilityManager.draw()
//
//        使用能力：按数字键1-4或空格键使用能力
//
//        升级能力：通过游戏内商店或升级系统调用upgradeAbility()
//
//        这个设计具有以下优点：
//
//        可扩展：通过继承Ability类可以轻松添加新能力
//
//        可升级：每级都有不同的效果增强
//
//        可配置：冷却时间、伤害、范围等都可以配置
//
//        可视化：有动画和视觉效果
//
//        模块化：能力逻辑与玩家逻辑分离

public abstract class Ability {
    protected String name;
    protected String description;
    protected boolean isActive = false;
    protected boolean isReady = true;
    protected float cooldown;
    protected float currentCooldown = 0;
    protected float duration;
    protected float currentDuration = 0;
    protected int manaCost = 0;
    protected int level = 1;
    protected int maxLevel = 5;

    // 升级数据
    protected int upgradeDamage = 0;
    protected float upgradeCooldown = 0;
    protected float upgradeRange = 0;
    protected float upgradeDuration = 0;

    public Ability(String name, String description, float cooldown, float duration) {
        this.name = name;
        this.description = description;
        this.cooldown = cooldown;
        this.duration = duration;
    }

    public abstract void activate(Player player, GameManager gameManager);

    public abstract void update(float deltaTime);

    public abstract void draw(SpriteBatch batch, ShapeRenderer shapeRenderer, Player player);

    public void upgrade() {
        if (level < maxLevel) {
            level++;
            onUpgrade();
        }
    }

    protected abstract void onUpgrade();

    public boolean canActivate(Player player) {
        return isReady && player.getMana() >= manaCost;
    }

    public String getStatus() {
        if (!isReady) {
            return String.format("%.1f", currentCooldown);
        }
        return "Ready";
    }

    // Getters
    public String getName() { return name; }
    public String getDescription() { return description; }
    public int getLevel() { return level; }
    public int getMaxLevel() { return maxLevel; }
    public boolean isActive() { return isActive; }
    public boolean isReady() { return isReady; }
    public float getCooldownPercent() {
        return Math.min(1.0f, currentCooldown / cooldown);
    }
}