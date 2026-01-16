package de.tum.cit.fop.maze.game.story;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.JsonWriter;

import java.util.HashMap;
import java.util.Map;

/**
 * 🔒 StoryProgress
 * =========================
 * 剧情模式【永久进度存档】
 *
 * ❗ 与 GameSaveData 完全独立
 * ❗ 只在关键剧情节点写入
 * ❗ 手动删文件才能重置
 */
public final class StoryProgress {

    private static final String FILE_NAME = "story_progress.json";
    private static StoryProgress instance;

    private final Map<String, ChapterProgress> chapters = new HashMap<>();

    /* =======================
       Singleton
       ======================= */

    private StoryProgress() {}

    public static StoryProgress load() {
        if (instance != null) return instance;

        FileHandle file = Gdx.files.local(FILE_NAME);
        Json json = new Json();

        if (file.exists()) {
            try {
                instance = json.fromJson(StoryProgress.class, file);
            } catch (Exception e) {
                Gdx.app.error("StoryProgress", "Failed to load, creating new", e);
                instance = new StoryProgress();
            }
        } else {
            instance = new StoryProgress();
        }

        return instance;
    }

    public void save() {
        Json json = new Json();
        json.setOutputType(JsonWriter.OutputType.json);
        json.setUsePrototypes(false);

        FileHandle file = Gdx.files.local(FILE_NAME);
        file.writeString(json.prettyPrint(this), false);
    }


    /* =======================
       Chapter Access
       ======================= */

    private ChapterProgress chapter(int chapterId) {
        String key = "chapter" + chapterId;
        return chapters.computeIfAbsent(key, k -> new ChapterProgress());
    }

    /* =======================
       Query API
       ======================= */

    public boolean isPvWatched(int chapterId) {
        return chapter(chapterId).pvWatched;
    }

    public boolean isTutorialUnlocked(int chapterId) {
        return chapter(chapterId).tutorialUnlocked;
    }

    public boolean isBossUnlocked(int chapterId) {
        return chapter(chapterId).bossUnlocked;
    }

    public boolean isBossDefeated(int chapterId) {
        return chapter(chapterId).bossDefeated;
    }

    public boolean isChapterFinished(int chapterId) {
        return chapter(chapterId).chapterFinished;
    }

    /* =======================
       Mark API (写入点)
       ======================= */

    /** 存档点①：PV 播放完点击继续 */
    public void markPvWatched(int chapterId) {
        ChapterProgress c = chapter(chapterId);
        c.pvWatched = true;
        c.tutorialUnlocked = true;
    }

    /** 存档点②：点击「迎战 Boss」 */
    public void markBossUnlocked(int chapterId) {
        ChapterProgress c = chapter(chapterId);
        c.bossUnlocked = true;
    }

    /** 存档点③：Boss 战结束 */
    public void markBossDefeated(int chapterId) {
        ChapterProgress c = chapter(chapterId);
        c.bossDefeated = true;
        c.chapterFinished = true;
    }

    /* =======================
       Debug / Reset
       ======================= */

    /** ❗ 仅调试用：删除永久剧情存档 */
    public static void deleteAll() {
        FileHandle file = Gdx.files.local(FILE_NAME);
        if (file.exists()) file.delete();
        instance = null;
    }

    /* =======================
       JSON Model
       ======================= */

    public static class ChapterProgress {
        public boolean pvWatched = false;
        public boolean tutorialUnlocked = false;
        public boolean bossUnlocked = false;
        public boolean bossDefeated = false;
        public boolean chapterFinished = false;
    }
}
