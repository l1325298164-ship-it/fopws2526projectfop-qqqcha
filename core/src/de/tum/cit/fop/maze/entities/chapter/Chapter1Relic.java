package de.tum.cit.fop.maze.entities.chapter;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import de.tum.cit.fop.maze.entities.GameObject;
import de.tum.cit.fop.maze.entities.Player;
import de.tum.cit.fop.maze.game.GameConstants;
import de.tum.cit.fop.maze.utils.Logger;
import de.tum.cit.fop.maze.utils.TextureManager;

public class Chapter1Relic extends GameObject {

    private final RelicData data;
    private final ChapterContext chapterContext;

    /** 本局是否移除（读/丢弃后立即从世界隐藏） */
    private boolean removedThisRun = false;

    private static Texture relicTexture;

    public Chapter1Relic(int x, int y, RelicData data, ChapterContext chapterContext) {
        super(x, y);
        this.data = data;
        this.chapterContext = chapterContext;

        // ✅ 新系统：如果这个 id 已经处理过（READ/DISCARDED），永远不再显示
        if (chapterContext != null && chapterContext.isRelicConsumed(data.id)) {
            removedThisRun = true;
            return;
        }

        if (relicTexture == null) {
            relicTexture = new Texture("imgs/Items/chapter1_relic.png");
        }

        Logger.gameEvent("📜 Relic spawned id=" + data.id + " at " + getPositionString());
    }

    @Override
    public void onInteract(Player player) {
        if (removedThisRun) {
            Logger.error("❌ onInteract called but relic already removed id=" + data.id);
            return;
        }
        if (player == null) {
            Logger.error("❌ onInteract called with null player id=" + data.id);
            return;
        }

        Logger.error("👉 RELIC INTERACT id=" + data.id);

        player.requestChapter1Relic(this);
    }

    /* ================= 玩家选择结果 ================= */

    public void onRead() {
        if (removedThisRun) {
            Logger.error("❌ onRead called but already removed id=" + data.id);
            return;
        }

        Logger.error("📖 RELIC READ CLICKED id=" + data.id);

        if (chapterContext != null) {
            chapterContext.markRelicRead(data.id);
        } else {
            Logger.error("❌ chapterContext is NULL onRead id=" + data.id);
        }

        removedThisRun = true;
    }

    public void onDiscard() {
        if (removedThisRun) {
            Logger.error("❌ onDiscard called but already removed id=" + data.id);
            return;
        }

        Logger.error("🗑 RELIC DISCARDED id=" + data.id);

        if (chapterContext != null) {
            chapterContext.markRelicDiscarded(data.id);
        }

        removedThisRun = true;
    }

    /* ================= 给 UI 取数据 ================= */

    public RelicData getData() {
        return data;
    }

    /* ================= GameObject ================= */

    @Override
    public boolean isInteractable() {
        return !removedThisRun;
    }

    @Override
    public boolean isPassable() {
        return true;
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
        // no-op
    }

    @Override
    public RenderType getRenderType() {
        TextureManager.TextureMode mode = TextureManager.getInstance().getCurrentMode();
        if (mode == TextureManager.TextureMode.IMAGE || mode == TextureManager.TextureMode.PIXEL) {
            return RenderType.SPRITE;
        }
        return RenderType.SHAPE;
    }
}
