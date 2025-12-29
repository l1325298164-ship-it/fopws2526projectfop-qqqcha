package de.tum.cit.fop.maze.maze;


import de.tum.cit.fop.maze.game.GameConstants;
import de.tum.cit.fop.maze.utils.Logger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;
import java.util.Stack;

public class MazeGenerator {
    private Random random;

    // 新的单元格配置
    private static final int WALL_WIDTH = 1;      // 墙宽度：1格
    private static final int WALL_HEIGHT = 1;     // 墙高度：2格
    private static final int PATH_WIDTH = 3;      // 道路宽度：3格
    private static final int PATH_HEIGHT = 3;     // 道路高度：3格
    private static final int BORDER_THICKNESS = 4;


    public MazeGenerator() {
        random = new Random();
        Logger.debug("MazeGenerator initialized with 3x3 paths and 1x2 walls");
    }

    public int[][] generateMaze() {
        long startTime = System.currentTimeMillis();

        // 计算调整后的尺寸
        int cellGroupWidth = PATH_WIDTH + WALL_WIDTH;
        int cellGroupHeight = PATH_HEIGHT + WALL_HEIGHT;

        int adjustedWidth = adjustSize(GameConstants.MAZE_WIDTH, cellGroupWidth);
        int adjustedHeight = adjustSize(GameConstants.MAZE_HEIGHT, cellGroupHeight);

        int[][] maze = new int[adjustedHeight][adjustedWidth];

        // 初始化迷宫，全部设为墙
        for (int y = 0; y < adjustedHeight; y++) {
            Arrays.fill(maze[y], 0);
        }

        // 确保边界是墙
        for (int x = 0; x < adjustedWidth; x++) {
            maze[0][x] = 0; // 上边界
            maze[adjustedHeight - 1][x] = 0; // 下边界
        }
        for (int y = 0; y < adjustedHeight; y++) {
            maze[y][0] = 0; // 左边界
            maze[y][adjustedWidth - 1] = 0; // 右边界
        }

        // 使用新的DFS生成迷宫（基于3x3道路和1x2墙）
        generate3x3PathDFS(maze);

        // 确保起点和终点区域是通路（3x3区域）
//        ensureStartEnd3x3Areas(maze);

        // 添加更多通路（防止死胡同）
        add3x3AdditionalPaths(maze, 0.19f);

        // 验证迷宫连通性
        validate3x3Maze(maze);

        // 清理孤立的小墙块
        cleanupSmallWalls(maze);

        long endTime = System.currentTimeMillis();
        Logger.performance("3x3 Maze generated in " + (endTime - startTime) + "ms");
        Logger.debug("Maze generation completed - Size: " + adjustedWidth + "x" + adjustedHeight);
        Logger.debug("Configuration - Path: " + PATH_WIDTH + "x" + PATH_HEIGHT +
            ", Wall: " + WALL_WIDTH + "x" + WALL_HEIGHT);
        addOuterBorderWalls(maze);
        return maze;
    }

    /**
     * 在迷宫四周生成固定厚度的外墙（用于主题贴图）
     */
    private void addOuterBorderWalls(int[][] maze) {
        int height = maze.length;
        int width = maze[0].length;

        // 上下边界
        for (int y = 0; y < BORDER_THICKNESS; y++) {
            for (int x = 0; x < width; x++) {
                maze[y][x] = 0;                     // 下
                maze[height - 1 - y][x] = 0;        // 上
            }
        }

        // 左右边界
        for (int x = 0; x < BORDER_THICKNESS; x++) {
            for (int y = 0; y < height; y++) {
                maze[y][x] = 0;                     // 左
                maze[y][width - 1 - x] = 0;         // 右
            }
        }

        Logger.debug("Applied outer border walls with thickness = " + BORDER_THICKNESS);
    }

    /**
     * 调整尺寸为cellGroup的倍数
     */
    private int adjustSize(int originalSize, int cellGroup) {
        int remainder = originalSize % cellGroup;
        if (remainder != 0) {
            int adjusted = originalSize + (cellGroup - remainder);
            Logger.debug("Adjusted size from " + originalSize + " to " + adjusted);
            return adjusted;
        }
        return originalSize;
    }

