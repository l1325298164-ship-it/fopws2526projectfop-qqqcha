package de.tum.cit.fop.maze.entities.trap;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Array;
import de.tum.cit.fop.maze.entities.Player;
import de.tum.cit.fop.maze.entities.enemy.Enemy;
import de.tum.cit.fop.maze.game.GameConstants;
import de.tum.cit.fop.maze.game.GameManager;
import de.tum.cit.fop.maze.utils.Logger;
import de.tum.cit.fop.maze.utils.TextureManager;

public class TrapT02_PearlMine extends Trap {

    private enum State {
        IDLE,           // 待机状态
        WARNING,        // 警告闪烁
        EXPLODING,      // 爆炸膨胀中
        COLLAPSING,     // 垮掉收缩
        VANISHING       // 消失
    }

    private State state = State.IDLE;
    private float timer = 0f;

    // 🔥 爆炸效果参数
    private float explosionScale = 1.0f;      // 当前缩放
    private float fragmentOffset = 0f;        // 碎片偏移
    private Color currentColor = new Color(1, 1, 1, 1); // 当前颜色
    private float rotation = 0f;              // 旋转角度

    // 🔥 碎片系统
    private int fragmentCount = 8; // 可配置的碎片数量
    private Array<FragmentData> fragments;
    private boolean fragmentsInitialized = false;

    // 🔥 碎片数据类
    private static class FragmentData {
        float dirX, dirY;     // 飞散方向
        float speed;          // 飞散速度
        float rotationSpeed;  // 旋转速度
        float scale;          // 碎片大小比例
        float alpha;          // 透明度
    }

    // 🔥 动画相关
    private TextureAtlas atlas;
    private Array<TextureAtlas.AtlasRegion> frames;
    private int totalFrames = 0;

    /* ===== 参数 ===== */
    private static final float WARNING_DURATION = 0.8f;     // 警告闪烁时间
    private static final float EXPLODE_DURATION = 0.4f;     // 爆炸膨胀时间
    private static final float COLLAPSE_DURATION = 0.3f;    // 垮掉收缩时间
    private static final float VANISH_DURATION = 0.2f;      // 消失时间
    private static final int DAMAGE = 15;

    // 🔥 爆炸效果参数
    private static final float MAX_EXPLOSION_SCALE = 2.0f;  // 最大膨胀倍数
    private static final float MIN_COLLAPSE_SCALE = 0.3f;   // 最小收缩倍数
    private static final float MAX_ROTATION = 45f;          // 最大旋转角度

    private final GameManager gm;

    public TrapT02_PearlMine(int x, int y, GameManager gm) {
        super(x, y);
        this.gm = gm;

        Logger.debug("=== T02 珍珠地雷创建于 (" + x + "," + y + ") ===");
        loadAnimation();
        initFragments(); // 初始化碎片系统
    }

    private void loadAnimation() {
        try {
            TextureManager tm = TextureManager.getInstance();
            atlas = tm.getTrapT02Atlas();

            if (atlas == null) {
                Logger.warning("T02 Atlas 为空，尝试直接加载");
                atlas = new TextureAtlas("ani/T02/T02.atlas");
            }

            if (atlas != null) {
                frames = atlas.findRegions("T02");
                if (frames == null || frames.size == 0) {
                    frames = atlas.findRegions("mine");
                }
                if (frames == null || frames.size == 0) {
                    frames = atlas.findRegions("pearl_mine");
                }

                if (frames != null && frames.size > 0) {
                    totalFrames = frames.size;
                } else {
                    frames = new Array<>();
                }
            } else {
                Logger.error("❌ T02 无法加载 Atlas 文件");
                frames = new Array<>();
            }
        } catch (Exception e) {
            Logger.error("❌ T02 加载动画失败: " + e.getMessage());
            frames = new Array<>();
        }
    }

    @Override
    public boolean isPassable() {
        return true;
    }

