package de.tum.cit.fop.maze.game;

import com.badlogic.gdx.Screen;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Array;
import de.tum.cit.fop.maze.effects.boba.BobaBulletManager;
import de.tum.cit.fop.maze.effects.key.KeyEffectManager;
import de.tum.cit.fop.maze.entities.*;
import de.tum.cit.fop.maze.entities.enemy.*;
import de.tum.cit.fop.maze.entities.enemy.EnemyBoba.BobaBullet;
import de.tum.cit.fop.maze.entities.enemy.EnemyBoba.EnemyCorruptedBoba;
import de.tum.cit.fop.maze.entities.trap.*;
import de.tum.cit.fop.maze.maze.MazeGenerator;
import de.tum.cit.fop.maze.utils.Logger;

import java.util.ArrayList;
import java.util.List;

import static com.badlogic.gdx.math.MathUtils.random;

public class GameManager {

    private int[][] maze;

    private Player player;
    private Key key;


    private final List<Enemy> enemies = new ArrayList<>();
    private final List<Trap> traps = new ArrayList<>();
    private final List<Heart> hearts = new ArrayList<>();
    private final List<Treasure> treasures = new ArrayList<>();
    private final List<ExitDoor> exitDoors = new ArrayList<>();
    private final Array<BobaBullet> bullets = new Array<>();

    private Compass compass;
    private MazeGenerator generator = new MazeGenerator();
    private KeyEffectManager keyEffectManager;


    private int currentLevel = 1;

    /* ================= 生命周期 ================= */

    public GameManager() {
        resetGame();

    }

    public void resetGame() {
        maze = generator.generateMaze();

        enemies.clear();
        traps.clear();
        hearts.clear();
        treasures.clear();
        exitDoors.clear();

        int[] spawn = randomEmptyCell();
        if (player == null) {
            player = new Player(spawn[0], spawn[1], this);
        } else {
            player.reset();
            player.setPosition(spawn[0], spawn[1]);
        }

        generateLevel();
        compass = new Compass(player);
        bobaBulletEffectManager.clearAllBullets(false);
        keyEffectManager = new KeyEffectManager();

        Logger.gameEvent("Game reset complete");

    }

    public void update(float delta) {
        player.update(delta);
        enemies.forEach(e -> e.update(delta, this));
        checkExitReached();
        updateBullets(delta);
        bobaBulletEffectManager.addBullets(bullets);
        bobaBulletEffectManager.update(delta);
        handlePlayerEnemyCollision();
        handleDashHitEnemies();
        checkAutoPickup();
        if (keyEffectManager != null) {
            keyEffectManager.update(delta);
        }

    }

    private void updateBullets(float delta) {
        for (int i = bullets.size - 1; i >= 0; i--) {
            BobaBullet bullet = bullets.get(i);

            bullet.update(delta, this); // ⚠️ 你子弹里已经在用 GameManager

            if (!bullet.isActive()) {
                bullets.removeIndex(i);
            }
        }
    }


    /* ================= 随机生成核心 ================= */

    private void generateLevel() {
        generateExitDoors();
        generateEnemies();
        generateTraps();
        generateHearts();
        generateTreasures();
        spawnKey();
    }

    private void spawnKey() {
        int x, y;

        do {
            x = random.nextInt(GameConstants.MAZE_WIDTH);
            y = random.nextInt(GameConstants.MAZE_HEIGHT);
        } while (
                getMazeCell(x, y) != 1          // 只能放在路上
                        || isOccupied(x, y)             // 不能和别的东西重叠
                        || isExitDoorAt(x, y)            // 不能刷在出口
        );

        key = new Key(x, y,this);
    }

    private boolean isExitDoorAt(int x, int y) {
        for (ExitDoor door : exitDoors) {
            if (door.getX() == x && door.getY() == y) {
                return true;
            }
        }
        return false;
    }


