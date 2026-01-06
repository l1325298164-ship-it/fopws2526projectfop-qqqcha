package de.tum.cit.fop.maze.effects.Player.combat.instances;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import de.tum.cit.fop.maze.effects.Player.combat.CombatEffect;

public class FloatingTextEffect extends CombatEffect {
    private String text;
    private Color color;
    private BitmapFont font;
    private float startY;

    public FloatingTextEffect(float x, float y, String text, Color color, BitmapFont font) {
        super(x, y, 1.0f); // 持续1秒
        this.text = text;
        this.color = color;
        this.font = font;
        this.startY = y;
    }

    @Override
    public void update(float delta) {
        super.update(delta);
        // 向上飘动
        y += delta * 30f;
    }

    @Override
    public void draw(SpriteBatch batch) {
        // 保存旧颜色
        Color oldColor = font.getColor();

        // 设置颜色和透明度 (随时间淡出)
        float alpha = Math.max(0, 1f - (time / duration));
        font.setColor(color.r, color.g, color.b, alpha);

        // 绘制文字 (居中)
        // 注意：font.draw 的坐标通常是文字左下角，这里简单处理
        font.draw(batch, text, x, y);

        // 恢复
        font.setColor(oldColor);
    }

    @Override
    public void drawDebug(ShapeRenderer shapeRenderer) {
        // 不需要 Debug 框
    }
}