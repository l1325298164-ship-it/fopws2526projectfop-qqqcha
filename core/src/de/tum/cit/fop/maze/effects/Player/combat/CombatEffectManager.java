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

    private static final int MAX_EFFECTS = 300;
    private final List<CombatEffect> effects;
    private final CombatParticleSystem particleSystem;

    private final BitmapFont scoreFont;
    private final BitmapFont textFont;

    public CombatEffectManager() {
        this.effects = new ArrayList<>();
        this.particleSystem = new CombatParticleSystem();

        BitmapFont tmpScoreFont;
        try {
            if (Gdx.files.internal("ui/font.fnt").exists()) {
                tmpScoreFont = new BitmapFont(Gdx.files.internal("ui/font.fnt"));
            } else {
                tmpScoreFont = new BitmapFont();
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
        particleSystem.update(delta);
        Iterator<CombatEffect> iterator = effects.iterator();
        while (iterator.hasNext()) {
            CombatEffect effect = iterator.next();
            effect.update(delta, particleSystem);
            if (effect.isFinished()) {
                iterator.remove();
            }
        }
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
            if (!effects.isEmpty()) effects.remove(0);
        }
        effects.add(effect);
    }

    // ... (其他原有方法保持不变，省略以节省空间) ...

    public void spawnHitSpark(float x, float y) {
        safeAddEffect(new HitSparkEffect(x, y));
    }

    public void spawnAggroPulse(float x, float y) {
        safeAddEffect(new AggroPulseEffect(x, y));
    }

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

    public void spawnMagicCircle(float x, float y, float radius, float duration) {
        safeAddEffect(new MagicCircleEffect(x, y, radius, duration));
    }

    public void spawnMagicCircle(float x, float y) {
        spawnMagicCircle(x, y, 64f, 1.0f);
    }

    public void spawnMagicPillar(float x, float y, float radius) {
        safeAddEffect(new MagicPillarEffect(x, y, radius));
    }

    public void spawnMagicPillar(float x, float y) {
        spawnMagicPillar(x, y, 64f);
    }

    public void spawnMagicEssence(float startX, float startY, float targetX, float targetY) {
        safeAddEffect(new MagicEssenceEffect(startX, startY, targetX, targetY));
    }

    public void spawnMagicEssence(float targetX, float targetY) {
        float startX = targetX + MathUtils.random(-100, 100);
        float startY = targetY + MathUtils.random(-100, 100);
        spawnMagicEssence(startX, startY, targetX, targetY);
    }

    public void spawnSlash(float x, float y, float angle, int type) {
        safeAddEffect(new SlashEffect(x, y, angle, type));
    }

    public void spawnDash(float x, float y, float directionAngle) {
        safeAddEffect(new DashEffect(x, y, directionAngle));
    }

    // 🔥 [修复] 新增重载方法，解决 DashAbility 报错
    public void spawnDash(float x, float y, float directionAngle, int level) {
        safeAddEffect(new DashEffect(x, y, directionAngle, level));
    }

    public void spawnHeal(float x, float y) {
        safeAddEffect(new HealEffect(x, y));
    }

    public void spawnLaser(float startX, float startY, float endX, float endY) {
        safeAddEffect(new LaserEffect(startX, startY, endX, endY));
    }


    // 🔥 [核心修改] 统一的分数飘字方法
    public void spawnScoreText(float x, float y, int score) {
        if (score == 0) return;
        String text = (score > 0 ? "+" : "") + score;
        Color color = (score > 0) ? Color.GOLD : Color.RED;

        FloatingTextEffect effect = new FloatingTextEffect(x, y, text, color, scoreFont);

        // 🔥 [关键] 统一设为 0.55f (让所有分数飘字都变成统一的小尺寸)
        effect.setTargetScale(0.55f);

        safeAddEffect(effect);
    }

    public void spawnStatusText(float x, float y, String text, Color color) {
        if (text == null || text.isEmpty()) return;
        safeAddEffect(new FloatingTextEffect(x, y, text, color, textFont));
    }

    public void spawnFloatingText(float x, float y, String text, Color color) {
        spawnStatusText(x, y, text, color);
    }

    public void spawnFloatingText(float x, float y, int value, boolean isCrit) {
        Color c = isCrit ? Color.GOLD : Color.RED;
        spawnStatusText(x, y, String.valueOf(value), c);
    }

    public void dispose() {
        effects.clear();
        if (scoreFont != null) scoreFont.dispose();
        if (textFont != null) textFont.dispose();
        particleSystem.clear();
    }
}