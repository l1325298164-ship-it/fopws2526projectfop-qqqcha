package de.tum.cit.fop.maze.effects.portal;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Array;

public class PortalParticlePool {
    private final Array<PortalParticle> activeParticles = new Array<>();
    private final Array<PortalParticle> freeParticles = new Array<>();

    private Texture trailTexture; // 动态生成的流星拖尾纹理

    // 颜色配置：蓝色奥术光辉 (Cyan -> Royal Blue)
    private final Color startColor = new Color(0.2f, 0.8f, 1.0f, 1f); // 亮青色
    private final Color endColor = new Color(0.1f, 0.1f, 0.9f, 0f);   // 深蓝透明

    public PortalParticlePool() {
        createTrailTexture();
    }

    /**
     * 使用代码动态生成一个 两头尖中间宽 的发光长条纹理
     * 避免引入外部图片资源
     */
    private void createTrailTexture() {
        int width = 8;
        int height = 32;
        Pixmap pixmap = new Pixmap(width, height, Pixmap.Format.RGBA8888);

        // 填充白色，利用 Alpha 通道做渐变
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                // 计算距离中心的归一化距离
                float distY = Math.abs(y - height / 2f) / (height / 2f); // 0(中心) -> 1(两端)
                float distX = Math.abs(x - width / 2f) / (width / 2f);

                // 简单的渐变算法：中心最亮，边缘透明
                float alpha = (1.0f - distY) * (1.0f - distX);
                alpha = MathUtils.clamp(alpha, 0f, 1f);

                pixmap.setColor(1, 1, 1, alpha);
                pixmap.drawPixel(x, y);
            }
        }
        trailTexture = new Texture(pixmap);
        pixmap.dispose();
    }

    /**
     * 发射龙卷风粒子
     * @param centerX 漩涡中心X
     * @param centerY 漩涡中心Y
     * @param baseRadius 底部半径
     */
    public void spawnTornadoParticles(float centerX, float centerY, float baseRadius) {
        // 每帧发射 3-6 个粒子
        int count = MathUtils.random(3, 6);

        for (int i = 0; i < count; i++) {
            PortalParticle p = obtain();
            initTornadoParticle(p, centerX, centerY, baseRadius);
            activeParticles.add(p);
        }
    }

    private void initTornadoParticle(PortalParticle p, float cx, float cy, float baseRadius) {
        p.lifeTimer = 0;
        p.maxLife = MathUtils.random(0.8f, 1.2f); // 存活时间短一点，让龙卷风看起来转得快

        p.angle = MathUtils.random(0f, 360f) * MathUtils.degreesToRadians;
        p.radius = baseRadius * MathUtils.random(0.5f, 1.5f); // 初始散布范围
        p.height = -10f; // 从脚底稍微下面一点生成
        p.speed = MathUtils.random(100f, 200f); // 上升速度很快

        // 初始大小
        p.scale = MathUtils.random(0.6f, 1.2f);
        p.active = true;

        // 设置初始位置
        updateParticlePosition(p, cx, cy, 0);
    }

    public void update(float delta, float centerX, float centerY) {
        for (int i = activeParticles.size - 1; i >= 0; i--) {
            PortalParticle p = activeParticles.get(i);

            p.lifeTimer += delta;
            if (p.lifeTimer >= p.maxLife) {
                free(p);
                activeParticles.removeIndex(i);
                continue;
            }

            // --- 核心运动逻辑：螺旋上升 ---
            p.height += p.speed * delta;
            p.angle += 8.0f * delta; // 旋转速度极快 (弧度/秒)
            p.radius -= 10.0f * delta; // 半径收缩
            if (p.radius < 5f) p.radius = 5f; // 最小半径限制

            updateParticlePosition(p, centerX, centerY, delta);
        }
    }

    private void updateParticlePosition(PortalParticle p, float cx, float cy, float delta) {
        float oldX = p.position.x;
        float oldY = p.position.y;

        // 螺旋坐标公式 (加入 0.3f 的 Y 轴压缩系数制造透视感)
        float offsetX = MathUtils.cos(p.angle) * p.radius;
        float offsetY = MathUtils.sin(p.angle) * p.radius * 0.3f;

        p.position.set(cx + offsetX, cy + offsetY + p.height);

        // 计算速度向量（用于让光条指向运动方向）
        if (delta > 0) {
            p.velocity.set(p.position.x - oldX, p.position.y - oldY).nor();
        }
    }

    public void render(SpriteBatch batch) {
        if (activeParticles.size == 0) return;

        int srcFunc = batch.getBlendSrcFunc();
        int dstFunc = batch.getBlendDstFunc();
        Color oldColor = batch.getColor();

        // 开启加法混合 (GL_ONE) 制造强烈的发光感
        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE);

        for (PortalParticle p : activeParticles) {
            float progress = p.lifeTimer / p.maxLife;

            // 颜色渐变: 青色 -> 深蓝
            batch.setColor(
                    MathUtils.lerp(startColor.r, endColor.r, progress),
                    MathUtils.lerp(startColor.g, endColor.g, progress),
                    MathUtils.lerp(startColor.b, endColor.b, progress),
                    (1.0f - progress) // 透明度淡出
            );

            // 计算旋转角度：让纹理长边对准运动方向
            // -90度 是因为我们的纹理默认是垂直的，而角度0度通常是水平向右
            float rotation = p.velocity.angleDeg() - 90;

            // 绘制: 宽度随时间变细，长度随速度拉伸（模拟动态模糊）
            float width = 8f * p.scale * (1f - progress);
            float height = 30f * p.scale;

            batch.draw(trailTexture,
                    p.position.x - width/2, p.position.y - height/2, // 居中绘制
                    width/2, height/2, // 旋转中心
                    width, height,
                    1f, 1f,
                    rotation,
                    0, 0, trailTexture.getWidth(), trailTexture.getHeight(), false, false
            );
        }

        // 恢复状态
        batch.setColor(oldColor);
        batch.setBlendFunction(srcFunc, dstFunc);
    }

    // 对象池逻辑
    private PortalParticle obtain() {
        return (freeParticles.size > 0) ? freeParticles.pop() : new PortalParticle();
    }

    private void free(PortalParticle p) {
        p.active = false;
        freeParticles.add(p);
    }

    public void dispose() {
        if (trailTexture != null) trailTexture.dispose();
    }
}