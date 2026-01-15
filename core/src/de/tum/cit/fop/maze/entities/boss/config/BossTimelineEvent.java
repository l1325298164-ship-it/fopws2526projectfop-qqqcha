package de.tum.cit.fop.maze.entities.boss.config;

public class BossTimelineEvent {
    public float time;
    public String type;

    // ===== Dialogue =====
    public String speaker;   // "BOSS" / "SYSTEM"
    public String text;      // 显示文本
    public String voice;     // 音频路径（可空）

    // ===== 通用参数 =====
    public Float threshold;
    public Float duration;
    public Float tickInterval;
    public Integer damage;
    public String intensity;
    public String onFail;
    public String onSuccess;

    // 运行时
    public boolean triggered = false;
}
