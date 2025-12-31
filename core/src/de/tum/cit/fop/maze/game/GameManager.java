package de.tum.cit.fop.maze.game;

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
import java.util.Iterator;
import java.util.List;

import static com.badlogic.gdx.math.MathUtils.random;
import static de.tum.cit.fop.maze.maze.MazeGenerator.BORDER_THICKNESS;

public class GameManager {

    private int[][] maze;
    private Player player;

    private final List<Enemy> enemies = new ArrayList<>();
    private final List<Trap> traps = new ArrayList<>();
    private final List<Heart> hearts = new ArrayList<>();
    private final List<Treasure> treasures = new ArrayList<>();
    private final List<ExitDoor> exitDoors = new ArrayList<>();
    private final Array<BobaBullet> bullets = new Array<>();

    private Compass compass;
    private MazeGenerator generator = new MazeGenerator();
    private KeyEffectManager keyEffectManager;

    // ===== Keys =====
    private final List<Key> keys = new ArrayList<>();
    private boolean keyProcessed = false;

    // ===== Reset Control =====
    private boolean pendingReset = false;
    private boolean justReset = false;

    // 🔥 新增：动画状态管理
    private boolean levelTransitionInProgress = false;
    private ExitDoor currentExitDoor = null;
    private float levelTransitionTimer = 0f;
    private static final float LEVEL_TRANSITION_DELAY = 0.5f; // 动画完成后延迟0.5秒

    private int currentLevel = 1;

    /* ================= 生命周期 ================= */
    public GameManager() {
        resetGame();
    }

    private void resetGame() {
        maze = generator.generateMaze();

        enemies.clear();
        traps.clear();
        hearts.clear();
        treasures.clear();
        // 🔥 注意：不清空 exitDoors，只重置状态
        for (ExitDoor door : exitDoors) {
            if (door != null) {
                door.resetDoor();
            }
        }
        keys.clear();

        int[] spawn = randomEmptyCell();

        if (player == null) {
            player = new Player(spawn[0], spawn[1], this);
        } else {
            player.reset();
            player.setPosition(spawn[0], spawn[1]);
        }

        generateLevel();

        compass = new Compass(player);
        bullets.clear();
        bobaBulletEffectManager.clearAllBullets(false);
        keyEffectManager = new KeyEffectManager();

        // 🔥 重置动画状态
        levelTransitionInProgress = false;
        currentExitDoor = null;
        levelTransitionTimer = 0f;

        Logger.gameEvent("Game reset complete");
    }

    public void update(float delta) {
        // 🔥 如果关卡过渡正在进行，只更新相关逻辑
        if (levelTransitionInProgress) {
            if (currentExitDoor != null) {
                // 只更新当前触发的出口门
                currentExitDoor.update(delta, this);
            }

            // 更新关卡过渡计时器
            levelTransitionTimer += delta;
            if (levelTransitionTimer >= LEVEL_TRANSITION_DELAY) {
                // 延迟时间到，触发重置
                levelTransitionInProgress = false;
                levelTransitionTimer = 0f;
                currentExitDoor = null;
                requestReset();
            }
            return;
        }

        // 正常游戏逻辑
        player.update(delta);

        // ===== 修复: 使用 Iterator 遍历敌人，避免并发修改异常 =====
        Iterator<Enemy> enemyIterator = enemies.iterator();
        while (enemyIterator.hasNext()) {
            Enemy e = enemyIterator.next();
            e.update(delta, this);

            if (e.isDead() || !e.isActive()) {
                enemyIterator.remove();
            }
        }

        // 更新出口门
        for (ExitDoor door : exitDoors) {
            door.update(delta, this);
        }

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

        handleKeyLogic();

        // ===== 🔥 统一重置执行点 =====
        if (pendingReset) {
            pendingReset = false;
            resetGame();
            justReset = true;
        }
    }

    public void requestReset() {
        pendingReset = true;
    }

    private void handleKeyLogic() {
        if (keyProcessed) return;

        for (Key key : keys) {
            if (key.isCollected()) {
                unlockAllExitDoors();
                keyProcessed = true;
                break;
            }
        }
    }

