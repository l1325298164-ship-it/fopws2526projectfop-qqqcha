package de.tum.cit.fop.maze.utils;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Json;
import de.tum.cit.fop.maze.game.GameManager;
import de.tum.cit.fop.maze.game.GameSaveData;
import de.tum.cit.fop.maze.entities.Player;

public class SaveManager {
    private static final String SAVE_FILE = "savegame.json";

    // 💾 保存游戏
    public static void saveGame(GameManager gameManager) {
        if (gameManager.getPlayer() == null) return;

        Player player = gameManager.getPlayer();
        GameSaveData data = new GameSaveData();

        // 1. 记录关卡
        data.currentLevel = gameManager.getCurrentLevel();

        // 🔥 【关键修改】启用分数保存
        data.score = player.getScore();

        // 2. 记录属性
        data.lives = player.getLives();
        data.maxLives = player.getMaxLives();
        data.mana = player.getMana();
        data.hasKey = player.hasKey();

        // 3. 记录 Buff
        data.buffAttack = player.hasBuffAttack();
        data.buffRegen = player.hasBuffRegen();
        data.buffManaEfficiency = player.hasBuffManaEfficiency();

        // 写入文件
        Json json = new Json();
        FileHandle file = Gdx.files.local(SAVE_FILE);
        file.writeString(json.toJson(data), false);

        Logger.info("Game Saved! Score: " + data.score + ", Level: " + data.currentLevel);
    }

    // 📂 读取游戏
    public static GameSaveData loadGame() {
        FileHandle file = Gdx.files.local(SAVE_FILE);
        if (!file.exists()) return null;

        Json json = new Json();
        try {
            return json.fromJson(GameSaveData.class, file.readString());
        } catch (Exception e) {
            Logger.error("Failed to load save file: " + e.getMessage());
            return null;
        }
    }

    // 检查是否有存档
    public static boolean hasSaveFile() {
        return Gdx.files.local(SAVE_FILE).exists();
    }
}