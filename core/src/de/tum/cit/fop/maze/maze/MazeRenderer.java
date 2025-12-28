package de.tum.cit.fop.maze.maze;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Array;
import de.tum.cit.fop.maze.game.GameConstants;
import de.tum.cit.fop.maze.game.GameManager;
import de.tum.cit.fop.maze.utils.Logger;
import de.tum.cit.fop.maze.utils.TextureManager;
import java.util.*;

public class MazeRenderer {

    private GameManager gameManager;
    private TextureManager textureManager;

    private Texture floorTexture;
    private TextureAtlas wallAtlas;
    private TextureRegion[] wallRegions;
    private boolean texturesReady = false;

    private static final int TOTAL_WALL_VARIANTS = 4; // 000, 001, 002, 003
    private static final String WALL_REGION_NAME = "Wallpaper";

    // 存储墙壁分组
    private List<WallGroup> wallGroups = new ArrayList<>();
    private boolean groupsAnalyzed = false;

    // 墙壁分组类
    public static class WallGroup {
        public int startX, startY, length;
        public int textureIndex; // 使用的纹理索引

        public WallGroup(int startX, int startY, int length, int textureIndex) {
            this.startX = startX;
            this.startY = startY;
            this.length = length;
            this.textureIndex = textureIndex;
        }

        // 检查坐标是否在这个分组内
        public boolean contains(int x, int y) {
            return y == startY && x >= startX && x < startX + length;
        }

        // 获取分组渲染信息
        public void render(SpriteBatch batch, TextureRegion[] regions, float cellSize, float wallHeight, int wallOverlap) {
            if (textureIndex < 0 || textureIndex >= regions.length) {
                return;
            }

            TextureRegion region = regions[textureIndex];
            if (region == null) return;

            float totalWidth = length * cellSize;
            float startXPos = startX * cellSize;
            float startYPos = startY * cellSize - wallOverlap;

            batch.draw(region, startXPos, startYPos, totalWidth, wallHeight);
        }

        @Override
        public String toString() {
            String textureName = "";
            switch (textureIndex) {
                case 0: textureName = "000(单墙)"; break;
                case 1: textureName = "001(二连)"; break;
                case 2: textureName = "002(三连)"; break;
                case 3: textureName = "003(五连)"; break;
            }
            return String.format("墙组[%d,%d] 长度%d %s", startX, startY, length, textureName);
        }
    }

    public MazeRenderer(GameManager gameManager) {
        this.gameManager = gameManager;
        this.textureManager = TextureManager.getInstance();
    }

    private void ensureTextures() {
        if (!texturesReady) {
            floorTexture = textureManager.getFloorTexture();
            loadWallAtlas();
            texturesReady = true;
            Logger.debug("MazeRenderer textures loaded");
        }
    }

    private void loadWallAtlas() {
        try {
            String atlasPath = "Wallpaper/Wallpaper.atlas";
            Logger.debug("尝试加载图集: " + atlasPath);

            FileHandle file = Gdx.files.internal(atlasPath);
            if (!file.exists()) {
                Logger.error("图集文件不存在: " + atlasPath);
                createFallbackRegions();
                return;
            }

            wallAtlas = new TextureAtlas(file);
            initWallRegionsFromAtlas();
            Logger.debug("图集加载成功");

        } catch (Exception e) {
            Logger.error("加载墙壁图集失败: " + e.getMessage());
            createFallbackRegions();
        }
    }

