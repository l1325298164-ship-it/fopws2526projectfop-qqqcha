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
    // startY 似乎没有用到，如果想做基于 startY 的偏移计算可以保留，这里简化直接修改 y

    public FloatingTextEffect(float x, float y, String text, Color color, BitmapFont font) {
        super(x, y, 1.0f); // 持续1秒
        this.text = text;
        this.color = color;
        this.font = font;
    }

    @Override
    protected void onUpdate(float delta, CombatParticleSystem ps) {
        // 修正：将飘动逻辑从 update(delta) 移到这里
        y += delta * 30f;
    }

    @Override
    public void renderShape(ShapeRenderer sr) {
        // 文字不需要几何渲染，留空实现抽象方法
    }

    @Override
    public void renderSprite(SpriteBatch batch) { // 修正：重命名为 renderSprite
        // 保存旧颜色
        Color oldColor = font.getColor();

        // 设置颜色和透明度 (随时间淡出)
        // 注意：timer 和 maxDuration 是父类字段
        float alpha = Math.max(0, 1f - (timer / maxDuration));
        font.setColor(color.r, color.g, color.b, alpha);

        // 绘制文字 (居中)
        font.draw(batch, text, x, y);

        // 恢复
        font.setColor(oldColor);
    }
}