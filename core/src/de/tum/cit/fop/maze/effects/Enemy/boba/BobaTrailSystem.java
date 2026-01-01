package de.tum.cit.fop.maze.effects.Enemy.boba;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ObjectMap;
import de.tum.cit.fop.maze.entities.enemy.EnemyBoba.BobaBullet;
import de.tum.cit.fop.maze.game.GameConstants;

/**
 * 子弹拖尾系统（适配格子坐标）
 */
public class BobaTrailSystem {

    private static class TrailPoint {
        Vector2 position;
        float size;
        float alpha;
        float lifetime;

        TrailPoint(Vector2 pos, float size, float lifetime) {
            this.position = new Vector2(pos);
            this.size = size;
            this.alpha = 0.8f;
            this.lifetime = lifetime;
        }

        void update(float delta) {
            lifetime -= delta;
            alpha = Math.max(0, lifetime);
            size *= 0.95f; // 逐渐缩小
        }
    }

    private ObjectMap<BobaBullet, Array<TrailPoint>> bulletTrails;
    private float trailIntensity = 0.7f;
    private float trailLifetime = 0.5f;
    private int maxPointsPerBullet = 15;

    public BobaTrailSystem() {
        bulletTrails = new ObjectMap<>();
    }

    public void trackBullet(BobaBullet bullet) {
        if (bullet == null || bulletTrails.containsKey(bullet)) return;

        bulletTrails.put(bullet, new Array<TrailPoint>());
    }

    public void untrackBullet(BobaBullet bullet) {
        if (bullet == null) return;

        bulletTrails.remove(bullet);
    }

    public void update(float delta) {
        // 为每个被跟踪的子弹更新拖尾
        for (ObjectMap.Entry<BobaBullet, Array<TrailPoint>> entry : bulletTrails.entries()) {
            BobaBullet bullet = entry.key;
            Array<TrailPoint> trail = entry.value;

            if (bullet.isActive()) {
                // 添加新的拖尾点
                Vector2 currentPos = new Vector2(bullet.getRealX(), bullet.getRealY());
                addTrailPoint(bullet, currentPos);
            }

            // 更新现有拖尾点
            for (int i = trail.size - 1; i >= 0; i--) {
                TrailPoint point = trail.get(i);
                point.update(delta);

                if (point.lifetime <= 0) {
                    trail.removeIndex(i);
                }
            }
        }
    }

    private void addTrailPoint(BobaBullet bullet, Vector2 position) {
        Array<TrailPoint> trail = bulletTrails.get(bullet);
        if (trail == null) return;

        // 控制拖尾点数量
        if (trail.size >= maxPointsPerBullet) {
            trail.removeIndex(0);
        }

        float baseSize = GameConstants.CELL_SIZE * 0.15f * trailIntensity;
        TrailPoint point = new TrailPoint(position, baseSize, trailLifetime);
        trail.add(point);
    }

    public void render(SpriteBatch batch) {
        // 使用 ShapeRenderer 绘制拖尾点
        // 注意：需要在 SpriteBatch begin/end 外部使用

        // 临时实现：在实际项目中，你需要整合 ShapeRenderer
        // 这里仅提供概念
    }

    public void setIntensity(float intensity) {
        this.trailIntensity = Math.max(0, Math.min(1, intensity));
        this.maxPointsPerBullet = (int)(15 * intensity);
    }

    public void clearAllTrails() {
        bulletTrails.clear();
    }

    public int getActiveParticleCount() {
        int count = 0;
        for (Array<TrailPoint> trail : bulletTrails.values()) {
            count += trail.size;
        }
        return count;
    }

    public void resetStats() {
        // 可以添加统计重置逻辑
    }

    public void dispose() {
        clearAllTrails();
    }
}