    @Override
    public void update(float delta) {
        if (!active) return;

        timer += delta;

        switch (state) {
            case IDLE:
                // 待机状态 - 不做任何事，等待触发
                updateIdle(delta);
                break;

            case WARNING:
                // 警告状态 - 计时后爆炸
                updateWarning(delta);
                if (timer >= WARNING_DURATION) {
                    state = State.EXPLODING;
                    timer = 0f;
                    Logger.debug("T02 开始爆炸膨胀！");
                }
                break;

            case EXPLODING:
                // 爆炸膨胀状态
                updateExploding(delta);
                if (timer >= EXPLODE_DURATION) {
                    explode(); // 执行爆炸伤害逻辑
                    state = State.COLLAPSING;
                    timer = 0f;
                    Logger.debug("T02 进入垮掉阶段");
                }
                break;

            case COLLAPSING:
                // 垮掉收缩状态
                updateCollapsing(delta);
                if (timer >= COLLAPSE_DURATION) {
                    state = State.VANISHING;
                    timer = 0f;
                    Logger.debug("T02 开始消失");
                }
                break;

            case VANISHING:
                // 消失状态
                updateVanishing(delta);
                if (timer >= VANISH_DURATION) {
                    active = false; // 完全消失
                    Logger.debug("T02 消失完成");
                }
                break;
        }

        // 更新碎片状态
        if (state == State.COLLAPSING || state == State.VANISHING) {
            updateFragments(delta);
        }
    }

    // 🔥 更新碎片状态
    private void updateFragments(float delta) {
        if (!fragmentsInitialized) return;

        for (FragmentData frag : fragments) {
            // 逐渐降低透明度
            frag.alpha = Math.max(0, frag.alpha - delta * 2f);
            // 逐渐缩小
            frag.scale = Math.max(0.1f, frag.scale - delta * 0.5f);
        }
    }

    // 🔥 各状态更新方法
    private void updateIdle(float delta) {
        // 轻微的呼吸效果
        float breath = (float) Math.sin(timer * 2f) * 0.05f;
        explosionScale = 1.0f + breath;
        currentColor.set(1, 1, 1, 1);
    }

    private void updateWarning(float delta) {
        // 快速闪烁红→白
        float blink = (float) Math.sin(timer * 20f);
        if (blink > 0) {
            // 红色闪烁
            currentColor.set(1, 0.2f, 0.2f, 1);
            explosionScale = 1.0f + 0.1f;
        } else {
            // 恢复白色
            currentColor.set(1, 1, 1, 1);
            explosionScale = 1.0f;
        }
    }

    private void updateExploding(float delta) {
        float progress = timer / EXPLODE_DURATION;

        // 1. 快速膨胀
        explosionScale = 1.0f + progress * (MAX_EXPLOSION_SCALE - 1.0f);

        // 2. 颜色从白→鲜红→暗红
        if (progress < 0.5f) {
            // 快速变红
            float redProgress = progress * 2;
            currentColor.r = 1.0f;
            currentColor.g = 1.0f - redProgress;
            currentColor.b = 1.0f - redProgress * 0.8f;
        } else {
            // 变暗红
            float darkProgress = (progress - 0.5f) * 2;
            currentColor.r = 1.0f - darkProgress * 0.3f;
            currentColor.g = 0.2f;
            currentColor.b = 0.2f;
        }

        // 3. 开始轻微旋转
        rotation = progress * MAX_ROTATION;
    }

    private void updateCollapsing(float delta) {
        float progress = timer / COLLAPSE_DURATION;

        // 1. 快速收缩垮掉
        explosionScale = MAX_EXPLOSION_SCALE - progress * (MAX_EXPLOSION_SCALE - MIN_COLLAPSE_SCALE);

        // 2. 颜色从暗红→黑
        currentColor.r = 0.7f - progress * 0.7f;
        currentColor.g = 0.2f - progress * 0.2f;
        currentColor.b = 0.2f - progress * 0.2f;

        // 3. 继续旋转并开始破碎
        rotation = MAX_ROTATION + progress * 30f;
        fragmentOffset = progress * 15f; // 增加偏移量
    }

    private void updateVanishing(float delta) {
        float progress = timer / VANISH_DURATION;

        // 1. 继续收缩
        explosionScale = MIN_COLLAPSE_SCALE * (1.0f - progress);

        // 2. 变透明
        currentColor.a = 1.0f - progress;

        // 3. 加快旋转和破碎
        rotation += delta * 180f;
        fragmentOffset += delta * 30f; // 更快飞散
    }

