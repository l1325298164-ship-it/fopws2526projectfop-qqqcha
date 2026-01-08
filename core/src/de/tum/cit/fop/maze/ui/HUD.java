package de.tum.cit.fop.maze.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.*;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Matrix4;
import de.tum.cit.fop.maze.abilities.Ability;
import de.tum.cit.fop.maze.abilities.DashAbility;
import de.tum.cit.fop.maze.entities.Compass;
import de.tum.cit.fop.maze.game.GameConstants;
import de.tum.cit.fop.maze.game.GameManager;
import de.tum.cit.fop.maze.utils.Logger;
import de.tum.cit.fop.maze.utils.TextureManager;

import java.util.function.ToDoubleBiFunction;

public class HUD {
    private BitmapFont font;
    private GameManager gameManager;
    private TextureManager textureManager;

    // ❤ 生命值贴图
    private Texture heartFull;   // live_00
    private Texture heartHalf;   // live_01
    private static final int MAX_HEARTS_DISPLAY = 40; // 最多显示 40 颗
    private static final int HEARTS_PER_ROW = 20;     // 每行最多 20 颗
    private static final int HEART_SPACING = 70;      // 爱心之间的水平间距
    private static final int ROW_SPACING = 30;        // 行距

    // ===== Mana UI (image-based) =====
    private Texture manaBase;
    private Texture manaFill;
    private Texture manaGlow;
    private Texture manadeco_1;
    private Texture manadeco_2;
    private float manaGlowTime = 0f;

    // 尺寸
    private static final float MANA_BAR_WIDTH  = 220f;
    private static final float MANA_BAR_HEIGHT = 28f;

    // 位置（右下角）
    private static final float MANA_MARGIN_RIGHT = 24f;
    private static final float MANA_MARGIN_BOTTOM = 180f;

    // 🐱 HUD 小猫
    private TextureAtlas catAtlas;
    private Animation<TextureRegion> catNoKeyAnim;
    private Animation<TextureRegion> catHasKeyAnim;
    private float catStateTime = 0f;

    // 🐱 HUD 小猫位置与大小
    private static final float CAT_SIZE = 506f;
    private static final float CAT_MARGIN = 10f; // 距离屏幕边缘

    // ❤ 抖动动画相关
    private int lastLives = -1;
    private boolean shaking = false;
    private float shakeTimer = 0f;
    private static final float SHAKE_DURATION = 0.2f; // 抖动 0.2 秒
    private static final float SHAKE_AMPLITUDE = 4f;  // 抖动幅度（像素）

    // ===== 技能图标：冲刺 =====
    private Texture dashIcon;
    private static final int DASH_MAX_CHARGES = 2;

    // ===== Mana UI =====
    private ShapeRenderer shapeRenderer;
    float barWidth  = Gdx.graphics.getWidth() * 0.66f; // 2/3 屏宽
    float barHeight = barWidth * (32f / 256f);         // 保持 PNG 比例

    private static final int MANA_UI_MARGIN_X = 20;
    private static final int MANA_UI_MARGIN_Y = 100; // 在 Dash 图标上方

    // UI 尺寸
    private static final int DASH_ICON_SIZE = 200;
    private static final int DASH_ICON_SPACING = 10;
    // ===== Dash UI 布局 =====
    private static final int DASH_UI_MARGIN_X = 20; // 距离左边
    private static final int DASH_UI_MARGIN_Y = 20; // 距离下边

    // 🔥 [Treasure] Buff 图标
    private Texture iconAtk;
    private Texture iconRegen;
    private Texture iconMana;

    // ✨ [新增] 分数显示位置
    private static final float SCORE_Y_OFFSET = 60f; // 距离顶部的距离

