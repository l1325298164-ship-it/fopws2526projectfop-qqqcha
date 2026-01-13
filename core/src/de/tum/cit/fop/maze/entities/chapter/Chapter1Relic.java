package de.tum.cit.fop.maze.entities.chapter;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import de.tum.cit.fop.maze.entities.GameObject;
import de.tum.cit.fop.maze.entities.Player;
import de.tum.cit.fop.maze.game.ChapterContext;
import de.tum.cit.fop.maze.game.GameConstants;
import de.tum.cit.fop.maze.utils.Logger;
import de.tum.cit.fop.maze.utils.TextureManager;

public class Chapter1Relic extends GameObject {

    private final ChapterContext chapterContext;

    /** 本局是否移除（丢弃 or 阅读后） */
    private boolean removedThisRun = false;

    private static Texture relicTexture;

    public Chapter1Relic(int x, int y, ChapterContext chapterContext) {
        super(x, y);
        this.chapterContext = chapterContext;

        if (chapterContext.isChapter1RelicRead()) {
            removedThisRun = true;
            return;
        }

        if (relicTexture == null) {
            relicTexture = new Texture("Items/chapter1_relic.png");
        }

        Logger.gameEvent("📜 Chapter 1 Relic spawned at " + getPositionString());
    }

    @Override
    public void onInteract(Player player) {
        if (removedThisRun) return;

        // ⚠️ Entity 不直接创建 UI
        // 只通知 Player / GameManager
        player.requestChapter1Relic(this);
    }

    /* ================= 玩家选择结果 ================= */

    /** 玩家选择【阅读】 → 永久消失 */
    public void onRead() {
        chapterContext.markChapter1RelicRead();
        removedThisRun = true;
        Logger.gameEvent("📖 Chapter 1 Relic READ (permanent)");
    }

    /** 玩家选择【丢弃】 → 本局消失，下次还会生成 */
    public void onDiscard() {
        removedThisRun = true;
        Logger.gameEvent("🗑 Chapter 1 Relic DISCARDED (respawn next run)");
    }

    /* ================= GameObject ================= */

    @Override
    public boolean isInteractable() {
        return !removedThisRun;
    }

    @Override
    public boolean isPassable() {
        return true; // 踩过去不阻挡
    }

    @Override
    public void drawSprite(SpriteBatch batch) {
        if (removedThisRun || relicTexture == null) return;

        batch.draw(
                relicTexture,
                x * GameConstants.CELL_SIZE,
                y * GameConstants.CELL_SIZE,
                GameConstants.CELL_SIZE,
                GameConstants.CELL_SIZE
        );
    }

    @Override
    public void drawShape(ShapeRenderer shapeRenderer) {
        // 不需要 Shape fallback
    }

    @Override
    public RenderType getRenderType() {
        TextureManager.TextureMode mode =
                TextureManager.getInstance().getCurrentMode();

        if (mode == TextureManager.TextureMode.IMAGE
                || mode == TextureManager.TextureMode.PIXEL) {
            return RenderType.SPRITE;
        }

        return RenderType.SHAPE;
    }


}
