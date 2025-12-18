// GameUI.java - 更新版本
package de.tum.cit.fop.maze.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import de.tum.cit.fop.maze.game.GameConstants;
import de.tum.cit.fop.maze.utils.Logger;

public class HUD {
    private BitmapFont font;
    private KeyStatusUI keyStatusUI;
    private LivesUI livesUI;

    public HUD() {
        font = new BitmapFont();
        keyStatusUI = new KeyStatusUI();
        livesUI = new LivesUI();
        Logger.debug("GameUI initialized");
    }

    public void render(SpriteBatch batch, boolean hasKey, int lives, int maxLives) {
        keyStatusUI.render(batch, hasKey);
        livesUI.render(batch, lives, maxLives);

        // 绘制操作说明
        font.setColor(Color.WHITE);
        font.draw(batch, "方向键移动，Shift加速",
            GameConstants.UI_MARGIN,
            Gdx.graphics.getHeight() - GameConstants.UI_MARGIN);
    }


    // GameUI.java - 修复字体宽度计算
    public void renderGameOver(SpriteBatch batch) {
        String message = "游戏结束！";
        font.getData().setScale(2);
        font.setColor(Color.RED);

        // 使用 font.getBounds() 获取准确的文本宽度
        com.badlogic.gdx.graphics.g2d.GlyphLayout layout = new com.badlogic.gdx.graphics.g2d.GlyphLayout();
        layout.setText(font, message);

        float x = (Gdx.graphics.getWidth() - layout.width) / 2;
        float y = Gdx.graphics.getHeight() / 2;

        font.draw(batch, message, x, y);

        // 显示重新开始提示
        font.getData().setScale(1);
        font.setColor(Color.WHITE);
        String restartMsg = "按R键重新开始游戏";
        layout.setText(font, restartMsg);

        float restartX = (Gdx.graphics.getWidth() - layout.width) / 2;
        font.draw(batch, restartMsg, restartX, y - 50);
    }

    public void renderGameComplete(SpriteBatch batch) {
        String message = "恭喜！你成功逃出了迷宫！";
        font.getData().setScale(2);
        font.setColor(Color.GREEN);

        com.badlogic.gdx.graphics.g2d.GlyphLayout layout = new com.badlogic.gdx.graphics.g2d.GlyphLayout();
        layout.setText(font, message);

        float x = (Gdx.graphics.getWidth() - layout.width) / 2;
        float y = Gdx.graphics.getHeight() / 2;

        font.draw(batch, message, x, y);

        // 显示重新开始提示
        font.getData().setScale(1);
        font.setColor(Color.WHITE);
        String restartMsg = "按R键重新开始游戏";
        layout.setText(font, restartMsg);

        float restartX = (Gdx.graphics.getWidth() - layout.width) / 2;
        font.draw(batch, restartMsg, restartX, y - 50);
    }

    public BitmapFont getFont() {
        return font;
    }

    public void dispose() {
        font.dispose();
        keyStatusUI.dispose();
        livesUI.dispose();
        Logger.debug("GameUI disposed");
    }


}
