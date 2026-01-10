// HUD.java - Debug 修复版本（Part 1）
package de.tum.cit.fop.maze.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.*;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Matrix4;

import de.tum.cit.fop.maze.abilities.*;
import de.tum.cit.fop.maze.entities.Compass;
import de.tum.cit.fop.maze.entities.Player;
import de.tum.cit.fop.maze.game.GameManager;
import de.tum.cit.fop.maze.game.achievement.*;
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

        font = new BitmapFont();
        font.getData().setScale(1.2f);

        shapeRenderer = new ShapeRenderer();

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
            renderBottomCenterHUD(uiBatch);
        } catch (Exception e) {
            Logger.error("HUD render failed"+e);
        }
    }

    private void renderSinglePlayerHUD(SpriteBatch uiBatch) {
        Player player = gameManager.getPlayer();
        if (player == null) return;

        renderManaBarForPlayer(
                uiBatch,
                player,
                0,
                (Gdx.graphics.getWidth() * 0.17f),
                50,
                Gdx.graphics.getWidth() * 0.66f
        );

        renderLivesAsHearts(
                uiBatch,
                player,
                20,
                Gdx.graphics.getHeight() - 90,
                false
        );

        renderScore(uiBatch);
        renderCat(uiBatch);
        renderCompassAsUI(uiBatch);
        renderDashIcon(uiBatch, player, false);
        renderMeleeIcon(uiBatch, player, false);
        renderAchievementPopup(uiBatch);
    }

    private void renderTwoPlayerHUD(SpriteBatch uiBatch) {
        var players = gameManager.getPlayers();
        if (players == null || players.isEmpty()) return;

        // ===== Mana Bar =====
        float barWidth = 500f;
        float marginX  = 40f;
        float marginY  = 30f;

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
        float dashX = mirror
                ? Gdx.graphics.getWidth() - DASH_ICON_SIZE - DASH_UI_MARGIN_X
                : DASH_UI_MARGIN_X;

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

        float dashX = mirror
                ? Gdx.graphics.getWidth() - DASH_ICON_SIZE - DASH_UI_MARGIN_X
                : DASH_UI_MARGIN_X;

        float x = mirror
                ? dashX - MELEE_UI_OFFSET_X
                : dashX + MELEE_UI_OFFSET_X;

        float y = DASH_UI_MARGIN_Y + (DASH_ICON_SIZE - size) / 2f;

        if (phase != MagicAbility.Phase.IDLE) {
            float originX = size * 0.5f;
            float originY = size * 0.62f;

            float rotation =
                    phase == MagicAbility.Phase.AIMING
                            ? time * 720f
                            : 0f;

            batch.setColor(
                    phase == MagicAbility.Phase.COOLDOWN
                            ? 0.35f : 1f,
                    phase == MagicAbility.Phase.COOLDOWN
                            ? 0.35f : 1f,
                    phase == MagicAbility.Phase.COOLDOWN
                            ? 0.35f : 1f,
                    1f
            );

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
        }

        if (phase != MagicAbility.Phase.IDLE
                && phase != MagicAbility.Phase.COOLDOWN) {

            float pulse = 0.6f + 0.4f * (float) Math.sin(time * 6.5f);
            batch.setBlendFunction(GL_SRC_ALPHA, GL_ONE);
            batch.setColor(0.9f, 0.2f, 0.9f, pulse);
            batch.draw(magicGrow, x, y, size, size);
            batch.setBlendFunction(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        }

        batch.setColor(
                phase == MagicAbility.Phase.COOLDOWN ? 0.35f : 1f,
                phase == MagicAbility.Phase.COOLDOWN ? 0.35f : 1f,
                phase == MagicAbility.Phase.COOLDOWN ? 0.35f : 1f,
                1f
        );
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
    // Score
    private void renderScore(SpriteBatch uiBatch) {
        int score = gameManager.getScore();
        String text = "SCORE: " + formatScore(score);

        font.getData().setScale(1.5f);
        GlyphLayout layout = new GlyphLayout(font, text);

        float x = Gdx.graphics.getWidth() - layout.width - 30;
        float y = Gdx.graphics.getHeight() - 60;

        font.setColor(0f, 0f, 0f, 0.7f);
        font.draw(uiBatch, text, x + 2, y - 2);

        font.setColor(Color.GOLD);
        font.draw(uiBatch, text, x, y);

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

        if (manaBase == null || manaFill == null) return;

        float maxMana = Math.max(1f, player.getMaxMana());
        float percent = Math.max(0f, Math.min(1f, player.getMana() / maxMana));

        float barHeight = barWidth * (32f / 256f);

        uiBatch.setColor(1f, 1f, 1f, 1f);
        uiBatch.draw(manaBase, x, y, barWidth, barHeight);

        if (percent <= 0f) return;

        int srcW = (int) (manaFill.getWidth() * percent);
        if (srcW > 0) {
            TextureRegion fillRegion =
                    new TextureRegion(manaFill, 0, 0, srcW, manaFill.getHeight());

            uiBatch.draw(fillRegion, x, y, barWidth * percent, barHeight);
        }

        renderManaGlowEffect(
                uiBatch,
                manaGlow,
                x,
                y,
                barWidth,
                barHeight,
                percent
        );

        List<ManaParticle> particles =
                manaParticlesMap.computeIfAbsent(playerId, k -> new ArrayList<>());

        updateAndRenderLongTrail(
                uiBatch,
                manaGlow,
                particles,
                playerId,
                x,
                y,
                barWidth,
                barHeight,
                percent
        );

        if (manaDeco != null) {
            float decoWidth = barWidth * 0.12f;
            float decoX = x + barWidth * percent - decoWidth * 0.5f;
            uiBatch.draw(manaDeco, decoX, y, decoWidth, barHeight);
        }
    }

    private void renderManaGlowEffect(
            SpriteBatch uiBatch,
            Texture manaGlow,
            float x,
            float y,
            float w,
            float h,
            float percent
    ) {
        if (manaGlow == null || percent <= 0f) return;

        manaGlowTime += Gdx.graphics.getDeltaTime();
        float alpha = 0.4f + 0.3f * (float) Math.sin(manaGlowTime * 3f);

        uiBatch.setBlendFunction(GL_SRC_ALPHA, GL_ONE);
        uiBatch.setColor(1f, 0.8f, 0.95f, alpha);

        int srcW = (int) (manaGlow.getWidth() * percent);
        if (srcW > 0) {
            TextureRegion glow =
                    new TextureRegion(manaGlow, 0, 0, srcW, manaGlow.getHeight());

            uiBatch.draw(glow, x, y + h * 0.15f, w * percent, h * 0.7f);
        }

        uiBatch.setBlendFunction(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        uiBatch.setColor(1f, 1f, 1f, 1f);
    }

    private void updateAndRenderLongTrail(
            SpriteBatch uiBatch,
            Texture manaGlow,
            List<ManaParticle> particles,
            int playerId,
            float x,
            float y,
            float w,
            float h,
            float percent
    ) {
        if (percent < 0.999f || manaGlow == null) {
            particles.clear();
            return;
        }

        float endX = x + w * percent;
        float delta = Gdx.graphics.getDeltaTime();

        for (int i = 0; i < 6 && particles.size() < MAX_PARTICLES; i++) {
            ManaParticle p = new ManaParticle();
            p.x = endX;
            p.y = y + h * (0.3f + Math.random() * 0.4f);
            p.vx = (float) (Math.random() * -300 - 150);
            p.vy = (float) (Math.random() * 40 - 20);
            p.life = 1.2f + (float) Math.random() * 0.8f;
            p.color =
                    playerId == 0
                            ? new Color(1f, 0.85f, 0.3f, 1f)
                            : new Color(0.3f, 0.8f, 1f, 1f);
            particles.add(p);
        }

        uiBatch.setBlendFunction(GL_SRC_ALPHA, GL_ONE);

        for (int i = particles.size() - 1; i >= 0; i--) {
            ManaParticle p = particles.get(i);
            p.life -= delta;
            if (p.life <= 0 || p.x < x) {
                particles.remove(i);
                continue;
            }

            p.x += p.vx * delta;
            p.y += p.vy * delta;
            p.vx *= 0.97f;

            float size = 14f * (p.life / 2f);
            TextureRegion region =
                    playerId == 0 ? sparkleStar : sparkleFlower;

            uiBatch.setColor(p.color.r, p.color.g, p.color.b, p.life * 0.7f);
            uiBatch.draw(region, p.x - size / 2, p.y - size / 2, size, size);
        }

        uiBatch.setBlendFunction(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        uiBatch.setColor(1f, 1f, 1f, 1f);
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
        int lives = player.getLives();

        int lastLives = lastLivesMap.getOrDefault(idx, -1);
        boolean shaking = shakingMap.getOrDefault(idx, false);
        float shakeTimer = shakeTimerMap.getOrDefault(idx, 0f);

        if (lastLives != -1 && lives < lastLives) {
            shaking = true;
            shakeTimer = 0f;
        }

        lastLivesMap.put(idx, lives);

        if (shaking) {
            shakeTimer += Gdx.graphics.getDeltaTime();
            if (shakeTimer >= SHAKE_DURATION) {
                shaking = false;
            }
        }

        shakingMap.put(idx, shaking);
        shakeTimerMap.put(idx, shakeTimer);

        int fullHearts = lives / 10;
        int remainder = lives % 10;
        boolean hasHalf = remainder > 0 && remainder <= 5;
        boolean hasExtraFull = remainder > 5;

        int totalHearts =
                fullHearts
                        + (hasHalf ? 1 : 0)
                        + (hasExtraFull ? 1 : 0);

        totalHearts = Math.min(totalHearts, MAX_HEARTS_DISPLAY);

        if (mirror && iceHeartShader != null) {
            uiBatch.setShader(iceHeartShader);
            iceHeartShader.setUniformf("u_intensity", 1f);
            iceHeartShader.setUniformf("u_cooldown", -1f);
        }

        float shakeX =
                shaking ? (float) Math.sin(shakeTimer * 40f) * SHAKE_AMPLITUDE : 0f;

        int drawn = 0;

        for (int i = 0; i < fullHearts && drawn < totalHearts; i++) {
            int row = drawn / HEARTS_PER_ROW;
            int col = drawn % HEARTS_PER_ROW;

            float x =
                    mirror
                            ? startX - col * HEART_SPACING
                            : startX + col * HEART_SPACING;

            uiBatch.draw(
                    heartFull,
                    x + shakeX,
                    startY - row * ROW_SPACING
            );
            drawn++;
        }

        if (hasHalf && drawn < totalHearts) {
            int row = drawn / HEARTS_PER_ROW;
            int col = drawn % HEARTS_PER_ROW;
            float x =
                    mirror
                            ? startX - col * HEART_SPACING
                            : startX + col * HEART_SPACING;

            uiBatch.draw(heartHalf, x, startY - row * ROW_SPACING);
            drawn++;
        }

        if (hasExtraFull && drawn < totalHearts) {
            int row = drawn / HEARTS_PER_ROW;
            int col = drawn % HEARTS_PER_ROW;
            float x =
                    mirror
                            ? startX - col * HEART_SPACING
                            : startX + col * HEART_SPACING;

            uiBatch.draw(heartFull, x, startY - row * ROW_SPACING);
        }

        uiBatch.setShader(null);
        uiBatch.setColor(1f, 1f, 1f, 1f);
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
        if (gameManager.getCompass() == null) return;

        if (!gameManager.isTwoPlayerMode()) {
            renderCat(uiBatch);
            renderCompassAsUI(uiBatch);
            return;
        }

        float centerX = Gdx.graphics.getWidth() / 2f;
        float baseY = 10f;

        Compass compass = gameManager.getCompass();

        float catX = centerX - CAT_SIZE / 2f;
        float catY = baseY + CAT_Y_OFFSET;

        float compassX = centerX - compass.getUIWidth() / 2f;
        float compassY =
                catY + CAT_SIZE + CAT_COMPASS_GAP + COMPASS_Y_OFFSET;

        renderCatAt(uiBatch, catX, catY);
        renderCompassAt(uiBatch, compassX, compassY);
    }

    private void renderCatAt(SpriteBatch uiBatch, float x, float y) {
        catStateTime += Gdx.graphics.getDeltaTime();
        boolean hasKey = gameManager.getPlayer().hasKey();
        Animation<TextureRegion> anim =
                hasKey ? catHasKeyAnim : catNoKeyAnim;

        TextureRegion frame = anim.getKeyFrame(catStateTime, true);
        uiBatch.draw(frame, x, y, CAT_SIZE, CAT_SIZE);
    }

    private void renderCompassAt(SpriteBatch uiBatch, float x, float y) {
        Compass compass = gameManager.getCompass();
        if (!compass.isActive()) return;
        compass.drawAsUIAt(uiBatch, x, y);
    }

    // =========================================================
    // Utils / Dispose
    private String formatScore(int score) {
        return String.format("%,d", score);
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
