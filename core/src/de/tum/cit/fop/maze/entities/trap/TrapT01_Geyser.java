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

public class TrapT01_Geyser extends Trap {

    private enum State {
        IDLE,
        WARNING,
        ERUPTING,
        COOLDOWN
    }

    private State state = State.IDLE;
    private float timer = 0f;
    private float damageTickTimer = 0f;

    /* ===== 可调参数 ===== */
    private final float idleDuration     = 1.0f;
    private final float warningDuration  = 1.0f;
    private final float eruptDuration    = 1.0f;
    private final float cooldownDuration = 0.8f;
    private final int damagePerTick = 10;
    private final float damageInterval = 0.5f;

    /* ===== 动画相关 ===== */
    private TextureAtlas atlas;
    private Array<TextureAtlas.AtlasRegion> frames;
    private int totalFrames = 0;



    public TrapT01_Geyser(int x, int y, float cycleDuration) {
        super(x, y);

        Logger.debug("=== T01 地热喷口创建于 (" + x + "," + y + ") ===");
        Logger.debug("IDLE: " + idleDuration + "s, WARNING: " + warningDuration + "s");
        Logger.debug("ERUPT: " + eruptDuration + "s, COOLDOWN: " + cooldownDuration + "s");

        loadAnimation();

        // 确认加载结果
        if (frames != null && frames.size > 0) {
            Logger.debug("✅ T01 准备就绪，动画帧数: " + frames.size);
        } else {
            Logger.warning("⚠️ T01 没有动画帧，将使用回退渲染");
        }
    }
    // 🔥 加载动画资源
    private void loadAnimation() {
        try {
            // 尝试从 TextureManager 获取
            TextureManager tm = TextureManager.getInstance();
            atlas = tm.getTrapT01Atlas(); // 需要在 TextureManager 中添加这个方法

            if (atlas == null) {
                Logger.warning("T01 Atlas 为空，尝试直接加载");
                atlas = new TextureAtlas("ani/T01/T01.atlas");
            }

            if (atlas != null) {
                // 🔥 尝试多个可能的动画名称
                String[] possibleNames = {"T01", "geyser", "T01_anim", "geyser_anim", "anim"};
                for (String name : possibleNames) {
                    frames = atlas.findRegions(name);
                    if (frames != null && frames.size > 0) {
                        totalFrames = frames.size;
                        Logger.debug("✅ T01 找到动画: " + name + " (" + totalFrames + "帧)");
                        break;
                    }
                }

                if (frames == null || frames.size == 0) {
                    Logger.warning("❌ T01 没有找到动画帧，检查 atlas 文件");
                    frames = new Array<>();
                }
            } else {
                Logger.error("❌ T01 无法加载 Atlas 文件");
                frames = new Array<>();
            }
        } catch (Exception e) {
            Logger.error("❌ T01 加载动画失败: " + e.getMessage());
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
            case IDLE -> {
                if (timer >= idleDuration) {
                    state = State.WARNING;
                    timer = 0f;
                    Logger.debug("T01 进入警告状态");
                }
            }

            case WARNING -> {
                if (timer >= warningDuration) {
                    state = State.ERUPTING;
                    timer = 0f;
                    damageTickTimer = 0f;
                    Logger.debug("T01 开始喷发！");
                }
            }

            case ERUPTING -> {
                damageTickTimer -= delta;

                if (timer >= eruptDuration) {
                    state = State.COOLDOWN;
                    timer = 0f;
                    Logger.debug("T01 进入冷却状态");
                }
            }

            case COOLDOWN -> {
                if (timer >= cooldownDuration) {
                    state = State.IDLE;
                    timer = 0f;
                    Logger.debug("T01 恢复待机状态");
                }
            }
        }
    }

    @Override
    public void onPlayerStep(Player player) {
        if (!active || state != State.ERUPTING) return;

        // 在喷射阶段，每 0.5s 扣一次血
        if (damageTickTimer <= 0f) {
            player.takeDamage(damagePerTick);
            damageTickTimer = damageInterval;
            Logger.debug("T01 对玩家造成伤害: " + damagePerTick);
        }
    }

