package de.tum.cit.fop.maze.game.achievement;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import de.tum.cit.fop.maze.utils.TextureManager;

/**
 * 游戏内成就解锁弹窗
 * <p>
 * 效果：从屏幕顶部向下滑入，停留2秒，然后自动收起。
 * <p>
 * 生命周期：
 * 1. SLIDING_IN (0.5秒) - 从屏幕顶部滑入
 * 2. VISIBLE (2.0秒) - 显示成就信息
 * 3. SLIDING_OUT (0.5秒) - 滑出屏幕
 * 4. HIDDEN - 完全隐藏，不占用屏幕空间
 * <p>
 * 总时长：约3秒（0.5 + 2.0 + 0.5），之后自动关闭，不会一直占用屏幕。
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
    private static final float DISPLAY_DURATION = 2.0f; // 停留耗时（缩短到2秒，避免长时间占用屏幕）

    // === 布局参数 ===
    // 弹窗尺寸：更紧凑的设计
    private static final float POPUP_WIDTH_BASE = 320f;  // 基础宽度
    private static final float POPUP_HEIGHT = 65f;       // 减小高度，更紧凑
    private static final float MARGIN_TOP = 100f;        // 距离屏幕顶部（稍微减小，因为弹窗更小了）
    private static final float PADDING = 12f;             // 内边距

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
        
        // 根据屏幕宽度自适应弹窗宽度（但不超过基础宽度的1.2倍）
        float popupWidth = Math.min(POPUP_WIDTH_BASE * 1.2f, screenW * 0.4f);
        popupWidth = Math.max(popupWidth, POPUP_WIDTH_BASE); // 最小为基础宽度

        // 实际绘制位置 Y (根据动画偏移)
        float drawY = 0;

        switch (state) {
            case SLIDING_IN: {
                float progress = Math.min(1f, timer / ANIM_DURATION);
                // 使用平滑函数 (Ease Out Quad)
                progress = 1 - (1 - progress) * (1 - progress);
                // 修正逻辑：
                // Start: screenH (屏幕外)
                // End: screenH - MARGIN_TOP - POPUP_HEIGHT
                float startY = screenH;
                float endY = screenH - MARGIN_TOP - POPUP_HEIGHT;
                drawY = startY + (endY - startY) * progress;
                break;
            }
            case VISIBLE: {
                drawY = screenH - MARGIN_TOP - POPUP_HEIGHT;
                break;
            }
            case SLIDING_OUT: {
                float progress = Math.min(1f, timer / ANIM_DURATION);
                // Ease In Quad
                progress = progress * progress;
                float startY = screenH - MARGIN_TOP - POPUP_HEIGHT;
                float endY = screenH;
                drawY = startY + (endY - startY) * progress;
                break;
            }
            // HIDDEN状态在方法开头已经处理，不会执行到这里
        }

        float drawX = (screenW - popupWidth) / 2f;

        // 1. 绘制背景框 (半透明黑色，带圆角效果)
        batch.setColor(0f, 0f, 0f, 0.9f); // 稍微更不透明，更清晰
        batch.draw(whitePixel, drawX, drawY, popupWidth, POPUP_HEIGHT);

        // 2. 绘制金色边框 (左侧粗，其他细)
        batch.setColor(1f, 0.84f, 0f, 1f); // 金色
        float borderThickness = 4f; // 左侧边框稍微细一点
        batch.draw(whitePixel, drawX, drawY, borderThickness, POPUP_HEIGHT); // 左侧金条
        batch.draw(whitePixel, drawX, drawY, popupWidth, 2); // 下边
        batch.draw(whitePixel, drawX, drawY + POPUP_HEIGHT - 2, popupWidth, 2); // 上边
        batch.draw(whitePixel, drawX + popupWidth - 2, drawY, 2, POPUP_HEIGHT); // 右边

        // 3. 绘制文字（更紧凑的布局）
        float textX = drawX + PADDING;
        
        // 标题 "ACHIEVEMENT UNLOCKED"（更小更紧凑）
        font.getData().setScale(0.65f);
        font.setColor(Color.GOLD);
        font.draw(batch, "ACHIEVEMENT UNLOCKED", textX, drawY + POPUP_HEIGHT - 12);

        // 成就名称（稍微减小）
        font.getData().setScale(0.95f);
        font.setColor(Color.WHITE);
        font.draw(batch, currentAchievement.displayName, textX, drawY + POPUP_HEIGHT - 30);

        // 成就描述（更紧凑，只显示关键信息）
        font.getData().setScale(0.6f);
        font.setColor(Color.LIGHT_GRAY);
        String desc = currentAchievement.description;
        // 根据弹窗宽度调整描述长度
        int maxDescLength = (int)(popupWidth / 8f); // 大约每8像素一个字符
        if (desc.length() > maxDescLength) {
            desc = desc.substring(0, maxDescLength - 3) + "...";
        }
        font.draw(batch, desc, textX, drawY + 15);

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
            case HIDDEN:
                // HIDDEN状态不需要更新
                break;
        }
    }

    public boolean isBusy() {
        return state != State.HIDDEN;
    }
}