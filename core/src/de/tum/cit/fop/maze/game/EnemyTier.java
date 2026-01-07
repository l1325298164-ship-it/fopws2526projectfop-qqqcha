package de.tum.cit.fop.maze.game;

/**
 * 敌人等级/类型定义
 * 对应 ScoreManager 中的 switch case
 */
public enum EnemyTier {
    E01,    // 对应 E01_CorruptedPearl
    E02,    // 对应 E02_SmallCoffeeBean
    E03,    // 对应 E03_CaramelJuggernaut
    E04,    // 对应 E04_CrystallizedCaramelShell
    BOSS    // 对应 BOSS 战
}