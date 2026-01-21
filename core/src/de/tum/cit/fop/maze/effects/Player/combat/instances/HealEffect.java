package de.tum.cit.fop.maze.effects.Player.combat.instances;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import de.tum.cit.fop.maze.effects.Player.combat.CombatEffect;
import de.tum.cit.fop.maze.effects.Player.combat.CombatParticleSystem;

public class HealEffect extends CombatEffect {

    private float spawnTimer = 0f;

    public HealEffect(float x, float y) {
        super(x, y, 1.0f); // 持续1秒
    }

    @Override
    protected void onUpdate(float delta, CombatParticleSystem ps) {
        spawnTimer += delta;
        // 每 0.1 秒生成特效
        if (spawnTimer > 0.1f) {
            spawnTimer = 0f;
            for (int i = 0; i < 2; i++) {
                // 🔥 [修改] 缩小生成范围，让十字架更集中在角色中心
                float offsetX = MathUtils.random(-10, 10);
                float offsetY = MathUtils.random(-10, 10);

                ps.spawn(
                        x + offsetX,
                        y + offsetY,
                        Color.GREEN,
                        0,
                        30f, // 向上飘的速度
                        // 🔥 [修改] 缩小粒子尺寸 (原先是 huge 的，现在改小)
                        MathUtils.random(4f, 7f),
                        0.8f,
                        false, // 不是实心圆
                        true   // isCross = true (绘制十字架)
                );
            }
        }
    }

    @Override
    public void renderShape(ShapeRenderer sr) {
        // 粒子系统负责渲染，这里留空
    }

    @Override
    public void renderSprite(SpriteBatch batch) {
        // 不需要贴图
    }
}