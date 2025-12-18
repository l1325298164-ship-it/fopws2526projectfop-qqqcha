// LivesUI.java
package de.tum.cit.fop.maze.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import de.tum.cit.fop.maze.game.GameConstants;

public class LivesUI {
    private BitmapFont font;

    public LivesUI() {
        font = new BitmapFont();
    }

    public void render(SpriteBatch batch, int lives, int maxLives) {
        // 在右上角显示生命值
        String livesText = "生命: " + lives + "/" + maxLives;
        font.setColor(lives > 1 ? Color.GREEN : Color.RED);
        font.draw(batch, livesText,
            Gdx.graphics.getWidth() - font.getSpaceXadvance() * livesText.length() - GameConstants.UI_MARGIN,
            Gdx.graphics.getHeight() - GameConstants.UI_MARGIN);
    }

    public void dispose() {
        font.dispose();
    }
}
