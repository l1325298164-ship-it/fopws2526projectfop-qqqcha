package de.tum.cit.fop.maze.game.score;

import de.tum.cit.fop.maze.game.DifficultyConfig;
import de.tum.cit.fop.maze.game.EnemyTier;
import de.tum.cit.fop.maze.game.GameSaveData;
import de.tum.cit.fop.maze.game.event.GameListener;
import de.tum.cit.fop.maze.utils.Logger;

/**
 * 分数管理器
 * <p>
 * 改进：
 * 增加了与 GameSaveData 的状态同步方法 (saveState/restoreState)。
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
     * 将当前分数状态保存到存档数据对象中
     * (应在 StorageManager.saveGame 前调用)
     */
    public void saveState(GameSaveData data) {
        data.levelBaseScore = this.levelBaseScore;
        data.levelPenalty = this.levelPenalty;
        data.sessionDamageTaken = this.hitsTaken; // 确保受击数同步
    }

    /**
     * 从存档数据恢复分数状态
     * (应在 StorageManager.loadGame 后调用)
     */
    public void restoreState(GameSaveData data) {
        this.levelBaseScore = data.levelBaseScore;
        this.levelPenalty = data.levelPenalty;
        this.hitsTaken = data.sessionDamageTaken;
        Logger.info("ScoreManager restored: Base=" + levelBaseScore + ", Penalty=" + levelPenalty);
    }

    public int getCurrentScore() {
        int rawScore = Math.max(0, levelBaseScore - levelPenalty);
        return (int) (rawScore * config.scoreMultiplier);
    }

    @Override
    public void onEnemyKilled(EnemyTier tier, boolean isDashKill) {
        int points = 0;
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
        int penalty = (int) (source.penaltyScore * config.penaltyMultiplier);
        levelPenalty += penalty;
        Logger.debug("Score Penalty - " + penalty + " (" + source + ")");
    }

    @Override
    public void onItemCollected(String itemType) {
        if (itemType == null) return;
        int points = 0;

        // 统一逻辑：HEART/BOBA 通常是同一个物品，这里只关注加分
        if (itemType.equals("HEART") || itemType.equals("BOBA")) {
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

        if (ratio >= 0.90 && config.scoreMultiplier >= 1.5) return "S";
        if (ratio >= 0.70) return "A";
        if (ratio >= 0.50) return "B";
        if (ratio >= 0.30) return "C";
        return "D";
    }

    public void reset() {
        levelBaseScore = 0;
        levelPenalty = 0;
        hitsTaken = 0;
    }

    public int getHitsTaken() { return hitsTaken; }
}