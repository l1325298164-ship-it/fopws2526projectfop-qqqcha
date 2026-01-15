package de.tum.cit.fop.maze.entities.boss.config;

public class BossTimelineEvent {
    public float time;
    public String type;

    // ===== Dialogue =====
    public String speaker;
    public String text;
    public String voice;

    // ===== 通用事件参数 =====
    public Float threshold;
    public Float duration;      // ⭐ 统一：事件持续时间
    public Float tickInterval;
    public Integer damage;

    // ===== CUP_SHAKE 专用参数 =====
    public Float xAmp;
    public Float yAmp;
    public Float xFreq;
    public Float yFreq;

    // ===== 运行时 =====
    public boolean triggered = false;
}

