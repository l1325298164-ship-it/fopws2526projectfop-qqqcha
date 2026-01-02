package de.tum.cit.fop.maze.utils;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Json;
import java.util.Collections;

public class LeaderboardManager {
    private static final String LEADERBOARD_FILE = "leaderboard.json";
    private static final int MAX_SCORES = 10; // 只保留前10名

    // 数据类 (内部静态类方便序列化)
    public static class HighScore implements Comparable<HighScore> {
        public String name;
        public int score;

        public HighScore() {} // Json 需要空构造函数

        public HighScore(String name, int score) {
            this.name = name;
            this.score = score;
        }

        @Override
        public int compareTo(HighScore other) {
            // 降序排列 (分数高的在前)
            return Integer.compare(other.score, this.score);
        }
    }

    private Array<HighScore> scores;

    public LeaderboardManager() {
        scores = new Array<>();
        load();
    }

    // 📥 添加分数 (如果够高的话)
    public void addScore(String name, int score) {
        scores.add(new HighScore(name, score));
        sortAndTrim();
        save();
    }

    // 检查这个分数是否有资格上榜
    public boolean isHighScore(int score) {
        if (scores.size < MAX_SCORES) return true;
        return score > scores.get(scores.size - 1).score;
    }

    public Array<HighScore> getScores() {
        return scores;
    }

    // 内部逻辑：排序并截断
    private void sortAndTrim() {
        scores.sort(); // 使用 compareTo 降序
        if (scores.size > MAX_SCORES) {
            scores.truncate(MAX_SCORES);
        }
    }

    // 💾 保存到本地
    private void save() {
        Json json = new Json();
        FileHandle file = Gdx.files.local(LEADERBOARD_FILE);
        file.writeString(json.toJson(scores), false);
    }

    // 📂 读取
    @SuppressWarnings("unchecked")
    private void load() {
        FileHandle file = Gdx.files.local(LEADERBOARD_FILE);
        if (file.exists()) {
            try {
                Json json = new Json();
                scores = json.fromJson(Array.class, HighScore.class, file);
                sortAndTrim(); //再一次确保排序
            } catch (Exception e) {
                Logger.error("Failed to load leaderboard");
            }
        }
    }
}