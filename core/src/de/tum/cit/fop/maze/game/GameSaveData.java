package de.tum.cit.fop.maze.game;

public class GameSaveData {
    // 基础进度
    public int currentLevel = 1;
    public int score = 0;

    // 玩家状态
    public int lives;
    public int maxLives;
    public int mana;
    public boolean hasKey;

    // Buff 状态 (非常重要，这是玩家积累的变强资本)
    public boolean buffAttack;
    public boolean buffRegen;
    public boolean buffManaEfficiency;
}