package de.tum.cit.fop.maze.effects.environment.items.traps;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import de.tum.cit.fop.maze.effects.environment.EnvironmentEffect;
import de.tum.cit.fop.maze.effects.environment.EnvironmentParticleSystem;

public class PearlMineEffect extends EnvironmentEffect {
    // 芋圆三色：芋头紫、地瓜橙、糯米白
    private static final Color TARO_PURPLE = new Color(0.65f, 0.4f, 0.9f, 1f);
    private static final Color POTATO_ORANGE = new Color(1.0f, 0.6f, 0.2f, 1f);
    private static final Color RICE_WHITE = new Color(0.95f, 0.9f, 0.85f, 1f);

    private final Color[] taroColors = {TARO_PURPLE, POTATO_ORANGE, RICE_WHITE};

    public PearlMineEffect(float x, float y) {
        super(x, y, 0.6f); // 爆炸持续时间
    }

    @Override
    protected void onUpdate(float delta, EnvironmentParticleSystem ps) {
        // 只有第一帧触发爆炸 (生成一堆芋圆粒子)
        if (timer < delta * 2) {
            for (int i = 0; i < 18; i++) { // 增加一点粒子数量，更丰盛
                // 向四周炸开
                float angle = MathUtils.random(0, 360);
                float speed = MathUtils.random(60, 180);

                // 随机选一个颜色
                Color randomColor = taroColors[MathUtils.random(0, taroColors.length - 1)];

                ps.spawn(x, y + 10, randomColor,
                        MathUtils.cosDeg(angle) * speed, MathUtils.sinDeg(angle) * speed,
                        MathUtils.random(8, 14), // 芋圆比较大颗
                        0.8f,
                        true, true); // gravity=true(掉落), friction=true(Q弹减速)
            }
        }
    }

    @Override
    public void render(ShapeRenderer sr) {
        // 爆炸前的一瞬间(闪烁帧)，画三个挤在一起的小芋圆，而不是一个大黑球
        if (timer < 0.05f) {
            // 左边：紫色
            sr.setColor(TARO_PURPLE);
            sr.circle(x - 8, y + 8, 10);

            // 右边：橙色
            sr.setColor(POTATO_ORANGE);
            sr.circle(x + 8, y + 8, 10);

            // 上面：白色
            sr.setColor(RICE_WHITE);
            sr.circle(x, y + 18, 10);
        } else {
            // 爆炸后的地面残留印记 (混合色，看起来像糖水渍)
            float p = timer / maxDuration;
            sr.setColor(0.4f, 0.3f, 0.2f, 0.4f * (1-p)); // 糖水色
            sr.ellipse(x - 20, y - 5, 40, 12);
        }
    }
}