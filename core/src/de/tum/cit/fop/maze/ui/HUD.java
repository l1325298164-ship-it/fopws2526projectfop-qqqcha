package de.tum.cit.fop.maze.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.*;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Matrix4;

import com.badlogic.gdx.utils.TimeUtils;
import de.tum.cit.fop.maze.abilities.*;
import de.tum.cit.fop.maze.entities.Compass;
import de.tum.cit.fop.maze.entities.Player;
import de.tum.cit.fop.maze.game.GameConstants;
import de.tum.cit.fop.maze.game.GameManager;
import de.tum.cit.fop.maze.game.achievement.*;
import de.tum.cit.fop.maze.game.score.UpgradeCost;
import de.tum.cit.fop.maze.utils.Logger;
import de.tum.cit.fop.maze.utils.TextureManager;

import java.util.*;

import static com.badlogic.gdx.graphics.GL20.*;

public class HUD {

    // ===== HUD 布局模式 =====
    private enum HUDLayoutMode {
        SINGLE,
        TWO_PLAYER
    }

    private BitmapFont font;
    private final GameManager gameManager;
    private final TextureManager textureManager;

    // ===== 成就弹窗 =====
    private AchievementPopup achievementPopup;

    // ===== Shader =====
    private ShaderProgram iceHeartShader;

    // ===== 生命值 =====
    private Texture heartFull;
    private Texture heartHalf;

    private static final int MAX_HEARTS_DISPLAY = 40;
    private static final int HEARTS_PER_ROW = 5;
    private static final int HEART_SPACING = 70;
    private static final int ROW_SPACING = 30;

    // 抖动
    private static final float SHAKE_DURATION = 0.2f;
    private static final float SHAKE_AMPLITUDE = 4f;

    private final Map<Player.PlayerIndex, Integer> lastLivesMap = new HashMap<>();
    private final Map<Player.PlayerIndex, Boolean> shakingMap = new HashMap<>();
    private final Map<Player.PlayerIndex, Float> shakeTimerMap = new HashMap<>();
    // ===== Mana UI =====
    private Texture manaBaseP1;
    private Texture manaFillP1;
    private Texture manaGlowP1;

    private Texture manaBaseP2;
    private Texture manaFillP2;
    private Texture manaGlowP2;

    private Texture manadeco_1;
    private Texture manadeco_2;

    private float manaGlowTime = 0f;


    // 粒子
    private TextureAtlas sparkleAtlas;
    private TextureRegion sparkleStar;
    private TextureRegion sparkleFlower;

    private static final int MAX_PARTICLES = 150;
    private final Map<Integer, List<ManaParticle>> manaParticlesMap = new HashMap<>();

    // ===== Dash / Melee / Magic =====
    private Texture dashIconP1;
    private Texture dashIconP2;
    private Texture dashIcon;

    private Texture meleeIcon;

    private Texture magicBg;
    private Texture magicGrow;
    private Texture magicIconTop;

    private static final int DASH_ICON_SIZE = 200;
    private static final int MELEE_ICON_SIZE = 160;
    private static final int DASH_UI_MARGIN_X = 20;
    private static final int DASH_UI_MARGIN_Y = 90;
    private static final int MELEE_UI_OFFSET_X = DASH_ICON_SIZE + 20;

    // ===== Buff Icons =====
    private Texture iconAtk;
    private Texture iconRegen;
    private Texture iconMana;

    // ===== Cat HUD =====
    private TextureAtlas catAtlas;
    private Animation<TextureRegion> catNoKeyAnim;
    private Animation<TextureRegion> catHasKeyAnim;
    private float catStateTime = 0f;

    private static final float CAT_SIZE = 506f;
    private static final float CAT_MARGIN = 10f;
    private static final float CAT_COMPASS_GAP = 40f;
    private static final float CAT_Y_OFFSET = -150f;
    private static final float COMPASS_Y_OFFSET = 650f;

    // ===== Shape =====
    private ShapeRenderer shapeRenderer;

    // =========================================================

