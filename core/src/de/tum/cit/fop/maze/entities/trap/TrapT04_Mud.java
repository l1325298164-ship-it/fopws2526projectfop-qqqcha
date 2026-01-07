package de.tum.cit.fop.maze.entities.trap;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Array;
import de.tum.cit.fop.maze.entities.Player;
import de.tum.cit.fop.maze.game.GameConstants;
import de.tum.cit.fop.maze.utils.Logger;
import de.tum.cit.fop.maze.utils.TextureManager;

public class TrapT04_Mud extends Trap {

    /* ===== 参数 ===== */
    private static final float SLOW_DURATION = 1.5f; // 每次踩刷新 1.5s

    // 🔥 动画相关
    private TextureAtlas atlas;
    private Array<TextureAtlas.AtlasRegion> frames;
    private int totalFrames = 0;
    private float animationTimer = 0f;
    private float frameDuration = 0.2f; // 每帧持续时间

    // 🔥 气泡效果参数
    private float bubbleTimer = 0f;
    private final Array<MudBubble> bubbles = new Array<>();
    private static final int MAX_BUBBLES = 3;
    private static final float BUBBLE_SPAWN_INTERVAL = 1.5f;

    // 🔥 泥潭波动效果
    private float waveOffset = 0f;
    private float waveSpeed = 1.5f;
    private float waveAmplitude = 0.05f; // 波动幅度

    // 🔥 气泡类
    private static class MudBubble {
        float x, y;           // 位置（相对坐标 0-1）
        float size;           // 大小
        float speed;          // 上升速度
        float life;           // 寿命
        float maxLife;        // 最大寿命
        float startTime;      // 开始时间（用于延迟出现）

        public void update(float delta) {
            life += delta;
            y += speed * delta;
        }

        public boolean isAlive() {
            return life < maxLife;
        }

        public float getAlpha() {
            if (life < 0.3f) {
                return life / 0.3f; // 淡入
            } else if (life > maxLife - 0.3f) {
                return (maxLife - life) / 0.3f; // 淡出
            }
            return 1.0f;
        }
    }

    public TrapT04_Mud(int x, int y) {
        super(x, y);
        loadAnimation();
        initBubbles();
    }

    private void loadAnimation() {
        try {
            TextureManager tm = TextureManager.getInstance();
            atlas = tm.getTrapT04Atlas();

            if (atlas == null) {
                Logger.warning("T04 Atlas 为空，尝试直接加载");
                atlas = new TextureAtlas("ani/T04/T04.atlas");
            }

            if (atlas != null) {
                // 尝试不同的帧名称
                frames = atlas.findRegions("mud");
                if (frames == null || frames.size == 0) {
                    frames = atlas.findRegions("T04");
                }
                if (frames == null || frames.size == 0) {
                    frames = atlas.findRegions("swamp");
                }
                if (frames == null || frames.size == 0) {
                    frames = atlas.findRegions("trap_mud");
                }

                if (frames != null && frames.size > 0) {
                    totalFrames = frames.size;
                    Logger.debug("✅ T04 动画加载成功: " + frames.size + "帧");
                } else {
                    frames = new Array<>();
                    Logger.debug("⚠️ T04 没有动画帧，将使用形状渲染");
                }
            } else {
//                Logger.error("❌ T04 无法加载 Atlas 文件");
                frames = new Array<>();
            }
        } catch (Exception e) {
//            Logger.error("❌ T04 加载动画失败: " + e.getMessage());
            frames = new Array<>();
        }
    }

    private void initBubbles() {
        bubbles.clear();
        // 初始创建几个气泡
        for (int i = 0; i < MAX_BUBBLES; i++) {
            createBubble(MathUtils.random(0f, 2f)); // 随机延迟出现
        }
    }

    private void createBubble(float delay) {
        MudBubble bubble = new MudBubble();
        bubble.x = MathUtils.random(0.1f, 0.9f); // 随机水平位置
        bubble.y = -0.1f; // 从底部开始
        bubble.size = MathUtils.random(0.05f, 0.15f); // 随机大小
        bubble.speed = MathUtils.random(0.1f, 0.3f); // 随机上升速度
        bubble.maxLife = MathUtils.random(1.5f, 3.0f); // 随机寿命
        bubble.life = -delay; // 负值表示延迟
        bubble.startTime = delay;
        bubbles.add(bubble);
    }

    @Override
    public void update(float delta) {
        if (!active) return;

        // 🔥 更新动画计时器
        animationTimer += delta;

        // 🔥 更新波动效果
        waveOffset += delta * waveSpeed;

        // 🔥 更新气泡
        bubbleTimer += delta;
        if (bubbleTimer >= BUBBLE_SPAWN_INTERVAL) {
            bubbleTimer = 0f;
            // 移除死亡的气泡
            for (int i = bubbles.size - 1; i >= 0; i--) {
                if (!bubbles.get(i).isAlive()) {
                    bubbles.removeIndex(i);
                }
            }
            // 补充气泡
            if (bubbles.size < MAX_BUBBLES) {
                createBubble(0f);
            }
        }

        // 🔥 更新所有气泡
        for (MudBubble bubble : bubbles) {
            bubble.update(delta);
        }
    }

