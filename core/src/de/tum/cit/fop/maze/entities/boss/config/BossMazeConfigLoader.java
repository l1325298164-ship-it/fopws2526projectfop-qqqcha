package de.tum.cit.fop.maze.entities.boss.config;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.Json;

public class BossMazeConfigLoader {

    public static BossMazeConfig loadOne(String path) {
        Json json = new Json();

        BossMazeConfig config = json.fromJson(
                BossMazeConfig.class,
                Gdx.files.internal(path)
        );

        // ⭐ 给每个 Phase 赋 index
        for (int i = 0; i <3; i++) {
            config.phases.get(i).index = i;
        }

        return config;
    }

}
