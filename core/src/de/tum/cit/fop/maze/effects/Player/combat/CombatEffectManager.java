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
    private static final int MAX_EFFECTS = 200; // 限制最大特效数量，防止内存溢出
    
    private final List<CombatEffect> effects;
    private final CombatParticleSystem particleSystem; // 粒子系统
    private final BitmapFont font;

    public CombatEffectManager() {
        this.effects = new ArrayList<>();
        this.particleSystem = new CombatParticleSystem();

        // 加载字体 (沿用你之前的逻辑)
        try {
            this.font = new BitmapFont(Gdx.files.internal("ui/font.fnt"));
        } catch (Exception e) {
            // fallback
            throw new RuntimeException("Could not load ui/font.fnt");
        }
        this.font.setUseIntegerPositions(false);
        this.font.getData().setScale(0.8f);
    }

    public void update(float delta) {
        // 1. 更新粒子系统
        particleSystem.update(delta);

        // 2. 更新所有特效逻辑
        Iterator<CombatEffect> iterator = effects.iterator();
        while (iterator.hasNext()) {
            CombatEffect effect = iterator.next();
            // 将粒子系统传给特效，让它能在 update 时生成新粒子
            effect.update(delta, particleSystem);
            if (effect.isFinished()) {
                iterator.remove();
            }
        }
    }

    /**
     * 🟢 阶段 1: 几何/粒子渲染 (ShapeRenderer)
     * 必须在 GameScreen 中单独调用，建议配合 Gdx.gl.GL_BLEND
     */
    public void renderShapes(ShapeRenderer shapeRenderer) {
        // 绘制特效的形状部分 (刀光、冲击波)
        for (CombatEffect effect : effects) {
            effect.renderShape(shapeRenderer);
        }
        // 绘制独立粒子 (火花)
        particleSystem.render(shapeRenderer);
    }

    /**
     * 🔵 阶段 2: 贴图/文字渲染 (SpriteBatch)
     * 必须在 batch.begin() 和 batch.end() 之间调用
     */
    public void renderSprites(SpriteBatch batch) {
        for (CombatEffect effect : effects) {
            effect.renderSprite(batch);
        }
    }

    // ===== 生成接口 =====
    
    /**
     * 安全添加特效，如果超过最大数量则移除最旧的特效
     */
    private void safeAddEffect(CombatEffect effect) {
        if (effects.size() >= MAX_EFFECTS) {
            // 移除最旧的特效（列表第一个）
            if (!effects.isEmpty()) {
                effects.remove(0);
            }
        }
        effects.add(effect);
    }

    public void spawnSlash(float x, float y, float angle, int type) {
        safeAddEffect(new SlashEffect(x, y, angle, type));
    }

    public void spawnFloatingText(float x, float y, String text, Color color) {
        safeAddEffect(new FloatingTextEffect(x, y, text, color, font));
    }

    // 1. 冲刺特效：需要坐标和角度
    // 对应 DashEffect(float x, float y, float directionAngle)
    public void spawnDash(float x, float y, float directionAngle) {
        safeAddEffect(new DashEffect(x, y, directionAngle));
    }

    // 2. 火焰魔法：目前逻辑是全方位(360度)喷射，仅需坐标
    // 对应 FireMagicEffect(float x, float y)
    public void spawnFireMagic(float x, float y) {
        safeAddEffect(new FireMagicEffect(x, y));
    }

    // 3. 治疗特效：仅需坐标
    // 对应 HealEffect(float x, float y)
    public void spawnHeal(float x, float y) {
        safeAddEffect(new HealEffect(x, y));
    }

    // 4. 负面状态：目前没有区分类型，仅需坐标
    // 对应 DebuffEffect(float x, float y)
    public void spawnDebuff(float x, float y) {
        safeAddEffect(new DebuffEffect(x, y));
    }

    // 5. 激光特效：需要起点和终点
    // 对应 LaserEffect(float startX, float startY, float endX, float endY)
    public void spawnLaser(float startX, float startY, float endX, float endY) {
        safeAddEffect(new LaserEffect(startX, startY, endX, endY));
    }

    // 如果你有其他的生成方法，请保留...

    public void dispose() {
        if (font != null) font.dispose();
        particleSystem.clear();
    }
}