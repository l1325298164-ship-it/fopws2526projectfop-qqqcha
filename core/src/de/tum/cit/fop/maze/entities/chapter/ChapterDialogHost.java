package de.tum.cit.fop.maze.entities.chapter;


public interface ChapterDialogHost {

    /**
     * 打开章节文本对话框（relic / lore 通用）
     */
    void openChapterDialog(
            RelicData data,
            Runnable onRead,
            Runnable onDiscard
    );
}