    public HUD(GameManager gameManager) {
        this.gameManager = gameManager;
        this.font = new BitmapFont();
        this.font.getData().setScale(1.2f);
        this.textureManager = TextureManager.getInstance();
        Logger.debug("HUD initialized with compass support");
        this.shapeRenderer = new ShapeRenderer();

        manaBase = new Texture(Gdx.files.internal("HUD/manabar_base.png"));
        manaFill = new Texture(Gdx.files.internal("HUD/manabar_progress_fill.png"));
        manaGlow = new Texture(Gdx.files.internal("HUD/manabar_progress_grow.png"));
        manadeco_1=new Texture(Gdx.files.internal("HUD/manabar_progress_decoration.png"));
        manadeco_2=new Texture(Gdx.files.internal("HUD/manabar_progress_decoration2.png"));
        heartFull = new Texture("HUD/live_000.png");
        heartHalf = new Texture("HUD/live_001.png");

        dashIcon = new Texture("HUD/icon_dash.png");

        // 🐱 加载 HUD 小猫 Atlas
        catAtlas = new TextureAtlas(Gdx.files.internal("Character/cat/cat.atlas"));

        // 没钥匙动画
        catNoKeyAnim = new Animation<>(
                0.25f,
                catAtlas.findRegions("cat_nokey"),
                Animation.PlayMode.LOOP
        );

        // 有钥匙动画
        catHasKeyAnim = new Animation<>(
                0.25f,
                catAtlas.findRegions("cat_key"),
                Animation.PlayMode.LOOP
        );

        Logger.debug("HUD initialized with heart-based life bar");
        // 🔥 [Treasure] 加载图标
        try {
            iconAtk = new Texture(Gdx.files.internal("Items/icon_atk.png"));
            iconRegen = new Texture(Gdx.files.internal("Items/icon_regen.png"));
            iconMana = new Texture(Gdx.files.internal("Items/icon_mana.png"));
        } catch (Exception e) {
            Logger.error("Buff icons not found! Please check assets/Items/ folder.");
        }

        Logger.debug("HUD initialized");
    }

    /**
     * 渲染游戏进行中的UI
     */
    public void renderInGameUI(SpriteBatch uiBatch) {
        try {
            // 1. 生命值（❤显示）
            renderLivesAsHearts(uiBatch);

            // 2. ✨ [新增] 实时分数显示
            renderScore(uiBatch);

            // 3. 关卡信息
            font.setColor(Color.CYAN);
            font.draw(uiBatch, "Level: " + gameManager.getCurrentLevel(),
                    20, Gdx.graphics.getHeight() - 120);

            // 4. 操作说明
            font.setColor(Color.WHITE);
            font.draw(uiBatch, "WASD to move, Shift to sprint",
                    20, Gdx.graphics.getHeight() - 160);

            // 5. 纹理模式提示 (调试用)
            TextureManager.TextureMode currentMode = textureManager.getCurrentMode();
            if (currentMode != TextureManager.TextureMode.COLOR) {
                font.setColor(Color.GREEN);
                font.draw(uiBatch, "mode: " + currentMode + " (F1-F4 to switch)",
                        Gdx.graphics.getWidth() - 250,
                        Gdx.graphics.getHeight() - 20);
            }

            renderManaBar(uiBatch);
            renderCat(uiBatch);

            // 6. 指南针
            renderCompassAsUI(uiBatch);

            // 7. 技能图标
            renderDashIcon(uiBatch);

            // ============================================
            // 🔥 [Treasure] 左侧 Buff 状态栏 (图标 + 大字)
            // ============================================

            de.tum.cit.fop.maze.entities.Player player = gameManager.getPlayer();

            if (player != null) {
                float startX = 20;
                float startY = Gdx.graphics.getHeight() - 250;
                float iconSize = 48; // 图标大小
                float gap = 60;      // 行间距加大

                // 1. 攻击 Buff (红色)
                if (player.hasBuffAttack()) {
                    if (iconAtk != null) uiBatch.draw(iconAtk, startX, startY, iconSize, iconSize);
                    font.getData().setScale(2.0f);
                    font.setColor(Color.RED);
                    font.draw(uiBatch, "ATK +50%", startX + iconSize + 10, startY + 35);
                    startY -= gap;
                }

                // 2. 回血 Buff (绿色)
                if (player.hasBuffRegen()) {
                    if (iconRegen != null) uiBatch.draw(iconRegen, startX, startY, iconSize, iconSize);
                    font.getData().setScale(2.0f);
                    font.setColor(Color.GREEN);
                    font.draw(uiBatch, "REGEN ON", startX + iconSize + 10, startY + 35);
                    startY -= gap;
                }

                // 3. 耗蓝 Buff (青色)
                if (player.hasBuffManaEfficiency()) {
                    if (iconMana != null) uiBatch.draw(iconMana, startX, startY, iconSize, iconSize);
                    font.getData().setScale(2.0f);
                    font.setColor(Color.CYAN);
                    font.draw(uiBatch, "MANA COST -50%", startX + iconSize + 10, startY + 35);
                    startY -= gap;
                }

                // ⚠️ 还原字体设置
                font.setColor(Color.WHITE);
                font.getData().setScale(1.2f);


                // ============================================
                // 🔥 [Treasure] 屏幕中央飘字 (超大字体通知)
                // ============================================
                String msg = player.getNotificationMessage();
                if (msg != null && !msg.isEmpty()) {
                    float w = Gdx.graphics.getWidth();
                    float h = Gdx.graphics.getHeight();

                    // 设置超大字体
                    font.getData().setScale(2.5f);

                    // 阴影
                    font.setColor(Color.BLACK);
                    font.draw(uiBatch, msg, w / 2f - 200 + 3, h / 2f + 100 - 3);

                    // 正文
                    font.setColor(Color.YELLOW);
                    font.draw(uiBatch, msg, w / 2f - 200, h / 2f + 100);

                    // 还原
                    font.setColor(Color.WHITE);
                    font.getData().setScale(1.2f);
                }
            }

        } catch (Exception e) {
            Logger.debug("HUD failed");
        }
    }

