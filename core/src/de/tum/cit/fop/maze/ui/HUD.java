// HUD.java - 修复版本
package de.tum.cit.fop.maze.ui;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.*;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Matrix4;
import de.tum.cit.fop.maze.abilities.Ability;
import de.tum.cit.fop.maze.abilities.DashAbility;
import de.tum.cit.fop.maze.entities.Compass;
import de.tum.cit.fop.maze.entities.Player;
import de.tum.cit.fop.maze.game.GameConstants;
import de.tum.cit.fop.maze.game.GameManager;
import de.tum.cit.fop.maze.utils.Logger;
import de.tum.cit.fop.maze.utils.TextureManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static com.badlogic.gdx.graphics.GL20.GL_ONE_MINUS_SRC_ALPHA;
import static com.badlogic.gdx.graphics.GL20.GL_SRC_ALPHA;

public class HUD {

    // ===== Mana UI (P1 / P2) =====
    private Texture manaBaseP1;
    private Texture manaFillP1;
    private Texture manaGlowP1;

    private Texture manaBaseP2;
    private Texture manaFillP2;
    private Texture manaGlowP2;

    // 公共装饰（可以共用）



    private BitmapFont font;
    private GameManager gameManager;
    private TextureManager textureManager;
    // ❤ 生命值贴图
    private Texture heartFull;   // live_00
    private Texture heartHalf;   // live_01
    private static final int MAX_HEARTS_DISPLAY = 40; // 最多显示 50 颗
    private static final int HEARTS_PER_ROW = 20;     // 每行最多 10 颗
    private static final int HEART_SPACING = 70;      // 爱心之间的水平间距
    private static final int ROW_SPACING = 30;        // 行距
    // ===== Mana UI (image-based) =====
    private Texture manadeco_1;
    private Texture manadeco_2;
    private float manaGlowTime = 0f;
    // Mana special states
    private float manaFullPulse = 0f;
    private float manaLowAlert = 0f;

    // 尺寸
    private static final float MANA_BAR_WIDTH  = 220f;
    private static final float MANA_BAR_HEIGHT = 28f;

    // 位置（右下角，猫上方）
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
    // ===== 技能图标：近战 Melee =====
    private Texture meleeIcon;

    // ===== Mana UI =====
    private ShapeRenderer shapeRenderer;
    //粒子特效列表
    private final Map<Integer, List<ManaParticle>> manaParticlesMap = new HashMap<>();


    // UI 尺寸
    private static final int DASH_ICON_SIZE = 200;
    private static final int DASH_ICON_SPACING = 10;
    private static final int MELEE_ICON_SIZE = 160; // 👈 比 Dash 小一档（推荐 150~170）

    // ===== Dash UI 布局 =====

    private static final int DASH_UI_MARGIN_X = 20; // 距离左边
    private static final int DASH_UI_MARGIN_Y = 20; // 距离下边
    private static final int MELEE_UI_OFFSET_X = DASH_ICON_SIZE + 20;
    // 🔥 [Treasure] 新增：Buff 图标
    private Texture iconAtk;
    private Texture iconRegen;
    private Texture iconMana;


    // 进度条缓存 - 用于平滑动画
    private float currentManaPercent = 0f;
    private float targetManaPercent = 0f;

