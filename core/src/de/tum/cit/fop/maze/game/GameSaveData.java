package de.tum.cit.fop.maze.game;

import java.util.HashMap;

public class GameSaveData {
    // 基础进度
    public int currentLevel = 1;
    public int score = 0; // 🔥 启用分数

    // 玩家状态
    public int lives;
    public int maxLives;
    public int mana;
    public boolean hasKey;

    // Buff 状态
    public boolean buffAttack;
    public boolean buffRegen;
    public boolean buffManaEfficiency;

    // 统计数据 (用于成就判定)
    public int totalKills = 0;        // 累计击杀
    public int totalHearts = 0;       // 累计捡爱心
    public boolean hasClearedLevel1 = false; // 是否通过第一关

    // 成就解锁记录 (防止重复弹窗)
    // Key: 成就ID, Value: 是否解锁
    public HashMap<String, Boolean> unlockedAchievements = new HashMap<>();
}