    private boolean isOccupied(int x, int y) {

        // 玩家
        if (player != null && player.getX() == x && player.getY() == y) {
            return true;
        }

        // 敌人
        for (Enemy e : enemies) {
            if (e.isActive() && e.getX() == x && e.getY() == y) {
                return true;
            }
        }

        // 宝箱
        for (Treasure t : treasures) {
            if (t.isActive() && t.getX() == x && t.getY() == y) {
                return true;
            }
        }

        // 爱心
        for (Heart h : hearts) {
            if (h.isActive() && h.getX() == x && h.getY() == y) {
                return true;
            }
        }

        // 钥匙（防止重复刷）
        if (key != null && key.isActive()
                && key.getX() == x && key.getY() == y) {
            return true;
        }

        // 陷阱
        for (Trap trap : traps) {
            if (trap.isActive() && trap.getX() == x && trap.getY() == y) {
                return true;
            }
        }

        return false;
    }



    private void checkExitReached() {
        Player p = player;

        for (ExitDoor door : exitDoors) {
            if (!door.isLocked()
                    && door.isActive()
                    && door.getX() == p.getX()
                    && door.getY() == p.getY()) {

                Logger.gameEvent("Exit reached → next level");
//                goToNextLevel();
                return;
            }
        }
    }
    public void nextLevel() {
        currentLevel++;

        if (currentLevel > GameConstants.MAX_LEVELS) {
            Logger.gameEvent("Game completed!");
            return;
        }

        resetGame();   // 🔥 重生成：地图 / 敌人 / 门 / 宝箱 / 心
    }

    public void onKeyCollected() {
        player.setHasKey(true);

        for (ExitDoor door : exitDoors) {
            door.unlock();   // 🔥 只解锁，不删、不替换
        }

        Logger.gameEvent("All exits unlocked");
    }
    /* ---------- Exit Doors ---------- */

    private void generateExitDoors() {
        for (int i = 0; i < GameConstants.EXIT_COUNT; i++) {
            int[] p = randomEmptyCell();
            exitDoors.add(new ExitDoor(p[0], p[1], i));
        }
    }

    /* ---------- Enemies ---------- */

    private void generateEnemies() {
        for (int i = 0; i < GameConstants.ENEMY_E01_PEARL_COUNT; i++) {
            int[] p = randomEmptyCell();
            enemies.add(new EnemyCorruptedBoba(p[0], p[1]));
        }

        for (int i = 0; i < GameConstants.ENEMY_E02_COFFEE_BEAN_COUNT; i++) {
            int[] p = randomEmptyCell();
            enemies.add(new EnemyE02_SmallCoffeeBean(p[0], p[1]));
        }

        for (int i = 0; i < GameConstants.ENEMY_E03_CARAMEL_COUNT; i++) {
            int[] p = randomEmptyCell();
            enemies.add(new EnemyE03_CaramelJuggernaut(p[0], p[1]));
        }
    }

    /* ---------- Traps ---------- */

    private void generateTraps() {
        for (int i = 0; i < GameConstants.TRAP_T01_GEYSER_COUNT; i++) {
            int[] p = randomEmptyCell();
            traps.add(new TrapT01_Geyser(p[0], p[1], 3f));
        }

        for (int i = 0; i < GameConstants.TRAP_T02_PEARL_MINE_COUNT; i++) {
            int[] p = randomEmptyCell();
            traps.add(new TrapT02_PearlMine(p[0], p[1], this));
        }

        for (int i = 0; i < GameConstants.TRAP_T03_TEA_SHARDS_COUNT; i++) {
            int[] p = randomEmptyCell();
            traps.add(new TrapT03_TeaShards(p[0], p[1]));
        }

        for (int i = 0; i < GameConstants.TRAP_T04_MUD_COUNT; i++) {
            int[] p = randomEmptyCell();
            traps.add(new TrapT04_Mud(p[0], p[1]));
        }
    }

    /* ---------- Hearts ---------- */

