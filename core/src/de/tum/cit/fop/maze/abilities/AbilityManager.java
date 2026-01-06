package de.tum.cit.fop.maze.abilities;

import de.tum.cit.fop.maze.abilities.interfaces.*;
import de.tum.cit.fop.maze.entities.Player;
import de.tum.cit.fop.maze.game.GameConstants;
import de.tum.cit.fop.maze.game.GameManager;
import de.tum.cit.fop.maze.utils.Logger;

// 🔥 新增导入
import java.util.HashMap;
import java.util.Map;

public class AbilityManager {
    private final Player player;
    private final GameManager gameManager;
    private Ability[] abilities;

    public AbilityManager(Player player, GameManager gameManager) {
        this.player = player;
        this.gameManager = gameManager;
        this.abilities = new Ability[4];

        // 绑定默认技能
        abilities[0] = new MeleeAttackAbility();
        abilities[1] = new DashAbility();
        // abilities[2] = new FireballAbility(); // 示例
        // abilities[3] = new HealAbility();     // 示例
    }

    public void update(float delta) {
        for (Ability ability : abilities) {
            if (ability != null) ability.update(delta);
        }
    }

    public boolean activateSlot(int slot) {
        if (slot < 0 || slot >= abilities.length) return false;
        Ability ability = abilities[slot];
        if (ability == null) return false;

        // 简单的耗蓝检查示例 (具体逻辑看 Ability 内部实现)
        if (!(ability instanceof MeleeAttackAbility) && !(ability instanceof DashAbility)) {
            if (!player.useMana(20)) return false;
        }

        if (ability.canActivate(player)) {
            ability.activate(player, gameManager);

            // 触发特效
            playAbilityEffect(slot);

            Logger.debug("Used ability in slot " + slot);
            return true;
        }
        return false;
    }

    // 播放技能特效
    private void playAbilityEffect(int slot) {
        if (gameManager.getCombatEffectManager() == null) return;

        float px = player.getX() * GameConstants.CELL_SIZE + GameConstants.CELL_SIZE / 2f;
        float py = player.getY() * GameConstants.CELL_SIZE + GameConstants.CELL_SIZE / 2f;

        float angle = 0f;
        switch(player.getDirection()) {
            case RIGHT: angle = 0f; break;
            case UP:    angle = 90f; break;
            case LEFT:  angle = 180f; break;
            case DOWN:  angle = 270f; break;
        }

        switch (slot) {
            case 0: // 普攻 -> 挥砍
                gameManager.getCombatEffectManager().spawnSlash(px, py, angle, 1);
                break;
            case 1: // Dash
                // Dash -> 🔥 新增：生成冲刺气浪
                gameManager.getCombatEffectManager().spawnDash(px, py, angle);
                break;
            case 2: // 技能3 -> 模拟火球
                gameManager.getCombatEffectManager().spawnFire(px, py);
                break;
            case 3: // 技能4 -> 模拟治疗
                gameManager.getCombatEffectManager().spawnHeal(px, py);
                break;
        }
    }

    public void reset() {
        // 如果有状态重置逻辑写在这里
    }

    // 获取单个技能 (HUD调用)
    public Ability getAbility(int slot) {
        if (slot < 0 || slot >= abilities.length) return null;
        return abilities[slot];
    }

    /**
     * 🔥 修复的方法：获取所有技能的 Map
     * Key: Slot Index (Integer)
     * Value: Ability Object
     */
    public Map<Object, Object> getAbilities() {
        Map<Object, Object> map = new HashMap<>();
        for (int i = 0; i < abilities.length; i++) {
            if (abilities[i] != null) {
                map.put(i, abilities[i]);
            }
        }
        return map;
    }
}