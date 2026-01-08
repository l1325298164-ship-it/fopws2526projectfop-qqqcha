package de.tum.cit.fop.maze.game.score;

import de.tum.cit.fop.maze.game.DifficultyConfig;
import de.tum.cit.fop.maze.game.EnemyTier;
import de.tum.cit.fop.maze.game.GameSaveData;
import de.tum.cit.fop.maze.game.event.GameListener;
import de.tum.cit.fop.maze.utils.Logger;

public class ScoreManager implements GameListener {

    private final DifficultyConfig config;

    // === 历史数据 (从存档恢复) ===
    private int accumulatedScore = 0;

    // === 本关临时统计 ===
    private int levelBaseScore = 0;
    private int levelPenalty = 0;
    private int hitsTaken = 0;

    public ScoreManager(DifficultyConfig config) {
        this.config = config;
    }

    public void saveState(GameSaveData data) {
        data.levelBaseScore = this.levelBaseScore;
        data.levelPenalty = this.levelPenalty;
        data.sessionDamageTaken = this.hitsTaken;
        // 注意：总分已由 SettlementScreen 更新到 data.score，此处只需保存临时统计
    }

    public void restoreState(GameSaveData data) {
        // 当从存档恢复时，data.score 代表了之前的总分（如果是过关存档）
        this.accumulatedScore = data.score;

        // 恢复临时统计（如果是中途存档，这些值会有意义；如果是过关存档，通常为0）
        this.levelBaseScore = data.levelBaseScore;
        this.levelPenalty = data.levelPenalty;
        this.hitsTaken = data.sessionDamageTaken;

        Logger.info("ScoreManager Restored: Total=" + accumulatedScore + ", LevelBase=" + levelBaseScore);
    }

    public int getCurrentScore() {
        // 实时总分 = 历史分 + (本关基础分 - 本关扣分) * 倍率
        int currentLevelRaw = Math.max(0, levelBaseScore - levelPenalty);
        int currentLevelFinal = (int) (currentLevelRaw * config.scoreMultiplier);
        return accumulatedScore + currentLevelFinal;
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

        if (itemType.equals("HEART") || itemType.equals("BOBA")) {
            points = ScoreConstants.SCORE_HEART;
        } else if (itemType.startsWith("TREASURE")) {
            points = ScoreConstants.SCORE_TREASURE;
        } else if (itemType.equals("KEY")) {
            points = 50;
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
        // 关卡结束逻辑主要在 SettlementScreen 处理
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

        if (ratio >= 0.90) return "S";
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