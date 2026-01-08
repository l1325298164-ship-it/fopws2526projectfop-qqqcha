package de.tum.cit.fop.maze.game;

import java.util.HashMap;
import java.util.HashSet;

/**
 * 单局/当前关卡存档数据 (Level Snapshot)
 * <p>
 * 职责：
 * 保存**当前关卡**的运行时状态。
 * <p>
 * 改进：
 * 1. 移除了 resetSessionStats 中对 score 的清零，确保分数跨关卡继承。
 * 2. 增加了 levelBaseScore/levelPenalty 字段，支持关卡内中断存档恢复。
 */
public class GameSaveData {

    // ==========================================
    // 1. 基础存档信息 (用于恢复游戏状态)
    // ==========================================

    /** 当前关卡数 */
    public int currentLevel = 1;

    /** * 本局游戏累计总分 (Total Run Score)
     * 注意：跨关卡时会累加，不要在单关重置时清零。
     */
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
    // 2. ScoreManager 状态同步 (用于读档恢复)
    // ==========================================
    /** 本关当前已获得的基础分 */
    public int levelBaseScore = 0;
    /** 本关当前已累计的扣分 */
    public int levelPenalty = 0;

    // ==========================================
    // 3. 本局统计信息 (Session Stats - 用于结算界面)
    // ==========================================

    /**
     * 本局游戏内各类敌人的击杀数。
     * Key: EnemyTier.name()
     */
    public HashMap<String, Integer> sessionKills = new HashMap<>();

    /**
     * 本局刚刚解锁的成就 ID 列表。
     * 用途：在结算界面弹窗展示。
     */
    public HashSet<String> newAchievements = new HashSet<>();

    /**
     * 本局受到的总伤害次数。
     * 用途：用于判定 "封口:滴水不漏" (无伤/少伤) 成就。
     */
    public int sessionDamageTaken = 0;

    // ==========================================
    // 4. 辅助方法
    // ==========================================

    public void addSessionKill(String enemyType) {
        sessionKills.put(enemyType, sessionKills.getOrDefault(enemyType, 0) + 1);
    }

    public void recordNewAchievement(String achievementId) {
        newAchievements.add(achievementId);
    }

    /**
     * 重置本关统计数据 (用于进入新关卡时)
     * ⚠️ 注意：不要重置 score (总分)
     */
    public void resetSessionStats() {
        sessionKills.clear();
        newAchievements.clear();
        sessionDamageTaken = 0;
        levelBaseScore = 0;
        levelPenalty = 0;

        // score = 0; // ❌ 修正：绝对不能在这里重置总分，否则过关就白打了
    }
}