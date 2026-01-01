package de.tum.cit.fop.maze.effects.Player.combat.instances;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import de.tum.cit.fop.maze.effects.Player.combat.CombatEffect;
import de.tum.cit.fop.maze.effects.Player.combat.CombatParticleSystem;

public class SlashEffect extends CombatEffect {
    private final float rotation; // 攻击角度 (0=右, 90=上...)
    private final int level;      // 1, 2, 3
    private final Color baseColor;

    public SlashEffect(float x, float y, float rotation, int level) {
        super(x, y, 0.25f); // 挥砍非常快
        this.rotation = rotation;
        this.level = MathUtils.clamp(level, 1, 3);

        // 基础色定义
        if (this.level == 1) this.baseColor = Color.WHITE;
        else if (this.level == 2) this.baseColor = new Color(1f, 0.9f, 0.4f, 1f); // 金色
        else this.baseColor = new Color(0.5f, 1f, 1f, 1f); // Lv3 炫彩基底
    }

    @Override
    protected void onUpdate(float delta, CombatParticleSystem ps) {
        // Lv1: 无粒子，极致简洁
        if (level == 1) return;

        // Lv2: 少量星火溅射
        if (level == 2) {
            if (MathUtils.randomBoolean(0.3f)) { // 30% 几率
                spawnSpark(ps, 30);
            }
        }

        // Lv3: 粒子爆发！
        if (level == 3) {
            // 每一帧都产生轨迹粒子
            for (int i = 0; i < 3; i++) {
                spawnSpark(ps, 50);
            }
            // 第一帧产生一圈爆炸波
            if (timer < delta * 2) {
                for(int i=0; i<20; i++) {
                    float angle = MathUtils.random(0, 360);
                    float speed = MathUtils.random(100, 300);
                    ps.spawn(x, y, getRandomRainbowColor(),
                            MathUtils.cosDeg(angle)*speed, MathUtils.sinDeg(angle)*speed,
                            MathUtils.random(4, 8), 0.5f, true, false);
                }
            }
        }
    }

    private void spawnSpark(CombatParticleSystem ps, float spread) {
        // 在剑气路径上随机生成
        float dist = MathUtils.random(10, 60);
        float px = x + MathUtils.cosDeg(rotation) * dist + MathUtils.random(-10, 10);
        float py = y + MathUtils.sinDeg(rotation) * dist + MathUtils.random(-10, 10);

        Color c = (level == 3) ? getRandomRainbowColor() : baseColor;

        // 带有阻力的粒子 (friction=true)，模拟火花溅射感
        ps.spawn(px, py, c,
                MathUtils.random(-spread, spread), MathUtils.random(-spread, spread),
                MathUtils.random(3, 5), 0.4f, true, false);
    }

    private Color getRandomRainbowColor() {
        return new Color(MathUtils.random(), MathUtils.random(), MathUtils.random(), 1f);
    }

    @Override
    public void render(ShapeRenderer sr) {
        float p = timer / maxDuration;
        // 挥砍扇面角度
        float swingArc = (level == 3) ? 140f : 100f;
        float currentAngle = rotation - (swingArc / 2f) + (p * swingArc);

        float length = (level == 3) ? 100 : 60;
        float width = (level == 3) ? 12 : 5;

        // 设置颜色
        if (level == 3) {
            // 炫彩流光
            float r = (MathUtils.sin(timer * 20) + 1) / 2f;
            float g = (MathUtils.sin(timer * 20 + 2) + 1) / 2f;
            float b = (MathUtils.sin(timer * 20 + 4) + 1) / 2f;
            sr.setColor(r, g, b, 1f - p);
        } else {
            sr.setColor(baseColor.r, baseColor.g, baseColor.b, 1f - p);
        }

        // 计算剑尖位置
        float endX = x + MathUtils.cosDeg(currentAngle) * length;
        float endY = y + MathUtils.sinDeg(currentAngle) * length;

        // 1. 绘制主剑气
        sr.rectLine(x, y, endX, endY, width * (1-p));

        // 2. Lv2+ 绘制“剑影” (Lag Effect)
        if (level >= 2) {
            float lagAngle = currentAngle - 10;
            float wingX = x + MathUtils.cosDeg(lagAngle) * (length * 0.8f);
            float wingY = y + MathUtils.sinDeg(lagAngle) * (length * 0.8f);

            Color c = sr.getColor();
            sr.setColor(c.r, c.g, c.b, (1f-p) * 0.4f); // 半透明
            sr.rectLine(x, y, wingX, wingY, width * 0.6f * (1-p));
        }
    }
}