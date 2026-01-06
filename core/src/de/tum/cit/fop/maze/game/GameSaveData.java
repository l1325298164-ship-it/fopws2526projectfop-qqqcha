package de.tum.cit.fop.maze.game;

import java.util.HashMap;

public class GameSaveData {
    // 基础进度
    public int currentLevel = 1;
    public int score = 0;

    // 玩家状态
    public int lives;
    public int maxLives;
    public int mana;
    public boolean hasKey;

    // Buff 状态
    public boolean buffAttack;
    public boolean buffRegen;
    public boolean buffManaEfficiency;

    // ==========================================
    // 🔥 [Phase 1 New] 生涯统计数据 (Career Stats)
    // ==========================================

    // 每日备料成就 - 怪物击杀计数
    public int totalKills_E01 = 0; // 腐败珍珠 (需60)
    public int totalKills_E02 = 0; // 咖啡豆 (需40)
    public int totalKills_E03 = 0; // 焦糖重装 (需50)
    public int totalKills_E04 = 0; // 结晶焦糖 (需50)
    public int totalKills_Global = 0; // 总击杀 (爆单王)

    // 引导类成就 - 状态标记
    public boolean hasWatchedPV = false;   // ACH_01: 背诵配方
    public boolean hasHealedOnce = false;  // ACH_03: 脆波波救急

    // 成就解锁记录 (Key: 成就ID, Value: 是否解锁)
    public HashMap<String, Boolean> unlockedAchievements = new HashMap<>();

    // 结算统计 (当前局)
    public int damageTakenCount = 0; // 本局受伤次数 (用于扣分统计和无伤成就)
}