    private void initWallRegionsFromAtlas() {
        if (wallAtlas == null) {
            Logger.warning("墙壁图集为空");
            createFallbackRegions();
            return;
        }

        Array<TextureAtlas.AtlasRegion> allRegions = wallAtlas.findRegions(WALL_REGION_NAME);

        if (allRegions.size == 0) {
            Logger.warning("图集中没有找到名为 '" + WALL_REGION_NAME + "' 的区域");
            createFallbackRegions();
            return;
        }

        Logger.debug("找到 " + allRegions.size + " 个 '" + WALL_REGION_NAME + "' 区域");

        wallRegions = new TextureRegion[TOTAL_WALL_VARIANTS];

        // 纹理分配：
        // 索引0: 000 - 单墙
        // 索引1: 001 - 二连墙
        // 索引2: 002 - 三连墙
        // 索引3: 003 - 五连墙

        for (int i = 0; i < TOTAL_WALL_VARIANTS; i++) {
            int regionIndex = i % allRegions.size;
            wallRegions[i] = allRegions.get(regionIndex);
            Logger.debug("纹理索引[" + i + "] 分配图集区域[" + regionIndex + "]");
        }
    }

    private void createFallbackRegions() {
        wallRegions = new TextureRegion[TOTAL_WALL_VARIANTS];
        Texture fallbackTexture = textureManager.getWallTexture();
        for (int i = 0; i < TOTAL_WALL_VARIANTS; i++) {
            wallRegions[i] = new TextureRegion(fallbackTexture);
        }
        Logger.debug("创建了 " + TOTAL_WALL_VARIANTS + " 个备用纹理区域");
    }

    // 渲染地板
    public void renderFloor(SpriteBatch batch) {
        ensureTextures();
        int[][] maze = gameManager.getMazeForRendering();
        int size = GameConstants.CELL_SIZE;
        for (int y = 0; y < maze.length; y++) {
            for (int x = 0; x < maze[y].length; x++) {
                if (maze[y][x] == 1) {
                    batch.draw(floorTexture, x * size, y * size, size, size);
                }
            }
        }
    }

    // 分析墙壁分组
    private void analyzeWallGroups() {
        if (groupsAnalyzed) return;

        wallGroups.clear();
        int[][] maze = gameManager.getMazeForRendering();

        for (int y = 0; y < maze.length; y++) {
            for (int x = 0; x < maze[y].length; x++) {
                if (maze[y][x] == 0) {
                    // 检查水平连续长度
                    int length = 1;
                    while (x + length < maze[y].length && maze[y][x + length] == 0) {
                        length++;
                    }

                    // 智能分割连续墙壁
                    splitWallSegment(x, y, length);

                    x += length - 1; // 跳过已处理的墙壁
                }
            }
        }

        groupsAnalyzed = true;
        Logger.debug("墙壁分组分析完成，共 " + wallGroups.size() + " 个分组");
    }

    // 智能分割墙壁段
    private void splitWallSegment(int startX, int startY, int totalLength) {
        int remaining = totalLength;
        int currentX = startX;

        while (remaining > 0) {
            // 优先创建5连分组
            if (remaining >= 5) {
                wallGroups.add(new WallGroup(currentX, startY, 5, 3)); // 3 = 003 (五连墙)
                currentX += 5;
                remaining -= 5;
            }
            // 然后是3连
            else if (remaining >= 3) {
                // 特殊处理：如果剩下4个，分成2+2
                if (remaining == 4) {
                    wallGroups.add(new WallGroup(currentX, startY, 2, 1)); // 1 = 001 (二连墙)
                    wallGroups.add(new WallGroup(currentX + 2, startY, 2, 1)); // 1 = 001 (二连墙)
                    remaining = 0;
                } else {
                    wallGroups.add(new WallGroup(currentX, startY, 3, 2)); // 2 = 002 (三连墙)
                    currentX += 3;
                    remaining -= 3;
                }
            }
            // 然后是2连
            else if (remaining >= 2) {
                wallGroups.add(new WallGroup(currentX, startY, 2, 1)); // 1 = 001 (二连墙)
                currentX += 2;
                remaining -= 2;
            }
            // 最后是单墙
            else {
                wallGroups.add(new WallGroup(currentX, startY, 1, 0)); // 0 = 000 (单墙)
                remaining = 0;
            }
        }
    }

