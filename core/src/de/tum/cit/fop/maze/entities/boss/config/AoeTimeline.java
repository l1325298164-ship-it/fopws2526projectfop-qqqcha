package de.tum.cit.fop.maze.entities.boss.config;

import com.badlogic.gdx.utils.Array;

public class AoeTimeline {

    /** 一个完整循环（秒），比如 30s */
    public float cycle;

    /** 同一个 cycle 里的多个 AOE 行为 */
    public Array<AoePattern> patterns;

    public static class AoePattern {
        public float start;     // cycle 内开始时间（秒）
        public float end;       // cycle 内结束时间（秒）
        public float interval;  // 生成间隔
        public int count;       // 一次生成多少个圈
        public float radius;    // AOE 半径
        public float duration;  // 单个 AOE 持续多久
        public int damage;      // 伤害
    }
}
