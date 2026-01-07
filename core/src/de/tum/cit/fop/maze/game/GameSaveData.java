package de.tum.cit.fop.maze.game;

import java.util.HashMap;
import java.util.HashSet;

/**
 * 单局游戏数据 (Session Data)
 * <p>
 * 职责：
 * 1. 存档/读档：保存当前未完成的游戏进度 (Level, HP, Mana, Buffs)。
 * 2. 临时统计：记录本局游戏的表现 (本次击杀、本局受击)，用于结算界面展示。
 * <p>
 * 注意：
 * - 全局生涯数据（如"累计击杀500怪"、"已解锁成就"）不再存放于此。
 * - 请参阅 {@link de.tum.cit.fop.maze.game.achievement.CareerData} 获取生涯数据。
 */
public class GameSaveData {

    // ==========================================
    // 1. 基础存档信息 (用于恢复游戏状态)
    // ==========================================

    /** 当前关卡数 */
    public int currentLevel = 1;

    /** 本局当前累计分数 (显示在HUD上的分数) */
    public int score = 0;

    // --- 玩家状态 ---
    public int lives = 0;
    public int maxLives = 0;
    public int mana = 0;
    public boolean hasKey = false;

    // --- Buff 状态 (道具增益) ---
    public boolean buffAttack = false;
    public boolean buffRegen = false;
    public boolean buffManaEfficiency = false;

    // ==========================================
    // 2. 本局统计信息 (Session Stats - 用于结算界面)
    // ==========================================

    /**
     * 本局游戏内各类敌人的击杀数。
     * Key: EnemyTier.name() (如 "E01", "E04")
     * Value: 击杀数量
     */
    public HashMap<String, Integer> sessionKills = new HashMap<>();

    /**
     * 本局刚刚解锁的成就 ID 列表。
     * 用途：在结算界面弹窗展示 "New Achievements Unlocked!"。
     * 注意：这只是用于UI展示的临时列表，真正的成就记录在 CareerData 中。
     */
    public HashSet<String> newAchievements = new HashSet<>();

    /**
     * 本局受到的总伤害次数。
     * 用途：用于判定 "封口:滴水不漏" (无伤/少伤) 成就。
     */
    public int sessionDamageTaken = 0;

    // ==========================================
    // 3. 辅助方法
    // ==========================================

    /**
     * 增加本局击杀计数
     * @param enemyType 敌人类型标识，建议使用 EnemyTier.name()
     */
    public void addSessionKill(String enemyType) {
        sessionKills.put(enemyType, sessionKills.getOrDefault(enemyType, 0) + 1);
    }

    /**
     * 记录本局新解锁的成就 (用于UI展示)
     * @param achievementId 成就ID
     */
    public void recordNewAchievement(String achievementId) {
        newAchievements.add(achievementId);
    }

    /**
     * 重置本局统计数据
     * (通常在 startNewGame 或 彻底重置 Reset Run 时调用)
     */
    public void resetSessionStats() {
        sessionKills.clear();
        newAchievements.clear();
        sessionDamageTaken = 0;
        score = 0;
        // lives, mana, currentLevel 等基础状态通常由 GameManager 的重置逻辑处理
    }
}