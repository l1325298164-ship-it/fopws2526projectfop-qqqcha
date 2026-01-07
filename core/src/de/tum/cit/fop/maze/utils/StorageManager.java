package de.tum.cit.fop.maze.utils;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonWriter;
import de.tum.cit.fop.maze.game.GameSaveData;
import de.tum.cit.fop.maze.game.achievement.CareerData;

/**
 * 统一存储管理器 (Storage Manager)
 * <p>
 * 职责：
 * 1. 负责所有游戏数据的持久化 (JSON 格式)。
 * 2. 管理单局存档 (save_data.json)。
 * 3. 管理生涯档案 (career_data.json)。
 * <p>
 * 使用 LibGDX 的 {@link Json} 工具类进行序列化。
 */
public class StorageManager {

    private static final String SAVE_FILE_NAME = "save_data.json";
    private static final String CAREER_FILE_NAME = "career_data.json";

    private final Json json;

    public StorageManager() {
        this.json = new Json();
        // 设置输出格式为 JSON 标准格式 (避免 LibGDX 默认的压缩格式难以阅读)
        this.json.setOutputType(JsonWriter.OutputType.json);
        // 不使用原型模式，确保所有字段都写入，避免版本兼容问题
        this.json.setUsePrototypes(false);
    }

    // ==========================================
    // 1. 单局存档 (GameSaveData)
    // ==========================================

    /**
     * 保存当前单局进度
     * @param data 要保存的数据对象
     */
    public void saveGame(GameSaveData data) {
        if (data == null) return;
        try {
            FileHandle file = getFile(SAVE_FILE_NAME);
            String jsonStr = json.toJson(data);
            file.writeString(jsonStr, false); // false = 覆盖模式
            Logger.info("Game progress saved to " + file.path());
        } catch (Exception e) {
            Logger.error("Failed to save game data: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 读取单局存档
     * @return 如果存在存档则返回 GameSaveData 对象，否则返回 null
     */
    public GameSaveData loadGame() {
        FileHandle file = getFile(SAVE_FILE_NAME);
        if (!file.exists()) {
            Logger.info("No save file found.");
            return null;
        }

        try {
            String jsonStr = file.readString();
            GameSaveData data = json.fromJson(GameSaveData.class, jsonStr);
            Logger.info("Game progress loaded successfully.");
            return data;
        } catch (Exception e) {
            Logger.error("Failed to load save data (file might be corrupted): " + e.getMessage());
            return null; // 读取失败视作无存档
        }
    }

    /**
     * 删除单局存档 (例如玩家死亡或主动重置时调用)
     */
    public void deleteSave() {
        FileHandle file = getFile(SAVE_FILE_NAME);
        if (file.exists()) {
            file.delete();
            Logger.info("Save file deleted.");
        }
    }

    /**
     * 检查是否存在存档
     */
    public boolean hasSaveFile() {
        return getFile(SAVE_FILE_NAME).exists();
    }

    // ==========================================
    // 2. 生涯档案 (CareerData)
    // ==========================================

    /**
     * 保存生涯数据 (成就、统计)
     * 建议在每次解锁成就或关卡结束时调用
     */
    public void saveCareer(CareerData data) {
        if (data == null) return;
        try {
            FileHandle file = getFile(CAREER_FILE_NAME);
            String jsonStr = json.toJson(data);
            file.writeString(jsonStr, false);
            // 生涯保存频率较高，可以把 log 级别设为 debug 避免刷屏
            Logger.debug("Career data saved.");
        } catch (Exception e) {
            Logger.error("Failed to save career data: " + e.getMessage());
        }
    }

    /**
     * 读取生涯数据
     * @return 总是返回一个对象。如果文件不存在，则返回一个新的空白 CareerData。
     */
    public CareerData loadCareer() {
        FileHandle file = getFile(CAREER_FILE_NAME);
        if (!file.exists()) {
            Logger.info("No career data found, creating new profile.");
            return new CareerData();
        }

        try {
            String jsonStr = file.readString();
            CareerData data = json.fromJson(CareerData.class, jsonStr);
            Logger.info("Career data loaded successfully.");
            return data;
        } catch (Exception e) {
            Logger.error("Failed to load career data, resetting to default: " + e.getMessage());
            return new CareerData(); // 读取失败时回退到默认，防止游戏崩溃
        }
    }

    // ==========================================
    // 3. 工具方法
    // ==========================================

    /**
     * 获取文件句柄 (Local Storage)
     * Windows/Linux: 也就是 jar 包同级目录或 user home 下
     * Android: App 私有存储空间
     */
    private FileHandle getFile(String fileName) {
        return Gdx.files.local(fileName);
    }
}