    @Override
    public void onPlayerStep(Player player) {
        // 只能被主角触发 & 只能触发一次
        if (state != State.IDLE) return;

        state = State.WARNING;
        timer = 0f;
        Logger.debug("T02 被触发，进入警告状态");
    }

    /** 爆炸逻辑 */
    private void explode() {
        int cx = x;
        int cy = y;

        // ===== 伤害玩家 =====
        Player player = gm.getPlayer();
        if (Math.abs(player.getX() - cx) <= 1 &&
                Math.abs(player.getY() - cy) <= 1) {
            player.takeDamage(DAMAGE);
            Logger.debug("T02 对玩家造成伤害: " + DAMAGE);
        }

        // ===== 伤害范围内所有小怪 =====
        for (Enemy enemy : gm.getEnemies()) {
            if (Math.abs(enemy.getX() - cx) <= 1 &&
                    Math.abs(enemy.getY() - cy) <= 1) {
                enemy.takeDamage(DAMAGE);
                Logger.debug("T02 对敌人造成伤害: " + DAMAGE);
            }
        }
    }

    // 🔥 计算当前帧索引
    private int getFrameIndex() {
        if (totalFrames == 0) return 0;

        float progress = 0f;

        switch (state) {
            case IDLE:
                // 待机状态：缓慢循环前几帧
                progress = (timer % 3.0f) / 3.0f;
                return (int)(progress * Math.min(4, totalFrames));

            case WARNING:
                // 警告状态：快速闪烁
                progress = timer / WARNING_DURATION;
                int warningStart = Math.min(4, totalFrames - 1);
                int warningEnd = Math.min(8, totalFrames - 1);
                int warningFrames = warningEnd - warningStart + 1;
                if (warningFrames <= 0) warningFrames = 1;
                return warningStart + (int)(progress * warningFrames);

            case EXPLODING:
            case COLLAPSING:
            case VANISHING:
                // 爆炸相关状态：使用爆炸帧
                progress = timer / (EXPLODE_DURATION + COLLAPSE_DURATION + VANISH_DURATION);
                int explodeStart = Math.max(0, totalFrames - 6);
                int explodeFrames = totalFrames - explodeStart;
                if (explodeFrames <= 0) explodeFrames = 1;
                return explodeStart + (int)(progress * explodeFrames);

            default:
                return 0;
        }
    }

    /* ================= 渲染 ================= */

    @Override
    public void drawShape(ShapeRenderer sr) {
        if (frames != null && frames.size > 0) return;
        if (!active) return;

        float size = GameConstants.CELL_SIZE;
        float px = x * size;
        float py = y * size;

        switch (state) {
            case IDLE:
                sr.setColor(new Color(0.6f, 0.6f, 0.6f, 1f));
                sr.rect(px, py, size, size);
                break;
            case WARNING:
                float blink = (float) Math.sin(timer * 10f) * 0.5f + 0.5f;
                sr.setColor(1f, blink, blink, 1f);
                sr.rect(px, py, size, size);
                break;
            case EXPLODING:
            case COLLAPSING:
            case VANISHING:
                // 对于形状渲染，也应用颜色变化
                sr.setColor(currentColor);
                float scaledSize = size * explosionScale;
                float offset = (scaledSize - size) / 2f;
                sr.rect(px - offset, py - offset, scaledSize, scaledSize);
                break;
        }
    }

