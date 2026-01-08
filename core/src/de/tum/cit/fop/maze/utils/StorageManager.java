package de.tum.cit.fop.maze.utils;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonWriter;
import de.tum.cit.fop.maze.game.GameSaveData;
import de.tum.cit.fop.maze.game.achievement.CareerData;

/**
 * 统一存储管理器 (Storage Manager) - 单例模式
 * <p>
 * 改进：
 * 增加了原子写入机制 (Write-to-temp -> Move)，防止存档损坏。
 * 使用单例模式确保全局唯一实例。
 */
public class StorageManager {

    // ==========================================
    // 单例模式实现
    // ==========================================
    private static StorageManager instance;

    /**
     * 获取单例实例
     * @return StorageManager 单例对象
     */
    public static StorageManager getInstance() {
        if (instance == null) {
            instance = new StorageManager();
        }
        return instance;
    }

    // ==========================================
    // 原有实现
    // ==========================================
    private static final String SAVE_FILE_NAME = "save_data.json";
    private static final String CAREER_FILE_NAME = "career_data.json";

    private final Json json;

    /**
     * 私有构造函数，防止外部直接实例化
     */
    private StorageManager() {
        this.json = new Json();
        this.json.setOutputType(JsonWriter.OutputType.json);
        this.json.setUsePrototypes(false);
    }

    // ==========================================
    // 通用原子写入方法
    // ==========================================
    private void writeJsonSafely(String fileName, Object data) {
        if (data == null) return;
        FileHandle tmpFile = null;
        try {
            // 1. 清理可能存在的旧临时文件
            FileHandle oldTmpFile = getFile(fileName + ".tmp");
            if (oldTmpFile.exists()) {
                try {
                    oldTmpFile.delete();
                } catch (Exception e) {
                    Logger.warning("Failed to delete old temp file: " + e.getMessage());
                }
            }
            
            // 2. 写入临时文件
            tmpFile = getFile(fileName + ".tmp");
            String jsonStr = json.toJson(data);
            tmpFile.writeString(jsonStr, false); // 覆盖写入临时文件

            // 3. 移动/重命名临时文件覆盖正式文件 (原子操作)
            FileHandle targetFile = getFile(fileName);
            tmpFile.moveTo(targetFile);
            tmpFile = null; // 移动成功后，tmpFile 不再指向有效文件

            Logger.debug("Data saved safely to " + fileName);
        } catch (Exception e) {
            Logger.error("Failed to save data to " + fileName + ": " + e.getMessage());
            e.printStackTrace();
            
            // 清理失败的临时文件
            if (tmpFile != null && tmpFile.exists()) {
                try {
                    tmpFile.delete();
                } catch (Exception cleanupEx) {
                    Logger.warning("Failed to cleanup temp file: " + cleanupEx.getMessage());
                }
            }
        }
    }

    // ==========================================
    // 1. 单局存档 (GameSaveData)
    // ==========================================
    public void saveGame(GameSaveData data) {
        writeJsonSafely(SAVE_FILE_NAME, data);
        Logger.info("Game progress saved.");
    }

    public GameSaveData loadGame() {
        FileHandle file = getFile(SAVE_FILE_NAME);
        if (!file.exists()) {
            Logger.info("No save file found.");
            return null;
        }
        try {
            String jsonStr = file.readString();
            if (jsonStr == null || jsonStr.trim().isEmpty()) {
                Logger.warning("Save file is empty, treating as no save.");
                return null;
            }
            
            GameSaveData data = json.fromJson(GameSaveData.class, jsonStr);
            
            // 数据验证
            if (data == null) {
                Logger.error("Failed to parse save data: data is null");
                return null;
            }
            
            // 验证关键字段的合理性
            if (data.currentLevel < 1) {
                Logger.warning("Invalid level in save data: " + data.currentLevel + ", resetting to 1");
                data.currentLevel = 1;
            }
            if (data.score < 0) {
                Logger.warning("Invalid score in save data: " + data.score + ", resetting to 0");
                data.score = 0;
            }
            if (data.lives < 0) {
                Logger.warning("Invalid lives in save data: " + data.lives + ", resetting to 0");
                data.lives = 0;
            }
            
            Logger.info("Game progress loaded successfully.");
            return data;
        } catch (Exception e) {
            Logger.error("Failed to load save data: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    public void deleteSave() {
        FileHandle file = getFile(SAVE_FILE_NAME);
        if (file.exists()) {
            file.delete();
            Logger.info("Save file deleted.");
        }
    }

    public boolean hasSaveFile() {
        return getFile(SAVE_FILE_NAME).exists();
    }

    // ==========================================
    // 2. 生涯档案 (CareerData)
    // ==========================================
    public void saveCareer(CareerData data) {
        writeJsonSafely(CAREER_FILE_NAME, data);
    }

    public CareerData loadCareer() {
        FileHandle file = getFile(CAREER_FILE_NAME);
        if (!file.exists()) {
            Logger.info("No career data found, creating new profile.");
            return new CareerData();
        }
        try {
            String jsonStr = file.readString();
            if (jsonStr == null || jsonStr.trim().isEmpty()) {
                Logger.warning("Career file is empty, creating new profile.");
                return new CareerData();
            }
            
            CareerData data = json.fromJson(CareerData.class, jsonStr);
            
            // 数据验证
            if (data == null) {
                Logger.warning("Failed to parse career data: data is null, creating new profile.");
                return new CareerData();
            }
            
            // 验证数据的合理性（防止负数或异常大的值）
            if (data.totalKills_E01 < 0) data.totalKills_E01 = 0;
            if (data.totalKills_E02 < 0) data.totalKills_E02 = 0;
            if (data.totalKills_E03 < 0) data.totalKills_E03 = 0;
            if (data.totalDashKills_E04 < 0) data.totalDashKills_E04 = 0;
            if (data.totalKills_Global < 0) data.totalKills_Global = 0;
            if (data.totalHeartsCollected < 0) data.totalHeartsCollected = 0;
            
            // 确保 HashSet 不为 null
            if (data.collectedBuffTypes == null) {
                data.collectedBuffTypes = new java.util.HashSet<>();
            }
            if (data.unlockedAchievements == null) {
                data.unlockedAchievements = new java.util.HashSet<>();
            }
            
            Logger.info("Career data loaded successfully.");
            return data;
        } catch (Exception e) {
            Logger.error("Failed to load career data, resetting: " + e.getMessage());
            e.printStackTrace();
            return new CareerData();
        }
    }

    private FileHandle getFile(String fileName) {
        return Gdx.files.local(fileName);
    }
}