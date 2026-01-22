package de.tum.cit.fop.maze.effects.Player.combat.instances;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import de.tum.cit.fop.maze.effects.Player.combat.CombatEffect;
import de.tum.cit.fop.maze.effects.Player.combat.CombatParticleSystem;

public class FloatingTextEffect extends CombatEffect {
    private String text;
    private Color color;
    private BitmapFont font;

    // 默认缩放，稍后会被 Manager 覆盖
    private float targetScale = 1.0f;

    public FloatingTextEffect(float x, float y, String text, Color color, BitmapFont font) {
        super(x, y, 1.0f); // 持续1秒
        this.text = text;
        this.color = color;
        this.font = font;
        // 记录字体当前的缩放值作为默认值
        this.targetScale = font.getData().scaleX;
    }

    // ✅ 新增：允许外部设置统一的缩放大小
    public void setTargetScale(float scale) {
        this.targetScale = scale;
    }

    @Override
    protected void onUpdate(float delta, CombatParticleSystem ps) {
        // 向上飘动
        y += delta * 50f;
    }

    @Override
    public void renderShape(ShapeRenderer sr) {
    }

    @Override
    public void renderSprite(SpriteBatch batch) {
        if (font == null) return;

        // 1. 保存旧状态
        Color oldColor = font.getColor();
        float oldScaleX = font.getData().scaleX;
        float oldScaleY = font.getData().scaleY;

        // 2. 计算透明度
        // 🔥 [调整] 乘以 0.8f，让它整体稍微透明一点
        float alpha = Math.max(0, 1f - (timer / maxDuration));
        font.setColor(color.r, color.g, color.b, alpha * 0.8f);

        // 3. 设置统一的“缩小版”尺寸
        font.getData().setScale(targetScale);

        // 4. 绘制 (无阴影)
        font.draw(batch, text, x, y);

        // 5. 恢复旧状态 (关键！防止影响全局字体)
        font.setColor(oldColor);
        font.getData().setScale(oldScaleX, oldScaleY);
    }
}