    // 🔥 安全的帧索引计算
    private int getFrameIndex() {
        if (totalFrames == 0) return 0;

        int frameIndex = 0;

        switch (state) {
            case IDLE -> {
                // 待机阶段：使用前 1-3 帧（占总帧数的 20%）
                float t = timer / idleDuration;
                int idleFrames = Math.max(1, totalFrames / 5);
                frameIndex = Math.min(idleFrames - 1, (int)(t * idleFrames));
            }

            case WARNING -> {
                // 警告阶段：使用接下来的 4-6 帧（占总帧数的 20%）
                float t = timer / warningDuration;
                int warningFrames = Math.max(1, totalFrames / 5);
                int startFrame = Math.max(1, totalFrames / 5); // 跳过待机帧
                frameIndex = startFrame + Math.min(warningFrames - 1, (int)(t * warningFrames));
            }

            case ERUPTING -> {
                // 喷发阶段：使用中间的 7-12 帧（占总帧数的 40%）
                float t = timer / eruptDuration;
                int eruptFrames = Math.max(1, totalFrames * 2 / 5);
                int startFrame = Math.max(1, totalFrames * 2 / 5); // 跳过待机和警告帧
                frameIndex = startFrame + Math.min(eruptFrames - 1, (int)(t * eruptFrames));
            }

            case COOLDOWN -> {
                // 冷却阶段：使用最后的 13-15 帧（占总帧数的 20%）
                float t = timer / cooldownDuration;
                int cooldownFrames = Math.max(1, totalFrames / 5);
                int startFrame = Math.max(1, totalFrames * 4 / 5); // 跳过前面的帧
                frameIndex = startFrame + Math.min(cooldownFrames - 1, (int)(t * cooldownFrames));
            }
        }

        // 确保索引在有效范围内
        return MathUtils.clamp(frameIndex, 0, totalFrames - 1);
    }

    /* ================= 渲染 ================= */

    @Override
    public void drawShape(ShapeRenderer sr) {
        if (!active) return;

        float size = GameConstants.CELL_SIZE;
        float px = x * size;
        float py = y * size;

        switch (state) {
            case IDLE -> sr.setColor(new Color(0.4f, 0.25f, 0.1f, 0.4f));
            case WARNING -> sr.setColor(Color.RED);
            case ERUPTING -> sr.setColor(new Color(1f, 0.5f, 0f, 1f));
            case COOLDOWN -> sr.setColor(new Color(0.8f, 0.8f, 0.8f, 0.6f));
        }

        sr.rect(px, py, size, size);
    }

    @Override
    public void drawSprite(SpriteBatch batch) {
        if (!active) return;
        if (frames == null || frames.size == 0) return;

        int frameIndex = getFrameIndex();
        if (frameIndex < 0 || frameIndex >= frames.size) {
            frameIndex = MathUtils.clamp(frameIndex, 0, frames.size - 1);
        }

        TextureRegion frame = frames.get(frameIndex);
        if (frame == null) return;

        float size = GameConstants.CELL_SIZE;

        // 🔥 竖向放大参数（可调整）
        float verticalScale = 3.5f; // 竖向放大1.5倍
        float horizontalScale = 3.0f; // 横向保持原样

        // 🔥 计算渲染尺寸
        float renderWidth = size * horizontalScale;
        float renderHeight = size * verticalScale;

        // 🔥 计算位置：底部对齐，水平居中
        // 这样喷口看起来是从地面向上延伸
        float offsetX = (size - renderWidth) / 2f;
        float offsetY = 0; // 底部对齐

        // 🔥 喷发时添加闪烁效果
        if (state == State.ERUPTING) {
            float pulse = (float) Math.sin(timer * 10f) * 0.2f + 0.8f;
            batch.setColor(1f, pulse, pulse, 1f);
        }

        // 🔥 根据状态调整竖向放大倍数
        switch (state) {
            case IDLE:
                // 待机状态稍小一些
                renderHeight = size * 1.2f;
                break;
            case WARNING:
                // 警告状态开始变大
                renderHeight = size * 1.8f;
                break;
            case ERUPTING:
                // 喷发状态最大
                renderHeight = size * 2.0f;
                break;
            case COOLDOWN:
                // 冷却状态恢复
                renderHeight = size * 1.3f;
                break;
        }

        // 重新计算Y偏移（保持底部对齐）
        offsetY = 0;

        batch.draw(
                frame,
                x * size + offsetX,
                y * size + offsetY,
                renderWidth,
                renderHeight
        );

        // 🔥 恢复颜色
        if (state == State.ERUPTING) {
            batch.setColor(1, 1, 1, 1);
        }

        // 🔥 调试信息
        if (Logger.isDebugEnabled()) {
            Logger.debug("T01渲染: 状态=" + state +
                    " 尺寸=" + renderWidth + "x" + renderHeight +
                    " 位置=(" + (x * size + offsetX) + "," + (y * size + offsetY) + ")");
        }
    }

    @Override
    public RenderType getRenderType() {
        // 🔥 如果有动画帧就使用精灵渲染，否则使用形状渲染
        return (frames != null && frames.size > 0) ? RenderType.SPRITE : RenderType.SHAPE;
    }


}