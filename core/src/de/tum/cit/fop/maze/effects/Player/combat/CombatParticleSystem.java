package de.tum.cit.fop.maze.effects.Player.combat;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.utils.Array;
import java.util.Iterator;

public class CombatParticleSystem {

    public static class Particle {
        float x, y;
        float vx, vy;
        float life, maxLife;
        float size;
        Color color = new Color();

        // 战斗物理特性
        boolean friction = false; // 空气阻力 (让爆炸瞬间快，然后急停)
        boolean gravity = false;  // 反向重力 (用于火焰/治疗上升)

        public void update(float dt) {
            x += vx * dt;
            y += vy * dt;

            if (friction) {
                vx *= 0.90f; // 强阻力，模拟空气感
                vy *= 0.90f;
            }
            if (gravity) {
                vy += 400f * dt; // 向上飘 (比如火焰)
            }

            life -= dt;
        }
    }

    private Array<Particle> particles = new Array<>();

    public void update(float delta) {
        Iterator<Particle> it = particles.iterator();
        while (it.hasNext()) {
            Particle p = it.next();
            p.update(delta);
            if (p.life <= 0) it.remove();
        }
    }

    public void render(ShapeRenderer sr) {
        for (Particle p : particles) {
            float alpha = p.life / p.maxLife;
            sr.setColor(p.color.r, p.color.g, p.color.b, alpha);

            // 绘制粒子
            sr.rect(p.x - p.size/2, p.y - p.size/2, p.size, p.size);

            // 高光十字 (让大粒子看起来像星星闪烁)
            if (p.size > 4) {
                float len = p.size;
                sr.rectLine(p.x - len, p.y, p.x + len, p.y, 1);
                sr.rectLine(p.x, p.y - len, p.x, p.y + len, 1);
            }
        }
    }

    /**
     * 全能生成接口
     */
    public void spawn(float x, float y, Color c, float vx, float vy, float size, float life, boolean friction, boolean gravity) {
        Particle p = new Particle();
        p.x = x; p.y = y;
        p.vx = vx; p.vy = vy;
        p.color.set(c);
        p.maxLife = life;
        p.life = life;
        p.size = size;
        p.friction = friction;
        p.gravity = gravity;
        particles.add(p);
    }

    public void clear() {
        particles.clear();
    }
}