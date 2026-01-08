package de.tum.cit.fop.maze.game.event;

import de.tum.cit.fop.maze.game.EnemyTier;
import de.tum.cit.fop.maze.game.score.DamageSource;

import java.util.ArrayList;
import java.util.List;

/**
 * 游戏事件源 - 单例模式
 * 负责管理所有游戏事件监听器，并将事件分发给所有注册的监听器
 */
public class GameEventSource {

    // 单例实例
    private static GameEventSource instance;

    // 监听器列表
    private final List<GameListener> listeners;

    /**
     * 私有构造函数，防止外部实例化
     */
    private GameEventSource() {
        this.listeners = new ArrayList<>();
    }

    /**
     * 获取单例实例
     * @return GameEventSource 单例对象
     */
    public static GameEventSource getInstance() {
        if (instance == null) {
            instance = new GameEventSource();
        }
        return instance;
    }

    /**
     * 添加事件监听器
     * @param listener 要添加的监听器
     */
    public void addListener(GameListener listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    /**
     * 移除事件监听器
     * @param listener 要移除的监听器
     */
    public void removeListener(GameListener listener) {
        listeners.remove(listener);
    }

    /**
     * 清空所有监听器
     */
    public void clearListeners() {
        listeners.clear();
    }

    /**
     * 当敌人被击杀时触发
     * @param tier 敌人等级
     * @param isDashKill 是否通过冲刺击杀
     */
    public void onEnemyKilled(EnemyTier tier, boolean isDashKill) {
        for (GameListener listener : listeners) {
            listener.onEnemyKilled(tier, isDashKill);
        }
    }

    /**
     * 当玩家受到伤害时触发
     * @param currentHp 当前生命值
     * @param source 伤害来源
     */
    public void onPlayerDamage(int currentHp, DamageSource source) {
        for (GameListener listener : listeners) {
            listener.onPlayerDamage(currentHp, source);
        }
    }

    /**
     * 当收集物品时触发
     * @param itemType 物品类型（如 "HEART", "TREASURE", "KEY"）
     */
    public void onItemCollected(String itemType) {
        for (GameListener listener : listeners) {
            listener.onItemCollected(itemType);
        }
    }

    /**
     * 当关卡完成时触发
     * @param levelNumber 关卡编号
     */
    public void onLevelFinished(int levelNumber) {
        for (GameListener listener : listeners) {
            listener.onLevelFinished(levelNumber);
        }
    }

    /**
     * 重置事件源（用于测试或游戏重启）
     */
    public void reset() {
        clearListeners();
    }
}