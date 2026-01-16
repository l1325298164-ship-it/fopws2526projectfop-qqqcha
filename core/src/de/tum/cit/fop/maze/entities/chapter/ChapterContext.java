package de.tum.cit.fop.maze.entities.chapter;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Json;
import de.tum.cit.fop.maze.utils.Logger;

import java.util.*;
import java.util.stream.Collectors;
public class ChapterContext {

    private final int chapterId;
    private ChapterDropData dropData;

    // relicId -> state
    private final Map<String, RelicState> relicStates = new HashMap<>();

    // ⭐ 当前世界中是否已经有一张 relic
    private RelicData activeRelic = null;

    private boolean fogOverride = false;

    /* =========================
       Factory
       ========================= */

    public static ChapterContext chapter1() {
        ChapterContext ctx = new ChapterContext(1);
        ctx.loadDropData("chapters/chapter1_relics.json");
        return ctx;
    }

    private ChapterContext(int chapterId) {
        this.chapterId = chapterId;
    }

    /* =========================
       Init
       ========================= */

    private void loadDropData(String path) {
        Json json = new Json();
        dropData = json.fromJson(
                ChapterDropData.class,
                Gdx.files.internal(path)
        );

        for (RelicData r : dropData.relics) {
            relicStates.put(r.id, RelicState.UNTOUCHED);
        }
    }

    /* =========================
       🔥 New Relic Logic
       ========================= */

    /** 是否还能生成新的 relic */
    public boolean hasRemainingRelic() {
        return relicStates.values().stream()
                .anyMatch(s -> s == RelicState.UNTOUCHED);
    }

    /**
     * 请求生成一张 relic
     * - 如果已有 active relic → 返回 null
     * - 如果没有未读 relic → 返回 null
     */
    public RelicData requestRelic() {
        if (activeRelic != null) return null;

        List<RelicData> pool = dropData.relics.stream()
                .filter(r -> relicStates.get(r.id) == RelicState.UNTOUCHED)
                .toList();

        if (pool.isEmpty()) return null;

        activeRelic = pool.get(MathUtils.random(pool.size() - 1));
        return activeRelic;
    }

    /* =========================
       State Update
       ========================= */

    public void markRelicRead(String id) {
        relicStates.put(id, RelicState.READ);
        Logger.error("✅ CONTEXT MARK READ id=" + id);
        if (activeRelic != null && activeRelic.id.equals(id)) {
            activeRelic = null;
        }
    }

    public void markRelicDiscarded(String id) {
        relicStates.put(id, RelicState.DISCARDED);
        Logger.error("🟡 CONTEXT MARK DISCARDED id=" + id);
        if (activeRelic != null && activeRelic.id.equals(id)) {
            activeRelic = null;
        }
    }

    /* =========================
       Query
       ========================= */

    public boolean areAllRelicsRead() {
        return relicStates.values().stream()
                .allMatch(s -> s == RelicState.READ);
    }

    public boolean isRelicUntouched(String id) {
        return relicStates.get(id) == RelicState.UNTOUCHED;
    }

    public int getChapterId() {
        return chapterId;
    }

    /* =========================
       Fog
       ========================= */

    public boolean enableFogOverride() {
        return fogOverride;
    }

    public void setFogOverride(boolean enable) {
        this.fogOverride = enable;
    }
    public boolean isRelicConsumed(String id) {
        RelicState state = relicStates.get(id);
        return state != null && state != RelicState.UNTOUCHED;
    }

    public void dumpRelicStates() {
        for (var e : relicStates.entrySet()) {
            Logger.error("Relic " + e.getKey() + " -> " + e.getValue());
        }
    }
    private boolean bossUnlocked = false;

    public void markBossUnlocked() {
        bossUnlocked = true;
    }

    public boolean consumeBossUnlocked() {
        if (bossUnlocked) {
            bossUnlocked = false;
            return true;
        }
        return false;
    }

    private boolean bossPending = false;

    public void markBossPending() {
        Logger.error("👁 BossPending SET");
        bossPending = true;
    }

    public boolean consumeBossPending() {
        Logger.error("👁 BossPending CONSUME = " + bossPending);
        if (bossPending) {
            bossPending = false;
            return true;
        }
        return false;
    }

    public boolean areAllRelicsReadAfter(String justReadId) {
        relicStates.put(justReadId, RelicState.READ);

        for (RelicState s : relicStates.values()) {
            if (s != RelicState.READ) {
                return false;
            }
        }
        return true;
    }

}


