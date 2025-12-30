// AbilityManager.java
package de.tum.cit.fop.maze.abilities;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import de.tum.cit.fop.maze.entities.Player;
import de.tum.cit.fop.maze.game.GameManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AbilityManager {
    private Map<String, Ability> abilities = new HashMap<>();
    private List<Ability> activeAbilities = new ArrayList<>();
    private Player player;
    private GameManager gameManager;

    // 能力槽位
    private Ability[] abilitySlots = new Ability[4]; // 4个能力槽

    public AbilityManager(Player player, GameManager gameManager) {
        this.player = player;
        this.gameManager = gameManager;
        initializeAbilities();
    }

    private void initializeAbilities() {
        // 初始能力：近战攻击
        MeleeAttackAbility meleeAttack = new MeleeAttackAbility();
        abilities.put("melee", meleeAttack);
        abilitySlots[0] = meleeAttack; // 放在第一个槽位

        // 可以在这里添加更多初始能力
        // abilities.put("dash", new DashAbility());
        // abilities.put("fireball", new FireballAbility());
        // abilities.put("shield", new ShieldAbility());
    }

    public void update(float deltaTime) {
        // 更新所有能力
        for (Ability ability : abilities.values()) {
            ability.update(deltaTime);
        }

        // 更新激活的能力
        activeAbilities.removeIf(ability -> !ability.isActive());
    }

    public void draw(SpriteBatch batch, ShapeRenderer shapeRenderer) {
        // 绘制所有激活的能力效果
        for (Ability ability : abilities.values()) {
            if (ability.isActive()) {
                ability.draw(batch, shapeRenderer, player);
            }
        }
    }

    public boolean activateAbility(String abilityId) {
        Ability ability = abilities.get(abilityId);
        if (ability != null && ability.canActivate(player)) {
            ability.activate(player, gameManager);
            if (ability.isActive()) {
                activeAbilities.add(ability);
            }
            return true;
        }
        return false;
    }

    public boolean activateSlot(int slot) {
        if (slot >= 0 && slot < abilitySlots.length) {
            Ability ability = abilitySlots[slot];
            if (ability != null) {
                return activateAbility(getAbilityId(ability));
            }
        }
        return false;
    }

    private String getAbilityId(Ability ability) {
        for (Map.Entry<String, Ability> entry : abilities.entrySet()) {
            if (entry.getValue() == ability) {
                return entry.getKey();
            }
        }
        return null;
    }

    public void upgradeAbility(String abilityId) {
        Ability ability = abilities.get(abilityId);
        if (ability != null) {
            ability.upgrade();
        }
    }

    public void unlockAbility(String abilityId, Ability ability) {
        if (!abilities.containsKey(abilityId)) {
            abilities.put(abilityId, ability);
            // 自动放入第一个空槽位
            for (int i = 0; i < abilitySlots.length; i++) {
                if (abilitySlots[i] == null) {
                    abilitySlots[i] = ability;
                    break;
                }
            }
        }
    }

    public void equipAbility(String abilityId, int slot) {
        if (slot >= 0 && slot < abilitySlots.length) {
            abilitySlots[slot] = abilities.get(abilityId);
        }
    }

    // Getters
    public Map<String, Ability> getAbilities() { return abilities; }
    public Ability[] getAbilitySlots() { return abilitySlots; }
    public List<Ability> getActiveAbilities() { return activeAbilities; }
}