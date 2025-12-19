// HUD.java - 更新版本
package de.tum.cit.fop.maze.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Matrix4;
import de.tum.cit.fop.maze.entities.Compass;
import de.tum.cit.fop.maze.game.GameManager;
import de.tum.cit.fop.maze.utils.Logger;
import de.tum.cit.fop.maze.utils.TextureManager;

public class HUD {
    private BitmapFont font;
    private GameManager gameManager;
    private ShapeRenderer shapeRenderer; // 用于绘制指南针
    private TextureManager textureManager;

    public HUD(GameManager gameManager) {
        this.gameManager = gameManager;
        this.font = new BitmapFont();
        this.font.getData().setScale(1.2f);
        this.shapeRenderer = new ShapeRenderer();
        this.textureManager = TextureManager.getInstance();
        Logger.debug("HUD initialized with compass support");
    }

    /**
     * 渲染游戏进行中的UI
     */
    public void renderInGameUI(SpriteBatch uiBatch) {
        try {
            // 1. 钥匙状态
            if (gameManager.getPlayer().hasKey()) {
                font.setColor(Color.GREEN);
                font.draw(uiBatch, "key: get", 20, Gdx.graphics.getHeight() - 40);
            } else {
                font.setColor(Color.YELLOW);
                font.draw(uiBatch, "key: needed", 20, Gdx.graphics.getHeight() - 40);
            }

            // 2. 生命值
            font.setColor(Color.RED);
            font.draw(uiBatch, "life: " + gameManager.getLives() + "/" + gameManager.getMaxLives(),
                20, Gdx.graphics.getHeight() - 80);

            // 3. 关卡信息
            font.setColor(Color.CYAN);
            font.draw(uiBatch, "start: " + gameManager.getCurrentLevel(),
                20, Gdx.graphics.getHeight() - 120);

            // 4. 操作说明
            font.setColor(Color.WHITE);
            font.draw(uiBatch, "direction buttons to move，Shift to sprint",
                20, Gdx.graphics.getHeight() - 160);

            // 5. 纹理模式提示
            TextureManager.TextureMode currentMode = textureManager.getCurrentMode();
            if (currentMode != TextureManager.TextureMode.COLOR) {
                font.setColor(Color.GREEN);
                font.draw(uiBatch, "mode: " + currentMode + " (F1-F4 to switch)",
                    Gdx.graphics.getWidth() - 250,
                    Gdx.graphics.getHeight() - 20);
            }

        } catch (Exception e) {
            Logger.error("Error rendering in-game UI", e);
        }
    }

    /**
     * 渲染指南针（UI模式）
     */
    public void renderCompassAsUI() {
        if (gameManager == null || gameManager.getCompass() == null) {
            return;
        }

        Compass compass = gameManager.getCompass();
        if (!compass.isActive()) {
            return;
        }

        try {
            // 保存原始的投影矩阵
            Matrix4 originalMatrix = shapeRenderer.getProjectionMatrix().cpy();

            // 设置ShapeRenderer使用屏幕坐标
            shapeRenderer.setProjectionMatrix(
                new Matrix4()
                    .setToOrtho2D(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight())
            );

            // 创建一个临时的SpriteBatch用于指南针文字
            SpriteBatch compassBatch = new SpriteBatch();
            compassBatch.setProjectionMatrix(
                new Matrix4()
                    .setToOrtho2D(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight())
            );

            // 调用指南针的绘制方法
            compassBatch.begin();
            compass.drawAsUI(shapeRenderer, compassBatch);
            compassBatch.end();

            compassBatch.dispose();

            // 恢复原始的投影矩阵
            shapeRenderer.setProjectionMatrix(originalMatrix);

            Logger.debug("Compass rendered as UI by HUD");

        } catch (Exception e) {
            Logger.error("Error rendering compass as UI in HUD", e);
        }
    }

    /**
     * 渲染游戏结束画面
     */
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

    /**
     * 渲染游戏结束画面
     */
    public void renderGameOver(SpriteBatch batch) {
        String message = "游戏结束！";
        font.getData().setScale(2);
        font.setColor(Color.RED);

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
        if (font != null) {
            font.dispose();
        }
        if (shapeRenderer != null) {
            shapeRenderer.dispose();
        }
        Logger.debug("HUD disposed");
    }
}
