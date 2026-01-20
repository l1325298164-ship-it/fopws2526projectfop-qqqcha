package de.tum.cit.fop.maze.game.save;

import de.tum.cit.fop.maze.entities.Player;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

/**
 * 单局/当前关卡存档数据 (Level Snapshot)
 * <p>
 * 职责：
 * 保存**当前关卡**的运行时状态。
 * <p>
 * 改进：
 * 1. 移除了 resetSessionStats 中对 score 的清零，确保分数跨关卡继承。
 * 2. 增加了 levelBaseScore/levelPenalty 字段，支持关卡内中断存档恢复。
 * 3. [Fix] 添加了拷贝构造函数，修复编译错误。
 */
public class GameSaveData {

    // ==========================================
    // 1. 基础存档信息 (用于恢复游戏状态)
    // ==========================================
    public int[][] maze;
    /** 当前关卡数 */
    public int currentLevel = 1;

    /** * 本局游戏累计总分 (Total Run Score)
     * 注意：跨关卡时会累加，不要在单关重置时清零。
     */
    public int score = 0;

    /** ✨ [新增] 游戏难度（用于恢复存档时使用正确的难度配置） */
    public String difficulty = "NORMAL";

    /** ✨ [新增] 单/双人模式（用于 Continue 时恢复正确的玩家数量） */
    // 默认单人模式（旧存档缺字段时也会落到 false）
    public boolean twoPlayerMode = false;

    // --- 玩家状态 ---
    public Map<String, PlayerSaveData> players = new HashMap<>();

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
    // 4. 构造函数
    // ==========================================

    /**
     * 默认无参构造函数 (Json 序列化必需)
     */
    public GameSaveData() {
    }

    /**
     * 拷贝构造函数 (用于创建存档快照)
     * @param other 被拷贝的源数据
     */
    public GameSaveData(GameSaveData other) {
        if (other == null) return;

        this.currentLevel = other.currentLevel;
        this.score = other.score;
        this.difficulty = other.difficulty;
        this.twoPlayerMode = other.twoPlayerMode;

        this.levelBaseScore = other.levelBaseScore;
        this.levelPenalty = other.levelPenalty;
        this.sessionDamageTaken = other.sessionDamageTaken;

        // 深度拷贝迷宫数组
        if (other.maze != null) {
            this.maze = new int[other.maze.length][];
            for (int i = 0; i < other.maze.length; i++) {
                this.maze[i] = other.maze[i].clone();
            }
        }

        // 深度拷贝集合 (防止原集合被 clear 后影响快照)
        if (other.players != null) {
            this.players = new HashMap<>(other.players);
        }

        if (other.sessionKills != null) {
            this.sessionKills = new HashMap<>(other.sessionKills);
        }

        if (other.newAchievements != null) {
            this.newAchievements = new HashSet<>(other.newAchievements);
        }
    }

    // ==========================================
    // 5. 辅助方法
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