    private void unlockAllExitDoors() {
        for (ExitDoor door : exitDoors) {
            if (door.isLocked()) {
                door.unlock();
            }
        }
    }

    private void updateBullets(float delta) {
        for (int i = bullets.size - 1; i >= 0; i--) {
            BobaBullet bullet = bullets.get(i);
            bullet.update(delta, this);

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
        generateKeys();
    }

    private void generateKeys() {
        int keyCount = 10;

        for (int i = 0; i < keyCount; i++) {
            int x, y;
            do {
                x = random.nextInt(GameConstants.MAZE_WIDTH);
                y = random.nextInt(GameConstants.MAZE_HEIGHT);
            } while (
                    getMazeCell(x, y) != 1 ||
                            isOccupied(x, y) ||
                            isExitDoorAt(x, y)
            );
            keys.add(new Key(x, y, this));
        }
    }

    public boolean isExitDoorAt(int x, int y) {
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

        for (Key k : keys) {
            if (k.isActive() && k.getX() == x && k.getY() == y) {
                return true;
            }
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
            if (!door.isLocked() &&
                    door.isActive() &&
                    door.getX() == p.getX() &&
                    door.getY() == p.getY() &&
                    !levelTransitionInProgress) { // 🔥 防止重复触发

                // 触发门动画
                door.onPlayerStep(p);

                // 开始关卡过渡
                startLevelTransition(door);
                return;
            }
        }
    }

    // 🔥 新增：开始关卡过渡
    private void startLevelTransition(ExitDoor door) {
        levelTransitionInProgress = true;
        currentExitDoor = door;
        levelTransitionTimer = 0f;
    }

    public void nextLevel() {
        currentLevel++;

        if (currentLevel > GameConstants.MAX_LEVELS) {
            Logger.gameEvent("Game completed!");
            return;
        }

        requestReset();
    }

    public void onKeyCollected() {
        player.setHasKey(true);

        for (ExitDoor door : exitDoors) {
            door.unlock();
        }

        Logger.gameEvent("All exits unlocked");
    }

    /* ---------- Exit Doors ---------- */
    private void generateExitDoors() {
        for (int i = 0; i < GameConstants.EXIT_COUNT; i++) {
            int[] p = randomWallCell();
            int attempts = 0;

            // 🔥 确保门的位置是有效的
            while (!isValidDoorPosition(p[0], p[1]) && attempts < 50) {
                p = randomWallCell();
                attempts++;
            }

            exitDoors.add(new ExitDoor(p[0], p[1], i));
            Logger.debug("ExitDoor created at (" + p[0] + ", " + p[1] + ")");
        }
    }

    private boolean isValidDoorPosition(int x, int y) {
        int[][] maze = getMaze();
        int width = maze[0].length;
        int height = maze.length;

        // 必须是墙
        if (maze[y][x] != 0) return false;

        // 🔥 关键：检查相邻格子是否有通路
        boolean hasAdjacentPath = false;

        // 四个主要方向
        if (y + 1 < height && maze[y + 1][x] == 1) hasAdjacentPath = true;
        if (y - 1 >= 0 && maze[y - 1][x] == 1) hasAdjacentPath = true;
        if (x - 1 >= 0 && maze[y][x - 1] == 1) hasAdjacentPath = true;
        if (x + 1 < width && maze[y][x + 1] == 1) hasAdjacentPath = true;

        // 🔥 额外：确保玩家可以到达这个位置
        // 检查至少有一个相邻的通路格子
        if (!hasAdjacentPath) {
            // 检查斜角
            if (x - 1 >= 0 && y + 1 < height && maze[y + 1][x - 1] == 1) hasAdjacentPath = true;
            if (x + 1 < width && y + 1 < height && maze[y + 1][x + 1] == 1) hasAdjacentPath = true;
            if (x - 1 >= 0 && y - 1 >= 0 && maze[y - 1][x - 1] == 1) hasAdjacentPath = true;
            if (x + 1 < width && y - 1 >= 0 && maze[y - 1][x + 1] == 1) hasAdjacentPath = true;
        }

        return hasAdjacentPath;
    }

    private int[] randomWallCell() {
        int[][] maze = getMaze();
        int width = maze[0].length;
        int height = maze.length;

        for (int attempt = 0; attempt < 1000; attempt++) {
            int x = BORDER_THICKNESS + random.nextInt(width - BORDER_THICKNESS * 2);
            int y = BORDER_THICKNESS + random.nextInt(height - BORDER_THICKNESS * 2);

            // 1️⃣ 必须是墙
            if (maze[y][x] != 0) continue;

            // 2️⃣ 不能已经有出口门
            if (isExitDoorAt(x, y)) continue;

            // 🔥 3️⃣ 关键修复：检查相邻格子是否有通路
            // 检查上下左右四个方向
            boolean hasAdjacentPath = false;

            // 上
            if (y + 1 < height && maze[y + 1][x] == 1) hasAdjacentPath = true;
            // 下
            if (y - 1 >= 0 && maze[y - 1][x] == 1) hasAdjacentPath = true;
            // 左
            if (x - 1 >= 0 && maze[y][x - 1] == 1) hasAdjacentPath = true;
            // 右
            if (x + 1 < width && maze[y][x + 1] == 1) hasAdjacentPath = true;

            // 🔥 额外检查：确保不是完全封闭的死胡同
            // 检查斜角方向
            if (!hasAdjacentPath) {
                // 左上
                if (x - 1 >= 0 && y + 1 < height && maze[y + 1][x - 1] == 1) hasAdjacentPath = true;
                // 右上
                if (x + 1 < width && y + 1 < height && maze[y + 1][x + 1] == 1) hasAdjacentPath = true;
                // 左下
                if (x - 1 >= 0 && y - 1 >= 0 && maze[y - 1][x - 1] == 1) hasAdjacentPath = true;
                // 右下
                if (x + 1 < width && y - 1 >= 0 && maze[y - 1][x + 1] == 1) hasAdjacentPath = true;
            }

            if (!hasAdjacentPath) continue;

            return new int[]{x, y};
        }

        Logger.warning("randomWallCell fallback triggered");
        // 🔥 改进的 fallback：找一个至少有相邻通路的墙
        for (int y = BORDER_THICKNESS; y < height - BORDER_THICKNESS; y++) {
            for (int x = BORDER_THICKNESS; x < width - BORDER_THICKNESS; x++) {
                if (maze[y][x] != 0) continue;
                if (isExitDoorAt(x, y)) continue;

                // 检查相邻通路
                if ((y + 1 < height && maze[y + 1][x] == 1) ||
                        (y - 1 >= 0 && maze[y - 1][x] == 1) ||
                        (x - 1 >= 0 && maze[y][x - 1] == 1) ||
                        (x + 1 < width && maze[y][x + 1] == 1)) {
                    return new int[]{x, y};
                }
            }
        }

        return new int[]{BORDER_THICKNESS, BORDER_THICKNESS};
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
        int count = 10;
        for (int i = 0; i < count; i++) {
            int[] p = randomEmptyCell();
            hearts.add(new Heart(p[0], p[1]));
        }
    }

    /* ---------- Treasures ---------- */
    private void generateTreasures() {
        int count = 5;
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

    public boolean canPlayerMoveTo(int x, int y) {
        // 1️⃣ 越界
        if (x < 0 || y < 0 || y >= maze.length || x >= maze[0].length) {
            return false;
        }

        // 2️⃣ 出口门优先判断
        for (ExitDoor door : exitDoors) {
            if (door.getX() == x && door.getY() == y) {
                return !door.isLocked();
            }
        }

        // 3️⃣ 普通墙体
        return maze[y][x] == 1;
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
    public List<Key> getKeys() { return keys; }

    // 🔥 新增：获取动画状态
    public boolean isLevelTransitionInProgress() {
        return levelTransitionInProgress;
    }


    /* ================= 输入 ================= */
    public void onMoveInput(int dx, int dy) {
        if (player == null || levelTransitionInProgress) return; // 🔥 过渡期间禁用移动

        int nx = player.getX() + dx;
        int ny = player.getY() + dy;

        if (canPlayerMoveTo(nx, ny)) {
            player.move(dx, dy);
        }
    }

    public boolean onAbilityInput(int slot) {
        if (levelTransitionInProgress) return false; // 🔥 过渡期间禁用技能
        player.useAbility(slot);
        return true;
    }

    public void onInteractInput() {
        if (levelTransitionInProgress) return; // 🔥 过渡期间禁用交互

        int px = player.getX();
        int py = player.getY();

        // 出口
        for (ExitDoor door : exitDoors) {
            if (door.isInteractable() && door.getX() == px && door.getY() == py) {
                door.onInteract(player);
                return;
            }
        }

        // 宝箱
        for (Treasure t : treasures) {
            if (t.isInteractable() && t.getX() == px && t.getY() == py) {
                t.onInteract(player);
                return;
            }
        }

        // 爱心
        for (Heart h : hearts) {
            if (h.isActive() && h.getX() == px && h.getY() == py) {
                h.onInteract(player);
                return;
            }
        }
    }

    private void checkAutoPickup() {
        if (levelTransitionInProgress) return; // 🔥 过渡期间禁用自动拾取

        int px = player.getX();
        int py = player.getY();

        // ===== 钥匙 =====
        Iterator<Key> keyIterator = keys.iterator();
        while (keyIterator.hasNext()) {
            Key key = keyIterator.next();
            if (!key.isActive()) continue;

            if (key.getX() == px && key.getY() == py) {
                float effectX = key.getX() * GameConstants.CELL_SIZE;
                float effectY = key.getY() * GameConstants.CELL_SIZE;

                if (key.getTexture() != null) {
                    keyEffectManager.spawnKeyEffect(effectX, effectY, key.getTexture());
                }

                key.onInteract(player);
                keyIterator.remove();
                onKeyCollected();
                break;
            }
        }

        // ===== 爱心 =====
        Iterator<Heart> heartIterator = hearts.iterator();
        while (heartIterator.hasNext()) {
            Heart h = heartIterator.next();
            if (h.isActive() && h.getX() == px && h.getY() == py) {
                h.onInteract(player);
                heartIterator.remove();
            }
        }

        // ===== 宝箱 =====
        Iterator<Treasure> treasureIterator = treasures.iterator();
        while (treasureIterator.hasNext()) {
            Treasure t = treasureIterator.next();
            if (t.isInteractable() && t.getX() == px && t.getY() == py) {
                t.onInteract(player);
                treasureIterator.remove();
            }
        }
    }

    /**
     * Enemy 专用移动判定
     */
    public boolean isEnemyValidMove(int x, int y) {
        // 越界 = 不可走
        if (x < 0 || y < 0 || x >= maze[0].length || y >= maze.length) {
            return false;
        }

        // 墙 = 不可走
        if (maze[y][x] == 0) {
            return false;
        }

        // Trap 是否阻挡
        for (var trap : traps) {
            if (trap.getX() == x && trap.getY() == y && !trap.isPassable()) {
                return false;
            }
        }

        return true;
    }

    /**
     * 获取指定格子上的所有敌人
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
     */
    public int getMazeCell(int x, int y) {
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

    public void spawnProjectile(BobaBullet bullet) {
        if (bullet == null) return;
        bullets.add(bullet);
    }

    // GameManager.java
    private BobaBulletManager bobaBulletEffectManager = new BobaBulletManager();
    public BobaBulletManager getBobaBulletEffectManager() {
        return bobaBulletEffectManager;
    }

    private void handlePlayerEnemyCollision() {
        if (levelTransitionInProgress) return; // 🔥 过渡期间禁用碰撞检测

        Player player = this.player;
        if (player == null || player.isDead()) return;

        for (Enemy enemy : enemies) {
            if (!enemy.isActive() || enemy.isDead()) continue;

            if (enemy.getX() == player.getX() && enemy.getY() == player.getY()) {
                if (player.isDashInvincible()) {
                    continue;
                }
                player.takeDamage(enemy.getAttackDamage());
            }
        }
    }

    private void handleDashHitEnemies() {
        if (levelTransitionInProgress) return; // 🔥 过渡期间禁用Dash伤害

        Player player = this.player;
        if (player == null) return;
        if (!player.isDashing()) return;

        for (Enemy enemy : enemies) {
            if (!enemy.isActive() || enemy.isDead()) continue;

            if (enemy.getX() == player.getX() && enemy.getY() == player.getY()) {
                enemy.takeDamage(2);
            }
        }
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