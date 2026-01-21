package de.tum.cit.fop.maze.abilities;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import de.tum.cit.fop.maze.entities.Player;
import de.tum.cit.fop.maze.game.GameManager;
import de.tum.cit.fop.maze.utils.Logger;

import java.util.*;

public class AbilityManager {

    private final Map<String, Ability> abilities = new HashMap<>();
    private final List<Ability> activeAbilities = new ArrayList<>();
    private final Ability[] abilitySlots = new Ability[4];

    private final Player player;
    private final GameManager gameManager;

    public AbilityManager(Player player, GameManager gameManager) {
        if (player == null) {
            throw new IllegalArgumentException("Player cannot be null");
        }
        if (gameManager == null) {
            throw new IllegalArgumentException("GameManager cannot be null");
        }
        this.player = player;
        this.gameManager = gameManager;
        initializeAbilities();
    }

    private void initializeAbilities() {
        if (player.getPlayerIndex() == Player.PlayerIndex.P1) {
            register(new MeleeAttackAbility(), 0);
            register(new DashAbility(), 1);
        } else {
            register(new MagicAbility(), 0);
            register(new DashAbility(), 1);
        }
    }

    private void register(Ability ability, int slot) {
        abilities.put(ability.getId(), ability);
        if (slot >= 0 && slot < abilitySlots.length) {
            abilitySlots[slot] = ability;
        }
    }

    public void update(float delta) {
        for (Ability ability : abilitySlots) {
            if (ability != null) {
                ability.update(delta, player, gameManager);
            }
        }

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

        boolean activated = ability.activate(player, gameManager);

        if (activated && ability.isActive() && !activeAbilities.contains(ability)) {
            activeAbilities.add(ability);
        }
        return activated;
    }

    public void upgradeAbility(String abilityId) {
        Ability ability = abilities.get(abilityId);
        if (ability != null) ability.upgrade();
    }

    public void equipAbility(String abilityId, int slot) {
        if (slot < 0 || slot >= abilitySlots.length) return;
        Ability ability = abilities.get(abilityId);
        if (ability == null) {
            Logger.warning("Cannot equip ability: " + abilityId + " not found");
            return;
        }
        abilitySlots[slot] = ability;
    }

    public void reset() {
        for (Ability ability : abilities.values()) {
            ability.forceReset();
        }
        activeAbilities.clear();
    }

    public void drawAbilities(SpriteBatch batch, ShapeRenderer sr, Player player) {
        if (batch == null || sr == null || player == null) {
            Logger.warning("drawAbilities called with null parameters");
            return;
        }
        for (Ability ability : abilities.values()) {
            ability.draw(batch, sr, player);
        }
    }

    public Ability getAbilityInSlot(int slot) {
        if (slot < 0 || slot >= abilitySlots.length) return null;
        return abilitySlots[slot];
    }

    public Map<String, Ability> getAbilities() { return abilities; }
    public List<Ability> getActiveAbilities() { return activeAbilities; }
    public Ability[] getAbilitySlots() { return abilitySlots; }
}