    public HUD(GameManager gameManager) {
        this.gameManager = gameManager;
        this.textureManager = TextureManager.getInstance();

        this.font = new BitmapFont();
        this.font.getData().setScale(1.2f);

        this.shapeRenderer = new ShapeRenderer();


        achievementPopup = new AchievementPopup(font);

        // Shader
        String vertexSrc = SpriteBatch.createDefaultShader().getVertexShaderSource();
        String fragmentSrc = Gdx.files.internal("shaders/ice_heart.frag").readString();
        iceHeartShader = new ShaderProgram(vertexSrc, fragmentSrc);
        if (!iceHeartShader.isCompiled()) {
            Logger.error("IceHeartShader compile error:\n" + iceHeartShader.getLog());
        }

        // 粒子
        sparkleAtlas = new TextureAtlas(Gdx.files.internal("effects/sparkle.atlas"));
        sparkleStar = sparkleAtlas.findRegion("star");
        sparkleFlower = sparkleAtlas.findRegion("flower");

        // Mana
        manaBaseP1 = new Texture(Gdx.files.internal("HUD/manabar_base.png"));
        manaBaseP2 = manaBaseP1;

        manaFillP1 = new Texture(Gdx.files.internal("HUD/manabar_1_fill.png"));
        manaGlowP1 = new Texture(Gdx.files.internal("HUD/manabar_1_grow.png"));
        manadeco_1 = new Texture(Gdx.files.internal("HUD/bar_star1.png"));

        manaFillP2 = new Texture(Gdx.files.internal("HUD/manabar_2_fill.png"));
        manaGlowP2 = new Texture(Gdx.files.internal("HUD/manabar_2_grow.png"));
        manadeco_2 = new Texture(Gdx.files.internal("HUD/bar_star2.png"));

        // Hearts
        heartFull = new Texture(Gdx.files.internal("HUD/live_000.png"));
        heartHalf = new Texture(Gdx.files.internal("HUD/live_001.png"));

        // Dash / Melee / Magic
        dashIconP1 = new Texture(Gdx.files.internal("HUD/icon_dash.png"));
        dashIconP2 = new Texture(Gdx.files.internal("HUD/icon_dash_2.png"));

        meleeIcon = new Texture(Gdx.files.internal("HUD/icon_melee.png"));
        magicBg = new Texture(Gdx.files.internal("HUD/magicicon_bg.png"));
        magicGrow = new Texture(Gdx.files.internal("HUD/magicicon_grow.png"));
        magicIconTop = new Texture(Gdx.files.internal("HUD/icon_magic_base.png"));

        // Buff
        iconAtk = new Texture(Gdx.files.internal("Items/icon_atk.png"));
        iconRegen = new Texture(Gdx.files.internal("Items/icon_regen.png"));
        iconMana = new Texture(Gdx.files.internal("Items/icon_mana.png"));

        // Cat
        catAtlas = new TextureAtlas(Gdx.files.internal("Character/cat/cat.atlas"));
        catNoKeyAnim = new Animation<>(0.25f, catAtlas.findRegions("cat_nokey"), Animation.PlayMode.LOOP);
        catHasKeyAnim = new Animation<>(0.25f, catAtlas.findRegions("cat_key"), Animation.PlayMode.LOOP);

        Logger.debug("HUD initialized (Part 1)");
    }

    // =========================================================

    public void renderInGameUI(SpriteBatch uiBatch) {
        try {
            if (gameManager.isTwoPlayerMode()) {
                renderTwoPlayerHUD(uiBatch);
            } else {
                renderSinglePlayerHUD(uiBatch);
            }
            // 🔥 修复：将分数渲染移到这里，确保单人/双人都能显示，且根据模式自动调整位置
            renderScore(uiBatch);

            renderBottomCenterHUD(uiBatch);
        } catch (Exception e) {
            Logger.error("HUD render failed"+e);
        }
    }

