package de.tum.cit.fop.maze.utils;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Json;

public class LeaderboardManager {
    private static final String LEADERBOARD_FILE = "leaderboard.json";
    private static final int MAX_SCORES = 10; // 只保留前10名

    // 数据类
    public static class HighScore implements Comparable<HighScore> {
        public String name;
        public int score;

        public HighScore() {}

        public HighScore(String name, int score) {
            this.name = name;
            this.score = score;
        }

        @Override
        public int compareTo(HighScore other) {
            // 降序排列
            return Integer.compare(other.score, this.score);
        }
    }

    private Array<HighScore> scores;

    public LeaderboardManager() {
        scores = new Array<>();
        load();
    }

    public void addScore(String name, int score) {
        scores.add(new HighScore(name, score));
        sortAndTrim();
        save();
    }

    public boolean isHighScore(int score) {
        if (scores.size < MAX_SCORES) return true;
        return score > scores.get(scores.size - 1).score;
    }

    public Array<HighScore> getScores() {
        return scores;
    }

    private void sortAndTrim() {
        scores.sort();
        if (scores.size > MAX_SCORES) {
            scores.truncate(MAX_SCORES);
        }
    }

    private void save() {
        Json json = new Json();
        FileHandle file = Gdx.files.local(LEADERBOARD_FILE);
        file.writeString(json.toJson(scores), false);
    }

    @SuppressWarnings("unchecked")
    private void load() {
        FileHandle file = Gdx.files.local(LEADERBOARD_FILE);
        if (file.exists()) {
            try {
                Json json = new Json();
                scores = json.fromJson(Array.class, HighScore.class, file);
                if (scores == null) {
                    scores = new Array<>();
                }
                sortAndTrim();
            } catch (Exception e) {
                // 如果加载失败，使用空列表
                scores = new Array<>();
                Logger.warning("Failed to load leaderboard: " + e.getMessage());
            }
        }
    }
}