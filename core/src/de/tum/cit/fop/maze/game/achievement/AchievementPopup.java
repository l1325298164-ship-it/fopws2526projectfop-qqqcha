package de.tum.cit.fop.maze.game.achievement;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import de.tum.cit.fop.maze.utils.TextureManager;

/**
 * 游戏内成就解锁弹窗
 * <p>
 * 效果：从屏幕顶部向下滑入，停留几秒，然后收起。
 */
public class AchievementPopup {

    // === 状态定义 ===
    private enum State {
        HIDDEN,
        SLIDING_IN,
        VISIBLE,
        SLIDING_OUT
    }

    private State state = State.HIDDEN;
    private AchievementType currentAchievement;

    // === 动画参数 ===
    private float timer = 0f;
    private static final float ANIM_DURATION = 0.5f; // 滑入/滑出耗时
    private static final float DISPLAY_DURATION = 3.0f; // 停留耗时
    private float currentYOffset = 0f; // 当前Y轴偏移量

    // === 布局参数 ===
    private static final float POPUP_WIDTH = 400f;
    private static final float POPUP_HEIGHT = 80f;
    private static final float MARGIN_TOP = 20f; // 距离屏幕顶部最终位置的距离

    // === 资源 ===
    private final BitmapFont font;
    private final TextureRegion whitePixel; // 用于绘制纯色背景

    public AchievementPopup(BitmapFont font) {
        this.font = font;
        // ❌ 原代码（报错）：
        // this.whitePixel = TextureManager.getInstance().getWhitePixel();

        // ✅ 修正后（加上 new TextureRegion(...)）：
        this.whitePixel = new TextureRegion(TextureManager.getInstance().getWhitePixel());
    }

    /**
     * 触发显示成就
     */
    public void show(AchievementType achievement) {
        this.currentAchievement = achievement;
        this.state = State.SLIDING_IN;
        this.timer = 0f;
        this.currentYOffset = -POPUP_HEIGHT; // 初始在屏幕外上方
    }

    /**
     * 渲染弹窗
     * 必须在 SpriteBatch.begin() / end() 之间调用
     */
    public void render(SpriteBatch batch) {
        if (state == State.HIDDEN || currentAchievement == null) return;

        float delta = Gdx.graphics.getDeltaTime();
        updateAnimation(delta);

        float screenW = Gdx.graphics.getWidth();
        float screenH = Gdx.graphics.getHeight();

        // 计算最终停靠位置 (屏幕顶部向下一点)
        float targetY = screenH - MARGIN_TOP - POPUP_HEIGHT;

        // 实际绘制位置 Y (根据动画偏移)
        // 偏移量 0 表示完全展示，-POPUP_HEIGHT 表示完全隐藏在上方
        // 实际上我们用 slideProgress 插值
        float drawY = 0;

        switch (state) {
            case SLIDING_IN -> {
                float progress = Math.min(1f, timer / ANIM_DURATION);
                // 使用平滑函数 (Ease Out Quad)
                progress = 1 - (1 - progress) * (1 - progress);
                drawY = screenH - (progress * (MARGIN_TOP + POPUP_HEIGHT)); // 从 screenH 滑到 targetY 好像反了，修正逻辑
                // 修正逻辑：
                // Start: screenH (屏幕外)
                // End: screenH - MARGIN_TOP - POPUP_HEIGHT
                float startY = screenH;
                float endY = screenH - MARGIN_TOP - POPUP_HEIGHT;
                drawY = startY + (endY - startY) * progress;
            }
            case VISIBLE -> {
                drawY = screenH - MARGIN_TOP - POPUP_HEIGHT;
            }
            case SLIDING_OUT -> {
                float progress = Math.min(1f, timer / ANIM_DURATION);
                // Ease In Quad
                progress = progress * progress;
                float startY = screenH - MARGIN_TOP - POPUP_HEIGHT;
                float endY = screenH;
                drawY = startY + (endY - startY) * progress;
            }
        }

        float drawX = (screenW - POPUP_WIDTH) / 2f;

        // 1. 绘制背景框 (半透明黑色)
        batch.setColor(0f, 0f, 0f, 0.85f);
        batch.draw(whitePixel, drawX, drawY, POPUP_WIDTH, POPUP_HEIGHT);

        // 2. 绘制金色边框 (左侧粗，其他细)
        batch.setColor(1f, 0.84f, 0f, 1f); // 金色
        batch.draw(whitePixel, drawX, drawY, 6, POPUP_HEIGHT); // 左侧金条
        batch.draw(whitePixel, drawX, drawY, POPUP_WIDTH, 2); // 下边
        batch.draw(whitePixel, drawX, drawY + POPUP_HEIGHT - 2, POPUP_WIDTH, 2); // 上边
        batch.draw(whitePixel, drawX + POPUP_WIDTH - 2, drawY, 2, POPUP_HEIGHT); // 右边

        // 3. 绘制文字
        // 标题 "ACHIEVEMENT UNLOCKED"
        font.getData().setScale(0.8f);
        font.setColor(Color.GOLD);
        font.draw(batch, "ACHIEVEMENT UNLOCKED", drawX + 20, drawY + POPUP_HEIGHT - 15);

        // 成就名称
        font.getData().setScale(1.1f);
        font.setColor(Color.WHITE);
        font.draw(batch, currentAchievement.displayName, drawX + 20, drawY + POPUP_HEIGHT - 35);

        // 成就描述 (截断过长文字)
        font.getData().setScale(0.7f);
        font.setColor(Color.LIGHT_GRAY);
        String desc = currentAchievement.description;
        if (desc.length() > 35) desc = desc.substring(0, 32) + "...";
        font.draw(batch, desc, drawX + 20, drawY + 20);

        // 恢复 batch 颜色
        batch.setColor(1f, 1f, 1f, 1f);
        // 恢复字体
        font.getData().setScale(1.2f);
        font.setColor(Color.WHITE);
    }

    private void updateAnimation(float delta) {
        timer += delta;

        switch (state) {
            case SLIDING_IN:
                if (timer >= ANIM_DURATION) {
                    state = State.VISIBLE;
                    timer = 0f;
                }
                break;
            case VISIBLE:
                if (timer >= DISPLAY_DURATION) {
                    state = State.SLIDING_OUT;
                    timer = 0f;
                }
                break;
            case SLIDING_OUT:
                if (timer >= ANIM_DURATION) {
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