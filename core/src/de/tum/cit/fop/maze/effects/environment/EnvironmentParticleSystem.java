package de.tum.cit.fop.maze.effects.environment;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.utils.Array;
import java.util.Iterator;

/**
 * 环境专用粒子系统
 * 支持多种形状 (方块、圆形、三角)
 */
public class EnvironmentParticleSystem {

    // 1. 定义形状枚举
    public enum Shape {
        RECTANGLE,  // 方块 (默认，像素风通用)
        CIRCLE,     // 圆形 (适合气泡、珍珠、粘液)
        TRIANGLE    // 三角 (适合碎片、尖刺)
    }

    public static class EnvParticle {
        public float x, y;
        public float vx, vy;
        public float life, maxLife;
        public float size;
        public Color color = new Color();
        public boolean gravity = false;
        public boolean friction = false;
        public Shape shape = Shape.RECTANGLE; // 新增形状字段，默认为方块

        public void update(float dt) {
            x += vx * dt;
            y += vy * dt;
            if (gravity) vy -= 800f * dt;
            if (friction) { vx *= 0.95f; vy *= 0.95f; }
            life -= dt;
        }
    }

    private Array<EnvParticle> particles = new Array<>();

    public void update(float delta) {
        Iterator<EnvParticle> it = particles.iterator();
        while (it.hasNext()) {
            EnvParticle p = it.next();
            p.update(delta);
            if (p.life <= 0) it.remove();
        }
    }

    public void render(ShapeRenderer sr) {
        for (EnvParticle p : particles) {
            float alpha = p.life / p.maxLife;
            sr.setColor(p.color.r, p.color.g, p.color.b, alpha);

            // 2. 根据形状绘制不同的几何图形
            switch (p.shape) {
                case CIRCLE:
                    // 绘制圆形 (半径 = size / 2)
                    sr.circle(p.x, p.y, p.size / 2);
                    break;
                case TRIANGLE:
                    // 绘制等腰三角形 (向上指)
                    float half = p.size / 2;
                    sr.triangle(p.x, p.y + half,       // 顶点
                            p.x - half, p.y - half, // 左下
                            p.x + half, p.y - half); // 右下
                    break;
                case RECTANGLE:
                default:
                    // 默认方块
                    sr.rect(p.x - p.size/2, p.y - p.size/2, p.size, p.size);
                    break;
            }
        }
    }

    // === 生成方法 ===

    /**
     * 新增：全参数生成 (指定形状)
     */
    public void spawn(float x, float y, Color c, float vx, float vy, float size, float life, boolean gravity, boolean friction, Shape shape) {
        EnvParticle p = new EnvParticle();
        p.x = x; p.y = y;
        p.vx = vx; p.vy = vy;
        p.color.set(c);
        p.maxLife = life;
        p.life = life;
        p.size = size;
        p.gravity = gravity;
        p.friction = friction;
        p.shape = shape; // 设置形状
        particles.add(p);
    }

    /**
     * 兼容旧代码：默认生成方块 (RECTANGLE)
     * 这样 TreasureEffect 等不需要改代码
     */
    public void spawn(float x, float y, Color c, float vx, float vy, float size, float life, boolean gravity, boolean friction) {
        spawn(x, y, c, vx, vy, size, life, gravity, friction, Shape.RECTANGLE);
    }

    public void clear() {
        particles.clear();
    }
}