    /**
     * ✨ [新增] 渲染屏幕顶部的实时分数
     */
    private void renderScore(SpriteBatch uiBatch) {
        if (gameManager == null) return;

        // 获取分数 (假设 GameManager 代理了 ScoreManager 的分数获取)
        int currentScore = gameManager.getScore();
        String scoreText = "SCORE: " + currentScore;

        // 临时设置大字体
        font.getData().setScale(1.5f);

        // 计算居中位置
        GlyphLayout layout = new GlyphLayout(font, scoreText);
        float x = (Gdx.graphics.getWidth() - layout.width) / 2f;
        float y = Gdx.graphics.getHeight() - SCORE_Y_OFFSET;

        // 绘制阴影
        font.setColor(0f, 0f, 0f, 0.5f);
        font.draw(uiBatch, scoreText, x + 3, y - 3);

        // 绘制金色正文
        font.setColor(Color.GOLD);
        font.draw(uiBatch, scoreText, x, y);

        // 还原字体设置
        font.setColor(Color.WHITE);
        font.getData().setScale(1.2f);
    }

    // 1. 修改类成员变量，增加上限
    private static final int MAX_PARTICLES = 150;

    public void renderManaBar(SpriteBatch uiBatch) {
        if (gameManager == null || gameManager.getPlayer() == null) return;

        var player = gameManager.getPlayer();
        float percent = Math.max(0f, Math.min(1f, player.getMana() / (float)player.getMaxMana()));

        // === 尺寸与位置 ===
        float barWidth  = Gdx.graphics.getWidth() * 0.66f;
        float barHeight = barWidth * (32f / 256f);
        float x = (Gdx.graphics.getWidth() - barWidth) / 2f - 50;
        float y = barHeight - 130;

        // --- 1. 底座渲染 ---
        uiBatch.setColor(1f, 1f, 1f, 1f);
        uiBatch.draw(manaBase, x, y, barWidth, barHeight);

        if (percent > 0f) {
            // --- 2. 进度条主体 (基础填充) ---
            int srcW = (int)(manaFill.getWidth() * percent);
            TextureRegion fillRegion = new TextureRegion(manaFill, 0, 0, srcW, manaFill.getHeight());

            uiBatch.setColor(1f, 0.7f, 0.9f, 1f); // 粉粉嫩嫩色
            uiBatch.draw(fillRegion, x, y, barWidth * percent, barHeight);

            // --- 3. 启用：renderManaGlowEffect (呼吸立体光) ---
            renderManaGlowEffect(uiBatch, x, y, barWidth, barHeight, percent);

            // --- 4. 超长粒子拖尾逻辑 ---
            updateAndRenderLongTrail(uiBatch, x, y, barWidth, barHeight, percent);

            // --- 5. 圆柱体高光带 (覆盖在呼吸光之上) ---
            uiBatch.setBlendFunction(com.badlogic.gdx.graphics.GL20.GL_SRC_ALPHA, com.badlogic.gdx.graphics.GL20.GL_ONE);
            uiBatch.setColor(1f, 1f, 1f, 0.35f);
            uiBatch.draw(TextureManager.getInstance().getWhitePixel(),
                    x, y + barHeight * 0.52f,
                    barWidth * percent * 0.99f,
                    barHeight * 0.07f);
            uiBatch.setBlendFunction(com.badlogic.gdx.graphics.GL20.GL_SRC_ALPHA, com.badlogic.gdx.graphics.GL20.GL_ONE_MINUS_SRC_ALPHA);
        }

        // --- 6. 装饰层 (最上层遮盖) ---
        uiBatch.setColor(1f, 1f, 1f, 1f);
        uiBatch.draw(manadeco_1, x, y, barWidth, barHeight);
        uiBatch.draw(manadeco_2, x, y, barWidth, barHeight);
    }

