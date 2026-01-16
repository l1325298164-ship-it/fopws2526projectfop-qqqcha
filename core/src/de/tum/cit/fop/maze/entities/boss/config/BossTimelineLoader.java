package de.tum.cit.fop.maze.entities.boss.config;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.Json;

public class BossTimelineLoader {

    public static BossTimeline load(String path) {
        Json json = new Json();

        BossTimeline timeline = json.fromJson(
                BossTimeline.class,
                Gdx.files.internal(path)
        );

        // ⭐ 计算时间轴总长度
        float maxTime = 0f;

        if (timeline.events != null) {
            for (BossTimelineEvent e : timeline.events) {
                if (e.time > maxTime) {
                    maxTime = e.time;
                }
            }
        }

        // ⭐ 给结尾留一点余韵（可调）
        timeline.length = maxTime + 1.0f;

        return timeline;
    }
}
