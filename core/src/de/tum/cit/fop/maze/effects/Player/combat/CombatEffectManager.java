package de.tum.cit.fop.maze.effects.Player.combat;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import de.tum.cit.fop.maze.effects.Player.combat.instances.*;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * 战斗特效管理器
 * 负责管理所有的战斗相关视觉反馈 (VFX)，包括技能特效、打击感反馈、飘字等。
 */
public class CombatEffectManager {

    private static final int MAX_EFFECTS = 300; // 稍微调高上限，防止粒子太多挤掉重要特效
    private final List<CombatEffect> effects;
    private final CombatParticleSystem particleSystem;

    // 字体资源
    private final BitmapFont scoreFont;
    private final BitmapFont textFont;

    // 调试/性能统计
    private int maxEffectsInFrame = 0;

    public CombatEffectManager() {
        this.effects = new ArrayList<>();
        this.particleSystem = new CombatParticleSystem();

        // --- 初始化字体 ---
        BitmapFont tmpScoreFont;
        try {
            if (Gdx.files.internal("ui/font.fnt").exists()) {
                tmpScoreFont = new BitmapFont(Gdx.files.internal("ui/font.fnt"));
            } else {
                tmpScoreFont = new BitmapFont(); // fallback
            }
        } catch (Exception e) {
            tmpScoreFont = new BitmapFont();
        }
        this.scoreFont = tmpScoreFont;
        this.scoreFont.setUseIntegerPositions(false);
        this.scoreFont.getData().setScale(0.8f);

        this.textFont = new BitmapFont();
        this.textFont.setUseIntegerPositions(false);
        this.textFont.getData().setScale(1.0f);
    }

    public void update(float delta) {
        // 1. 更新粒子系统
        particleSystem.update(delta);

        // 2. 更新所有特效实体
        Iterator<CombatEffect> iterator = effects.iterator();
        while (iterator.hasNext()) {
            CombatEffect effect = iterator.next();
            effect.update(delta, particleSystem);
            if (effect.isFinished()) {
                iterator.remove();
            }
        }

        maxEffectsInFrame = Math.max(maxEffectsInFrame, effects.size());
    }

    public void renderShapes(ShapeRenderer shapeRenderer) {
        // 渲染特效的几何形状 (ShapeRenderer 必须在外部 begin/end)
        for (CombatEffect effect : effects) {
            effect.renderShape(shapeRenderer);
        }
        // 渲染粒子
        particleSystem.render(shapeRenderer);
    }

    public void renderSprites(SpriteBatch batch) {
        // 渲染特效的贴图 (如果有)
        for (CombatEffect effect : effects) {
            effect.renderSprite(batch);
        }
    }

    /**
     * 安全添加特效，防止列表无限膨胀
     */
    private void safeAddEffect(CombatEffect effect) {
        if (effects.size() >= MAX_EFFECTS) {
            // 如果满了，移除最早的一个 (FIFO)
            if (!effects.isEmpty()) effects.remove(0);
        }
        effects.add(effect);
    }

    // =========================================================
    // 🔥 战斗反馈 (Combat Feedback Juice)
    // =========================================================

    /**
     * 生成受击火花 (强化版 X 闪光 + 飞溅粒子)
     * 用于增加打击感
     */
    public void spawnHitSpark(float x, float y) {
        safeAddEffect(new HitSparkEffect(x, y));
    }

    /**
     * 生成杀意波动 (暗紫色扩散圆环)
     * 用于敌人发现玩家时的警示
     */
    public void spawnAggroPulse(float x, float y) {
        safeAddEffect(new AggroPulseEffect(x, y));
    }

    /**
     * 生成 Buff 图标
     * @param type 0=十字架(回血), 1=剑(攻击), 2=星星(回蓝)
     */
    public void spawnBuffIcon(float x, float y, int type) {
        safeAddEffect(new StatusIconEffect(x, y, type));
    }

    /**
     * 敌人死亡爆炸特效 (灰色烟雾)
     */
    public void spawnEnemyDeathEffect(float x, float y) {
        // 直接生成一团灰色烟雾粒子
        for (int i = 0; i < 12; i++) {
            particleSystem.spawn(
                    x + MathUtils.random(-15, 15),
                    y + MathUtils.random(-15, 15),
                    Color.GRAY,
                    MathUtils.random(-60, 60),
                    MathUtils.random(-60, 60),
                    MathUtils.random(5, 10),     // 大小
                    MathUtils.random(0.5f, 0.8f), // 寿命
                    true,                        // 阻力
                    false                        // 重力
            );
        }
    }

    // =========================================================
    // 🔮 魔法技能特效 (Magic Ability)
    // =========================================================

    /**
     * 生成动态魔法阵 (吟唱阶段)
     * @param duration 持续时间 (通常等于吟唱时间)
     */
    public void spawnMagicCircle(float x, float y, float radius, float duration) {
        safeAddEffect(new MagicCircleEffect(x, y, radius, duration));
    }

    /**
     * 生成通天光柱 (AOE 爆发阶段)
     */
    public void spawnMagicPillar(float x, float y, float radius) {
        safeAddEffect(new MagicPillarEffect(x, y, radius));
    }

    /**
     * 生成魔力精华 (回能阶段)
     * 从敌人位置飞向玩家位置
     */
    public void spawnMagicEssence(float startX, float startY, float targetX, float targetY) {
        safeAddEffect(new MagicEssenceEffect(startX, startY, targetX, targetY));
    }

    // =========================================================
    // ⚔️ 玩家技能与动作 (Player Actions)
    // =========================================================

    public void spawnSlash(float x, float y, float angle, int type) {
        safeAddEffect(new SlashEffect(x, y, angle, type));
    }

    /**
     * 生成冲刺特效 (带等级分级)
     * @param level 技能等级 (1=基础白烟, 3=青色电光, 5=金色光辉)
     */
    public void spawnDash(float x, float y, float directionAngle, int level) {
        safeAddEffect(new DashEffect(x, y, directionAngle, level));
    }

    public void spawnFireMagic(float x, float y) {
        safeAddEffect(new FireMagicEffect(x, y));
    }

    public void spawnHeal(float x, float y) {
        safeAddEffect(new HealEffect(x, y));
    }



    public void spawnLaser(float startX, float startY, float endX, float endY) {
        safeAddEffect(new LaserEffect(startX, startY, endX, endY));
    }

    // =========================================================
    // 💬 UI 与 飘字 (Floating Text)
    // =========================================================

    /**
     * 生成分数飘字 (金/红)
     */
    public void spawnScoreText(float x, float y, int score) {
        if (score == 0) return;
        String text = (score > 0 ? "+" : "") + score;
        Color color = (score > 0) ? Color.GOLD : Color.RED;
        safeAddEffect(new FloatingTextEffect(x, y, text, color, scoreFont));
    }

    /**
     * 生成状态文字 (通用)
     */
    public void spawnStatusText(float x, float y, String text, Color color) {
        if (text == null || text.isEmpty()) return;
        safeAddEffect(new FloatingTextEffect(x, y, text, color, textFont));
    }

    // 兼容旧接口
    public void spawnFloatingText(float x, float y, String text, Color color) {
        spawnStatusText(x, y, text, color);
    }

    // =========================================================
    // 🗑️ 资源清理
    // =========================================================

    public void dispose() {
        effects.clear();
        if (scoreFont != null) scoreFont.dispose();
        if (textFont != null) textFont.dispose();
        particleSystem.clear();
    }
}