    /**
     * 负责管内液体的立体感呼吸光
     */
    private void renderManaGlowEffect(SpriteBatch uiBatch, float x, float y, float w, float h, float percent) {
        manaGlowTime += Gdx.graphics.getDeltaTime();
        // 呼吸频率
        float glowAlpha = 0.4f + 0.3f * (float)Math.sin(manaGlowTime * 3.0f);

        uiBatch.setBlendFunction(com.badlogic.gdx.graphics.GL20.GL_SRC_ALPHA, com.badlogic.gdx.graphics.GL20.GL_ONE);
        uiBatch.setColor(1f, 0.8f, 0.95f, glowAlpha); // 粉色高光

        int srcW = (int)(manaGlow.getWidth() * percent);
        TextureRegion glowRegion = new TextureRegion(manaGlow, 0, 0, srcW, manaGlow.getHeight());

        // 绘制在管子中心，高度稍微压缩以体现圆柱感
        uiBatch.draw(glowRegion, x, y + h * 0.15f, w * percent, h * 0.7f);

        uiBatch.setBlendFunction(com.badlogic.gdx.graphics.GL20.GL_SRC_ALPHA, com.badlogic.gdx.graphics.GL20.GL_ONE_MINUS_SRC_ALPHA);
        uiBatch.setColor(1f, 1f, 1f, 1f);
    }

    /**
     * 负责末端的喷射和超长拖尾
     */
    private void updateAndRenderLongTrail(SpriteBatch uiBatch, float x, float y, float w, float h, float percent) {
        float endX = x + (w * percent);
        float delta = Gdx.graphics.getDeltaTime();

        // --- 1. 粒子生成 (高度收缩 1/3) ---
        float centerOffset = h / 3f; // 计算 1/3 的偏移（上下各缩掉 1/6）
        float activeHeight = h * (2f/3f); // 粒子活动的有效高度

        for (int i = 0; i < 6; i++) {
            if (particles.size() < 150) {
                ManaParticle p = new ManaParticle();
                p.x = endX;
                // ⭐ 粒子高度限制在中心 2/3 区域内
                p.y = y + centerOffset + (float)(Math.random() * activeHeight);

                p.vx = (float) (Math.random() * -300 - 150);
                p.vy = (float) (Math.random() * 40 - 20); // 垂直抖动也稍微收窄
                p.life = 1.2f + (float)Math.random() * 0.8f;

                // ⭐ 颜色改为金色 (亮黄 r=1, g=0.9, b=0.2)
                p.color = new Color(1.0f, 0.85f, 0.3f, 1f);
                particles.add(p);
            }
        }

        // --- 2. 渲染逻辑 (加法混合增强金光) ---
        uiBatch.setBlendFunction(com.badlogic.gdx.graphics.GL20.GL_SRC_ALPHA, com.badlogic.gdx.graphics.GL20.GL_ONE);
        for (int i = particles.size() - 1; i >= 0; i--) {
            ManaParticle p = particles.get(i);
            p.life -= delta;
            if (p.life <= 0 || p.x < x) {
                particles.remove(i); continue;
            }

            p.x += p.vx * delta;
            p.y += p.vy * delta;
            p.vx *= 0.97f;

            // 粒子绘制
            float size = 14f * (p.life / 2.0f);
            uiBatch.setColor(p.color.r, p.color.g, p.color.b, p.life * 0.7f);
            uiBatch.draw(manaGlow, p.x - size/2, p.y - size/2, size, size);
        }
        uiBatch.setBlendFunction(com.badlogic.gdx.graphics.GL20.GL_SRC_ALPHA, com.badlogic.gdx.graphics.GL20.GL_ONE_MINUS_SRC_ALPHA);
    }

