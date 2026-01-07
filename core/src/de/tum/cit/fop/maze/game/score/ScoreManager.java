package de.tum.cit.fop.maze.game.score;

import de.tum.cit.fop.maze.game.DifficultyConfig;
import de.tum.cit.fop.maze.game.EnemyTier;
import de.tum.cit.fop.maze.game.event.GameListener;
import de.tum.cit.fop.maze.utils.Logger;
import de.tum.cit.fop.maze.game.score.ScoreConstants;

/**
 * 分数管理器
 * <p>
 * 职责：
 * 1. 监听战斗和探索事件，实时计算基础分。
 * 2. 监听受伤事件，根据难度配置计算扣分。
 * 3. 在关卡结束时，对比理论满分计算评级 (Rank)。
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
        // 使用常量定义
        switch (tier) {
            case E01 -> points = ScoreConstants.SCORE_E01_PEARL;
            case E02 -> points = ScoreConstants.SCORE_E02_COFFEE;
            case E03 -> points = ScoreConstants.SCORE_E03_CARAMEL;
            case E04 -> points = ScoreConstants.SCORE_E04_SHELL;
            case BOSS -> points = ScoreConstants.SCORE_BOSS;
        }
        levelBaseScore += points;
        Logger.debug("Score + " + points + " (Enemy: " + tier + ")");
    }

    @Override
    public void onPlayerDamage(int currentHp, DamageSource source) {
        hitsTaken++;

        // 实际扣分 = 基础扣分 * 惩罚倍率 (Penalty Multiplier)
        int penalty = (int) (source.penaltyScore * config.penaltyMultiplier);

        levelPenalty += penalty;
        Logger.debug("Score Penalty - " + penalty + " (" + source + ")");
    }

    @Override
    public void onItemCollected(String itemType) {
        int points = 0;

        // 修复：改用 if-else 处理 startsWith 逻辑，兼容 "TREASURE_ATK" 等变体
        if (itemType == null) return;

        if (itemType.equals("HEART")) {
            points = ScoreConstants.SCORE_HEART;
        } else if (itemType.startsWith("TREASURE")) {
            points = ScoreConstants.SCORE_TREASURE;
        } else if (itemType.equals("FOG_CLEARED")) {
            points = ScoreConstants.SCORE_FOG_CLEARED;
        }

        if (points > 0) {
            levelBaseScore += points;
            Logger.debug("Score + " + points + " (Item: " + itemType + ")");
        }
    }

    @Override
    public void onLevelFinished(int levelNumber) {
        Logger.info("Level " + levelNumber + " finished. Calculating score...");
    }

    public LevelResult calculateResult(int theoreticalMaxBaseScore) {
        int rawScore = Math.max(0, levelBaseScore - levelPenalty);
        int finalScore = (int) (rawScore * config.scoreMultiplier);
        double maxPossibleScore = theoreticalMaxBaseScore * config.scoreMultiplier;

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
        if (maxScore <= 0) return "S";

        double ratio = score / maxScore;

        if (ratio >= 0.90 && config.scoreMultiplier >= 1.5) {
            return "S";
        }
        if (ratio >= 0.70) return "A";
        if (ratio >= 0.50) return "B";
        if (ratio >= 0.30) return "C";
        return "D";
    }

    /**
     * 重置管理器状态 (通常在进入新关卡时调用)
     */
    public void reset() {
        levelBaseScore = 0;
        levelPenalty = 0;
        hitsTaken = 0;
    }

    public int getHitsTaken() { return hitsTaken; }
}