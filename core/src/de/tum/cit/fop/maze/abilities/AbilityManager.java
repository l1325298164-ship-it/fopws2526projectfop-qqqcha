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
        if (player.getPlayerIndex() == Player.PlayerIndex.P1) {
            MeleeAttackAbility melee = new MeleeAttackAbility();
            abilities.put("melee", melee);   // ← 加这一行
            abilitySlots[0] = melee;
        }else {
            MagicAbility magic = new MagicAbility();
            abilities.put("magic", magic);
            abilitySlots[0] = magic;
        }
    }

    public void update(float deltaTime) {
        // 更新所有能力
        for (Ability slotAbility : abilitySlots) {
            if (slotAbility != null) {
                slotAbility.update(deltaTime);
            }
        }

        // 更新激活的能力列表
        activeAbilities.clear();
        for (Ability ability : abilities.values()) {
            if (ability.isActive()) {
                activeAbilities.add(ability);
            }
        }
    }



    public boolean activateSlot(int slot) {
        if (slot < 0 || slot >= abilitySlots.length) return false;

        Ability ability = abilitySlots[slot];
        if (ability == null) return false;

        boolean activated = ability.tryActivate(player, gameManager);

        // 如果激活成功，添加到 activeAbilities
        if (activated && ability.isActive()) {
            if (!activeAbilities.contains(ability)) {
                activeAbilities.add(ability);
            }
        }

        return activated;
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

    public void reset() {
        for (Ability ability : abilities.values()) {
            ability.forceReset();
        }
        activeAbilities.clear();
    }



    public Ability getAbility(int slot) {
        if (slot >= 0 && slot < abilitySlots.length) {
            return abilitySlots[slot];
        }
        return null;
    }

    public void activateAbility(int slot, Player player) {
        activateSlot(slot);
    }

    public void drawAbilities(SpriteBatch batch,
                              ShapeRenderer shapeRenderer,
                              Player player) {
        for (Ability ability : abilities.values()) {
            ability.draw(batch, shapeRenderer, player);
        }
    }


}