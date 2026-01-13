package de.tum.cit.fop.maze.game.score;

/**
 * 关卡结算结果数据包
 * <p>
 * 用途：
 * 将 ScoreManager 计算好的本关数据传递给 UI 界面 (SettlementScreen) 进行展示。
 */
public class LevelResult {

    /** 最终得分 (经过倍率计算后的总分) */
    public final int finalScore;

    /** 基础得分 (击杀 + 拾取，未乘倍率) */
    public final int baseScore;

    /** 扣分合计 (基础扣分 * 惩罚倍率) */
    public final int penaltyScore;

    /** 评级 (S/A/B/C/D) */
    public final String rank;

    /** 本关受击次数 */
    public final int hitsTaken;

    /** 难度得分倍率 (用于UI展示，例如 "x1.5") */
    public final float scoreMultiplier;

    public LevelResult(int finalScore, int baseScore, int penaltyScore, String rank, int hitsTaken, float scoreMultiplier) {
        this.finalScore = finalScore;
        this.baseScore = baseScore;
        this.penaltyScore = penaltyScore;
        this.rank = rank;
        this.hitsTaken = hitsTaken;
        this.scoreMultiplier = scoreMultiplier;
    }

    @Override
    public String toString() {
        return "Rank: " + rank + " | Score: " + finalScore + " (Base: " + baseScore + " - Penalty: " + penaltyScore + ")";
    }
}