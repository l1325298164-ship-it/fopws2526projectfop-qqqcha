package de.tum.cit.fop.maze.entities.boss.config;

import de.tum.cit.fop.maze.game.Difficulty;
import de.tum.cit.fop.maze.game.DifficultyConfig;

public class BossDifficultyFactory {

    public static DifficultyConfig create(
            BossMazeConfig.Base base,
            BossMazeConfig.Phase phase
    ) {
        return new DifficultyConfig(
                Difficulty.BOSS,
                phase.mazeWidth,
                phase.mazeHeight,
                base.exitCount,

                phase.enemies.getOrDefault("E01_PEARL", 0),
                phase.enemies.getOrDefault("E02_COFFEE", 0),
                phase.enemies.getOrDefault("E03_CARAMEL", 0),
                0,

                phase.traps.getOrDefault("T01_GEYSER", 0),
                phase.traps.getOrDefault("T02_PEARL_MINE", 0),
                phase.traps.getOrDefault("T03_TEA_SHARD", 0),
                phase.traps.getOrDefault("T04_MUD_TILE", 0),

                base.initialLives,
                base.enemyHpMultiplier,
                base.enemyDamageMultiplier,
                base.keyCount
        );
    }
}
