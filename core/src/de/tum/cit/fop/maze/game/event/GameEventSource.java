package de.tum.cit.fop.maze.game.event;

import de.tum.cit.fop.maze.game.EnemyTier;
import de.tum.cit.fop.maze.game.score.DamageSource;
import de.tum.cit.fop.maze.utils.Logger;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 游戏事件源 - 单例模式
 * 负责管理所有游戏事件监听器，并将事件分发给所有注册的监听器
 * <p>
 * 修复：
 * 1. 使用 CopyOnWriteArrayList 防止并发修改异常
 * 2. 添加监听器数量检查，防止泄漏
 * 3. 添加调试日志
 */
public class GameEventSource {

    // 单例实例
    private static GameEventSource instance;

    // 监听器列表（使用线程安全的列表，防止并发修改异常）
    private final List<GameListener> listeners;
    
    // 监听器数量警告阈值
    private static final int MAX_LISTENERS_WARNING = 10;

    /**
     * 私有构造函数，防止外部实例化
     */
    private GameEventSource() {
        // 使用 CopyOnWriteArrayList 防止遍历时修改导致的异常
        this.listeners = new CopyOnWriteArrayList<>();
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
        if (listener == null) return;
        
        // 防止重复添加
        if (listeners.contains(listener)) {
            Logger.warning("GameEventSource: Listener already registered, skipping: " + listener.getClass().getSimpleName());
            return;
        }
        
        listeners.add(listener);
        
        // 监听器数量警告
        if (listeners.size() > MAX_LISTENERS_WARNING) {
            Logger.warning("GameEventSource: Too many listeners (" + listeners.size() + "), possible memory leak!");
        }
        
        Logger.debug("GameEventSource: Added listener " + listener.getClass().getSimpleName() + ", total: " + listeners.size());
    }

    /**
     * 移除事件监听器
     * @param listener 要移除的监听器
     */
    public void removeListener(GameListener listener) {
        if (listener == null) return;
        boolean removed = listeners.remove(listener);
        if (removed) {
            Logger.debug("GameEventSource: Removed listener " + listener.getClass().getSimpleName() + ", remaining: " + listeners.size());
        }
    }

    /**
     * 清空所有监听器
     */
    public void clearListeners() {
        int count = listeners.size();
        listeners.clear();
        Logger.debug("GameEventSource: Cleared " + count + " listeners");
    }
    
    /**
     * 获取当前监听器数量（用于调试）
     */
    public int getListenerCount() {
        return listeners.size();
    }

    /**
     * 当敌人被击杀时触发
     * @param tier 敌人等级
     * @param isDashKill 是否通过冲刺击杀
     */
    public void onEnemyKilled(EnemyTier tier, boolean isDashKill) {
        for (GameListener listener : listeners) {
            try {
                listener.onEnemyKilled(tier, isDashKill);
            } catch (Exception e) {
                Logger.error("GameEventSource: Error in onEnemyKilled listener: " + e.getMessage());
            }
        }
    }

    /**
     * 当玩家受到伤害时触发
     * @param currentHp 当前生命值
     * @param source 伤害来源
     */
    public void onPlayerDamage(int currentHp, DamageSource source) {
        for (GameListener listener : listeners) {
            try {
                listener.onPlayerDamage(currentHp, source);
            } catch (Exception e) {
                Logger.error("GameEventSource: Error in onPlayerDamage listener: " + e.getMessage());
            }
        }
    }

    /**
     * 当收集物品时触发
     * @param itemType 物品类型（如 "HEART", "TREASURE", "KEY"）
     */
    public void onItemCollected(String itemType) {
        for (GameListener listener : listeners) {
            try {
                listener.onItemCollected(itemType);
            } catch (Exception e) {
                Logger.error("GameEventSource: Error in onItemCollected listener: " + e.getMessage());
            }
        }
    }

    /**
     * 当关卡完成时触发
     * @param levelNumber 关卡编号
     */
    public void onLevelFinished(int levelNumber) {
        for (GameListener listener : listeners) {
            try {
                listener.onLevelFinished(levelNumber);
            } catch (Exception e) {
                Logger.error("GameEventSource: Error in onLevelFinished listener: " + e.getMessage());
            }
        }
    }

    /**
     * 重置事件源（用于测试或游戏重启）
     */
    public void reset() {
        clearListeners();
    }
}