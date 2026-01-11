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
    private static final int MAX_EFFECTS = 200;
    
    private final List<CombatEffect> effects;
    private final CombatParticleSystem particleSystem;
    private final BitmapFont font;
    
    private int maxEffectsInFrame = 0;
    private int effectsRemovedByLimit = 0;

    public CombatEffectManager() {
        this.effects = new ArrayList<>();
        this.particleSystem = new CombatParticleSystem();

        // 🔥 [修复] 安全加载字体，防止崩溃
        BitmapFont tmpFont;
        try {
            if (Gdx.files.internal("ui/font.fnt").exists()) {
                tmpFont = new BitmapFont(Gdx.files.internal("ui/font.fnt"));
            } else {
                Gdx.app.error("CombatEffectManager", "ui/font.fnt not found, using default font.");
                tmpFont = new BitmapFont(); // 使用 LibGDX 默认字体
            }
        } catch (Exception e) {
            Gdx.app.error("CombatEffectManager", "Error loading font: " + e.getMessage());
            tmpFont = new BitmapFont();
        }
        this.font = tmpFont;
        this.font.setUseIntegerPositions(false);
        this.font.getData().setScale(0.8f);
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

    public void spawnSlash(float x, float y, float angle, int type) {
        safeAddEffect(new SlashEffect(x, y, angle, type));
    }

    public void spawnFloatingText(float x, float y, String text, Color color) {
        safeAddEffect(new FloatingTextEffect(x, y, text, color, font));
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
        if (font != null) font.dispose();
        particleSystem.clear();
    }
}