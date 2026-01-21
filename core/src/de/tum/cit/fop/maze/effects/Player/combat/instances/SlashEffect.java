package de.tum.cit.fop.maze.effects.Player.combat.instances;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import de.tum.cit.fop.maze.effects.Player.combat.CombatEffect;
import de.tum.cit.fop.maze.effects.Player.combat.CombatParticleSystem;

import java.util.ArrayList;
import java.util.List;

public class SlashEffect extends CombatEffect {
    private final float rotation;
    private final int level;
    private final Color coreColor;
    private final Color glowColor;

    private final List<Vector2> arcPoints = new ArrayList<>();
    private float shockwaveRadius = 0f;

    public SlashEffect(float x, float y, float rotation, int level) {
        super(x, y, 0.2f); //稍微增加一点持续时间到0.2s
        this.rotation = rotation;
        this.level = MathUtils.clamp(level, 1, 3);

        // --- 1. 颜色配置 (增强可见度) ---
        if (this.level == 1) {
            // L1: 纯白风刃
            this.coreColor = new Color(1f, 1f, 1f, 0.9f); // 提高不透明度
            this.glowColor = new Color(0.8f, 0.8f, 1f, 0.3f);
        } else if (this.level == 2) {
            // L2: 金色剑气
            this.coreColor = new Color(1f, 0.9f, 0.2f, 0.9f);
            this.glowColor = new Color(1f, 0.5f, 0f, 0.5f);
        } else {
            // L3: 青色能量斩
            this.coreColor = new Color(0.2f, 1f, 1f, 0.9f);
            this.glowColor = new Color(0f, 0.5f, 1f, 0.6f);
        }

        // --- 2. 形状参数 ---
        float radius = (level == 3) ? 75f : 55f; // 半径
        int segments = 12;
        float sweepAngle = 120f;
        float startAngle = rotation - sweepAngle / 2f;

        for (int i = 0; i <= segments; i++) {
            float progress = (float) i / segments;
            float angle = startAngle + (progress * sweepAngle);
            // 增加一点弧度变化，模拟挥动轨迹
            float r = radius + MathUtils.sin(progress * MathUtils.PI) * 8f;
            float px = x + MathUtils.cosDeg(angle) * r;
            float py = y + MathUtils.sinDeg(angle) * r;
            arcPoints.add(new Vector2(px, py));
        }
    }

    @Override
    protected void onUpdate(float delta, CombatParticleSystem ps) {
        if (level >= 2 && MathUtils.randomBoolean(0.4f)) {
            spawnSpark(ps);
        }
        if (level == 3) {
            shockwaveRadius += delta * 400f;
        }
    }

    private void spawnSpark(CombatParticleSystem ps) {
        float angle = rotation + MathUtils.random(-50, 50);
        float dist = MathUtils.random(30, 60);
        float px = x + MathUtils.cosDeg(angle) * dist;
        float py = y + MathUtils.sinDeg(angle) * dist;

        float speed = MathUtils.random(80, 200);
        ps.spawn(px, py, glowColor,
                MathUtils.cosDeg(angle) * speed, MathUtils.sinDeg(angle) * speed,
                MathUtils.random(3, 5), 0.4f, true, false);
    }

    @Override
    public void renderShape(ShapeRenderer sr) {
        float p = timer / maxDuration;
        float alpha = 1f - p;
        if (alpha <= 0) return;

        for (int i = 0; i < arcPoints.size() - 1; i++) {
            Vector2 p1 = arcPoints.get(i);
            Vector2 p2 = arcPoints.get(i + 1);

            // 两头尖中间宽
            float progress = (float) i / (arcPoints.size() - 1);
            float thicknessFactor = MathUtils.sin(progress * MathUtils.PI);

            // ✅ 显著增加宽度，确保能被看见
            float baseWidth = (level == 3) ? 10f : 6f;
            if (level == 1) baseWidth = 4f;

            float w = baseWidth * thicknessFactor;

            // 辉光 (宽)
            sr.setColor(glowColor.r, glowColor.g, glowColor.b, alpha * glowColor.a);
            sr.rectLine(p1.x, p1.y, p2.x, p2.y, w * 2.5f);

            // 核心 (亮)
            sr.setColor(coreColor.r, coreColor.g, coreColor.b, alpha * coreColor.a);
            sr.rectLine(p1.x, p1.y, p2.x, p2.y, w);
        }

        // L3 冲击波
        if (level == 3) {
            sr.setColor(glowColor.r, glowColor.g, glowColor.b, alpha * 0.5f);
            float r = shockwaveRadius;
            for(int i=0; i<10; i++) {
                float a = i * 36 + rotation;
                float sx = x + MathUtils.cosDeg(a) * r;
                float sy = y + MathUtils.sinDeg(a) * r;
                float ex = x + MathUtils.cosDeg(a) * (r + 20);
                float ey = y + MathUtils.sinDeg(a) * (r + 20);
                sr.rectLine(sx, sy, ex, ey, 2f);
            }
        }
    }
}