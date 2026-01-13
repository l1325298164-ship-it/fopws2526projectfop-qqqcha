package de.tum.cit.fop.maze.game.achievement;

import de.tum.cit.fop.maze.game.Difficulty;
import de.tum.cit.fop.maze.game.EnemyTier;
import de.tum.cit.fop.maze.game.save.GameSaveData;
import de.tum.cit.fop.maze.game.event.GameListener;
import de.tum.cit.fop.maze.game.score.DamageSource;
import de.tum.cit.fop.maze.game.score.ScoreConstants;
import de.tum.cit.fop.maze.utils.Logger;
import de.tum.cit.fop.maze.game.save.StorageManager;

import java.util.LinkedList;
import java.util.Queue;

/**
 * 成就管理器
 * <p>
 * 修正：
 * 1. 修复 E04 冲刺击杀计数器变量名 (totalDashKills_E04)。
 * 2. 修复 BOBA/HEART 物品识别问题，确保 ACH_03 能解锁。
 * 3. [新增] 通知队列，用于 UI 弹窗展示。
 */
public class AchievementManager implements GameListener {

    private final CareerData careerData;
    private final GameSaveData gameSaveData;
    private final StorageManager storageManager;
    private final Difficulty currentDifficulty;

    // ✨ [新增] 待展示的成就队列 (限制大小防止内存溢出)
    private static final int MAX_NOTIFICATION_QUEUE_SIZE = 50;
    private final Queue<AchievementType> notificationQueue = new LinkedList<>();
    
    // 延迟保存标记，避免频繁I/O
    private boolean needsSave = false;
    
    // ✨ [新增] 本关受击次数（由 onPlayerDamage 累加）
    private int currentLevelDamageTaken = 0;

    public AchievementManager(CareerData careerData,
                              GameSaveData gameSaveData,
                              StorageManager storageManager,
                              Difficulty currentDifficulty) {
        this.careerData = careerData;
        this.gameSaveData = gameSaveData;
        this.storageManager = storageManager;
        this.currentDifficulty = currentDifficulty;
    }

    // ✨ [新增] 获取并移除下一个待展示的成就 (供 HUD 调用)
    public AchievementType pollNotification() {
        return notificationQueue.poll();
    }

    @Override
    public void onEnemyKilled(EnemyTier tier, boolean isDashKill) {
        // 1. 更新全局总击杀
        careerData.totalKills_Global++;
        if (careerData.totalKills_Global >= ScoreConstants.TARGET_KILLS_GLOBAL) {
            unlock(AchievementType.ACH_08_BEST_SELLER);
        }

        // 2. 更新特定怪物击杀
        switch (tier) {
            case E01 -> {
                careerData.totalKills_E01++;
                if (careerData.totalKills_E01 >= ScoreConstants.TARGET_KILLS_E01)
                    unlock(AchievementType.ACH_04_PEARL_SWEEPER);
            }
            case E02 -> {
                careerData.totalKills_E02++;
                if (careerData.totalKills_E02 >= ScoreConstants.TARGET_KILLS_E02)
                    unlock(AchievementType.ACH_05_COFFEE_GRINDER);
            }
            case E03 -> {
                careerData.totalKills_E03++;
                if (careerData.totalKills_E03 >= ScoreConstants.TARGET_KILLS_E03)
                    unlock(AchievementType.ACH_06_CARAMEL_MELT);
            }
            case E04 -> {
                if (isDashKill) {
                    careerData.totalDashKills_E04++;
                    if (careerData.totalDashKills_E04 >= ScoreConstants.TARGET_KILLS_E04_DASH)
                        unlock(AchievementType.ACH_07_SHELL_BREAKER);
                }
            }
            case BOSS -> {
                if (!careerData.hasKilledBoss) {
                    careerData.hasKilledBoss = true;
                    unlock(AchievementType.ACH_15_SUCCESS);
                }
            }
        }
    }

    @Override
    public void onPlayerDamage(int currentHp, DamageSource source) {
        // ✨ [修复] 使用本地计数，避免与 ScoreManager 冲突
        currentLevelDamageTaken++;
    }