    public HUD(GameManager gameManager) {
        this.gameManager = gameManager;
        this.font = new BitmapFont();
        this.font.getData().setScale(1.2f);
        this.textureManager = TextureManager.getInstance();
        Logger.debug("HUD initialized with compass support");
        this.shapeRenderer = new ShapeRenderer();
        try {
            // P1
            manaBaseP1 = new Texture(Gdx.files.internal("HUD/manabar_base.png"));
            manaFillP1 = new Texture(Gdx.files.internal("HUD/manabar_1_fill.png"));
            manaGlowP1 = new Texture(Gdx.files.internal("HUD/manabar_1_glow.png"));

            // P2
            manaBaseP2 = new Texture(Gdx.files.internal("HUD/manabar_base.png"));
            manaFillP2 = new Texture(Gdx.files.internal("HUD/manabar_2_fill.png"));
            manaGlowP2 = new Texture(Gdx.files.internal("HUD/manabar_2_glow.png"));

            // 装饰
            manadeco_1 = new Texture(Gdx.files.internal("HUD/bar_star1.png"));
            manadeco_2 = new Texture(Gdx.files.internal("HUD/bar_star2.png"));
        } catch (Exception e) {
            Logger.error("Mana bar textures load failed: " + e.getMessage());
        }

        // 加载法力条纹理


        heartFull = new Texture(Gdx.files.internal("HUD/live_000.png"));
        heartHalf = new Texture(Gdx.files.internal("HUD/live_001.png"));

        dashIcon = new Texture(Gdx.files.internal("HUD/icon_dash.png"));
        meleeIcon = new Texture(Gdx.files.internal("HUD/icon_melee.png")); // ⭐ 近战图标
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
        // 🔥 [Treasure] 加载图标 (请确保文件名正确！)
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
            if (gameManager.isTwoPlayerMode()) {
                renderTwoPlayerHUD(uiBatch);
            } else {
                renderSinglePlayerHUD(uiBatch);
            }
        } catch (Exception e) {
            Logger.debug("HUD failed: " + e.getMessage());
        }
    }

    private void renderSinglePlayerHUD(SpriteBatch uiBatch) {

            try {
                var player = gameManager.getPlayer();
                if (player == null) return;

                float barWidth = Gdx.graphics.getWidth() * 0.66f;
                float x = (Gdx.graphics.getWidth() - barWidth) / 2f - 50;
                float y = 50;

                renderManaBarForPlayer(uiBatch, player, 0,x, y, barWidth);
                // 2. 生命值（❤显示）
                renderLivesAsHearts(uiBatch);

                // 3. 关卡信息
                font.setColor(Color.CYAN);
                font.draw(uiBatch, "start: " + gameManager.getCurrentLevel(),
                        20, Gdx.graphics.getHeight() - 120);

                // 4. 操作说明
                font.setColor(Color.WHITE);
                font.draw(uiBatch, "direction buttons to move，Shift to sprint",
                        20, Gdx.graphics.getHeight() - 160);

                // 5. 纹理模式提示
                TextureManager.TextureMode currentMode = textureManager.getCurrentMode();
                if (currentMode != TextureManager.TextureMode.COLOR) {
                    font.setColor(Color.GREEN);
                    font.draw(uiBatch, "mode: " + currentMode + " (F1-F4 to switch)",
                            Gdx.graphics.getWidth() - 250,
                            Gdx.graphics.getHeight() - 20);
                }
                renderCat(uiBatch);
                // 6. 指南针
                renderCompassAsUI(uiBatch);
                // 7. 技能图标
                renderDashIcon(uiBatch);
                renderMeleeIcon(uiBatch);

                // ============================================
                // 🔥 [Treasure] 左侧 Buff 状态栏 (图标 + 大字)
                // ============================================


                if (player != null) {
                    float startX = 20;
                    float startY = Gdx.graphics.getHeight() - 250;
                    float iconSize = 48; // 图标大小
                    float gap = 60;      // 行间距加大，防止挤在一起

                    // 1. 攻击 Buff (红色)
                    if (player.hasBuffAttack()) {
                        // 画图标
                        if (iconAtk != null) uiBatch.draw(iconAtk, startX, startY, iconSize, iconSize);

                        // 画文字 (字体放大)
                        font.getData().setScale(2.0f); // 🔥 字体放大到 2.0
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

                    // ⚠️ 还原字体设置 (非常重要，否则界面其他地方会乱)
                    font.setColor(Color.WHITE);
                    font.getData().setScale(1.2f); // 还原回默认大小

                    // ============================================
                    // 🔥 [Treasure] 屏幕中央飘字 (超大字体通知)
                    // ============================================
                    String msg = player.getNotificationMessage();
                    if (msg != null && !msg.isEmpty()) {
                        float w = Gdx.graphics.getWidth();
                        float h = Gdx.graphics.getHeight();

                        // 设置超大字体
                        font.getData().setScale(2.5f); // 🔥 2.5倍大小

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
                Logger.debug("HUD failed: " + e.getMessage());
            }





    }
    private void renderTwoPlayerHUD(SpriteBatch uiBatch) {
        var players = gameManager.getPlayers();
        if (players == null || players.isEmpty()) return;

        float barWidth = 500f;
        float marginX  = 40f;
        float marginY  = 30f;

        // P1 - 左下
        renderManaBarForPlayer(
                uiBatch,
                players.get(0),
                0,          // ⭐ P1
                marginX,
                marginY,
                barWidth
        );

        // P2 - 右下
        if (players.size() > 1) {
            float x2 = Gdx.graphics.getWidth() - barWidth - marginX;
            renderManaBarForPlayer(
                    uiBatch,
                    players.get(1),
                    1,      // ⭐ P2
                    x2,
                    marginY,
                    barWidth
            );
        }
    }



    private void drawSimplePlayerInfo(
            SpriteBatch batch,
            de.tum.cit.fop.maze.entities.Player player,
            float x,
            float y,
            String label
    ) {
        font.setColor(Color.WHITE);
        font.getData().setScale(1.2f);

        font.draw(batch, label, x, y);
        font.draw(batch, "HP: " + player.getLives(), x, y - 20);
        font.draw(batch, "MP: " + player.getMana(), x, y - 40);
    }
    private void renderManaBarForPlayer(
            SpriteBatch uiBatch,
            Player player,
            int playerId,     // ⭐ 新增
            float x,
            float y,
            float barWidth
    )
    {

        Logger.debug(
                "[ManaBar] enter | playerId=" + playerId +
                        " mana=" + player.getMana() +
                        " maxMana=" + player.getMaxMana()
        );

        Texture manaBase;
        Texture manaFill;
        Texture manaGlow;
        Texture manaDeco;
        // ⭐ 根据 playerId 选择贴图
        if (playerId == 0) {
            manaBase = manaBaseP1;
            manaFill = manaFillP1;
            manaGlow = manaGlowP1;
            manaDeco = manadeco_1;

        } else {
            manaBase = manaBaseP2;
            manaFill = manaFillP2;
            manaGlow = manaGlowP2;
            manaDeco = manadeco_2;
        }
        Logger.debug(
                "[ManaBar] select textures | playerId=" + playerId +
                        " base=" + (manaBase != null) +
                        " fill=" + (manaFill != null) +
                        " glow=" + (manaGlow != null) +
                        " deco=" + (manaDeco != null)
        );


        if (player == null || manaFill == null || manaBase == null) return;
        List<ManaParticle> particles =
                manaParticlesMap.computeIfAbsent(playerId, k -> new ArrayList<>());




        float maxMana = Math.max(1f, player.getMaxMana()); // ⭐ 关键
        float percent = Math.max(
                0f,
                Math.min(1f, player.getMana() / maxMana)
        );

        Logger.debug(
                "[ManaBar] percent | playerId=" + playerId +
                        " percent=" + percent +
                        " (mana=" + player.getMana() + "/" + maxMana + ")"
        );


        float barHeight = barWidth * (32f / 256f);

        float fillInsetLeft  = barWidth * 0.02f;
        float fillInsetRight = barWidth * 0.02f;

        float fillStartX = x + fillInsetLeft;
        float fillWidth  = barWidth - fillInsetLeft - fillInsetRight;

        // ✅ 屏幕上的“帽子宽度”（跟 barWidth 成比例）
        float capW = fillWidth * 0.06f;          // 你可以微调 0.05~0.08
        capW = Math.max(8f, capW);              // 防止太小

        // ✅ 贴图中用于裁剪的“帽子宽度”（贴图像素单位）
        int capSrcW = (int)(manaFill.getWidth() * 0.09f);

        // ✅ 中段可用宽度
        float liquidMaxW = Math.max(0f, fillWidth - capW * 2f);
        float liquidW    = liquidMaxW * percent;

        // --- 底座 ---
        uiBatch.setColor(1f, 1f, 1f, 1f);
        uiBatch.draw(manaBase, x, y, barWidth, barHeight);

        if (percent <= 0f) {
            uiBatch.setColor(1f, 1f, 1f, 1f);
            return;
        }

        // --- 左帽 ---
        uiBatch.draw(
                manaFill,
                fillStartX,
                y,
                capW,
                barHeight,
                0, 0,
                capSrcW,
                manaFill.getHeight(),
                false, false
        );

        // --- 中段 ---
        if (liquidW > 0f) {
            int midSrcX = capSrcW;
            int midSrcW = manaFill.getWidth() - capSrcW * 2;

            uiBatch.draw(
                    manaFill,
                    fillStartX + capW,
                    y,
                    liquidW,
                    barHeight,
                    midSrcX, 0,
                    midSrcW,
                    manaFill.getHeight(),
                    false, false
            );
        }

        // --- 右帽（只有 percent>0 才画）---
        uiBatch.draw(
                manaFill,
                fillStartX + capW + liquidW,
                y,
                capW,
                barHeight,
                manaFill.getWidth() - capSrcW,
                0,
                capSrcW,
                manaFill.getHeight(),
                false, false
        );

        // === 特效（用 fillWidth / percent，不要用 capWidth 原来的像素）===
        renderManaGlowEffect(uiBatch,  manaGlow, fillStartX, y, fillWidth, barHeight, percent);
        updateAndRenderLongTrail(
                uiBatch,
                manaGlow,      // ⭐ 同一个 manaGlow
                particles,
                playerId,
                fillStartX,
                y,
                fillWidth,
                barHeight,
                percent
        );



        // =========================
// 🔥 装饰层：永远绘制
// =========================
        if (manaDeco != null) {
            float decoWidth = barWidth * 0.12f;

            float startCenterX = x + barWidth * 0.10f;
            float endCenterX   = x + barWidth * 0.87f;

            float t = Math.max(0f, Math.min(1f, percent));
            float decoCenterX = startCenterX + (endCenterX - startCenterX) * t;
            float decoX = decoCenterX - decoWidth * 0.5f;
            uiBatch.setBlendFunction(
                    GL_SRC_ALPHA,
                    GL_ONE_MINUS_SRC_ALPHA
            );
            uiBatch.setColor(1f, 1f, 1f, 1f);
            uiBatch.draw(manaDeco, decoX, y, decoWidth, barHeight);
        }


        uiBatch.setColor(1f, 1f, 1f, 1f);
    }








    /**
     * 负责管内液体的立体感呼吸光
     */

    private void renderManaGlowEffect(
            SpriteBatch uiBatch,
            Texture manaGlow,   // ⭐ 新增
            float fillStartX,
            float y,
            float fillWidth,
            float h,
            float percent
    ){
        if (manaGlow == null || percent <= 0f) return;

        manaGlowTime += Gdx.graphics.getDeltaTime();

        float glowAlpha = 0.4f + 0.3f * (float)Math.sin(manaGlowTime * 3.0f);

        uiBatch.setBlendFunction(
                GL_SRC_ALPHA,
                com.badlogic.gdx.graphics.GL20.GL_ONE
        );
        uiBatch.setColor(1f, 0.8f, 0.95f, glowAlpha);

        int srcW = (int)(manaGlow.getWidth() * percent);
        if (srcW > 0) {
            TextureRegion glowRegion =
                    new TextureRegion(manaGlow, 0, 0, srcW, manaGlow.getHeight());

            uiBatch.draw(
                    glowRegion,
                    fillStartX,
                    y + h * 0.15f,
                    fillWidth * percent,
                    h * 0.7f
            );
        }

        uiBatch.setBlendFunction(
                GL_SRC_ALPHA,
                GL_ONE_MINUS_SRC_ALPHA
        );
        uiBatch.setColor(1f, 1f, 1f, 1f);
    }


    /**
     * 负责末端的喷射和超长拖尾
     */
    private void updateAndRenderLongTrail(
            SpriteBatch uiBatch,
            Texture manaGlow,        // ⭐ 加这一行
            List<ManaParticle> particles,
            int playerId,
            float fillStartX,
            float y,
            float fillWidth,
            float h,
            float percent
    )
    {
        // 🔒 只有满蓝才显示拖尾
        if (percent < 0.999f) {
            particles.clear();   // 防止拖尾残影
            return;
        }
        if (manaGlow == null) return;

        float endX = fillStartX + fillWidth * percent;
        float delta = Gdx.graphics.getDeltaTime();

        float centerOffset = h / 3f;
        float activeHeight = h * (2f / 3f);

        // === 粒子生成 ===
        for (int i = 0; i < 6; i++) {
            if (particles.size() < 150) {
                ManaParticle p = new ManaParticle();
                p.x = endX;
                p.y = y + centerOffset + (float)(Math.random() * activeHeight);

                p.vx = (float)(Math.random() * -300 - 150);
                p.vy = (float)(Math.random() * 40 - 20);
                p.life = 1.2f + (float)Math.random() * 0.8f;

                p.color = (playerId == 0)
                        ? new Color(1.0f, 0.85f, 0.3f, 1f)   // P1 金色
                        : new Color(0.3f, 0.8f, 1.0f, 1f);   // P2 蓝色


                particles.add(p);
            }
        }

        // === 粒子渲染 ===
        uiBatch.setBlendFunction(
                GL_SRC_ALPHA,
                com.badlogic.gdx.graphics.GL20.GL_ONE
        );

        for (int i = particles.size() - 1; i >= 0; i--) {
            ManaParticle p = particles.get(i);
            p.life -= delta;

            // ⭐ 统一使用 fillStartX 作为消失边界
            if (p.life <= 0 || p.x < fillStartX) {
                particles.remove(i);
                continue;
            }

            p.x += p.vx * delta;
            p.y += p.vy * delta;
            p.vx *= 0.97f;

            float size = 14f * (p.life / 2.0f);
            uiBatch.setColor(p.color.r, p.color.g, p.color.b, p.life * 0.7f);
            uiBatch.draw(manaGlow, p.x - size / 2, p.y - size / 2, size, size);
        }

        uiBatch.setBlendFunction(
                GL_SRC_ALPHA,
                GL_ONE_MINUS_SRC_ALPHA
        );
    }


    private void renderDashIcon(SpriteBatch uiBatch) {
        if (dashIcon == null) return;

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
            // 满层：金光闪闪 (亮黄色 + 稍微一点点橘)
            uiBatch.setColor(1.0f, 0.9f, 0.8f, 1f);
        } else if (dashCharges == 1) {
            // 一层：暗金色
            uiBatch.setColor(0.8f, 0.9f, 0.8f, 1f);
        } else {
            // 0层：废旧金属色 (暗灰带点棕)
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
            uiBatch.setColor(1f, 1f, 1f, 1f); // 还原 Batch 颜色
        }

        // --- 3. 层数文字提示 ---
        // 在图标旁边或者角落画一个小数字，更直观
        font.getData().setScale(1.5f);
        font.setColor(dashCharges > 0 ? Color.WHITE : Color.GRAY);
        font.draw(uiBatch, "x" + dashCharges, x + DASH_ICON_SIZE - 30, y + 40);

        // 还原颜色
        uiBatch.setColor(1f, 1f, 1f, 1f);
        font.getData().setScale(1.2f);
    }

    private void renderMeleeIcon(SpriteBatch uiBatch) {
        if (meleeIcon == null) return;
        if (gameManager == null || gameManager.getPlayer() == null) return;

        // 找到近战技能
        de.tum.cit.fop.maze.abilities.MeleeAttackAbility melee = null;
        for (Ability a : gameManager.getPlayer().getAbilityManager().getAbilities().values()) {
            if (a instanceof de.tum.cit.fop.maze.abilities.MeleeAttackAbility m) {
                melee = m;
                break;
            }
        }
        if (melee == null) return;

        float progress = melee.getCooldownProgress(); // 0~1
        boolean actuallyOnCooldown = progress > 0f && progress < 1f;

        float x = DASH_UI_MARGIN_X + MELEE_UI_OFFSET_X;
        float y = DASH_UI_MARGIN_Y + (DASH_ICON_SIZE - MELEE_ICON_SIZE) / 2f;


        // === 1. 状态颜色 ===
        if (!actuallyOnCooldown) {
            // ✅ 初始状态 or 冷却完成：正常亮
            uiBatch.setColor(1f, 1f, 1f, 1f);
        } else if (progress > 0.85f) {
            // 🔥 快好了
            uiBatch.setColor(1f, 0.6f, 0.6f, 1f);
        } else {
            // ⏳ 冷却中
            uiBatch.setColor(0.35f, 0.35f, 0.35f, 0.85f);
        }

        uiBatch.draw(meleeIcon, x, y, MELEE_ICON_SIZE, MELEE_ICON_SIZE);

        uiBatch.setColor(1f, 1f, 1f, 1f);
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

        /* ================= 心数计算（你的规则） ================= */
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

    /**
     * 渲染游戏结束画面
     */
    public void renderGameComplete(SpriteBatch batch) {
        String message = "恭喜！你成功逃出了迷宫！";
        font.getData().setScale(2);
        font.setColor(Color.GREEN);

        com.badlogic.gdx.graphics.g2d.GlyphLayout layout = new com.badlogic.gdx.graphics.g2d.GlyphLayout();
        layout.setText(font, message);

        float x = (Gdx.graphics.getWidth() - layout.width) / 2;
        float y = Gdx.graphics.getHeight() / 2;

        font.draw(batch, message, x, y);

        // 显示重新开始提示
        font.getData().setScale(1);
        font.setColor(Color.WHITE);
        String restartMsg = "按R键重新开始游戏";
        layout.setText(font, restartMsg);

        float restartX = (Gdx.graphics.getWidth() - layout.width) / 2;
        font.draw(batch, restartMsg, restartX, y - 50);
    }

    /**
     * 渲染游戏结束画面
     */
    public void renderGameOver(SpriteBatch batch) {
        String message = "游戏结束！";
        font.getData().setScale(2);
        font.setColor(Color.RED);

        com.badlogic.gdx.graphics.g2d.GlyphLayout layout = new com.badlogic.gdx.graphics.g2d.GlyphLayout();
        layout.setText(font, message);

        float x = (Gdx.graphics.getWidth() - layout.width) / 2;
        float y = Gdx.graphics.getHeight() / 2;

        font.draw(batch, message, x, y);

        // 显示重新开始提示
        font.getData().setScale(1);
        font.setColor(Color.WHITE);
        String restartMsg = "按R键重新开始游戏";
        layout.setText(font, restartMsg);

        float restartX = (Gdx.graphics.getWidth() - layout.width) / 2;
        font.draw(batch, restartMsg, restartX, y - 50);
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
        if (manadeco_1 != null) manadeco_1.dispose();
        if (manadeco_2 != null) manadeco_2.dispose();
        if (dashIcon != null) dashIcon.dispose();
        if (meleeIcon != null) meleeIcon.dispose();
        if (manaBaseP1 != null) manaBaseP1.dispose();
        if (manaFillP1 != null) manaFillP1.dispose();
        if (manaGlowP1 != null) manaGlowP1.dispose();

        if (manaBaseP2 != null) manaBaseP2.dispose();
        if (manaFillP2 != null) manaFillP2.dispose();
        if (manaGlowP2 != null) manaGlowP2.dispose();

        Logger.debug("HUD disposed");
    }

    // 粒子辅助类
    private static class ManaParticle {
        float x, y, vx, vy, life;
        Color color;
    }
}