    private void renderDashIcon(SpriteBatch uiBatch) {
        DashAbility dash = null;
        for (Ability a : gameManager.getPlayer().getAbilityManager().getAbilities().values()) {
            if (a instanceof DashAbility d) {
                dash = d;
                break;
            }
        }
        if (dash == null) return;

        int dashCharges = dash.getCurrentCharges();
        float progress = dash.getCooldownProgress();

        float x = DASH_UI_MARGIN_X, y = DASH_UI_MARGIN_Y;

        // --- 1. 金色滤镜分级 ---
        if (dashCharges >= 2) {
            // 满层：金光闪闪
            uiBatch.setColor(1.0f, 0.9f, 0.4f, 1f);
        } else if (dashCharges == 1) {
            // 一层：暗金色
            uiBatch.setColor(0.8f, 0.7f, 0.3f, 1f);
        } else {
            // 0层：废旧金属色
            uiBatch.setColor(0.25f, 0.25f, 0.2f, 0.8f);
        }

        uiBatch.draw(dashIcon, x, y, DASH_ICON_SIZE, DASH_ICON_SIZE);

        // --- 2. 冷却遮罩 (金色边缘进度条) ---
        if (dashCharges < 2) {
            float maskHeight = DASH_ICON_SIZE * (1f - progress);
            uiBatch.setColor(0f, 0f, 0f, 0.5f);
            uiBatch.draw(TextureManager.getInstance().getWhitePixel(), x, y, DASH_ICON_SIZE, maskHeight);

            // 金色进度线
            uiBatch.setColor(1.0f, 0.85f, 0.2f, 0.9f);
            uiBatch.draw(TextureManager.getInstance().getWhitePixel(), x, y + maskHeight - 2, DASH_ICON_SIZE, 2);

            uiBatch.setColor(1f, 1f, 1f, 1f); // 还原 Batch 颜色

            // 可选：在遮罩边缘画一条细亮的进度线
            if (maskHeight > 2) {
                uiBatch.setColor(1f, 0.7f, 0.9f, 0.8f); // 粉色进度线
                uiBatch.draw(
                        TextureManager.getInstance().getWhitePixel(),
                        x, y + maskHeight - 2,
                        DASH_ICON_SIZE,
                        2
                );
            }
        }

        // --- 3. 层数文字提示 ---
        font.getData().setScale(1.5f);
        font.setColor(dashCharges > 0 ? Color.WHITE : Color.GRAY);
        font.draw(uiBatch, "x" + dashCharges, x + DASH_ICON_SIZE - 30, y + 40);

        // 还原颜色
        uiBatch.setColor(1f, 1f, 1f, 1f);
        font.getData().setScale(1.2f);
    }

    private void renderCat(SpriteBatch uiBatch) {
        if (gameManager == null || gameManager.getPlayer() == null) return;

        catStateTime += Gdx.graphics.getDeltaTime();

        boolean hasKey = gameManager.getPlayer().hasKey();
        Animation<TextureRegion> anim =
                hasKey ? catHasKeyAnim : catNoKeyAnim;

        TextureRegion frame = anim.getKeyFrame(catStateTime, true);

        float x = Gdx.graphics.getWidth() - CAT_SIZE - CAT_MARGIN+170;
        float y = CAT_MARGIN-80;

        uiBatch.setColor(1f, 1f, 1f, 1f);
        uiBatch.draw(frame, x, y, CAT_SIZE, CAT_SIZE);
    }

