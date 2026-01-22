package de.tum.cit.fop.maze.game.save;

import java.util.HashMap;
import java.util.Map;

public class PlayerSaveData {

    public int x;
    public int y;

    public int lives;
    public int maxLives;
    public int mana;

    public boolean hasKey;

    // Buff
    public boolean buffAttack;
    public boolean buffRegen;
    public boolean buffManaEfficiency;
    public Map<String, Map<String, Object>> abilityStates = new HashMap<>();

    // 技能等级
    public Map<String, Integer> abilityLevels = new HashMap<>();
}