    @Override
    public boolean isPassable() {
        return true;
    }

    @Override
    public void onPlayerStep(Player player) {
        // 只减速，不扣血
        player.applySlow(SLOW_DURATION);

        // 🔥 玩家踩踏时产生更多气泡
        if (bubbles.size < MAX_BUBBLES * 2) {
            for (int i = 0; i < 2; i++) {
                createBubble(MathUtils.random(0f, 0.5f));
            }
        }
    }

    /* ================= 渲染 ================= */

    @Override
    public void drawShape(ShapeRenderer sr) {
        if (!active) return;

        float size = GameConstants.CELL_SIZE;
        float px = x * size;
        float py = y * size;

        // 🔥 基础泥潭颜色：深棕 / 暗绿
        Color baseColor = new Color(0.35f, 0.25f, 0.15f, 1f);

        // 🔥 添加轻微的波动效果
        float wave = (float) Math.sin(waveOffset) * waveAmplitude;
        float adjustedSize = size * (1 + wave);
        float offset = (adjustedSize - size) / 2f;

        // 🔥 绘制泥潭主体
        sr.setColor(baseColor);
        sr.rect(px - offset, py - offset, adjustedSize, adjustedSize);

        // 🔥 绘制气泡
        drawBubbles(sr, px, py, size);
    }

    // 🔥 绘制气泡
    private void drawBubbles(ShapeRenderer sr, float px, float py, float cellSize) {
        sr.setColor(new Color(0.45f, 0.35f, 0.25f, 0.7f));

        for (MudBubble bubble : bubbles) {
            if (!bubble.isAlive()) continue;

            float alpha = bubble.getAlpha();
            if (alpha <= 0) continue;

            // 计算气泡位置和大小
            float bubbleX = px + bubble.x * cellSize;
            float bubbleY = py + bubble.y * cellSize;
            float bubbleSize = bubble.size * cellSize * alpha;

            // 绘制圆形气泡
            sr.circle(bubbleX, bubbleY, bubbleSize / 2, 8);

            // 添加高光
            sr.setColor(new Color(0.55f, 0.45f, 0.35f, alpha * 0.6f));
            sr.circle(bubbleX - bubbleSize * 0.2f, bubbleY + bubbleSize * 0.2f,
                    bubbleSize * 0.2f, 6);
            sr.setColor(new Color(0.45f, 0.35f, 0.25f, alpha * 0.7f));
        }
    }

    @Override
    public void drawSprite(SpriteBatch batch) {
        if (!active) return;
        if (frames == null || frames.size == 0) return;

        float size = GameConstants.CELL_SIZE;
        float px = x * size;
        float py = y * size;

        // 🔥 计算当前帧
        int frameIndex = (int)(animationTimer / frameDuration) % frames.size;
        TextureRegion frame = frames.get(frameIndex);
        if (frame == null) return;

        // 🔥 应用波动效果
        float wave = (float) Math.sin(waveOffset) * waveAmplitude;
        float adjustedSize = size * (1 + wave);
        float offset = (adjustedSize - size) / 2f;

        // 🔥 设置泥潭颜色（偏暗）
        batch.setColor(0.8f, 0.8f, 0.8f, 1f);

        // 🔥 绘制泥潭纹理
        batch.draw(frame,
                px - offset,
                py - offset,
                adjustedSize,
                adjustedSize);

        // 🔥 绘制气泡
        drawBubbles(batch, px, py, size);

        // 恢复颜色
        batch.setColor(1, 1, 1, 1);
    }

    // 🔥 用SpriteBatch绘制气泡
    private void drawBubbles(SpriteBatch batch, float px, float py, float cellSize) {
        // 如果有气泡纹理可以使用，这里简单用形状
        // 如果需要更复杂的气泡，可以添加气泡纹理
    }

    @Override
    public RenderType getRenderType() {
        // 如果有动画帧就使用精灵渲染，否则使用形状渲染
        return (frames != null && frames.size > 0) ? RenderType.SPRITE : RenderType.SHAPE;
    }

    // 🔥 辅助方法：获取当前帧索引（用于调试）
    public int getCurrentFrameIndex() {
        if (frames == null || frames.size == 0) return 0;
        return (int)(animationTimer / frameDuration) % frames.size;
    }

    // 🔥 获取气泡数量（用于调试）
    public int getBubbleCount() {
        return bubbles.size;
    }
}