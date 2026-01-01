package de.tum.cit.fop.maze.effects.environment;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.utils.Array;
import java.util.Iterator;

public class EnvironmentParticleSystem {
    public static class EnvParticle {
        public float x, y;
        public float vx, vy;
        public float life, maxLife;
        public float size;
        public Color color = new Color();
        public boolean gravity = false;
        public boolean friction = false;

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
            sr.rect(p.x - p.size/2, p.y - p.size/2, p.size, p.size);
        }
    }

    public void spawn(float x, float y, Color c, float vx, float vy, float size, float life, boolean gravity, boolean friction) {
        EnvParticle p = new EnvParticle();
        p.x = x; p.y = y;
        p.vx = vx; p.vy = vy;
        p.color.set(c);
        p.maxLife = life;
        p.life = life;
        p.size = size;
        p.gravity = gravity;
        p.friction = friction;
        particles.add(p);
    }

    public void clear() { particles.clear(); }
}