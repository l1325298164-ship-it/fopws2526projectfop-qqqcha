// KeyStatusUI.java
package de.tum.cit.fop.maze.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import de.tum.cit.fop.maze.game.GameConstants;
import de.tum.cit.fop.maze.utils.TextureManager;

public class KeyStatusUI {
    private BitmapFont font;

    public KeyStatusUI() {
        font = new BitmapFont();
    }

    public void render(SpriteBatch batch, boolean hasKey) {
        TextureManager textureManager = TextureManager.getInstance();

        // 绘制钥匙图标
        if (hasKey) {
            batch.draw(textureManager.getKeyTexture(),
                GameConstants.UI_MARGIN,
                GameConstants.UI_MARGIN,
                GameConstants.ICON_SIZE,
                GameConstants.ICON_SIZE
            );
            font.setColor(Color.YELLOW);
            font.draw(batch, "已获得钥匙！可以出去了！",
                GameConstants.UI_MARGIN + GameConstants.ICON_SIZE + 10,
                GameConstants.UI_MARGIN + GameConstants.ICON_SIZE - 10);
        } else {
            batch.draw(textureManager.getWallTexture(),
                GameConstants.UI_MARGIN,
                GameConstants.UI_MARGIN,
                GameConstants.ICON_SIZE,
                GameConstants.ICON_SIZE
            );
            font.setColor(Color.GRAY);
            font.draw(batch, "需要找到钥匙才能离开",
                GameConstants.UI_MARGIN + GameConstants.ICON_SIZE + 10,
                GameConstants.UI_MARGIN + GameConstants.ICON_SIZE - 10);
        }
    }

    public void dispose() {
        font.dispose();
    }
}
