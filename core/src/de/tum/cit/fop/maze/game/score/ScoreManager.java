package de.tum.cit.fop.maze.game.score;

import de.tum.cit.fop.maze.game.DifficultyConfig;
import de.tum.cit.fop.maze.game.EnemyTier;
import de.tum.cit.fop.maze.game.save.GameSaveData;
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
    }

    public void restoreState(GameSaveData data) {
        this.accumulatedScore = data.score;
        this.levelBaseScore = data.levelBaseScore;
        this.levelPenalty = data.levelPenalty;
        this.hitsTaken = data.sessionDamageTaken;
        Logger.info("ScoreManager Restored: Total=" + accumulatedScore + ", LevelBase=" + levelBaseScore);
    }

    public int getCurrentScore() {
        int currentLevelRaw = Math.max(0, levelBaseScore - levelPenalty);
        int currentLevelFinal = (int) (currentLevelRaw * config.scoreMultiplier);
        long totalScore = (long) accumulatedScore + currentLevelFinal;
        return (int) Math.min(totalScore, Integer.MAX_VALUE);
    }

    @Override
    public void onEnemyKilled(EnemyTier tier, boolean isDashKill) {
        int points = 0;
        switch (tier) {
            case E01 -> points = ScoreConstants.SCORE_E01_PEARL;
            case E02 -> points = ScoreConstants.SCORE_E02_COFFEE;
            case E03 -> points = ScoreConstants.SCORE_E03_CARAMEL;

            // 🔥 E04 必须使用 Dash 击杀才得分
            case E04 -> {
                if (isDashKill) {
                    points = ScoreConstants.SCORE_E04_SHELL;
                } else {
                    points = 0;
                    Logger.debug("E04 Normal Kill - No Score (Requires Dash)");
                }
            }

            case BOSS -> points = ScoreConstants.SCORE_BOSS;
        }

        if (points > 0) {
            levelBaseScore += points;
            // 注意：飘字逻辑需在 GameManager/Player 处调用 spawnScoreText，此处仅处理数值
        }
    }

    @Override
    public void onPlayerDamage(int currentHp, DamageSource source) {
        hitsTaken++;
        int penalty = (int) (source.penaltyScore * config.penaltyMultiplier);
        levelPenalty += penalty;
        // 注意：飘字逻辑需在 Player.takeDamage 处处理
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
            points = ScoreConstants.SCORE_KEY;
        } else if (itemType.equals("FOG_CLEARED")) {
            points = ScoreConstants.SCORE_FOG_CLEARED;
        }

        if (points > 0) {
            levelBaseScore += points;
            // 注意：拾取物品的飘字逻辑（如 KEY）需要在 GameManager 或 Item 逻辑中调用
        }
    }

    @Override
    public void onLevelFinished(int levelNumber) {
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
//新增 给升级用
    /**
     * 消费分数（用于技能升级 / 商店等）
     * @return 是否消费成功
     */
    public boolean spendScore(int amount) {
        if (amount <= 0) return true;

        int available = accumulatedScore;
        if (available < amount) {
            return false;
        }

        accumulatedScore -= amount;

        Logger.debug("Score spent: -" + amount + ", remaining=" + accumulatedScore);
        return true;
    }

    public int getHitsTaken() { return hitsTaken; }
}