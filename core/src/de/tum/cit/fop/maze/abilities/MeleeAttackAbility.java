package de.tum.cit.fop.maze.abilities;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import de.tum.cit.fop.maze.audio.AudioManager;
import de.tum.cit.fop.maze.audio.AudioType;
import de.tum.cit.fop.maze.entities.Player;
import de.tum.cit.fop.maze.entities.enemy.Enemy;
import de.tum.cit.fop.maze.game.GameConstants;
import de.tum.cit.fop.maze.game.GameManager;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MeleeAttackAbility extends Ability {

    private int baseDamage = 5;
    private int damagePerLevel = 1;
    private float hitTimeOffset = 0f;

    private static final float HIT_TIME = 0.12f;
    private GameManager gameManager;

    private final List<int[]> attackTiles = new ArrayList<>();
    private float attackTimer = 0f;
    private boolean damageDone = false;

    private static final Color DEBUG_COLOR = new Color(1f, 0.9f, 0f, 0.5f);

    public MeleeAttackAbility() {
        super("Sword Slash", "Slash enemies in front of you", 0.8f, HIT_TIME);
        this.manaCost = 10;
    }

    @Override
    protected void onActivate(Player player, GameManager gameManager) {
        this.gameManager = gameManager;
        attackTimer = 0f;
        damageDone = false;

        calculateAttackTiles(player);
        player.startAttack();

        AudioManager.getInstance().play(AudioType.SKILL_SLASH);

        if (gameManager.getCombatEffectManager() != null) {
            float angle = 0;
            switch (player.getDirection()) {
                case RIGHT -> angle = 0;
                case UP -> angle = 90;
                case LEFT -> angle = 180;
                case DOWN -> angle = 270;
            }
            gameManager.getCombatEffectManager().spawnSlash(
                    player.getWorldX() * GameConstants.CELL_SIZE,
                    player.getWorldY() * GameConstants.CELL_SIZE,
                    angle,
                    this.level
            );
        }
    }

    @Override
    protected boolean shouldStartCooldown() { return true; }

    @Override
    protected boolean shouldConsumeMana() { return manaCost > 0; }

    @Override
    public void update(float delta, Player player, GameManager gameManager) {
        super.update(delta, player, gameManager);
        if (gameManager == null) return;

        attackTimer += delta;

        if (!damageDone && attackTimer >= HIT_TIME - hitTimeOffset) {
            dealDamage(gameManager);
            damageDone = true;
        }
    }

    @Override
    protected boolean shouldBecomeActive() { return false; }

    private void calculateAttackTiles(Player player) {
        attackTiles.clear();
        int px = player.getX();
        int py = player.getY();
        switch (player.getDirection()) {
            case UP -> { attackTiles.add(new int[]{px, py + 1}); attackTiles.add(new int[]{px, py + 2}); }
            case DOWN -> { attackTiles.add(new int[]{px, py - 1}); attackTiles.add(new int[]{px, py - 2}); }
            case LEFT -> { attackTiles.add(new int[]{px - 1, py}); attackTiles.add(new int[]{px - 2, py}); }
            case RIGHT -> { attackTiles.add(new int[]{px + 1, py}); attackTiles.add(new int[]{px + 2, py}); }
        }
    }

    private void dealDamage(GameManager gameManager) {
        if (gameManager == null) return;
        int damage = (int)((baseDamage + (level - 1) * damagePerLevel));
        Set<Enemy> hitEnemies = new HashSet<>();

        for (int[] tile : attackTiles) {
            List<Enemy> enemies = gameManager.getEnemiesAt(tile[0], tile[1]);
            if (enemies != null) {
                for (Enemy enemy : enemies) {
                    if (enemy != null && !enemy.isDead() && hitEnemies.add(enemy)) {
                        enemy.takeDamage(damage);

                        // 🔥 [新增] 生成受击火花 (HitSpark)
                        if (gameManager.getCombatEffectManager() != null) {
                            float ex = (enemy.getX() + 0.5f) * GameConstants.CELL_SIZE;
                            float ey = (enemy.getY() + 0.5f) * GameConstants.CELL_SIZE;
                            gameManager.getCombatEffectManager().spawnHitSpark(ex, ey);
                        }
                    }
                }
            }
        }

        if (!hitEnemies.isEmpty()) {
            float shakeStrength = 2.0f + (level * 0.5f);
            de.tum.cit.fop.maze.utils.CameraManager.getInstance().shake(0.15f, shakeStrength);
            gameManager.triggerHitFeedback(shakeStrength);
        }
    }

    @Override
    public void draw(SpriteBatch batch, ShapeRenderer shapeRenderer, Player player) {
        if (!GameConstants.DEBUG_MODE) return;
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(DEBUG_COLOR);
        for (int[] tile : attackTiles) {
            shapeRenderer.rect(tile[0]*GameConstants.CELL_SIZE, tile[1]*GameConstants.CELL_SIZE, GameConstants.CELL_SIZE, GameConstants.CELL_SIZE);
        }
        shapeRenderer.end();
    }

    @Override
    protected void onUpgrade() {
        switch (level) {
            case 2 -> baseDamage += 2;
            case 3 -> damagePerLevel += 1;
            case 4 -> hitTimeOffset += 0.03f;
            case 5 -> baseDamage += 5;
        }
    }

    @Override
    public String getId() { return "melee"; }
}