package de.tum.cit.fop.maze.effects.Player.combat;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import de.tum.cit.fop.maze.effects.Player.combat.instances.*;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class CombatEffectManager {
    private final List<CombatEffect> effects;
    private final BitmapFont font; // 用于绘制飘字

    public CombatEffectManager() {
        this.effects = new ArrayList<>();
        // 加载字体：如果有自定义字体请替换路径，否则使用默认字体
        // 建议使用 Skin 中的字体以保持风格统一，这里为了独立性使用 new BitmapFont()
        try {
            this.font = new BitmapFont(Gdx.files.internal("ui/font.fnt"));
        } catch (Exception e) {
            // 如果找不到文件，回退到默认字体
            // this.font = new BitmapFont();
            throw new RuntimeException("Could not load font for CombatEffects. Ensure 'ui/font.fnt' exists.");
        }

        this.font.setUseIntegerPositions(false);
        this.font.getData().setScale(0.8f); // 稍微调小一点，避免太遮挡
    }

    public void update(float delta) {
        Iterator<CombatEffect> iterator = effects.iterator();
        while (iterator.hasNext()) {
            CombatEffect effect = iterator.next();
            effect.update(delta);
            if (effect.isFinished()) {
                iterator.remove();
            }
        }
    }

    public void draw(SpriteBatch batch) {
        for (CombatEffect effect : effects) {
            effect.draw(batch);
        }
    }

    public void drawDebug(ShapeRenderer shapeRenderer) {
        for (CombatEffect effect : effects) {
            effect.drawDebug(shapeRenderer);
        }
    }

    // ===== 生成特效的方法 =====

    public void spawnSlash(float x, float y, float angle, int type) {
        effects.add(new SlashEffect(x, y, angle));
    }

    public void spawnDash(float x, float y, float directionAngle) {
        // 如果你有 DashEffect 类
        // effects.add(new DashEffect(x, y, directionAngle));
    }

    /**
     * 🔥 [Phase 4] 生成飘字
     */
    public void spawnFloatingText(float x, float y, String text, Color color) {
        effects.add(new FloatingTextEffect(x, y, text, color, font));
    }

    public void dispose() {
        if (font != null) font.dispose();
    }
}