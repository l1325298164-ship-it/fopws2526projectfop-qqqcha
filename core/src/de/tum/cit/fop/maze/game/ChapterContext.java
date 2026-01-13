package de.tum.cit.fop.maze.game;

/**
 * ChapterContext
 *
 * 章节上下文：
 * - 用来承载【一次章节流程中】的状态
 * - 不负责渲染 / 输入
 * - 不直接控制迷宫生成
 *
 * 职责：
 * 1. 保存章节 ID
 * 2. 保存“只在本章节有效”的规则 / 标记
 * 3. 提供给 GameManager / GameScreen 查询
 */
public class ChapterContext {

    /* =========================
       基础信息
       ========================= */

    private final int chapterId;

    private ChapterContext(int chapterId) {
        this.chapterId = chapterId;
    }

    /**
     * Chapter 1 的工厂方法
     * - 统一定义 Chapter 1 的初始规则
     * - 所有 Chapter 1 的 GameContext 都必须从这里创建
     */
    public static ChapterContext chapter1() {
        ChapterContext ctx = new ChapterContext(1);

        // ===== Chapter 1 初始规则 =====
        ctx.fogOverride = false;          // 是否强制雾（需要可改）
        ctx.chapter1RelicRead = false;    // 初次进入一定没读过 relic

        return ctx;
    }

    public int getChapterId() {
        return chapterId;
    }

    /* =========================
       通用章节规则（跨系统）
       ========================= */

    /**
     * 是否强制开启雾（覆盖 Difficulty 默认行为）
     * - HARD 难度下可以不用
     * - 章节可以强制开启
     */
    private boolean fogOverride = false;

    public boolean enableFogOverride() {
        return fogOverride;
    }

    public void setFogOverride(boolean enable) {
        this.fogOverride = enable;
    }

    /* =========================
       Chapter 1：章节道具 / 剧情状态
       ========================= */

    /**
     * Chapter 1 的章节道具是否已经被【阅读过】
     * - true  → 以后不会再生成
     * - false → 可以再次生成
     */
    private boolean chapter1RelicRead = false;

    public boolean isChapter1RelicRead() {
        return chapter1RelicRead;
    }

    public void markChapter1RelicRead() {
        this.chapter1RelicRead = true;
    }

    /* =========================
       Chapter 1：章节道具生成规则
       ========================= */

    /**
     * Chapter 1 是否应该生成章节道具
     *
     * 规则：
     * - 只在 Chapter 1
     * - 没有被阅读过
     */
    public boolean shouldSpawnChapter1Relic() {
        return chapterId == 1 && !chapter1RelicRead;
    }

    /* =========================
       Debug / 扩展
       ========================= */

    @Override
    public String toString() {
        return "ChapterContext{" +
                "chapterId=" + chapterId +
                ", fogOverride=" + fogOverride +
                ", chapter1RelicRead=" + chapter1RelicRead +
                '}';
    }
}