    private void generateHearts() {
        int count = 10; // 想多就改
        for (int i = 0; i < count; i++) {
            int[] p = randomEmptyCell();
            hearts.add(new Heart(p[0], p[1]));
        }
    }

    /* ---------- Treasures ---------- */

    private void generateTreasures() {
        int count = 5; // 想多就改
        for (int i = 0; i < count; i++) {
            int[] p = randomEmptyCell();
            treasures.add(new Treasure(p[0], p[1]));
        }
    }

    /* ================= 工具 ================= */

    private int[] randomEmptyCell() {
        int x, y;
        do {
            x = random(1, GameConstants.MAZE_WIDTH - 2);
            y = random(1, GameConstants.MAZE_HEIGHT - 2);
        } while (maze[y][x] == 0);
        return new int[]{x, y};
    }
    // GameManager.java
    public boolean canPlayerMoveTo(int x, int y) {
        // 1️⃣ 越界
        if (x < 0 || y < 0 ||
                x >= GameConstants.MAZE_WIDTH ||
                y >= GameConstants.MAZE_HEIGHT) {
            return false;
        }

        // 2️⃣ 墙体阻挡
        if (maze[y][x] == 0) {
            return false;
        }

        // 3️⃣ 锁着的出口门
        for (ExitDoor door : exitDoors) {
            if (door.getX() == x && door.getY() == y && door.isLocked()) {
                return false;
            }
        }

        // 4️⃣ 其他不可通过物体（以后扩展）
        return true;
    }

    /* ================= Getter ================= */

    public Player getPlayer() { return player; }
    public int[][] getMaze() { return maze; }

    public List<Enemy> getEnemies() { return enemies; }
    public List<Trap> getTraps() { return traps; }
    public List<Heart> getHearts() { return hearts; }
    public List<Treasure> getTreasures() { return treasures; }
    public List<ExitDoor> getExitDoors() { return exitDoors; }

    public Compass getCompass() { return compass; }
    public int getCurrentLevel() { return currentLevel; }

    /* ================= 输入 ================= */

    public void onMoveInput(int dx, int dy) {
        if (player == null) return;

        int nx = player.getX() + dx;
        int ny = player.getY() + dy;

        if (canPlayerMoveTo(nx, ny)) {
            player.move(dx, dy);
        }
    }


    public boolean onAbilityInput(int slot) {
        player.useAbility(slot);
        return true;
    }

    public void onInteractInput() {
        int px = player.getX();
        int py = player.getY();

        // ① 钥匙（优先）开自动拾取了

        // ② 出口
        for (ExitDoor door : exitDoors) {
            if (door.isInteractable()
                    && door.getX() == px && door.getY() == py) {

                door.onInteract(player);
                return;
            }
        }

        // ③ 宝箱
        for (Treasure t : treasures) {
            if (t.isInteractable()
                    && t.getX() == px && t.getY() == py) {

                t.onInteract(player);
                return;
            }
        }

        // ④ 爱心（可自动拾取，也可手动）
        for (Heart h : hearts) {
            if (h.isActive()
                    && h.getX() == px && h.getY() == py) {

                h.onInteract(player);
                return;
            }
        }
    }
    private void checkAutoPickup() {
        int px = player.getX();
        int py = player.getY();

        // ===== 钥匙：自动拾取 =====
        if (key != null && key.isActive()
                && key.getX() == px && key.getY() == py) {

            // === 像素坐标（很重要）===
            float effectX = key.getX() * GameConstants.CELL_SIZE;
            float effectY = key.getY() * GameConstants.CELL_SIZE;

            // 🔥 生成钥匙收集特效
            if (key.getTexture() != null) {
                keyEffectManager.spawnKeyEffect(
                        effectX,
                        effectY,
                        key.getTexture()
                );
            }

            key.onInteract(player);
            onKeyCollected();
        }

        // ===== 爱心：自动拾取 =====
        for (Heart h : hearts) {
            if (h.isActive()
                    && h.getX() == px && h.getY() == py) {

                h.onInteract(player);
            }
        }

        // ===== 宝箱：踩上即开（如果你要）=====
        for (Treasure t : treasures) {
            if (t.isInteractable()
                    && t.getX() == px && t.getY() == py) {

                t.onInteract(player);
            }
        }
    }



