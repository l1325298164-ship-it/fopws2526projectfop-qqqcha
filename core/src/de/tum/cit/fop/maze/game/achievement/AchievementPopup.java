package de.tum.cit.fop.maze.game.achievement;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Interpolation;
import de.tum.cit.fop.maze.utils.TextureManager;

/**
 * 游戏内成就解锁弹窗 - 视觉升级版
 * <p>
 * 改进：
 * 1. 视觉：深色卡片 + 金色左边栏 + 星星图标。
 * 2. 动效：加入淡入淡出 (Alpha) 和 弹跳 (Bounce) 效果。
 * 3. 布局：左图右文，信息层次更清晰。
 */
public class AchievementPopup {

    // === 状态定义 ===
    private enum State {
        HIDDEN,
        ENTERING, // 进场动画
        VISIBLE,  // 停留展示
        EXITING   // 退场动画
    }

    private State state = State.HIDDEN;
    private AchievementType currentAchievement;

    // === 动画参数 ===
    private float timer = 0f;
    private static final float ANIM_IN_DURATION = 0.6f;  // 进场稍慢，配合弹跳
    private static final float DISPLAY_DURATION = 2.5f;  // 停留时间
    private static final float ANIM_OUT_DURATION = 0.4f; // 退场快一点

    // === 布局尺寸 ===
    private static final float POPUP_WIDTH = 360f;
    private static final float POPUP_HEIGHT = 80f;
    private static final float MARGIN_TOP = 80f; // 距离屏幕顶部的距离

    // === 资源 ===
    private final BitmapFont font;
    private final TextureRegion whitePixel;

    public AchievementPopup(BitmapFont font) {
        this.font = font;
        // 获取纯白像素用于绘制矩形
        this.whitePixel = new TextureRegion(TextureManager.getInstance().getWhitePixel());
    }

    /**
     * 显示成就弹窗
     */
    public void show(AchievementType achievement) {
        this.currentAchievement = achievement;
        this.state = State.ENTERING;
        this.timer = 0f;
    }

    /**
     * 渲染方法
     */
    public void render(SpriteBatch batch) {
        if (state == State.HIDDEN || currentAchievement == null) return;

        float delta = Gdx.graphics.getDeltaTime();
        updateAnimation(delta);

        // === 计算动画状态 ===
        float screenW = Gdx.graphics.getWidth();
        float screenH = Gdx.graphics.getHeight();

        float animProgress = 0f;
        float alpha = 1f;
        float yOffset = 0f;

        switch (state) {
            case ENTERING:
                animProgress = Math.min(1f, timer / ANIM_IN_DURATION);
                // 弹跳进场效果 (BounceOut)
                yOffset = Interpolation.swingOut.apply(-100f, 0f, animProgress);
                alpha = animProgress; // 同时淡入
                break;
            case VISIBLE:
                yOffset = 0f;
                alpha = 1f;
                break;
            case EXITING:
                animProgress = Math.min(1f, timer / ANIM_OUT_DURATION);
                // 上滑淡出
                yOffset = Interpolation.pow2In.apply(0f, 100f, animProgress);
                alpha = 1f - animProgress;
                break;
        }

        // 最终绘制坐标
        float drawX = (screenW - POPUP_WIDTH) / 2f;
        float drawY = screenH - MARGIN_TOP - POPUP_HEIGHT + yOffset;

        // === 1. 绘制背景 (深色卡片) ===
        batch.setColor(0.1f, 0.1f, 0.12f, 0.9f * alpha);
        batch.draw(whitePixel, drawX, drawY, POPUP_WIDTH, POPUP_HEIGHT);

        // === 2. 绘制左侧金色装饰条 (Accent) ===
        batch.setColor(1f, 0.8f, 0.0f, 1f * alpha); // 金色
        batch.draw(whitePixel, drawX, drawY, 6f, POPUP_HEIGHT); // 6px 宽

        // === 3. 绘制图标 (左侧星星) ===
        // 使用字体绘制一颗大星星作为图标
        float iconCenterX = drawX + 40f;
        float iconCenterY = drawY + POPUP_HEIGHT / 2f + 10f;

        font.getData().setScale(2.5f); // 大图标
        font.setColor(1f, 0.84f, 0f, alpha); // 金色星星
        // 简单的抖动效果
        if (state == State.VISIBLE) {
            float shake = (float)Math.sin(timer * 5f) * 2f;
            font.draw(batch, "★", iconCenterX - 10, iconCenterY + shake);
        } else {
            font.draw(batch, "★", iconCenterX - 10, iconCenterY);
        }

        // === 4. 绘制文字信息 (右侧) ===
        float textX = drawX + 70f; // 避开图标
        float textTopY = drawY + POPUP_HEIGHT - 10f;

        // 小标题: ACHIEVEMENT UNLOCKED
        font.getData().setScale(0.7f);
        font.setColor(1f, 0.8f, 0.2f, 0.8f * alpha); // 淡金色
        font.draw(batch, "ACHIEVEMENT UNLOCKED", textX, textTopY);

        // 主标题: 成就名称
        font.getData().setScale(1.1f);
        font.setColor(1f, 1f, 1f, 1f * alpha); // 亮白
        font.draw(batch, currentAchievement.displayName, textX, textTopY - 20f);

        // 描述: 具体内容
        font.getData().setScale(0.75f);
        font.setColor(0.8f, 0.8f, 0.8f, 0.8f * alpha); // 灰白

        // 简单截断防止溢出
        String desc = currentAchievement.description;
        if (desc.length() > 35) desc = desc.substring(0, 32) + "...";
        font.draw(batch, desc, textX, textTopY - 45f);

        // === 恢复环境 ===
        batch.setColor(Color.WHITE);
        font.getData().setScale(1.2f); // 还原默认大小
        font.setColor(Color.WHITE);
    }

    private void updateAnimation(float delta) {
        timer += delta;
        switch (state) {
            case ENTERING:
                if (timer >= ANIM_IN_DURATION) {
                    state = State.VISIBLE;
                    timer = 0f;
                }
                break;
            case VISIBLE:
                if (timer >= DISPLAY_DURATION) {
                    state = State.EXITING;
                    timer = 0f;
                }
                break;
            case EXITING:
                if (timer >= ANIM_OUT_DURATION) {
                    state = State.HIDDEN;
                    currentAchievement = null;
                }
                break;
        }
    }

    public boolean isBusy() {
        return state != State.HIDDEN;
    }
}