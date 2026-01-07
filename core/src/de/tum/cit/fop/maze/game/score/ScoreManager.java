package de.tum.cit.fop.maze.game.score;

import de.tum.cit.fop.maze.game.DifficultyConfig;
import de.tum.cit.fop.maze.game.EnemyTier;
import de.tum.cit.fop.maze.game.event.GameListener;
import de.tum.cit.fop.maze.utils.Logger;

/**
 * 分数管理器
 * <p>
 * 职责：
 * 1. 监听战斗和探索事件，实时计算基础分。
 * 2. 监听受伤事件，根据难度配置计算扣分。
 * 3. 在关卡结束时，对比理论满分计算评级 (Rank)。
 * <p>
 * 规则参考:
 */
public class ScoreManager implements GameListener {

    private final DifficultyConfig config;

    // === 本关临时统计 ===
    private int levelBaseScore = 0; // 基础分 (击杀 + 收集)
    private int levelPenalty = 0;   // 累计扣分 (已乘倍率)
    private int hitsTaken = 0;      // 受击次数

    public ScoreManager(DifficultyConfig config) {
        this.config = config;
    }

    /**
     * 获取当前实时显示的预估分数 (用于 HUD 显示)
     * 公式: (基础分 - 扣分) * 得分倍率
     */
    public int getCurrentScore() {
        int rawScore = Math.max(0, levelBaseScore - levelPenalty);
        return (int) (rawScore * config.scoreMultiplier);
    }

    @Override
    public void onEnemyKilled(EnemyTier tier, boolean isDashKill) {
        int points = 0;
        // 规则来源: - 1.战斗得分
        switch (tier) {
            case E01 -> points = 150; // 腐败珍珠
            case E02 -> points = 100; // 小包咖啡豆
            case E03 -> points = 600; // 焦糖重装
            case E04 -> points = 600; // 结晶焦糖壳
            case BOSS -> points = 5000;
        }
        levelBaseScore += points;
        Logger.debug("Score + " + points + " (Enemy: " + tier + ")");
    }

    @Override
    public void onPlayerDamage(int currentHp, DamageSource source) {
        hitsTaken++;

        // 规则来源: - 3.扣分项
        // 实际扣分 = 基础扣分 * 惩罚倍率 (Penalty Multiplier)
        int penalty = (int) (source.penaltyScore * config.penaltyMultiplier);

        levelPenalty += penalty;
        Logger.debug("Score Penalty - " + penalty + " (" + source + ")");
    }

    @Override
    public void onItemCollected(String itemType) {
        int points = 0;
        // 规则来源: - 2.探索得分
        switch (itemType) {
            case "HEART" -> points = 50;
            case "TREASURE" -> points = 800;
            case "FOG_CLEARED" -> points = 500;
            // 其他物品如 Key, Boba 暂无分数定义，可按需添加
        }

        if (points > 0) {
            levelBaseScore += points;
            Logger.debug("Score + " + points + " (Item: " + itemType + ")");
        }
    }

    @Override
    public void onLevelFinished(int levelNumber) {
        // 仅做日志记录，具体的结算数据获取通过 getResult() 主动调用
        Logger.info("Level " + levelNumber + " finished. Calculating score...");
    }

    /**
     * 计算并生成本关结算单
     * <p>
     * 评级逻辑:
     * - S: >90% (仅 Hard)
     * - A: 70% - 89%
     * - B: 50% - 69%
     * - C: 30% - 49%
     * - D: <30%
     *
     * @param theoreticalMaxBaseScore 本关地图资源的理论最大基础分 (所有怪的分值 + 所有宝箱分值 + 所有心分值)
     * 该值需由 GameManager 在生成地图时统计。
     * @return 封装好的 LevelResult 对象
     */
    public LevelResult calculateResult(int theoreticalMaxBaseScore) {
        // 最终得分 = [(基础分) - 扣分] * 得分倍率
        int rawScore = Math.max(0, levelBaseScore - levelPenalty);
        int finalScore = (int) (rawScore * config.scoreMultiplier);

        // 计算理论满分 (MaxScore) = (理论基础分 - 0扣分) * 得分倍率
        double maxPossibleScore = theoreticalMaxBaseScore * config.scoreMultiplier;

        // 计算评级
        String rank = determineRank(finalScore, maxPossibleScore);

        return new LevelResult(
                finalScore,
                levelBaseScore,
                levelPenalty,
                rank,
                hitsTaken,
                (float) config.scoreMultiplier
        );
    }

    private String determineRank(int score, double maxScore) {
        if (maxScore <= 0) return "S"; // 避免除零异常 (例如空关卡)

        double ratio = score / maxScore;

        // 规则: S级只有在困难模式 (x1.5) 下才能达到 (需要 >90%)
        if (ratio >= 0.90 && config.scoreMultiplier >= 1.5) {
            return "S";
        }
        if (ratio >= 0.70) return "A";
        if (ratio >= 0.50) return "B";
        if (ratio >= 0.30) return "C";

        return "D";
    }

    /**
     * 重置分数管理器状态 (通常在进入新关卡时调用)
     */
    public void reset() {
        levelBaseScore = 0;
        levelPenalty = 0;
        hitsTaken = 0;
    }

    // Getters
    public int getHitsTaken() { return hitsTaken; }
}