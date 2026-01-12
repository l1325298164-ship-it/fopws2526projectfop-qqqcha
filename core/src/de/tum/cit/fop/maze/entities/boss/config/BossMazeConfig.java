package de.tum.cit.fop.maze.entities.boss.config;

import com.badlogic.gdx.scenes.scene2d.ui.List;

import java.util.Map;

public class BossMazeConfig {

    public Base base;
    public List<Phase> phases;

    public static class Base {
        public int bossMaxHp;
        public float enemyHpMultiplier;
        public float enemyDamageMultiplier;
        public int initialLives;
        public int exitCount;
        public int keyCount;
    }

    public static class Phase {
        public float time;
        public int mazeWidth;
        public int mazeHeight;

        // ✅ 就写在这里
        public Map<String, Integer> enemies;
        public Map<String, Integer> traps;
    }
}
