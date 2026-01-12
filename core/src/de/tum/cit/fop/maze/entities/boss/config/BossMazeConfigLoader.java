package de.tum.cit.fop.maze.entities.boss.config;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.Json;

public class BossMazeConfigLoader {
    public static BossMazeConfig load(String path) {
        Json json = new Json();
        return json.fromJson(
                BossMazeConfig.class,
                Gdx.files.internal(path)
        );
    }
}
