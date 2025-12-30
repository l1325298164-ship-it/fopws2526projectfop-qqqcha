package de.tum.cit.fop.maze.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.utils.ScreenUtils;
import de.tum.cit.fop.maze.MazeRunnerGame;
import de.tum.cit.fop.maze.audio.AudioManager;
import de.tum.cit.fop.maze.audio.AudioType;
import de.tum.cit.fop.maze.effects.boba.BobaBulletManager;
import de.tum.cit.fop.maze.effects.key.KeyEffectManager;
import de.tum.cit.fop.maze.effects.portal.PortalEffectManager;
import de.tum.cit.fop.maze.entities.*;
import de.tum.cit.fop.maze.entities.enemy.Enemy;
import de.tum.cit.fop.maze.entities.enemy.EnemyBoba.BobaBullet;
import de.tum.cit.fop.maze.entities.enemy.EnemyBullet;
import de.tum.cit.fop.maze.entities.trap.Trap;
import de.tum.cit.fop.maze.game.GameConstants;
import de.tum.cit.fop.maze.game.GameManager;
import de.tum.cit.fop.maze.input.PlayerInputHandler;
import de.tum.cit.fop.maze.maze.MazeRenderer;
import de.tum.cit.fop.maze.ui.HUD;
import de.tum.cit.fop.maze.utils.CameraManager;
import de.tum.cit.fop.maze.utils.Logger;
import de.tum.cit.fop.maze.utils.TextureManager;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class GameScreen implements Screen {

    private final MazeRunnerGame game;

    private OrthographicCamera camera;
    private CameraManager cameraManager;
    private GameManager gameManager;
    private MazeRenderer mazeRenderer;

    private SpriteBatch worldBatch;
    private SpriteBatch uiBatch;
    private ShapeRenderer shapeRenderer;
    private BitmapFont font;

    private PlayerInputHandler inputHandler;
    private HUD hud;

    private boolean isPlayerMoving = false;
    private boolean playerHadKey = false;

    // 特效
    private BobaBulletManager bobaBulletManager;
    private KeyEffectManager keyEffectManager;
    private PortalEffectManager portalEffectManager;
    private boolean waitingForPortal = false;

//防止崩溃
    private boolean pendingExitToMenu = false;

    // Hp道具列表
    private java.util.List<Heart> hearts;
    private java.util.List<HeartContainer> heartContainers;
    private java.util.List<Treasure> treasures; // 🔥 新增：宝箱列表

    /* ================= 渲染结构 ================= */

    private enum RenderItemType {
        WALL_BEHIND,
        ENTITY,
        EFFECT,      // ⭐ 新增
        WALL_FRONT
    }


    private static class RenderItem {
        float y;                 // 用于深度排序
        int priority;
        RenderItemType type;

        GameObject entity;              // ENTITY 用
        MazeRenderer.WallGroup wall;    // WALL 用

        // 实体
        RenderItem(GameObject entity, int priority) {
            this.entity = entity;
            this.y = entity.getY();
            this.priority = priority;
            this.type = RenderItemType.ENTITY;
        }

        // 墙
        RenderItem(MazeRenderer.WallGroup wall, RenderItemType type) {
            this.wall = wall;
            this.y = wall.startY;
            this.type = type;
            this.priority = 0;
        }

        RenderItem(GameObject entity, int priority, RenderItemType type) {
            this.entity = entity;
            this.y = entity.getY();
            this.priority = priority;
            this.type = type;
        }
    }


    /* ================= 生命周期 ================= */

    public GameScreen(MazeRunnerGame game) {
        this.game = game;
    }

    @Override
    public void show() {
        worldBatch = game.getSpriteBatch();
        uiBatch = new SpriteBatch();
        shapeRenderer = new ShapeRenderer();
        font = new BitmapFont();

        gameManager = new GameManager();
        mazeRenderer = new MazeRenderer(gameManager);

        camera = new OrthographicCamera();
        camera.setToOrtho(false);
        camera.zoom = 0.75f;

        cameraManager = new CameraManager();
        inputHandler = new PlayerInputHandler();
        hud = new HUD(gameManager);

        bobaBulletManager = new BobaBulletManager();
        bobaBulletManager.setRenderMode(BobaBulletManager.RenderMode.MANAGED);

        keyEffectManager = new KeyEffectManager();
        portalEffectManager = new PortalEffectManager();

        cameraManager.centerOnPlayerImmediately(gameManager.getPlayer());

        // 🔥 初始化并生成测试道具
        initCollectibles();

        Logger.debug("GameScreen initialized");
    }
    // 🔥 修改后：随机生成 Heart，暂时移除 HeartContainer
    private void initCollectibles() {
        hearts = new java.util.ArrayList<>();
        heartContainers = new java.util.ArrayList<>(); // 初始化为空，等怪物掉落

        // === 随机生成回血道具 (Heart) ===
        // 1. 随机决定生成数量 (3 到 6 个)
        int minCount = 3;
        int maxCount = 6;
        int count = com.badlogic.gdx.math.MathUtils.random(minCount, maxCount);

        int spawned = 0;
        int attempts = 0; // 防止死循环的安全计数

        while (spawned < count && attempts < 100) {
            attempts++;

            // 假设地图大小大约是 15x15 或者更大，这里随机取坐标
            // 你可以用 GameConstants.LEVEL_WIDTH 如果有的话
            int rx = com.badlogic.gdx.math.MathUtils.random(1, 15);
            int ry = com.badlogic.gdx.math.MathUtils.random(1, 15);

            // 🔥 关键检查：这个位置必须能走 (isValidMove) 且没有其他东西
            if (gameManager.isValidMove(rx, ry)) {
                hearts.add(new Heart(rx, ry));
                spawned++;
                Logger.debug("randomly generate heart at: " + rx + ", " + ry);
            }
        }
        // === 3. 生成宝箱 (Treasure) ===
        treasures = new java.util.ArrayList<>();

        // 🔥 补上这一行，防止 player 报错
        if (gameManager.getPlayer() != null) {
            Player player = gameManager.getPlayer(); // 定义 player 变量

            // 随机生成 1-3 个宝箱
            int chestCount = com.badlogic.gdx.math.MathUtils.random(1, 3);
            int chestSpawned = 0;
            attempts = 0;

            while (chestSpawned < chestCount && attempts < 100) {
                attempts++;
                int tx = com.badlogic.gdx.math.MathUtils.random(1, 15);
                int ty = com.badlogic.gdx.math.MathUtils.random(1, 15);

                // 检查：必须是空地，且不能和玩家重叠
                boolean overlap = (tx == player.getX() && ty == player.getY());

                // 检查：不能和已生成的爱心重叠
                for (Heart h : hearts) {
                    if (h.getX() == tx && h.getY() == ty) {
                        overlap = true;
                        break; // 只有重叠了才跳出循环
                    }
                }

                // 检查：不能和已生成的宝箱重叠 (防止两个宝箱刷在一起)
                for (Treasure t : treasures) {
                    if (t.getX() == tx && t.getY() == ty) {
                        overlap = true;
                        break;
                    }
                }

                if (gameManager.isValidMove(tx, ty) && !overlap) {
                    treasures.add(new Treasure(tx, ty));
                    chestSpawned++;
                    Logger.debug("生成宝箱在: " + tx + ", " + ty);
                }
            }

        }
    }

    // 🔥 修复版：道具拾取检测
    // 🔥 最终逻辑：满血保留，残血拾取
    private void checkItemPickups() {
        Player player = gameManager.getPlayer();
        if (player == null) return;

        // 获取玩家当前的网格坐标
        int px = player.getX();
        int py = player.getY();

        // ==========================================
        // 1. 检测回血道具 (Heart)
        // ==========================================
        java.util.Iterator<Heart> heartIter = hearts.iterator();
        while (heartIter.hasNext()) {
            Heart heart = heartIter.next();

            // 只要坐标重合
            if (heart.isActive() && heart.getX() == px && heart.getY() == py) {

                // 🔥 核心逻辑：检查生命值
                // 如果当前生命值 >= 最大生命值，说明满血
                if (player.getLives() >= player.getMaxLives()) {
                    // 直接跳过这次循环，不执行移除，也不触发效果
                    // 效果就是：玩家踩在爱心上，但爱心还在原地不动
                    continue;
                }

                // --- 只有不满血时，代码才会走到这里 ---

                heart.onInteract(player); // 回血
                heartIter.remove();       // 移除道具
                AudioManager.getInstance().play(AudioType.UI_SUCCESS);
            }
        }

        // ==========================================
        // 2. 检测上限道具 (HeartContainer)
        // ==========================================
        java.util.Iterator<HeartContainer> containerIter = heartContainers.iterator();
        while (containerIter.hasNext()) {
            HeartContainer container = containerIter.next();

            // 上限道具不需要判断满血，随时都可以吃
            if (container.isActive() && container.getX() == px && container.getY() == py) {
                container.onInteract(player);
                containerIter.remove();
                AudioManager.getInstance().play(AudioType.UI_SUCCESS);
            }
        }
        // 3. 检测宝箱 (Treasure)
        for (Treasure treasure : treasures) {
            // 如果坐标重合，且宝箱还没开
            if (treasure.getX() == px && treasure.getY() == py) {
                // Treasure 内部自己会判断 isOpened，所以直接调用 onInteract 即可
                treasure.onInteract(player);

                // 注意：这里不需要 remove，也不需要播放音效
                // (因为 Treasure 内部逻辑或者 open 方法里可以控制音效)
            }
        }
    }
    @Override
    public void render(float delta) {

        handleInput(delta);

        updatePortalCheck();
        updateGameLogic(delta);
        updateEffects(delta);
        updateCamera(delta);

        clearScreen();

        renderWorld();
        renderUI();

        handlePendingExit();
    }

    private void handlePendingExit() {
        if (pendingExitToMenu) {
            game.goToMenu();
        }
    }

    private void clearScreen() {
        ScreenUtils.clear(0.1f, 0.1f, 0.1f, 1);
    }

    private void updateCamera(float delta) {
        cameraManager.update(delta, gameManager.getPlayer());
    }

    private void updateEffects(float delta) {

        registerNewBobaBullets();

        bobaBulletManager.update(delta);
        keyEffectManager.update(delta);
        portalEffectManager.update(delta);

        checkKeyPickupEffect();
        checkPortalFinished();
    }

    private void checkPortalFinished() {
        if (!portalEffectManager.isFinished()) return;

        gameManager.completeLevelTransition();
        cameraManager.centerOnPlayerImmediately(gameManager.getPlayer());

        portalEffectManager.reset();
        mazeRenderer.dispose();
        mazeRenderer = new MazeRenderer(gameManager);

        bobaBulletManager.clearAllBullets(false);
        keyEffectManager = new KeyEffectManager();

        playerHadKey = false;
        waitingForPortal = false;
        isPlayerMoving = false;
    }

    private void checkKeyPickupEffect() {
        if (!playerHadKey && gameManager.getPlayer().hasKey()) {
            Key key = gameManager.getKey();
            if (key != null) {
                float px = (key.getX() + 0.5f) * GameConstants.CELL_SIZE;
                float py = (key.getY() + 0.5f) * GameConstants.CELL_SIZE;
                keyEffectManager.spawnKeyEffect(px, py,
                        TextureManager.getInstance().getKeyTexture());
                AudioManager.getInstance().play(AudioType.PLAYER_GET_KEY);
            }
            playerHadKey = true;
        }
    }
    private void registerNewBobaBullets() {
        for (EnemyBullet bullet : gameManager.getBullets()) {
            if (bullet instanceof BobaBullet b && !b.isManagedByEffectManager()) {
                bobaBulletManager.addBullet(b);
            }
        }
    }


    private void updateGameLogic(float delta) {
        if (waitingForPortal) return;

        gameManager.update(delta);
        // 新增：每一帧都检查有没有捡到东西
        checkItemPickups();

        Player player = gameManager.getPlayer();
        boolean nowMoving = player.isMoving();

        if (nowMoving && !isPlayerMoving) {
            AudioManager.getInstance().playPlayerMove();
            isPlayerMoving = true;
        } else if (!nowMoving && isPlayerMoving) {
            AudioManager.getInstance().stopPlayerMove();
            isPlayerMoving = false;
        }
    }


    private void updatePortalCheck() {
        if (waitingForPortal) return;

        Player player = gameManager.getPlayer();
        for (ExitDoor door : gameManager.getExitDoors()) {
            if (!door.isLocked() && player.collidesWith(door)) {

                float px = (door.getX() + 0.5f) * GameConstants.CELL_SIZE;
                float py = (door.getY() + 0.5f) * GameConstants.CELL_SIZE;

                portalEffectManager.startExitAnimation(px, py);
                waitingForPortal = true;

                AudioManager.getInstance().play(AudioType.UI_SUCCESS);
                AudioManager.getInstance().stopPlayerMove();
                isPlayerMoving = false;
                break;
            }
        }
    }



    /* ================= 渲染 ================= */

    private void renderWorld() {

        worldBatch.setProjectionMatrix(cameraManager.getCamera().combined);
        shapeRenderer.setProjectionMatrix(cameraManager.getCamera().combined);

        worldBatch.begin();
        mazeRenderer.renderFloor(worldBatch);
        worldBatch.end();


        List<RenderItem> items = collectAllRenderItems();
        items.sort(Comparator
                .comparingDouble((RenderItem r) -> -r.y)
                .thenComparingInt(r -> r.type.ordinal())
                .thenComparingInt(r -> r.priority));

        boolean spriteBatchActive = false;
        boolean shapeBatchActive = false;


        for (RenderItem item : items) {

            // ===== 墙（永远是 Sprite）=====
            if (item.type == RenderItemType.WALL_BEHIND ||
                    item.type == RenderItemType.WALL_FRONT) {



                if (shapeBatchActive) {
                    shapeRenderer.end();
                    shapeBatchActive = false;
                }
                if (!spriteBatchActive) {
                    worldBatch.begin();
                    spriteBatchActive = true;
                }

                mazeRenderer.renderWallGroup(worldBatch, item.wall);
                continue;
            }

            // ===== 实体 =====
            GameObject entity = item.entity;


            if (entity.getRenderType() == GameObject.RenderType.SPRITE) {

                if (shapeBatchActive) {
                    shapeRenderer.end();
                    shapeBatchActive = false;
                }
                if (!spriteBatchActive) {
                    worldBatch.begin();
                    spriteBatchActive = true;
                }

                entity.drawSprite(worldBatch);
                //门的呼吸灯特效等实体墙砖整上去了再改善！！TODO
                if (entity instanceof ExitDoor door) {
                    portalEffectManager.renderBack(
                            worldBatch,
                            door.getX() * GameConstants.CELL_SIZE,
                            door.getY() * GameConstants.CELL_SIZE
                    );
                }

            } else { // SHAPE

                if (spriteBatchActive) {
                    worldBatch.end();
                    spriteBatchActive = false;
                }
                if (!shapeBatchActive) {
                    shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
                    shapeBatchActive = true;
                }

                entity.drawShape(shapeRenderer);
            }

        }

        if (spriteBatchActive) worldBatch.end();
        if (shapeBatchActive) shapeRenderer.end();
        worldBatch.begin();
        bobaBulletManager.render(worldBatch);

        keyEffectManager.render(worldBatch);
        portalEffectManager.renderFront(worldBatch);
        worldBatch.end();
    }



    private void renderUI() {
        uiBatch.begin();
        if (gameManager.isGameComplete()) {
            hud.renderGameComplete(uiBatch);
        } else {
            hud.renderInGameUI(uiBatch);
        }
        uiBatch.end();
    }

    /* ================= 收集渲染对象 ================= */

    private List<RenderItem> collectAllRenderItems() {
        List<RenderItem> items = new ArrayList<>();

        addAllWalls(items);
        addAllEntities(items);

        return items;
    }

    private void addAllWalls(List<RenderItem> items) {
        List<MazeRenderer.WallGroup> groups = mazeRenderer.getWallGroups();

        for (MazeRenderer.WallGroup group : groups) {

            // 🔥 关键修复：如果这个墙组位置是出口门，直接跳过
            if (isWallGroupOnExitDoor(group)) {
                continue;
            }

            boolean isFront =
                    mazeRenderer.isWallInFrontOfAnyEntity(
                            group.startX,
                            group.startY
                    );

            items.add(new RenderItem(
                    group,
                    isFront ? RenderItemType.WALL_FRONT : RenderItemType.WALL_BEHIND
            ));
        }
    }
    private boolean isWallGroupOnExitDoor(MazeRenderer.WallGroup group) {
        for (ExitDoor door : gameManager.getExitDoors()) {
            if (group.startX == door.getX() &&
                    group.startY == door.getY()) {
                return true;
            }
        }
        return false;
    }

    private void addAllEntities(List<RenderItem> items) {
        items.add(new RenderItem(gameManager.getPlayer(), 100));

        for (Trap t : gameManager.getTraps()) {
            if (t.isActive()) items.add(new RenderItem(t, 10));
        }

        for (Enemy e : gameManager.getEnemies()) {
            if (e.isActive()) items.add(new RenderItem(e, 50));
        }




        Key key = gameManager.getKey();
        if (key != null && key.isActive()) items.add(new RenderItem(key, 20));

        for (ExitDoor door : gameManager.getExitDoors()) {
            items.add(new RenderItem(door, 0));
        }
        // 🔥 新增：把道具加入渲染队列
        if (hearts != null) {
            for (Heart h : hearts) {
                if (h.isActive()) items.add(new RenderItem(h, 20)); // 20是层级优先级
            }
        }
        if (heartContainers != null) {
            for (HeartContainer hc : heartContainers) {
                if (hc.isActive()) items.add(new RenderItem(hc, 20));
            }
        }
        // 🔥 新增：渲染宝箱
        if (treasures != null) {
            for (Treasure t : treasures) {
                // 只要是激活的都画（包括打开的和关着的）
                if (t.isActive()) items.add(new RenderItem(t, 20));
            }
        }
    }


    /* ================= 输入 ================= */

    private void handleInput(float delta) {
        if (waitingForPortal) {
            stopMoveSoundIfNeeded();
            return;
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            AudioManager.getInstance().playUIClick();
            pendingExitToMenu = true;
            return;
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.R)) {
            AudioManager.getInstance().playUIClick();
            restartGame();
            return;
        }

        handleTextureModeSwitch();
        handlePlayerMovement(delta);
    }

    private void handlePlayerMovement(float delta) {
        Player player = gameManager.getPlayer();

        inputHandler.update(delta, new PlayerInputHandler.InputHandlerCallback() {

            @Override
            public void onMoveInput(int dx, int dy) {
                // Player 类里有 isMoving() 方法，返回 true 表示处于 0.15s 的冷却期
                if (player.isMoving()) {
                    return; // 如果正在移动/冷却中，直接无视这次输入
                }
                int nx = player.getX() + dx;
                int ny = player.getY() + dy;

                if (gameManager.isValidMove(nx, ny)) {
                    player.move(dx, dy);

                    // 🔥 播放移动音效（单次触发，持续由 render 控制）
                    if (!isPlayerMoving) {
                        AudioManager.getInstance().play(AudioType.PLAYER_MOVE);
                    }
                } else {
                    // 🔥 撞墙音效
                    AudioManager.getInstance().play(AudioType.PLAYER_HIT_WALL);
                }
            }

            @Override
            public float getMoveDelayMultiplier() {
                return player.getMoveDelayMultiplier();
            }

            @Override
            public boolean onAbilityInput(int slot) {
                return false;
            }

            @Override
            public void onInteractInput() {

            }

            @Override
            public void onMenuInput() {

            }
        });
    }


    //for test
    private void handleTextureModeSwitch() {
        TextureManager tm = TextureManager.getInstance();

        if (Gdx.input.isKeyJustPressed(Input.Keys.F1)) {
            tm.switchMode(TextureManager.TextureMode.COLOR);
            notifyExitDoorsTextureChanged();
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.F2)) {
            tm.switchMode(TextureManager.TextureMode.IMAGE);
            notifyExitDoorsTextureChanged();
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.F3)) {
            tm.switchMode(TextureManager.TextureMode.PIXEL);
            notifyExitDoorsTextureChanged();
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.F4)) {
            tm.switchMode(TextureManager.TextureMode.MINIMAL);
            notifyExitDoorsTextureChanged();
        }
    }

    private void notifyExitDoorsTextureChanged() {
        for (ExitDoor d : gameManager.getExitDoors()) {
            d.onTextureModeChanged();
        }
    }
    private void stopMoveSoundIfNeeded() {
        if (isPlayerMoving) {
            AudioManager.getInstance().stopPlayerMove();
            isPlayerMoving = false;
        }
    }

    private void restartGame() {
        gameManager.resetGame();
        bobaBulletManager.clearAllBullets(true);
        mazeRenderer.setGameManager(gameManager);
        hud = new HUD(gameManager);
        cameraManager.centerOnPlayerImmediately(gameManager.getPlayer());
        AudioManager.getInstance().stopPlayerMove();
        isPlayerMoving = false;
        initCollectibles(); // 重开时重新生成道具
    }

    /* ================= 释放 ================= */

    @Override public void resize(int w, int h) {}
    @Override public void pause() {}
    @Override public void resume() {}
    @Override
    public void hide() {
        // 🔥 离开游戏屏幕时停止玩家移动音效
        AudioManager.getInstance().stopPlayerMove();
        isPlayerMoving = false;
    }

    @Override
    public void dispose() {
        uiBatch.dispose();
        shapeRenderer.dispose();
        font.dispose();
        bobaBulletManager.dispose();
        keyEffectManager.dispose();
        portalEffectManager.dispose();
        mazeRenderer.dispose();
        Logger.debug("GameScreen disposed");
        // 🔥 清理宝箱资源
        if (treasures != null) {
            for (Treasure t : treasures) {
                t.dispose();
            }
            treasures.clear();
        }
    }
}
