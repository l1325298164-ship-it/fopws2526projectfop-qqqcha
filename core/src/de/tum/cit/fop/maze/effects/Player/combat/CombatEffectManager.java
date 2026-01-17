package de.tum.cit.fop.maze.effects.Player.combat;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import de.tum.cit.fop.maze.effects.Player.combat.instances.*;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class CombatEffectManager {
    private static final int MAX_EFFECTS = 200;

    private final List<CombatEffect> effects;
    private final CombatParticleSystem particleSystem;

    // 分数专用字体 (Big Font, 0-9, + -)
    private final BitmapFont scoreFont;
    // 通用文本字体 (Small Font, A-Z, a-z, 0-9)
    private final BitmapFont textFont;

    private int maxEffectsInFrame = 0;
    private int effectsRemovedByLimit = 0;

    public CombatEffectManager() {
        this.effects = new ArrayList<>();
        this.particleSystem = new CombatParticleSystem();

        // 1. 加载分数大字体 (Big)
        BitmapFont tmpScoreFont;
        try {
            if (Gdx.files.internal("ui/font.fnt").exists()) {
                tmpScoreFont = new BitmapFont(Gdx.files.internal("ui/font.fnt"));
            } else {
                Gdx.app.error("CombatEffectManager", "ui/font.fnt not found, using default.");
                tmpScoreFont = new BitmapFont();
            }
        } catch (Exception e) {
            Gdx.app.error("CombatEffectManager", "Error loading score font: " + e.getMessage());
            tmpScoreFont = new BitmapFont();
        }
        this.scoreFont = tmpScoreFont;
        this.scoreFont.setUseIntegerPositions(false);
        this.scoreFont.getData().setScale(0.8f);

        // 2. 加载通用小字体 (Small)
        this.textFont = new BitmapFont();
        this.textFont.setUseIntegerPositions(false);
        this.textFont.getData().setScale(1.0f);
    }

    public void update(float delta) {
        particleSystem.update(delta);
        Iterator<CombatEffect> iterator = effects.iterator();
        while (iterator.hasNext()) {
            CombatEffect effect = iterator.next();
            effect.update(delta, particleSystem);
            if (effect.isFinished()) {
                iterator.remove();
            }
        }
        maxEffectsInFrame = Math.max(maxEffectsInFrame, effects.size());
    }

    public void renderShapes(ShapeRenderer shapeRenderer) {
        for (CombatEffect effect : effects) {
            effect.renderShape(shapeRenderer);
        }
        particleSystem.render(shapeRenderer);
    }

    public void renderSprites(SpriteBatch batch) {
        for (CombatEffect effect : effects) {
            effect.renderSprite(batch);
        }
    }

    private void safeAddEffect(CombatEffect effect) {
        if (effects.size() >= MAX_EFFECTS) {
            CombatEffect oldestEffect = null;
            float minRemainingTime = Float.MAX_VALUE;

            for (CombatEffect e : effects) {
                float remainingTime = e.maxDuration - e.timer;
                if (remainingTime < minRemainingTime && remainingTime > 0) {
                    minRemainingTime = remainingTime;
                    oldestEffect = e;
                }
            }
            if (oldestEffect != null) {
                effects.remove(oldestEffect);
            } else if (!effects.isEmpty()) {
                effects.remove(0);
            }
            effectsRemovedByLimit++;
        }
        effects.add(effect);
    }

    // ==========================================
    // 🔥 核心修改：强制样式分离
    // ==========================================

    /**
     * 【大字专用】仅用于显示分数
     * 自动处理颜色：正分金色，负分红色
     * 自动处理前缀：+ / -
     */
    public void spawnScoreText(float x, float y, int score) {
        if (score == 0) return;

        String text = (score > 0 ? "+" : "") + score;
        // 强制颜色：正分 GOLD，负分 RED
        Color color = (score > 0) ? Color.GOLD : Color.RED;

        safeAddEffect(new FloatingTextEffect(x, y, text, color, scoreFont));
    }

    /**
     * 【小字专用】用于 HP, KEY, BUFF 等
     * 使用默认字体，支持字母
     */
    public void spawnStatusText(float x, float y, String text, Color color) {
        if (text == null || text.isEmpty()) return;
        safeAddEffect(new FloatingTextEffect(x, y, text, color, textFont));
    }

    /**
     * [保留兼容] 如果外部还在调用这个，我们进行严格清洗
     */
    public void spawnFloatingText(float x, float y, String text, Color color) {
        if (text == null) return;

        // 强制清洗，防止 "SCORE" 或 "Key" 混入大字字体导致方框
        String cleanText = text.replace("SCORE", "")
                .replace("Score", "")
                .replace("KEY", "")
                .replace("Key", "")
                .replace("key", "")
                .replace(":", "")
                .trim();

        if (cleanText.isEmpty()) return;

        safeAddEffect(new FloatingTextEffect(x, y, cleanText, color, scoreFont));
    }

    // 🔥 新增：敌人死亡特效接口
    // ==========================================
    public void spawnEnemyDeathEffect(float x, float y) {
        // 生成一圈灰色的爆炸粒子 (8-10个)
        for (int i = 0; i < 10; i++) {
            // 参数: x, y, color, vx, vy, size, life, friction, gravity
            particleSystem.spawn(
                    x + MathUtils.random(-15, 15),       // 位置稍微随机一点
                    y + MathUtils.random(-15, 15),
                    Color.GRAY,                          // 颜色：灰色烟雾
                    MathUtils.random(-80, 80),           // X轴速度
                    MathUtils.random(-80, 80),           // Y轴速度
                    MathUtils.random(4, 8),              // 粒子大小
                    MathUtils.random(0.4f, 0.7f),        // 存活时间
                    true,                                // 开启阻力 (摩擦力)
                    false                                // 关闭重力
            );
        }
    }

    // ==========================================

    public void spawnSlash(float x, float y, float angle, int type) {
        safeAddEffect(new SlashEffect(x, y, angle, type));
    }

    public void spawnDash(float x, float y, float directionAngle) {
        safeAddEffect(new DashEffect(x, y, directionAngle));
    }

    public void spawnFireMagic(float x, float y) {
        safeAddEffect(new FireMagicEffect(x, y));
    }

    public void spawnHeal(float x, float y) {
        safeAddEffect(new HealEffect(x, y));
    }

    public void spawnDebuff(float x, float y) {
        safeAddEffect(new DebuffEffect(x, y));
    }

    public void spawnLaser(float startX, float startY, float endX, float endY) {
        safeAddEffect(new LaserEffect(startX, startY, endX, endY));
    }

    public String getPerformanceStats() {
        return String.format(
                "CombatEffects - Count: %d, Max: %d, Removed: %d",
                effects.size(),
                maxEffectsInFrame,
                effectsRemovedByLimit
        );
    }

    public void resetPerformanceStats() {
        maxEffectsInFrame = 0;
        effectsRemovedByLimit = 0;
    }

    public int getActiveEffectCount() {
        return effects.size();
    }

    public void dispose() {
        effects.clear();
        if (scoreFont != null) scoreFont.dispose();
        if (textFont != null) textFont.dispose();
        particleSystem.clear();
    }
}