    @Override
    public void drawSprite(SpriteBatch batch) {
        if (!active) return;
        if (frames == null || frames.size == 0) return;

        int frameIndex = getFrameIndex();
        frameIndex = MathUtils.clamp(frameIndex, 0, frames.size - 1);
        TextureRegion frame = frames.get(frameIndex);
        if (frame == null) return;

        float size = GameConstants.CELL_SIZE;
        float halfSize = size / 2f;
        float centerX = x * size + halfSize;
        float centerY = y * size + halfSize;

        // 应用当前颜色
        batch.setColor(currentColor);

        switch (state) {
            case WARNING:
                // 警告状态：闪烁效果
                float scaledSize = size * explosionScale;
                float offset = (scaledSize - size) / 2f;
                batch.draw(frame,
                        x * size - offset,
                        y * size - offset,
                        scaledSize, scaledSize);
                break;

            case EXPLODING:
                // 膨胀阶段：正常缩放旋转
                renderExplosionPhase(batch, frame, centerX, centerY, halfSize);
                break;

            case COLLAPSING:
            case VANISHING:
                // 🔥 垮掉效果：使用碎片系统
                if (fragmentsInitialized) {
                    renderFragments(batch, frame, centerX, centerY, halfSize);
                } else {
                    renderExplosionPhase(batch, frame, centerX, centerY, halfSize);
                }
                break;

            default:
                // 待机状态：正常渲染
                batch.draw(frame, x * size, y * size, size, size);
                break;
        }

        // 恢复颜色
        batch.setColor(1, 1, 1, 1);
    }

    // 🔥 渲染爆炸膨胀阶段
    private void renderExplosionPhase(SpriteBatch batch, TextureRegion frame,
                                      float centerX, float centerY, float halfSize) {
        float scaledHalfSize = halfSize * explosionScale;
        batch.draw(frame,
                centerX - scaledHalfSize,
                centerY - scaledHalfSize,
                scaledHalfSize, scaledHalfSize,
                scaledHalfSize * 2, scaledHalfSize * 2,
                1, 1,
                rotation);
    }

    // 🔥 初始化碎片系统
    private void initFragments() {
        fragments = new Array<>();

        for (int i = 0; i < fragmentCount; i++) {
            FragmentData frag = new FragmentData();

            // 随机方向（圆形分布）
            float angle = MathUtils.random(0, 360);
            frag.dirX = MathUtils.cosDeg(angle);
            frag.dirY = MathUtils.sinDeg(angle);

            // 随机速度
            frag.speed = MathUtils.random(0.8f, 2.0f);

            // 随机旋转速度
            frag.rotationSpeed = MathUtils.random(-360f, 360f);

            // 随机大小
            frag.scale = MathUtils.random(0.3f, 0.8f);

            // 初始透明度
            frag.alpha = 1.0f;

            fragments.add(frag);
        }
        fragmentsInitialized = true;
    }

    // 🔥 渲染碎片
    private void renderFragments(SpriteBatch batch, TextureRegion frame,
                                 float centerX, float centerY, float halfSize) {
        if (!fragmentsInitialized || fragments.size == 0) return;

        float baseSize = halfSize * 0.7f;

        // 保存原始颜色
        Color originalColor = batch.getColor();

        for (FragmentData frag : fragments) {
            float currentOffset = fragmentOffset * frag.speed;
            float fragSize = baseSize * frag.scale;

            // 设置碎片的颜色和透明度
            Color fragColor = new Color(currentColor);
            fragColor.a *= frag.alpha;
            batch.setColor(fragColor);

            // 计算碎片位置
            float fragX = centerX + frag.dirX * currentOffset;
            float fragY = centerY + frag.dirY * currentOffset;

            // 绘制碎片
            batch.draw(frame,
                    fragX - fragSize,
                    fragY - fragSize,
                    fragSize, fragSize,
                    fragSize * 2, fragSize * 2,
                    1, 1,
                    rotation + frag.rotationSpeed * timer);
        }

        // 恢复原始颜色
        batch.setColor(originalColor);
    }

    // 🔥 设置碎片数量
    public void setFragmentCount(int count) {
        this.fragmentCount = Math.max(1, count); // 至少1个碎片
        fragmentsInitialized = false; // 标记需要重新初始化
    }

    @Override
    public RenderType getRenderType() {
        return (frames != null && frames.size > 0) ? RenderType.SPRITE : RenderType.SHAPE;
    }

    // 🔥 获取当前状态（用于调试）
    public State getState() {
        return state;
    }

    public float getTimer() {
        return timer;
    }

    // 🔥 获取爆炸效果参数（可用于粒子效果）
    public float getExplosionScale() {
        return explosionScale;
    }

    public Color getCurrentColor() {
        return currentColor;
    }

    public float getRotation() {
        return rotation;
    }

    // 🔥 获取碎片数量
    public int getFragmentCount() {
        return fragmentCount;
    }
}