    private void renderSinglePlayerHUD(SpriteBatch uiBatch) {
        var player = gameManager.getPlayer();
        if (player == null) return;

        float barWidth = Gdx.graphics.getWidth() * 0.66f;
        float x = (Gdx.graphics.getWidth() - barWidth) / 2f - 50;
        float y = 50;

        renderManaBarForPlayer(uiBatch, player, 0,x, y, barWidth);

        renderLivesAsHearts(
                uiBatch,
                player,
                20,
                Gdx.graphics.getHeight() - 90,
                false
        );
        //关卡信息TODO
        font.setColor(Color.CYAN);
        font.draw(uiBatch, "start: " + gameManager.getCurrentLevel(),
                20, Gdx.graphics.getHeight() - 120);

        renderCat(uiBatch);
        renderCompassAsUI(uiBatch);
        renderDashIcon(uiBatch, player, false);
        renderMeleeIcon(uiBatch, player, false);
        renderAchievementPopup(uiBatch);

        float startX = 20;
        float startY = Gdx.graphics.getHeight() - 250;
        float iconSize = 48; // 图标大小
        float gap = 60;      // 行间距加大，防止挤在一起

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


    private void renderTwoPlayerHUD(SpriteBatch uiBatch) {
        var players = gameManager.getPlayers();
        if (players == null || players.isEmpty()) return;

        // ===== Mana Bar =====
        float barWidth = 500f;
        float marginX  = 40f;
        float marginY  = 30f;
        renderReviveProgressBar(uiBatch);
        // P1 - 左下
        renderManaBarForPlayer(
                uiBatch,
                players.get(0),
                0,
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
                    1,
                    x2,
                    marginY,
                    barWidth
            );
        }

        // ===== 成就弹窗 =====
        renderAchievementPopup(uiBatch);

        int topY = Gdx.graphics.getHeight() - 90;

        // ❤️ P1：左上
        renderLivesAsHearts(
                uiBatch,
                players.get(0),
                20,
                topY,
                false
        );

        // ❤️ P2：右上（镜面）
        if (players.size() > 1) {
            int rightStartX =
                    Gdx.graphics.getWidth()
                            - 20
                            - heartFull.getWidth();

            renderLivesAsHearts(
                    uiBatch,
                    players.get(1),
                    rightStartX,
                    topY,
                    true
            );
        }

        // ===== 技能图标 =====
        // P1
        renderDashIcon(uiBatch, players.get(0), false);
        renderMeleeIcon(uiBatch, players.get(0), false);

        // P2
        if (players.size() > 1) {
            renderDashIcon(uiBatch, players.get(1), true);
            renderMagicIcon(uiBatch, players.get(1), true);
        }
    }