    /**
     * 生成基于3x3道路的DFS迷宫
     */
    private void generate3x3PathDFS(int[][] maze) {
        // 起始位置考虑墙的厚度
        int startX = BORDER_THICKNESS;
        int startY = BORDER_THICKNESS;

        Stack<int[]> stack = new Stack<>();
        stack.push(new int[]{startX, startY});

        // 标记起始3x3区域为通路
        set3x3AreaAsPath(maze, startX, startY);

        // 方向数组（每次移动 PATH_SIZE + WALL_SIZE 格）
        int horizontalStep = PATH_WIDTH + WALL_WIDTH;
        int verticalStep = PATH_HEIGHT + WALL_HEIGHT;

        int[][] directions = {
            {0, verticalStep},     // 上
            {horizontalStep, 0},   // 右
            {0, -verticalStep},    // 下
            {-horizontalStep, 0}   // 左
        };

        while (!stack.isEmpty()) {
            int[] current = stack.peek();
            int x = current[0];
            int y = current[1];

            // 获取未访问的3x3邻居
            ArrayList<int[]> unvisitedNeighbors = new ArrayList<>();

            for (int[] dir : directions) {
                int nx = x + dir[0];
                int ny = y + dir[1];

                // 检查是否在边界内且未被访问
                if (canCreate3x3Path(maze, nx, ny)) {
                    // 计算中间位置（墙区域）
                    int midX = x + dir[0] / 2;
                    int midY = y + dir[1] / 2;
                    unvisitedNeighbors.add(new int[]{nx, ny, midX, midY});
                }
            }

            if (!unvisitedNeighbors.isEmpty()) {
                // 随机选择一个邻居
                int[] neighbor = unvisitedNeighbors.get(random.nextInt(unvisitedNeighbors.size()));
                int nx = neighbor[0];
                int ny = neighbor[1];
                int midX = neighbor[2];
                int midY = neighbor[3];

                // 打通中间区域（当前是墙，需要变成3x3道路）
                set3x3AreaAsPath(maze, midX, midY);

                // 打通目标区域
                set3x3AreaAsPath(maze, nx, ny);

                stack.push(new int[]{nx, ny});
            } else {
                stack.pop();
            }
        }
    }

    /**
     * 检查是否可以在指定位置创建3x3通路
     */
    private boolean canCreate3x3Path(int[][] maze, int startX, int startY) {
        int width = maze[0].length;
        int height = maze.length;

        // 检查边界
        if (startX < BORDER_THICKNESS ||
                startY < BORDER_THICKNESS ||
                startX >= width - BORDER_THICKNESS - PATH_WIDTH ||
                startY >= height - BORDER_THICKNESS - PATH_HEIGHT) {
            return false;
        }

        // 检查3x3区域是否都是墙（未访问）
        for (int dy = 0; dy < PATH_HEIGHT; dy++) {
            for (int dx = 0; dx < PATH_WIDTH; dx++) {
                if (maze[startY + dy][startX + dx] != 0) {
                    return false;
                }
            }
        }

        return true;
    }

    /**
     * 设置3x3区域为通路
     */
    private void set3x3AreaAsPath(int[][] maze, int startX, int startY) {
        for (int dy = 0; dy < PATH_HEIGHT; dy++) {
            for (int dx = 0; dx < PATH_WIDTH; dx++) {
                if (startY + dy < maze.length && startX + dx < maze[0].length) {
                    maze[startY + dy][startX + dx] = 1;
                }
            }
        }
    }