    @Override
    public void onItemCollected(String itemType) {
        if (itemType == null) return;

        if ("HEART".equals(itemType) || "BOBA".equals(itemType)) {
            // ACH_09: 累计收集
            careerData.totalHeartsCollected++;
            if (careerData.totalHeartsCollected >= ScoreConstants.TARGET_HEARTS_COLLECTED) {
                unlock(AchievementType.ACH_09_FREE_TOPPING);
            }

            // ACH_03: 首次收集 (Boba Rescue)
            if (!careerData.hasHealedOnce) {
                careerData.hasHealedOnce = true;
                unlock(AchievementType.ACH_03_BOBA_RESCUE);
            }
        }
        else if (itemType.startsWith("TREASURE")) {
            careerData.collectedBuffTypes.add(itemType);
            if (careerData.collectedBuffTypes.size() >= ScoreConstants.TARGET_TREASURE_TYPES) {
                unlock(AchievementType.ACH_10_TREASURE_MASTER);
            }
        }
    }

    @Override
    public void onLevelFinished(int levelNumber) {
        // ACH_02: 通关第1小关
        if (levelNumber == 1) {
            unlock(AchievementType.ACH_02_FIRST_CUP);
        }

        // ACH_11: 滴水不漏
        // ✨ [修复] 使用本地计数而非 gameSaveData
        if (currentLevelDamageTaken <= ScoreConstants.TARGET_NO_DAMAGE_LIMIT) {
            unlock(AchievementType.ACH_11_SEALED_TIGHT);
        }
        
        // 重置本关计数
        currentLevelDamageTaken = 0;

        // ACH_14: 复兴 (困难模式通关)
        if (levelNumber >= 3 && currentDifficulty == Difficulty.HARD) {
            if (!careerData.hasClearedHardMode) {
                careerData.hasClearedHardMode = true;
                unlock(AchievementType.ACH_14_RENAISSANCE);
            }
        }

        // 关卡结束时保存（重要节点，同步保存）
        saveCareerSync();
    }

    public void onPVWatched() {
        if (!careerData.hasWatchedPV) {
            careerData.hasWatchedPV = true;
            unlock(AchievementType.ACH_01_TRAINING);
            // PV观看是重要节点，同步保存
            saveCareerSync();
        }
    }

    /**
     * 强制保存（立即执行）
     */
    public void forceSave() {
        saveCareer();
    }
    
    /**
     * 延迟保存（在合适的时机调用，如关卡结束、游戏暂停时）
     * 避免频繁I/O操作影响性能
     */
    public void saveIfNeeded() {
        if (needsSave) {
            saveCareer();
            needsSave = false;
        }
    }

    private void unlock(AchievementType type) {
        if (!careerData.unlockedAchievements.contains(type.id)) {
            careerData.unlockedAchievements.add(type.id);
            gameSaveData.recordNewAchievement(type.id);

            // ✨ [新增] 加入通知队列，等待 HUD 抓取 (限制队列大小)
            if (notificationQueue.size() < MAX_NOTIFICATION_QUEUE_SIZE) {
                notificationQueue.add(type);
            } else {
                // 队列满时，移除最旧的成就通知，添加新的（FIFO策略）
                AchievementType removed = notificationQueue.poll();
                notificationQueue.add(type);
                Logger.warning("Achievement notification queue is full, dropping oldest: " + 
                        (removed != null ? removed.displayName : "null") + 
                        ", adding new: " + type.displayName);
            }

            Logger.info("🏆 Achievement Unlocked: " + type.displayName);
            // 标记需要保存，但不立即保存（延迟保存策略）
            needsSave = true;
        }
    }

    private void saveCareer() {
        if (storageManager != null) {
            storageManager.saveCareer(careerData);
        }
    }
    
    /**
     * ✨ [新增] 同步保存生涯数据（用于关键节点）
     */
    private void saveCareerSync() {
        if (storageManager != null) {
            storageManager.saveCareerSync(careerData);
        }
    }
}