    /**
     * Enemy 专用移动判定
     * - 不吃钥匙
     * - 不管门是否上锁
     * - 允许和玩家重合（用于攻击）
     * - 不能穿墙
     */
    public boolean isEnemyValidMove(int x, int y) {

        // 越界 = 不可走
        if (x < 0 || y < 0 ||
                x >= maze[0].length ||
                y >= maze.length) {
            return false;
        }

        // 墙 = 不可走
        if (maze[y][x] == 0) {
            return false;
        }

        // Enemy 不检查门、不检查钥匙
        // Enemy 不检查玩家占位

        // Trap 是否阻挡（只有明确不可走的才挡）
        for (var trap : traps) {
            if (trap.getX() == x && trap.getY() == y && !trap.isPassable()) {
                return false;
            }
        }

        return true;
    }

    /**
     * 获取指定格子上的所有敌人
     * 用于近战 / 范围攻击判定
     */
    public List<Enemy> getEnemiesAt(int x, int y) {

        List<Enemy> result = new ArrayList<>();

        for (Enemy enemy : enemies) {
            if (enemy == null) continue;
            if (enemy.isDead()) continue;

            if (enemy.getX() == x && enemy.getY() == y) {
                result.add(enemy);
            }
        }

        return result;
    }

    /**
     * 获取迷宫某一格的值
     * 0 = 墙
     * 1 = 可行走地面
     */
    public int getMazeCell(int x, int y) {

        // 越界一律当墙处理
        if (x < 0 || x >= GameConstants.MAZE_WIDTH ||
                y < 0 || y >= GameConstants.MAZE_HEIGHT) {
            return 0;
        }

        return maze[y][x];
    }

    /**
     * 生成敌人子弹 / 投射物
     */
    public void spawnProjectile(EnemyBullet bullet) {
        if (bullet == null) return;
        bullets.add((BobaBullet) bullet);
    }
// GameManager.java

    public void spawnProjectile(BobaBullet bullet) {
        if (bullet == null) return;
        bullets.add(bullet);
    }
    // GameManager.java
    private BobaBulletManager bobaBulletEffectManager = new BobaBulletManager();
    public BobaBulletManager getBobaBulletEffectManager() {
        return bobaBulletEffectManager;
    }
//moving
private void handlePlayerEnemyCollision() {
    Player player = this.player;
    if (player == null || player.isDead()) return;

    for (Enemy enemy : enemies) {
        if (!enemy.isActive() || enemy.isDead()) continue;

        // 同一格 = 碰撞
        if (enemy.getX() == player.getX() &&
                enemy.getY() == player.getY()) {

            // Dash 无敌 → 不掉血
            if (player.isDashInvincible()) {
                continue;
            }

            player.takeDamage(enemy.getAttackDamage());
        }
    }
}
    private void handleDashHitEnemies() {
        Player player = this.player;
        if (player == null) return;

        // 只有 Dash 中才生效
        if (!player.isDashing()) return;

        for (Enemy enemy : enemies) {
            if (!enemy.isActive() || enemy.isDead()) continue;

            if (enemy.getX() == player.getX() &&
                    enemy.getY() == player.getY()) {

                // Dash 伤害（你可调）
                enemy.takeDamage(2);

//                // 可选：击退
//                enemy.applyKnockback(
//                        enemy.getX() - player.getX(),
//                        enemy.getY() - player.getY()
//                );
            }
        }
    }


    public GameObject getKey() {
        return key;
    }
    public KeyEffectManager getKeyEffectManager() {
        return keyEffectManager;
    }
    public void dispose() {
        if (keyEffectManager != null) {
            keyEffectManager.dispose();
        }
    }


}