    /**
     * 确保起点和终点的3x3区域
     */
    private void ensureStartEnd3x3Areas(int[][] maze) {
        int width = maze[0].length;
        int height = maze.length;

        // 起点区域（左下角）
        set3x3AreaAsPath(maze, WALL_WIDTH, WALL_HEIGHT);

        // 终点区域（右上角）
        int endX = width - WALL_WIDTH - PATH_WIDTH;
        int endY = height - WALL_HEIGHT - PATH_HEIGHT;
        set3x3AreaAsPath(maze, endX, endY);

        // 确保起点和终点有通路连接
//        ensurePathToStartEnd(maze);
        ensureBoundaryWalls(maze);
    }
    /**
     * 确保边界是墙 - 修复右上角问题
     */
    private void ensureBoundaryWalls(int[][] maze) {
        int width = maze[0].length;
        int height = maze.length;

        // 四周设置墙
        for (int x = 0; x < width; x++) {
            maze[0][x] = 0; // 上边界
            maze[height - 1][x] = 0; // 下边界
        }
        for (int y = 0; y < height; y++) {
            maze[y][0] = 0; // 左边界
            maze[y][width - 1] = 0; // 右边界
        }

        // 修复：特别确保右上角区域是完整的墙
        // 右上角区域（width-3, height-3）到（width-1, height-1）应该是墙
        for (int y = height - 3; y < height; y++) {
            for (int x = width - 3; x < width; x++) {
                if (y >= 0 && x >= 0) {
                    maze[y][x] = 0;
                }
            }
        }

        Logger.debug("Ensured boundary walls including fixed top-right corner");
    }

    /**
     * 确保通往起点和终点的路径
     */
    private void ensurePathToStartEnd(int[][] maze) {
        int width = maze[0].length;
        int height = maze.length;

        // 起点区域
        int startX = WALL_WIDTH;
        int startY = WALL_HEIGHT;

        // 确保起点至少有两个方向可走
        int startCenterX = startX + PATH_WIDTH / 2;
        int startCenterY = startY + PATH_HEIGHT / 2;

        // 右方向
        if (startCenterX + 1 < width) {
            for (int x = startCenterX; x <= startCenterX + 2; x++) {
                if (startCenterY < height) {
                    maze[startCenterY][x] = 1;
                }
            }
        }

        // 上方向
        if (startCenterY + 1 < height) {
            for (int y = startCenterY; y <= startCenterY + 2; y++) {
                if (startCenterX < width) {
                    maze[y][startCenterX] = 1;
                }
            }
        }

        // 终点区域
        int endX = width - WALL_WIDTH - PATH_WIDTH;
        int endY = height - WALL_HEIGHT - PATH_HEIGHT;
        int endCenterX = endX + PATH_WIDTH / 2;
        int endCenterY = endY + PATH_HEIGHT / 2;

        // 左方向
        if (endCenterX - 1 >= 0) {
            for (int x = endCenterX - 2; x <= endCenterX; x++) {
                if (endCenterY < height) {
                    maze[endCenterY][x] = 1;
                }
            }
        }

        // 下方向
        if (endCenterY - 1 >= 0) {
            for (int y = endCenterY - 2; y <= endCenterY; y++) {
                if (endCenterX < width) {
                    maze[y][endCenterX] = 1;
                }
            }
        }
    }

    /**
     * 为3x3迷宫添加额外通路
     */
    private void add3x3AdditionalPaths(int[][] maze, float chance) {
        int width = maze[0].length;
        int height = maze.length;
        int pathsAdded = 0;

        int horizontalStep = PATH_WIDTH + WALL_WIDTH;
        int verticalStep = PATH_HEIGHT + WALL_HEIGHT;

        for (int y = verticalStep; y < height - verticalStep; y += verticalStep) {
            for (int x = horizontalStep; x < width - horizontalStep; x += horizontalStep) {
                // 检查这个位置是否是墙区域
                if (is1x2WallArea(maze, x, y)) {
                    // 检查周围是否有足够的通路
                    int adjacentPaths = 0;

                    // 检查各个方向
                    int[][] checkDirs = {
                        {0, -verticalStep}, {0, verticalStep},
                        {-horizontalStep, 0}, {horizontalStep, 0}
                    };

                    for (int[] dir : checkDirs) {
                        int checkX = x + dir[0];
                        int checkY = y + dir[1];

                        if (checkX >= 0 && checkX < width - PATH_WIDTH &&
                            checkY >= 0 && checkY < height - PATH_HEIGHT &&
                            is3x3PathArea(maze, checkX, checkY)) {
                            adjacentPaths++;
                        }
                    }

                    // 如果连接两个以上的通路区域，有几率打通
                    if (adjacentPaths >= 2 && random.nextFloat() < chance) {
                        // 将这个1x2墙区域打通（变成3x3道路的一部分）
                        convertWallToPath(maze, x, y);
                        pathsAdded++;
                    }
                }
            }
        }

        Logger.debug("Added " + pathsAdded + " additional 3x3 paths");
    }

