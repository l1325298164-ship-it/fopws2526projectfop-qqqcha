package de.tum.cit.fop.maze.entities.boss.config;

import com.badlogic.gdx.utils.ObjectMap;
import de.tum.cit.fop.maze.game.Difficulty;
import de.tum.cit.fop.maze.game.DifficultyConfig;

public class BossDifficultyFactory {
    private static int getCount(ObjectMap<String, Integer> map, String key) {
        if (map == null) return 0;
        return map.containsKey(key) ? map.get(key) : 0;
    }
    public static DifficultyConfig create(
            BossMazeConfig.Base base,
            BossMazeConfig.Phase phase
    ) {
        return new DifficultyConfig(
                Difficulty.BOSS,
                phase.mazeWidth,
                phase.mazeHeight,
                base.exitCount,

                getCount(phase.enemies, "E01_PEARL"),
                getCount(phase.enemies, "E02_COFFEE"),
                getCount(phase.enemies, "E03_CARAMEL"),
                0,

                getCount(phase.traps, "T01_GEYSER"),
                getCount(phase.traps, "T02_PEARL_MINE"),
                getCount(phase.traps, "T03_TEA_SHARD"),
                getCount(phase.traps, "T04_MUD_TILE"),

                base.initialLives,
                base.enemyHpMultiplier,
                base.enemyDamageMultiplier,
                base.keyCount
        );

    }
}
