package de.tum.cit.fop.maze.entities;

public interface PushSource {
    int getPushStrength();     // 推力大小
    boolean isLethal();        // 是否致命（以后 Boss 用）
}