    // 获取所有墙壁分组
    public List<WallGroup> getWallGroups() {
        if (!groupsAnalyzed) {
            analyzeWallGroups();
        }
        return new ArrayList<>(wallGroups);
    }

    // 获取纹理区域
    public TextureRegion getWallRegion(int textureIndex) {
        if (textureIndex >= 0 && textureIndex < wallRegions.length) {
            return wallRegions[textureIndex];
        }
        return null;
    }

    // 获取单元格大小
    public float getCellSize() {
        return GameConstants.CELL_SIZE;
    }

    // 获取墙壁高度倍数
    public float getWallHeightMultiplier() {
        return 1.5f;
    }

    // 获取墙壁重叠量
    public int getWallOverlap() {
        return 6;
    }

    // 调试方法：获取分组详情
    public String debugWallGroups() {
        if (!groupsAnalyzed) {
            analyzeWallGroups();
        }

        StringBuilder sb = new StringBuilder();
        sb.append("=== 墙壁分组详情 ===\n");
        sb.append("总分组数: ").append(wallGroups.size()).append("\n");

        // 按行分组显示
        Map<Integer, List<WallGroup>> groupsByRow = new HashMap<>();
        for (WallGroup group : wallGroups) {
            groupsByRow.computeIfAbsent(group.startY, k -> new ArrayList<>()).add(group);
        }

        List<Integer> sortedRows = new ArrayList<>(groupsByRow.keySet());
        Collections.sort(sortedRows, Collections.reverseOrder()); // 从顶部到底部

        for (int row : sortedRows) {
            sb.append("行 ").append(String.format("%2d", row)).append(": ");
            List<WallGroup> rowGroups = groupsByRow.get(row);
            rowGroups.sort(Comparator.comparingInt(g -> g.startX));

            for (WallGroup group : rowGroups) {
                sb.append(group.toString()).append(" ");
            }
            sb.append("\n");
        }

        return sb.toString();
    }

    // 检查单个墙壁位置是否在任何实体前面
    public boolean isWallInFrontOfAnyEntity(int wallX, int wallY) {
        var player = gameManager.getPlayer();
        if (player != null && wallY > player.getY()) return true;

        var key = gameManager.getKey();
        if (key != null && key.isActive() && wallY > key.getY()) return true;

        for (var door : gameManager.getExitDoors()) {
            if (door != null && wallY > door.getY()) return true;
        }

        for (var enemy : gameManager.getEnemies()) {
            if (enemy != null && enemy.isActive() && wallY > enemy.getY()) {
                return true;
            }
        }

        return false;
    }

    public void onTextureModeChanged() {
        texturesReady = false;
        floorTexture = null;
        if (wallAtlas != null) wallAtlas.dispose();
        wallAtlas = null;
        wallGroups.clear();
        groupsAnalyzed = false;
        Logger.debug("纹理模式改变，清理缓存");
    }

    public void setGameManager(GameManager gameManager) {
        this.gameManager = gameManager;
        this.texturesReady = false;
        this.wallGroups.clear();
        this.groupsAnalyzed = false;

        // 强制重新加载纹理
        this.floorTexture = null;
        if (this.wallAtlas != null) {
            this.wallAtlas.dispose();
            this.wallAtlas = null;
        }

        Logger.debug("MazeRenderer 游戏管理器已更新，状态已重置");
    }
    public void renderWallGroup(SpriteBatch batch, WallGroup group) {
        ensureTextures();

        float cellSize = getCellSize();
        float wallHeight = cellSize * getWallHeightMultiplier();
        int overlap = getWallOverlap();

        group.render(batch, wallRegions, cellSize, wallHeight, overlap);
    }


    public void dispose() {
        if (wallAtlas != null) wallAtlas.dispose();
        wallGroups.clear();
        texturesReady = false;
        groupsAnalyzed = false;
        Logger.debug("MazeRenderer 已释放");
    }
}