    private void renderLivesAsHearts(SpriteBatch uiBatch) {
        // 🔴 关键：UI 颜色必须重置
        uiBatch.setColor(1f, 1f, 1f, 1f);

        int lives = gameManager.getPlayer().getLives();

        /* ================= 抖动触发 ================= */
        if (lastLives != -1 && lives < lastLives) {
            int oldSlot = (lastLives - 1) / 10;
            int newSlot = (lives - 1) / 10;
            int oldInSlot = lastLives - oldSlot * 10;
            int newInSlot = lives - newSlot * 10;
            boolean wasFull = oldInSlot > 5;
            boolean nowHalf = newInSlot <= 5;
            if (oldSlot == newSlot && wasFull && nowHalf) {
                shaking = true;
                shakeTimer = 0f;
            }
        }
        lastLives = lives;

        float delta = Gdx.graphics.getDeltaTime();
        if (shaking) {
            shakeTimer += delta;
            if (shakeTimer >= SHAKE_DURATION) {
                shaking = false;
            }
        }

        /* ================= 心数计算 ================= */
        int fullHearts = lives / 10;
        int remainder = lives % 10;
        boolean hasHalf = remainder > 0 && remainder <= 5;
        boolean hasExtraFull = remainder > 5;

        int totalHearts = fullHearts
                + (hasHalf ? 1 : 0)
                + (hasExtraFull ? 1 : 0);

        totalHearts = Math.min(totalHearts, MAX_HEARTS_DISPLAY);

        /* ================= 布局 ================= */
        int startX = 20;
        int startY = Gdx.graphics.getHeight() - 90;

        float shakeOffsetX =
                shaking ? (float) Math.sin(shakeTimer * 40f) * SHAKE_AMPLITUDE : 0f;

        int drawn = 0;

        /* ================= 画满心 ================= */
        for (int i = 0; i < fullHearts && drawn < totalHearts; i++) {
            int row = drawn / HEARTS_PER_ROW;
            int col = drawn % HEARTS_PER_ROW;

            boolean shakeThis =
                    shaking && i == fullHearts - 1 && !hasExtraFull;

            uiBatch.draw(
                    heartFull,
                    startX + col * HEART_SPACING + (shakeThis ? shakeOffsetX : 0f),
                    startY - row * ROW_SPACING
            );
            drawn++;
        }

        /* ================= 半心 ================= */
        if (hasHalf && drawn < totalHearts) {
            int row = drawn / HEARTS_PER_ROW;
            int col = drawn % HEARTS_PER_ROW;

            uiBatch.draw(
                    heartHalf,
                    startX + col * HEART_SPACING,
                    startY - row * ROW_SPACING
            );
            drawn++;
        }

        /* ================= 6–10 的补满心 ================= */
        if (hasExtraFull && drawn < totalHearts) {
            int row = drawn / HEARTS_PER_ROW;
            int col = drawn % HEARTS_PER_ROW;

            uiBatch.draw(
                    heartFull,
                    startX + col * HEART_SPACING,
                    startY - row * ROW_SPACING
            );
        }
    }

    /**
     * 渲染指南针（UI模式）
     */
    public void renderCompassAsUI(SpriteBatch uiBatch) {
        if (gameManager == null || gameManager.getCompass() == null) return;

        Compass compass = gameManager.getCompass();
        if (!compass.isActive()) return;

        uiBatch.setProjectionMatrix(
                new Matrix4().setToOrtho2D(
                        0, 0,
                        Gdx.graphics.getWidth(),
                        Gdx.graphics.getHeight()
                )
        );

        compass.drawAsUI(uiBatch);
    }

    public BitmapFont getFont() {
        return font;
    }

    public void dispose() {
        if (font != null) font.dispose();
        if (heartFull != null) heartFull.dispose();
        if (heartHalf != null) heartHalf.dispose();
        if (shapeRenderer != null) shapeRenderer.dispose();
        if (catAtlas != null) catAtlas.dispose();
        // 🔥 清理 Buff 图标
        if (iconAtk != null) iconAtk.dispose();
        if (iconRegen != null) iconRegen.dispose();
        if (iconMana != null) iconMana.dispose();
        if (manaBase != null) manaBase.dispose();
        if (manaFill != null) manaFill.dispose();
        if (manaGlow != null) manaGlow.dispose();
        Logger.debug("HUD disposed");
    }

    // 在 HUD 类成员变量区添加
    private java.util.List<ManaParticle> particles = new java.util.ArrayList<>();

    // 粒子辅助类
    private static class ManaParticle {
        float x, y, vx, vy, life;
        Color color;
    }
}