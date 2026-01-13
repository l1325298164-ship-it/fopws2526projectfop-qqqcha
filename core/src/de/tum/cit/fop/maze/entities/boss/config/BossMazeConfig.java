package de.tum.cit.fop.maze.entities.boss.config;

import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ObjectMap;

public class BossMazeConfig {

    public Base base;
    public Array<Phase> phases;

    public static class Base {
        public int bossMaxHp;
        public float enemyHpMultiplier;
        public float enemyDamageMultiplier;
        public int initialLives;
        public int exitCount;
        public int keyCount;
        public float scoreMultiplies;
        public float damageMultiplies;
    }

    public static class Phase {
        public float duration;
        public int mazeWidth;
        public int mazeHeight;
        public ObjectMap<String, Integer> enemies;
        public ObjectMap<String, Integer> traps;
    }
}
