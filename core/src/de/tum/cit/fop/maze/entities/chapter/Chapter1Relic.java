package de.tum.cit.fop.maze.entities.chapter;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import de.tum.cit.fop.maze.entities.GameObject;
import de.tum.cit.fop.maze.entities.Player;
import de.tum.cit.fop.maze.game.GameConstants;
import de.tum.cit.fop.maze.tools.ChapterContext;
import de.tum.cit.fop.maze.utils.Logger;

public class Chapter1Relic extends GameObject {

    private final ChapterContext chapterContext;
    private boolean removed = false;

    private static Texture relicTexture;

    public Chapter1Relic(int x, int y, ChapterContext chapterContext) {
        super(x, y);
        this.chapterContext = chapterContext;

        if (relicTexture == null) {
            relicTexture = new Texture("Items/chapter1_relic.png");
        }

        Logger.gameEvent("📜 Chapter 1 Relic spawned at " + getPositionString());
    }

    @Override
    public void onInteract(Player player) {
        if (removed) return;

        // 👉 第一版：先直接调用 UI（下一步实现）
        player.openChapter1RelicDialog(this, chapterContext);
    }

    /** 玩家选择【阅读】 */
    public void onRead() {
        chapterContext.markChapter1RelicRead();
        removed = true;
        Logger.gameEvent("📖 Chapter 1 Relic READ");
    }

    /** 玩家选择【丢弃】 */
    public void onDiscard() {
        removed = true;
        Logger.gameEvent("🗑 Chapter 1 Relic DISCARDED (will respawn next time)");
    }

    @Override
    public boolean isInteractable() {
        return !removed;
    }

    @Override
    public boolean isPassable() {
        return true;
    }

    @Override
    public void drawShape(ShapeRenderer shapeRenderer) {

    }

    @Override
    public void drawSprite(SpriteBatch batch) {
        if (removed) return;

        batch.draw(
                relicTexture,
                x * GameConstants.CELL_SIZE,
                y * GameConstants.CELL_SIZE,
                GameConstants.CELL_SIZE,
                GameConstants.CELL_SIZE
        );
    }

    @Override
    public RenderType getRenderType() {
        return null;
    }
}
