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

    // 存储刀光轨迹的骨架点
    private final List<Vector2> arcPoints = new ArrayList<>();

    // L3 冲击波扩散半径
    private float shockwaveRadius = 0f;

    public SlashEffect(float x, float y, float rotation, int level) {
        // 动作极快，0.15秒足够，太长会显得拖泥带水
        super(x, y, 0.15f);
        this.rotation = rotation;
        this.level = MathUtils.clamp(level, 1, 3);

        // --- 1. 颜色风格调整 (更通透，不遮挡吸管) ---
        if (this.level == 1) {
            // L1 疾风: 极淡的白，几乎透明
            this.coreColor = new Color(1f, 1f, 1f, 0.4f);
            this.glowColor = new Color(1f, 1f, 1f, 0f); // 无辉光
        } else if (this.level == 2) {
            // L2 烈焰: 亮黄色半透明
            this.coreColor = new Color(1f, 0.9f, 0.5f, 0.6f);
            this.glowColor = new Color(1f, 0.6f, 0.1f, 0.3f); // 极淡的橙色辉光
        } else {
            // L3 霓虹: 青色能量场
            this.coreColor = new Color(0.8f, 1f, 1f, 0.7f);
            this.glowColor = new Color(0f, 1f, 0.8f, 0.4f);
        }

        // --- 2. 形状参数预计算 ---
        // 假设吸管长度大概是 50-70 像素，我们只在尖端画轨迹
        float radius = (level == 3) ? 70f : 50f;

        int segments = 10; // 分段数
        float sweepAngle = 110f; // 挥砍角度范围
        float startAngle = rotation - sweepAngle / 2f;

        for (int i = 0; i <= segments; i++) {
            float progress = (float) i / segments;
            float angle = startAngle + (progress * sweepAngle);

            // 细微调整：让弧线稍微有一点动态曲率，不像圆规画的那么死板
            float r = radius + MathUtils.sin(progress * MathUtils.PI) * 5f;

            float px = x + MathUtils.cosDeg(angle) * r;
            float py = y + MathUtils.sinDeg(angle) * r;
            arcPoints.add(new Vector2(px, py));
        }
    }

    @Override
    protected void onUpdate(float delta, CombatParticleSystem ps) {
        // --- 粒子生成逻辑 ---
        // L2 & L3: 偶尔产生一点火花/气泡点缀，增加打击感
        if (level >= 2) {
            // 只有 30% 的几率产生粒子，避免喧宾夺主
            if (MathUtils.randomBoolean(0.3f)) {
                spawnSpark(ps);
            }
        }

        // L3: 冲击波快速扩散
        if (level == 3) {
            shockwaveRadius += delta * 350f;
        }
    }

    private void spawnSpark(CombatParticleSystem ps) {
        float angleRange = 100f;
        float angle = rotation + MathUtils.random(-angleRange/2, angleRange/2);
        float dist = MathUtils.random(40, 60); // 在刀尖附近

        float px = x + MathUtils.cosDeg(angle) * dist;
        float py = y + MathUtils.sinDeg(angle) * dist;

        Color pColor = new Color(glowColor);
        // L3 偶尔混入一点紫色粒子
        if (level == 3 && MathUtils.randomBoolean(0.3f)) {
            pColor.set(Color.MAGENTA);
        }

        // 粒子向外飞溅
        float speed = MathUtils.random(50, 150);
        ps.spawn(px, py, pColor,
                MathUtils.cosDeg(angle) * speed, MathUtils.sinDeg(angle) * speed,
                MathUtils.random(2, 4), // 粒子很小
                0.3f, // 寿命短
                true, false); // friction=true (阻力)
    }

    @Override
    public void renderShape(ShapeRenderer sr) {
        float p = timer / maxDuration;
        float alpha = 1f - p; // 线性淡出

        if (alpha <= 0) return;

        // --- 核心绘制：画细线而非粗块 ---
        for (int i = 0; i < arcPoints.size() - 1; i++) {
            Vector2 p1 = arcPoints.get(i);
            Vector2 p2 = arcPoints.get(i + 1);

            // 计算线宽：两头尖，中间略宽
            float progress = (float) i / (arcPoints.size() - 1);
            float thicknessFactor = MathUtils.sin(progress * MathUtils.PI);

            // 📏 关键调整：宽度大幅减小！
            float baseWidth = (level == 3) ? 6f : 3f; // L3最宽也就6像素，L1/L2只有3像素
            if (level == 1) baseWidth = 1.5f; // L1 像丝线一样

            float w = baseWidth * thicknessFactor;

            // 1. 画边缘辉光 (L2/L3) - 用极低透明度画宽一点，模拟空气扰动
            if (level >= 2) {
                sr.setColor(glowColor.r, glowColor.g, glowColor.b, alpha * glowColor.a);
                sr.rectLine(p1.x, p1.y, p2.x, p2.y, w * 3f);
            }

            // 2. 画核心细线
            sr.setColor(coreColor.r, coreColor.g, coreColor.b, alpha * coreColor.a);
            sr.rectLine(p1.x, p1.y, p2.x, p2.y, w);
        }

        // --- L3 冲击波：一圈细线 ---
        if (level == 3) {
            sr.setColor(glowColor.r, glowColor.g, glowColor.b, alpha * 0.3f);
            // 模拟空心圆环
            float r = shockwaveRadius;
            // 只画几段离散的弧线，不画完整的圆，更像冲击波
            for(int i=0; i<12; i+=2) {
                float a = i * 30 + rotation; // 随挥砍方向偏移
                float ex = x + MathUtils.cosDeg(a) * (r + 15);
                float ey = y + MathUtils.sinDeg(a) * (r + 15);
                float sx = x + MathUtils.cosDeg(a) * r;
                float sy = y + MathUtils.sinDeg(a) * r;

                sr.rectLine(sx, sy, ex, ey, 1.5f); // 极细
            }
        }
    }
}