    private void renderDashIcon(
            SpriteBatch uiBatch,
            Player player,
            boolean mirror
    ) {
        if (player == null) return;

        DashAbility dash = null;
        for (Ability a : player.getAbilityManager().getAbilities().values()) {
            if (a instanceof DashAbility d) {
                dash = d;
                break;
            }
        }
        if (dash == null) return;

        Texture icon =
                player.getPlayerIndex() == Player.PlayerIndex.P1
                        ? dashIconP1
                        : dashIconP2;

        if (icon == null) return;

        int dashCharges = dash.getCurrentCharges();
        float progress = dash.getCooldownProgress();

        float x = mirror
                ? Gdx.graphics.getWidth() - DASH_ICON_SIZE - DASH_UI_MARGIN_X
                : DASH_UI_MARGIN_X;
        float y = DASH_UI_MARGIN_Y;

        if (dashCharges >= 2) {
            uiBatch.setColor(1.0f, 0.9f, 0.8f, 1f);
        } else if (dashCharges == 1) {
            uiBatch.setColor(0.8f, 0.9f, 0.8f, 1f);
        } else {
            uiBatch.setColor(0.25f, 0.25f, 0.2f, 0.8f);
        }

        uiBatch.draw(icon, x, y, DASH_ICON_SIZE, DASH_ICON_SIZE);
        renderAbilityLevel(uiBatch, dash, x, y, DASH_ICON_SIZE);
        renderUpgradeButton(
                uiBatch,
                player,
                dash,
                x,
                y,
                DASH_ICON_SIZE
        );

        if (dashCharges < 2) {
            float maskHeight = DASH_ICON_SIZE * (1f - progress);
            uiBatch.setColor(0f, 0f, 0f, 0.5f);
            uiBatch.draw(
                    TextureManager.getInstance().getWhitePixel(),
                    x, y,
                    DASH_ICON_SIZE,
                    maskHeight
            );
        }

        uiBatch.setColor(1f, 1f, 1f, 1f);
    }
    private void renderMeleeIcon(
            SpriteBatch uiBatch,
            Player player,
            boolean mirror
    ) {
        if (meleeIcon == null || player == null) return;

        de.tum.cit.fop.maze.abilities.MeleeAttackAbility melee = null;
        for (Ability a : player.getAbilityManager().getAbilities().values()) {
            if (a instanceof de.tum.cit.fop.maze.abilities.MeleeAttackAbility m) {
                melee = m;
                break;
            }
        }
        if (melee == null) return;

        float progress = melee.getCooldownProgress();
        boolean onCooldown = progress > 0f && progress < 1f;

        float size = MELEE_ICON_SIZE;
        float dashX = getIconX(DASH_ICON_SIZE, mirror);

        float x = mirror
                ? dashX - MELEE_UI_OFFSET_X
                : dashX + MELEE_UI_OFFSET_X;

        float y = DASH_UI_MARGIN_Y + (DASH_ICON_SIZE - size) / 2f;

        if (onCooldown && iceHeartShader != null) {
            uiBatch.setShader(iceHeartShader);
            iceHeartShader.setUniformf("u_intensity", 0.0f);
            iceHeartShader.setUniformf("u_cooldown", progress);
            iceHeartShader.setUniformf("u_cdDarkness", 0.7f);
        }

        uiBatch.draw(meleeIcon, x, y, size, size);
        renderAbilityLevel(uiBatch, melee, x, y, size);
        renderUpgradeButton(
                uiBatch,
                player,
                melee,
                x,
                y,
                size
        );

        if (onCooldown) {
            uiBatch.setShader(null);
        }
    }
    private void renderMagicIcon(
            SpriteBatch batch,
            Player player,
            boolean mirror
    ) {
        if (player == null) return;

        MagicAbility magic = null;
        for (Ability a : player.getAbilityManager().getAbilities().values()) {
            if (a instanceof MagicAbility m) {
                magic = m;
                break;
            }
        }
        if (magic == null) return;

        MagicAbility.Phase phase = magic.getPhase();
        float time = magic.getPhaseTime();

        float baseSize = MELEE_ICON_SIZE;
        float size = mirror ? baseSize * 1.15f : baseSize;

        float baseX = getIconX(DASH_ICON_SIZE, mirror);
        float x = mirror
                ? baseX - MELEE_UI_OFFSET_X
                : baseX + MELEE_UI_OFFSET_X;
        float y = DASH_UI_MARGIN_Y + (DASH_ICON_SIZE - size) / 2f;

        if (phase != MagicAbility.Phase.IDLE) {


            float originX = size * 0.5f;
            float originY = size * 0.62f;

            float rotation =
                    phase == MagicAbility.Phase.AIMING
                            ? time * 720f
                            : 0f;

            if (phase == MagicAbility.Phase.COOLDOWN) {
                batch.setColor(0.35f, 0.35f, 0.35f, 1f);
            } else {
                batch.setColor(1f, 1f, 1f, 1f);
            }

            batch.draw(
                    magicBg,
                    x, y,
                    originX, originY,
                    size, size,
                    1f, 1f,
                    rotation,
                    0, 0,
                    magicBg.getWidth(),
                    magicBg.getHeight(),
                    false, false
            );
            renderAbilityLevel(batch, magic, x, y, size);
            renderUpgradeButton(
                    batch,
                    player,
                    magic,
                    x,
                    y,
                    size
            );

        }

        // ================= Grow（呼吸光） =================
        if (phase != MagicAbility.Phase.IDLE
                && phase != MagicAbility.Phase.COOLDOWN) {

            float pulse = 0.6f + 0.4f * (float)Math.sin(time * 6.5f);

            Color glow;
            switch (phase) {
                case AIMING, EXECUTED -> glow = new Color(0.9f, 0.2f, 0.9f, pulse); // 紫红
                default -> glow = Color.WHITE;
            }

            batch.setBlendFunction(GL_SRC_ALPHA, GL_ONE);
            batch.setColor(glow);
            batch.draw(magicGrow, x, y, size, size);
            batch.setBlendFunction(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        }

        // ================= 顶层 icon =================
        if (phase == MagicAbility.Phase.COOLDOWN) {
            batch.setColor(0.35f, 0.35f, 0.35f, 1f);
        } else {
            batch.setColor(1f, 1f, 1f, 1f);
        }

        batch.draw(magicIconTop, x, y, size, size);
        batch.setColor(1f, 1f, 1f, 1f);
    }


    // =========================================================
    // 粒子结构
    private static class ManaParticle {
        float x, y, vx, vy, life;
        Color color;
    }

    // =========================================================
    // Achievement Popup
    private void renderAchievementPopup(SpriteBatch uiBatch) {
        if (!achievementPopup.isBusy()) {
            AchievementManager am = gameManager.getAchievementManager();
            if (am != null) {
                AchievementType next = am.pollNotification();
                if (next != null) {
                    achievementPopup.show(next);
                }
            }
        }
        achievementPopup.render(uiBatch);
    }

    // =========================================================
    // Score (修复后)
    private void renderScore(SpriteBatch uiBatch) {
        int score = gameManager.getScore();
        String text = "SCORE: " + formatScore(score);

        font.getData().setScale(1.5f);
        GlyphLayout layout = new GlyphLayout(font, text);

        float x;
        // 🔥 如果是双人模式，分数居中显示；否则在右上角
        if (gameManager.isTwoPlayerMode()) {
            x = (Gdx.graphics.getWidth() - layout.width) / 2f;
        } else {
            x = Gdx.graphics.getWidth() - layout.width - 30;
        }

        float y = Gdx.graphics.getHeight() - 60;

        // 阴影
        font.setColor(0f, 0f, 0f, 0.7f);
        font.draw(uiBatch, text, x + 2, y - 2);

        // 正文
        font.setColor(Color.GOLD);
        font.draw(uiBatch, text, x, y);

        // 还原
        font.setColor(Color.WHITE);
        font.getData().setScale(1.2f);
    }

    // =========================================================
    // Mana Bar
    private void renderManaBarForPlayer(
            SpriteBatch uiBatch,
            Player player,
            int playerId,
            float x,
            float y,
            float barWidth
    ) {
        Texture manaBase = (playerId == 0) ? manaBaseP1 : manaBaseP2;
        Texture manaFill = (playerId == 0) ? manaFillP1 : manaFillP2;
        Texture manaGlow = (playerId == 0) ? manaGlowP1 : manaGlowP2;
        Texture manaDeco = (playerId == 0) ? manadeco_1 : manadeco_2;

        if (player == null || manaFill == null || manaBase == null) return;
        List<ManaParticle> particles =
                manaParticlesMap.computeIfAbsent(playerId, k -> new ArrayList<>());




        float maxMana = Math.max(1f, player.getMaxMana());
        float percent = Math.max(
                0f,
                Math.min(1f, player.getMana() / maxMana)
        );

        // barHeight 计算
        float barHeight = barWidth * (32f / 256f);

        float fillInsetLeft  = barWidth * 0.02f;
        float fillInsetRight = barWidth * 0.02f;

        float fillStartX = x + fillInsetLeft;
        float fillWidth  = barWidth - fillInsetLeft - fillInsetRight;

        float capW = fillWidth * 0.06f;
        capW = Math.max(8f, capW);

        int capSrcW = (int)(manaFill.getWidth() * 0.09f);

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

        // --- 右帽 ---
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

        renderManaGlowEffect(uiBatch,  manaGlow, fillStartX, y, fillWidth, barHeight, percent);
        updateAndRenderLongTrail(
                uiBatch,
                manaGlow,
                particles,
                playerId,
                fillStartX,
                y,
                fillWidth,
                barHeight,
                percent
        );

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


    private void renderManaGlowEffect(
            SpriteBatch uiBatch,
            Texture manaGlow,
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
                GL_ONE
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

    private void updateAndRenderLongTrail(
            SpriteBatch uiBatch,
            Texture manaGlow,
            List<ManaParticle> particles,
            int playerId,
            float fillStartX,
            float y,
            float fillWidth,
            float h,
            float percent
    )
    {
        if (percent < 0.999f) {
            particles.clear();
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
        uiBatch.setBlendFunction(GL_SRC_ALPHA, GL_ONE);

        for (int i = particles.size() - 1; i >= 0; i--) {
            ManaParticle p = particles.get(i);
            p.life -= delta;

            if (p.life <= 0 || p.x < fillStartX) {
                particles.remove(i);
                continue;
            }

            p.x += p.vx * delta;
            p.y += p.vy * delta;
            p.vx *= 0.97f;

            float size = 14f * (p.life / 2.0f);
            uiBatch.setColor(p.color.r, p.color.g, p.color.b, p.life * 0.7f);
            TextureRegion particleRegion =
                    (playerId == 0) ? sparkleStar : sparkleFlower;

            uiBatch.draw(
                    particleRegion,
                    p.x - size / 2f,
                    p.y - size / 2f,
                    size,
                    size
            );

        }

        uiBatch.setBlendFunction(
                GL_SRC_ALPHA,
                GL_ONE_MINUS_SRC_ALPHA
        );
    }

    // =========================================================
    // Hearts
    private void renderLivesAsHearts(
            SpriteBatch uiBatch,
            Player player,
            int startX,
            int startY,
            boolean mirror
    ) {
        if (player == null) return;

        Player.PlayerIndex idx = player.getPlayerIndex();
        boolean useIceShader = mirror && iceHeartShader != null;
        int lastLives = lastLivesMap.getOrDefault(idx, -1);
        boolean shaking = shakingMap.getOrDefault(idx, false);
        float shakeTimer = shakeTimerMap.getOrDefault(idx, 0f);

        if (mirror) {
            uiBatch.setShader(iceHeartShader);

            iceHeartShader.setUniformf(
                    "u_tintColor",
                    0.5f, 0.8f, 1.0f     // 冰蓝
            );
            iceHeartShader.setUniformf(
                    "u_intensity",
                    1.0f                // 1 = 完全冰化
            );
            iceHeartShader.setUniformf("u_cooldown", -1.0f);
        }
        uiBatch.setColor(1f, 1f, 1f, 1f);


        int lives = player.getLives();

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


        float shakeOffsetX =
                shaking ? (float) Math.sin(shakeTimer * 40f) * SHAKE_AMPLITUDE : 0f;

        int drawn = 0;

        /* ================= 画满心 ================= */
        for (int i = 0; i < fullHearts && drawn < totalHearts; i++) {
            int row = drawn / HEARTS_PER_ROW;
            int col = drawn % HEARTS_PER_ROW;

            boolean shakeThis =
                    shaking && i == fullHearts - 1 && !hasExtraFull;
            float x =
                    mirror
                            ? startX - col * HEART_SPACING   // 右 → 左
                            : startX + col * HEART_SPACING;  // 左 → 右
            uiBatch.draw(
                    heartFull,
                    x,
                    startY - row * ROW_SPACING
            );
            drawn++;
        }

        /* ================= 半心 ================= */
        if (hasHalf && drawn < totalHearts) {
            int row = drawn / HEARTS_PER_ROW;
            int col = drawn % HEARTS_PER_ROW;
            float x =
                    mirror
                            ? startX - col * HEART_SPACING   // 右 → 左
                            : startX + col * HEART_SPACING;
            uiBatch.draw(
                    heartHalf,
                    x,
                    startY - row * ROW_SPACING
            );
            drawn++;
        }

        /* ================= 6–10 的补满心 ================= */
        if (hasExtraFull && drawn < totalHearts) {
            int row = drawn / HEARTS_PER_ROW;
            int col = drawn % HEARTS_PER_ROW;
            float x =
                    mirror
                            ? startX - col * HEART_SPACING
                            : startX + col * HEART_SPACING;

            uiBatch.draw(
                    heartFull,
                    x,
                    startY - row * ROW_SPACING
            );
        }
        // ===== 关键：还原 Shader =====
        if (useIceShader) {
            uiBatch.setShader(null);
        }
        uiBatch.setColor(1f, 1f, 1f, 1f);

        lastLivesMap.put(idx, lives);
        shakingMap.put(idx, shaking);
        shakeTimerMap.put(idx, shakeTimer);
    }

    // =========================================================
    // Cat / Compass
    private void renderCat(SpriteBatch uiBatch) {
        if (gameManager.getPlayer() == null) return;

        catStateTime += Gdx.graphics.getDeltaTime();
        boolean hasKey = gameManager.getPlayer().hasKey();
        Animation<TextureRegion> anim =
                hasKey ? catHasKeyAnim : catNoKeyAnim;

        TextureRegion frame = anim.getKeyFrame(catStateTime, true);

        float x = Gdx.graphics.getWidth() - CAT_SIZE - CAT_MARGIN + 170;
        float y = CAT_MARGIN - 80;
        uiBatch.setColor(1f, 1f, 1f, 1f);
        uiBatch.draw(frame, x, y, CAT_SIZE, CAT_SIZE);
    }

    public void renderCompassAsUI(SpriteBatch uiBatch) {
        Compass compass = gameManager.getCompass();
        if (compass == null || !compass.isActive()) return;

        uiBatch.setProjectionMatrix(
                new Matrix4().setToOrtho2D(
                        0, 0,
                        Gdx.graphics.getWidth(),
                        Gdx.graphics.getHeight()
                )
        );

        compass.drawAsUI(uiBatch);
    }

    // =========================================================
    // Bottom Center HUD
    private void renderBottomCenterHUD(SpriteBatch uiBatch) {
        if (gameManager == null || gameManager.getCompass() == null) return;

        HUDLayoutMode mode = getHUDLayoutMode();

        // ===== 尺寸 =====
        float catW = CAT_SIZE;
        float catH = CAT_SIZE;

        Compass compass = gameManager.getCompass();
        float compassW = compass.getUIWidth();
        float compassH = compass.getUIHeight();

        // ===== 组合总高度 =====
        float totalHeight = compassH + CAT_COMPASS_GAP + catH;

        float centerX;
        float baseY;

        if (mode == HUDLayoutMode.SINGLE) {
            // 单人：维持你原来的右下布局
            renderCat(uiBatch);
            renderCompassAsUI(uiBatch);
            return;
        }

        // ===== 双人模式 =====
        // ⭐ 整体中心 = 屏幕中心
        centerX = Gdx.graphics.getWidth() / 2f;
        baseY   = 10f; // 贴底（你可以微调）

        // ===== 计算各自位置（相对不变）=====
        float catX = centerX - catW / 2f;
        float catY = baseY + CAT_Y_OFFSET;

        float compassX = centerX - compassW / 2f;
        float compassY =
                catY
                        + catH
                        + CAT_COMPASS_GAP
                        + COMPASS_Y_OFFSET;


        // ===== 画 =====
        renderCatAt(uiBatch, catX, catY);
        renderCompassAt(uiBatch, compassX, compassY);
    }

    private HUDLayoutMode getHUDLayoutMode() {
        if (gameManager != null && gameManager.isTwoPlayerMode()) {
            return HUDLayoutMode.TWO_PLAYER;
        }
        return HUDLayoutMode.SINGLE;
    }

    private void renderCatAt(SpriteBatch uiBatch, float x, float y) {
        catStateTime += Gdx.graphics.getDeltaTime();
        boolean hasKey = gameManager.getPlayer().hasKey();
        Animation<TextureRegion> anim =
                hasKey ? catHasKeyAnim : catNoKeyAnim;

        TextureRegion frame = anim.getKeyFrame(catStateTime, true);
        uiBatch.setColor(1f, 1f, 1f, 1f);
        uiBatch.draw(frame, x, y, CAT_SIZE, CAT_SIZE);
    }

    private void renderCompassAt(SpriteBatch uiBatch, float x, float y) {
        Compass compass = gameManager.getCompass();
        if (!compass.isActive()) return;
        uiBatch.setProjectionMatrix(
                new Matrix4().setToOrtho2D(
                        0, 0,
                        Gdx.graphics.getWidth(),
                        Gdx.graphics.getHeight()
                )
        );

        compass.drawAsUIAt(uiBatch, x, y);
    }

    private float getIconX(float iconWidth, boolean mirror) {
        if (!mirror) {
            // P1：左侧
            return DASH_UI_MARGIN_X;
        } else {
            // P2：右侧镜面
            return Gdx.graphics.getWidth()
                    - DASH_UI_MARGIN_X
                    - iconWidth;
        }
    }
    // =========================================================
    // Utils / Dispose
    private String formatScore(int score) {
        return String.format("%,d", score);
    }
    private void renderReviveProgressBar(SpriteBatch batch) {
        if (!gameManager.isTwoPlayerMode()) return;
        if (!gameManager.isReviving()) return;

        Player target = gameManager.getRevivingTarget();
        if (target == null) return; // ⭐ 防止极端帧状态

        float progress = Math.min(1f, gameManager.getReviveProgress());

        float barWidth  = 420f;
        float barHeight = 24f;

        float x = (Gdx.graphics.getWidth() - barWidth) / 2f-30;
        float y = Gdx.graphics.getHeight()  - 290;

        // 背景
        batch.setColor(0f, 0f, 0f, 0.65f);
        batch.draw(
                TextureManager.getInstance().getWhitePixel(),
                x, y,
                barWidth, barHeight
        );

        // 填充
        batch.setColor(0.2f, 0.9f, 0.3f, 0.9f);
        batch.draw(
                TextureManager.getInstance().getWhitePixel(),
                x + 2,
                y + 2,
                (barWidth - 4) * progress,
                barHeight - 4
        );

        // 文本
        font.getData().setScale(1.4f);
        font.setColor(Color.WHITE);

        String text = "REVIVING " +
                (target.getPlayerIndex() == Player.PlayerIndex.P1 ? "P1" : "P2");

        font.draw(batch, text,
                x + barWidth / 2f - 70,
                y + barHeight + 26
        );

        // 还原
        font.getData().setScale(1.2f);
        batch.setColor(1f, 1f, 1f, 1f);
    }

    private void renderAbilityLevel(
            SpriteBatch batch,
            Ability ability,
            float iconX,
            float iconY,
            float iconSize
    ) {
        if (ability == null) return;

        font.getData().setScale(1.0f);
        font.setColor(1f, 1f, 1f, 0.85f);

        String lv = "Lv." + ability.getLevel();

        GlyphLayout layout = new GlyphLayout(font, lv);

        float x = iconX + iconSize - layout.width - 6;
        float y = iconY + 18;

        // 阴影
        font.setColor(0f, 0f, 0f, 0.8f);
        font.draw(batch, lv, x + 1, y - 1);

        // 正文
        font.setColor(Color.WHITE);
        font.draw(batch, lv, x, y);

        font.setColor(Color.WHITE);
        font.getData().setScale(1.2f);
    }

    private boolean canShowUpgrade(Player player, Ability ability) {
        if (player == null || ability == null) return false;
        if (!ability.canUpgrade()) return false;

        return gameManager.getScore() >= UpgradeCost.SCORE_PER_UPGRADE;
    }

    private void renderUpgradeButton(
            SpriteBatch batch,
            Player player,
            Ability ability,
            float iconX,
            float iconY,
            float iconSize
    ) {
        if (!canShowUpgrade(player, ability)) return;

        float time = Gdx.graphics.getDeltaTime();
        float floatY = (float) Math.sin(TimeUtils.millis() * 0.005f) * 6f;

        float x = iconX + iconSize + 6;
        float y = iconY + iconSize / 2f + floatY;

        font.getData().setScale(2.0f);

        // 背景
        font.setColor(0f, 0f, 0f, 0.6f);
        font.draw(batch, "+", x + 2, y - 2);

        // 正文
        font.setColor(Color.GOLD);
        font.draw(batch, "+", x, y);

        font.setColor(Color.WHITE);
        font.getData().setScale(1.2f);

        // ⬇️ 点击检测
        checkUpgradeClick(player, ability, x, y);
    }
    private void checkUpgradeClick(
            Player player,
            Ability ability,
            float x,
            float y
    ) {
        if (!Gdx.input.justTouched()) return;

        float mx = Gdx.input.getX();
        float my = Gdx.graphics.getHeight() - Gdx.input.getY();

        float size = 30f;

        if (mx >= x && mx <= x + size &&
                my >= y - size && my <= y) {

            // 扣分
            gameManager.getScoreManager()
                    .addScore(-UpgradeCost.SCORE_PER_UPGRADE);

            // 升级
            ability.upgrade();

            // 反馈
            if (gameManager.getCombatEffectManager() != null) {
                gameManager.getCombatEffectManager().spawnStatusText(
                        player.getWorldX() * GameConstants.CELL_SIZE,
                        player.getWorldY() * GameConstants.CELL_SIZE + 60,
                        ability.getName() + " Lv." + ability.getLevel(),
                        Color.GOLD
                );
            }
        }
    }




    public void dispose() {
        font.dispose();
        heartFull.dispose();
        heartHalf.dispose();
        shapeRenderer.dispose();
        catAtlas.dispose();
        iconAtk.dispose();
        iconRegen.dispose();
        iconMana.dispose();
        sparkleAtlas.dispose();
        if (manaBaseP2 != manaBaseP1) manaBaseP2.dispose();
        manaFillP1.dispose();
        manaFillP2.dispose();
        manaGlowP1.dispose();
        manaGlowP2.dispose();
    }

}