    /**
     * 检查是否是1x2墙区域
     */
    private boolean is1x2WallArea(int[][] maze, int startX, int startY) {
        for (int dy = 0; dy < WALL_HEIGHT; dy++) {
            for (int dx = 0; dx < WALL_WIDTH; dx++) {
                if (startY + dy < maze.length && startX + dx < maze[0].length) {
                    if (maze[startY + dy][startX + dx] != 0) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    /**
     * 检查是否是3x3通路区域
     */
    private boolean is3x3PathArea(int[][] maze, int startX, int startY) {
        for (int dy = 0; dy < PATH_HEIGHT; dy++) {
            for (int dx = 0; dx < PATH_WIDTH; dx++) {
                if (startY + dy >= maze.length || startX + dx >= maze[0].length ||
                    maze[startY + dy][startX + dx] != 1) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * 将墙区域转换为道路
     */
    private void convertWallToPath(int[][] maze, int wallX, int wallY) {
        // 将墙区域扩展为3x3道路
        int pathX = Math.max(0, wallX - 1);
        int pathY = Math.max(0, wallY - 1);
        set3x3AreaAsPath(maze, pathX, pathY);
    }

    /**
     * 验证3x3迷宫的连通性
     */
    private void validate3x3Maze(int[][] maze) {
        int width = maze[0].length;
        int height = maze.length;

        // 起点和终点坐标
        int startX = WALL_WIDTH + 1;  // 起点区域的中心
        int startY = WALL_HEIGHT + 1;
        int endX = width - WALL_WIDTH - PATH_WIDTH + 1;
        int endY = height - WALL_HEIGHT - PATH_HEIGHT + 1;

        if (!isPathReachable(maze, startX, startY, endX, endY)) {
            Logger.warning("3x3 Maze may not be fully connected, fixing problematic areas");
            fix3x3MazeConnectivity(maze);
        }
    }

    /**
     * 修复3x3迷宫的连通性
     */
    private void fix3x3MazeConnectivity(int[][] maze) {
        int width = maze[0].length;
        int height = maze.length;

        int horizontalStep = PATH_WIDTH + WALL_WIDTH;
        int verticalStep = PATH_HEIGHT + WALL_HEIGHT;

        // 尝试打通一些关键路径
        for (int y = verticalStep; y < height - verticalStep; y += verticalStep) {
            for (int x = horizontalStep; x < width - horizontalStep; x += horizontalStep) {
                if (is1x2WallArea(maze, x, y)) {
                    // 检查是否连接多个区域
                    int connectedRegions = 0;

                    int[][] checkDirs = {
                        {0, -verticalStep}, {0, verticalStep},
                        {-horizontalStep, 0}, {horizontalStep, 0}
                    };

                    for (int[] dir : checkDirs) {
                        int nx = x + dir[0];
                        int ny = y + dir[1];

                        if (nx >= 0 && nx < width - PATH_WIDTH &&
                            ny >= 0 && ny < height - PATH_HEIGHT &&
                            is3x3PathArea(maze, nx, ny)) {
                            connectedRegions++;
                        }
                    }

                    if (connectedRegions >= 2) {
                        convertWallToPath(maze, x, y);
                        Logger.debug("Fixed 3x3 connectivity at (" + x + ", " + y + ")");
                        return;
                    }
                }
            }
        }
    }

    /**
     * 清理孤立的小墙块
     */
    private void cleanupSmallWalls(int[][] maze) {
        int width = maze[0].length;
        int height = maze.length;
        int cleaned = 0;

        for (int y = 1; y < height - 1; y++) {
            for (int x = 1; x < width - 1; x++) {
                if (maze[y][x] == 0) {
                    // 检查是否是孤立的小墙块
                    int pathNeighbors = 0;

                    if (maze[y-1][x] == 1) pathNeighbors++;
                    if (maze[y+1][x] == 1) pathNeighbors++;
                    if (maze[y][x-1] == 1) pathNeighbors++;
                    if (maze[y][x+1] == 1) pathNeighbors++;

                    // 如果小墙块被道路包围，清理它
                    if (pathNeighbors >= 3 && random.nextFloat() < 0.7f) {
                        maze[y][x] = 1;
                        cleaned++;
                    }
                }
            }
        }

        if (cleaned > 0) {
            Logger.debug("Cleaned " + cleaned + " small isolated walls");
        }
    }

    private boolean isPathReachable(int[][] maze, int startX, int startY, int endX, int endY) {
        int width = maze[0].length;
        int height = maze.length;

        boolean[][] visited = new boolean[height][width];
        java.util.Queue<int[]> queue = new java.util.LinkedList<>();

        queue.offer(new int[]{startX, startY});
        visited[startY][startX] = true;

        int[][] directions = {{0, 1}, {1, 0}, {0, -1}, {-1, 0}};

        while (!queue.isEmpty()) {
            int[] current = queue.poll();
            int x = current[0];
            int y = current[1];

            if (x == endX && y == endY) {
                return true;
            }

            for (int[] dir : directions) {
                int nx = x + dir[0];
                int ny = y + dir[1];

                if (nx >= 0 && nx < width &&
                    ny >= 0 && ny < height &&
                    !visited[ny][nx] && maze[ny][nx] == 1) {
                    visited[ny][nx] = true;
                    queue.offer(new int[]{nx, ny});
                }
            }
        }

        return false;
    }

    public static boolean isValidPosition(int[][] maze, int x, int y) {
        if (x < 0 || x >= maze[0].length || y < 0 || y >= maze.length) {
            return false;
        }

        // 对于3x3道路，检查当前位置为中心的3x3区域
        int pathCount = 0;
        for (int dy = -1; dy <= 1; dy++) {
            for (int dx = -1; dx <= 1; dx++) {
                int nx = x + dx;
                int ny = y + dy;
                if (nx >= 0 && nx < maze[0].length && ny >= 0 && ny < maze.length) {
                    if (maze[ny][nx] == 1) {
                        pathCount++;
                    }
                }
            }
        }

        // 至少需要大部分区域是通路
        return pathCount >= 5; // 3x3区域中至少5个是通路
    }

    public static void printMazeForDebug(int[][] maze) {
        StringBuilder sb = new StringBuilder();
        sb.append("\n=== 3x3 MAZE DEBUG ===\n");

        // 打印坐标轴
        sb.append("   ");
        for (int x = 0; x < Math.min(maze[0].length, 50); x++) {
            sb.append(x % 10);
        }
        sb.append("\n");

        for (int y = maze.length - 1; y >= Math.max(0, maze.length - 30); y--) {
            sb.append(String.format("%2d ", y));
            for (int x = 0; x < Math.min(maze[0].length, 50); x++) {
                // 用不同符号表示不同元素
                if (maze[y][x] == 1) {
                    // 通路：根据周围情况显示
                    int neighbors = 0;
                    if (y > 0 && maze[y-1][x] == 1) neighbors++;
                    if (y < maze.length-1 && maze[y+1][x] == 1) neighbors++;
                    if (x > 0 && maze[y][x-1] == 1) neighbors++;
                    if (x < maze[0].length-1 && maze[y][x+1] == 1) neighbors++;

                    if (neighbors >= 3) {
                        sb.append("╋"); // 十字路口
                    } else if (neighbors == 2) {
                        sb.append("━"); // 直路
                    } else {
                        sb.append("·"); // 端点
                    }
                } else {
                    // 墙：检查是否是高墙（1x2）
                    boolean isTallWall = false;
                    if (y > 0 && maze[y-1][x] == 0) isTallWall = true;
                    if (y < maze.length-1 && maze[y+1][x] == 0) isTallWall = true;

                    if (isTallWall) {
                        sb.append("█"); // 高墙
                    } else {
                        sb.append("░"); // 普通墙
                    }
                }
            }
            sb.append("\n");
        }
        sb.append("=======================\n");
        Logger.debug(sb.toString());
    }
}
