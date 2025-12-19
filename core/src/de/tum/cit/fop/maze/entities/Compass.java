// Compass.java - 新增的指南针类
package de.tum.cit.fop.maze.entities;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import de.tum.cit.fop.maze.utils.Logger;


public class Compass {
    private Player player;
    private ExitDoor nearestExit;
    private float angle = 0;
    private boolean active = true;
    private BitmapFont font;
    private GlyphLayout glyphLayout;


    public Compass(Player player) {
        this.player = player;
        this.font = new BitmapFont();
        this.font.getData().setScale(0.8f);
        this.glyphLayout = new GlyphLayout();
        Logger.debug("Compass initialized for player");
    }

    public void update(ExitDoor nearestExit) {
        this.nearestExit = nearestExit;

        if (nearestExit != null) {
            // 计算指向最近出口的角度
            float deltaX = nearestExit.getX() - player.getX();
            float deltaY = nearestExit.getY() - player.getY();
            angle = MathUtils.atan2(deltaY, deltaX) * MathUtils.radiansToDegrees;
            active = true;
        } else {
            active = false;
        }
    }

    /**
     * 绘制指南针（UI模式）
     */
    public void drawAsUI(ShapeRenderer shapeRenderer, SpriteBatch batch) {
        if (!active || nearestExit == null) {
            return;
        }

        // 固定在屏幕右上角
        float compassX = Gdx.graphics.getWidth() - 100;
        float compassY = Gdx.graphics.getHeight() - 100;

        // 根据状态选择颜色
        //TODO- 指南针贴图
        Color arrowColor = nearestExit.isLocked() ? Color.YELLOW : Color.GREEN;
        String statusText = nearestExit.isLocked() ? "需要钥匙" : "出口";

        // 绘制指南针箭头
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(arrowColor);

        float arrowSize = 20f;
        float[] vertices = calculateArrowVertices(compassX, compassY, angle, arrowSize);

        shapeRenderer.triangle(
            vertices[0], vertices[1],
            vertices[2], vertices[3],
            vertices[4], vertices[5]
        );

        // 在箭头下方添加状态指示方块

        shapeRenderer.rect(compassX - 5, compassY - 25, 10, 10);
        shapeRenderer.end();

        // ************* 注意：batch应该由调用者管理，这里不调用begin/end *************
        font.setColor(arrowColor);
        glyphLayout.setText(font, statusText);
        float textX = compassX - glyphLayout.width / 2;
        float textY = compassY - 45;
        font.draw(batch, statusText, textX, textY);
    }

    /**
     * 计算箭头三角形的三个顶点
     */
    /**
     * 计算箭头顶点
     */
    private float[] calculateArrowVertices(float x, float y, float angle, float arrowSize) {
        float[] vertices = new float[6];

        // 顶点1：箭头尖端
        vertices[0] = x + MathUtils.cosDeg(angle) * arrowSize;
        vertices[1] = y + MathUtils.sinDeg(angle) * arrowSize;

        // 顶点2：箭头左侧
        vertices[2] = x + MathUtils.cosDeg(angle + 150) * arrowSize * 0.7f;
        vertices[3] = y + MathUtils.sinDeg(angle + 150) * arrowSize * 0.7f;

        // 顶点3：箭头右侧
        vertices[4] = x + MathUtils.cosDeg(angle - 150) * arrowSize * 0.7f;
        vertices[5] = y + MathUtils.sinDeg(angle - 150) * arrowSize * 0.7f;

        return vertices;
    }

    /**
     * 计算到最近出口的距离
     */
    private float calculateDistanceToExit() {
        if (nearestExit == null) return 0;
        float dx = nearestExit.getX() - player.getX();
        float dy = nearestExit.getY() - player.getY();
        return (float) Math.sqrt(dx * dx + dy * dy);
    }

    /**
     * 获取最近的出口
     */
    public ExitDoor getNearestExit() {
        return nearestExit;
    }

    /**
     * 设置是否激活
     */
    public void setActive(boolean active) {
        this.active = active;
    }

    /**
     * 检查指南针是否激活
     */
    public boolean isActive() {
        return active && nearestExit != null;
    }

    public void dispose() {
        if (font != null) {
            font.dispose();
        }
    }
}
