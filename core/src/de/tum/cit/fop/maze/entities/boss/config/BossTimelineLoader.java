package de.tum.cit.fop.maze.entities.boss.config;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.Json;

public class BossTimelineLoader {

    public static BossTimeline load(String path) {
        return new Json().fromJson(
                BossTimeline.class,
                Gdx.files.internal(path)
        );
    }
}
