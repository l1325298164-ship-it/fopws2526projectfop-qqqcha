package de.tum.cit.fop.maze.effects.Player.combat;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import de.tum.cit.fop.maze.effects.Player.combat.instances.*;
import de.tum.cit.fop.maze.utils.Logger;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * 战斗特效管理器
 * 负责管理所有的战斗相关视觉反馈 (VFX)，包括技能特效、打击感反馈、飘字等。
 */
public class CombatEffectManager {

    private static final int MAX_EFFECTS = 300;
    private final List<CombatEffect> effects;
    private final CombatParticleSystem particleSystem;

    // 字体资源
    private final BitmapFont scoreFont;
    private final BitmapFont textFont;

    public CombatEffectManager() {
        this.effects = new ArrayList<>();
        this.particleSystem = new CombatParticleSystem();

        // --- 初始化字体 ---
        BitmapFont tmpScoreFont;
        try {
            if (Gdx.files.internal("ui/font.fnt").exists()) {
                tmpScoreFont = new BitmapFont(Gdx.files.internal("ui/font.fnt"));
            } else {
                tmpScoreFont = new BitmapFont(); // fallback
            }
        } catch (Exception e) {
            tmpScoreFont = new BitmapFont();
        }
        this.scoreFont = tmpScoreFont;
        this.scoreFont.setUseIntegerPositions(false);
        this.scoreFont.getData().setScale(0.8f);

        this.textFont = new BitmapFont();
        this.textFont.setUseIntegerPositions(false);
        this.textFont.getData().setScale(1.0f);
    }

    public void update(float delta) {
        // 1. 更新粒子系统
        particleSystem.update(delta);

        // 2. 更新所有特效实体
        Iterator<CombatEffect> iterator = effects.iterator();
        while (iterator.hasNext()) {
            CombatEffect effect = iterator.next();

            // ✅ [修复] 必须传入 particleSystem，解决基类方法签名不匹配的问题
            effect.update(delta, particleSystem);

            if (effect.isFinished()) {
                iterator.remove();
            }
        }
    }

    public void renderShapes(ShapeRenderer shapeRenderer) {
        if (shapeRenderer == null) {
            Logger.warning("ShapeRenderer is null, cannot render combat effect shapes");
            return;
        }
        // 渲染特效的几何形状
        for (CombatEffect effect : effects) {
            effect.renderShape(shapeRenderer);
        }
        // 渲染粒子
        particleSystem.render(shapeRenderer);
    }

    public void renderSprites(SpriteBatch batch) {
        if (batch == null) {
            Logger.warning("SpriteBatch is null, cannot render combat effect sprites");
            return;
        }
        // 渲染特效的贴图
        for (CombatEffect effect : effects) {
            effect.renderSprite(batch);
        }
    }

    /**
     * 安全添加特效，防止列表无限膨胀
     */
    private void safeAddEffect(CombatEffect effect) {
        if (effects.size() >= MAX_EFFECTS) {
            if (!effects.isEmpty()) effects.remove(0);
        }
        effects.add(effect);
    }

    // =========================================================
    // 🔥 战斗反馈 (Combat Feedback Juice)
    // =========================================================

    /**
     * 生成受击火花
     */
    public void spawnHitSpark(float x, float y) {
        safeAddEffect(new HitSparkEffect(x, y));
    }

    /**
     * 生成杀意波动
     */
    public void spawnAggroPulse(float x, float y) {
        safeAddEffect(new AggroPulseEffect(x, y));
    }

    /**
     * 敌人死亡爆炸特效
     */
    public void spawnEnemyDeathEffect(float x, float y) {
        for (int i = 0; i < 12; i++) {
            particleSystem.spawn(
                    x + MathUtils.random(-15, 15),
                    y + MathUtils.random(-15, 15),
                    Color.GRAY,
                    MathUtils.random(-60, 60),
                    MathUtils.random(-60, 60),
                    MathUtils.random(5, 10),
                    MathUtils.random(0.5f, 0.8f),
                    true,
                    false
            );
        }
    }

    // =========================================================
    // 🔮 魔法技能特效 (Magic Ability)
    // =========================================================

    public void spawnMagicCircle(float x, float y, float radius, float duration) {
        safeAddEffect(new MagicCircleEffect(x, y, radius, duration));
    }

    // 兼容接口
    public void spawnMagicCircle(float x, float y) {
        spawnMagicCircle(x, y, 64f, 1.0f);
    }

    public void spawnMagicPillar(float x, float y, float radius) {
        safeAddEffect(new MagicPillarEffect(x, y, radius));
    }

    // 兼容接口
    public void spawnMagicPillar(float x, float y) {
        spawnMagicPillar(x, y, 64f);
    }

    public void spawnMagicEssence(float startX, float startY, float targetX, float targetY) {
        safeAddEffect(new MagicEssenceEffect(startX, startY, targetX, targetY));
    }

    // 兼容接口
    public void spawnMagicEssence(float targetX, float targetY) {
        float startX = targetX + MathUtils.random(-100, 100);
        float startY = targetY + MathUtils.random(-100, 100);
        spawnMagicEssence(startX, startY, targetX, targetY);
    }

    // =========================================================
    // ⚔️ 玩家技能与动作 (Player Actions)
    // =========================================================

    public void spawnSlash(float x, float y, float angle, int type) {
        safeAddEffect(new SlashEffect(x, y, angle, type));
    }

    // ✅ [修复] 这里的调用需要配合 DashEffect 的新构造函数
    public void spawnDash(float x, float y, float directionAngle) {
        safeAddEffect(new DashEffect(x, y, directionAngle));
    }

    public void spawnFireMagic(float x, float y) {
        safeAddEffect(new FireMagicEffect(x, y));
    }

    public void spawnHeal(float x, float y) {
        safeAddEffect(new HealEffect(x, y));
    }

    public void spawnLaser(float startX, float startY, float endX, float endY) {
        safeAddEffect(new LaserEffect(startX, startY, endX, endY));
    }

    // =========================================================
    // 💬 UI 与 飘字 (Floating Text)
    // =========================================================

    public void spawnScoreText(float x, float y, int score) {
        if (score == 0) return;
        String text = (score > 0 ? "+" : "") + score;
        Color color = (score > 0) ? Color.GOLD : Color.RED;
        safeAddEffect(new FloatingTextEffect(x, y, text, color, scoreFont));
    }

    public void spawnStatusText(float x, float y, String text, Color color) {
        if (text == null || text.isEmpty()) return;
        safeAddEffect(new FloatingTextEffect(x, y, text, color, textFont));
    }

    // 兼容接口
    public void spawnFloatingText(float x, float y, String text, Color color) {
        spawnStatusText(x, y, text, color);
    }

    public void spawnFloatingText(float x, float y, int value, boolean isCrit) {
        Color c = isCrit ? Color.GOLD : Color.RED;
        spawnStatusText(x, y, String.valueOf(value), c);
    }

    // =========================================================
    // 🗑️ 资源清理
    // =========================================================

    public void dispose() {
        effects.clear();
        if (scoreFont != null) scoreFont.dispose();
        if (textFont != null) textFont.dispose();
        particleSystem.clear();
    }
}