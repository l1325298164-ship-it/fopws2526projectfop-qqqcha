// MazeGenerator.java - 修复版本
package de.tum.cit.fop.maze.maze;


import de.tum.cit.fop.maze.game.GameConstants;
import de.tum.cit.fop.maze.utils.Logger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;
import java.util.Stack;

public class MazeGenerator {
    private Random random;

    public MazeGenerator() {
        random = new Random();
        Logger.debug("MazeGenerator initialized");
    }

    public int[][] generateMaze() {
        long startTime = System.currentTimeMillis();

       int[][] maze = new int[GameConstants.MAZE_HEIGHT][GameConstants.MAZE_WIDTH];

        // 初始化迷宫，全部设为墙
        for (int y = 0; y < GameConstants.MAZE_HEIGHT; y++) {
            Arrays.fill(maze[y], 0);
        }

        // 确保边界是墙
        for (int x = 0; x < GameConstants.MAZE_WIDTH; x++) {
            maze[0][x] = 0; // 上边界
            maze[GameConstants.MAZE_HEIGHT - 1][x] = 0; // 下边界
        }
        for (int y = 0; y < GameConstants.MAZE_HEIGHT; y++) {
            maze[y][0] = 0; // 左边界
            maze[y][GameConstants.MAZE_WIDTH - 1] = 0; // 右边界
        }

        // 使用改进的DFS生成迷宫
        generateDFS(maze,1, 1);

        // 确保起点和终点是通路
        maze[1][1] = 1;
        maze[GameConstants.MAZE_HEIGHT - 2][GameConstants.MAZE_WIDTH - 2] = 1;

        // 添加更多通路（防止死胡同）
        //可以在这里增加难度
        addAdditionalPaths(maze,0.6f);

        // 验证迷宫连通性
        validateMaze(maze);

        long endTime = System.currentTimeMillis();
        Logger.performance("Maze generated in " + (endTime - startTime) + "ms");
        Logger.debug("Maze generation completed - Size: " + GameConstants.MAZE_WIDTH + "x" + GameConstants.MAZE_HEIGHT);


        ensureTopWalls(maze);
        return maze;
    }

    private void generateDFS(int[][] maze,int startX, int startY) {
        Stack<int[]> stack = new Stack<>();
        stack.push(new int[]{startX, startY});
        maze[startY][startX] = 1;

        int[][] directions = {{0, 2}, {2, 0}, {0, -2}, {-2, 0}};

        while (!stack.isEmpty()) {
            int[] current = stack.peek();
            int x = current[0];
            int y = current[1];

            // 获取未访问的邻居
            ArrayList<int[]> unvisitedNeighbors = new ArrayList<>();

            for (int[] dir : directions) {
                int nx = x + dir[0];
                int ny = y + dir[1];

                if (nx > 0 && nx < GameConstants.MAZE_WIDTH - 1 &&
                    ny > 0 && ny < GameConstants.MAZE_HEIGHT - 1 &&
                    maze[ny][nx] == 0) {
                    unvisitedNeighbors.add(new int[]{nx, ny, dir[0], dir[1]});
                }
            }

            if (!unvisitedNeighbors.isEmpty()) {
                // 随机选择一个邻居
                int[] neighbor = unvisitedNeighbors.get(random.nextInt(unvisitedNeighbors.size()));
                int nx = neighbor[0];
                int ny = neighbor[1];
                int dx = neighbor[2];
                int dy = neighbor[3];

                // 打通中间的墙
                maze[y + dy / 2][x + dx / 2] = 1;
                maze[ny][nx] = 1;

                stack.push(new int[]{nx, ny});
            } else {
                stack.pop();
            }
        }
    }

    private void addAdditionalPaths(int[][] maze, float chance) {
        int pathsAdded = 0;

        for (int y = 1; y < GameConstants.MAZE_HEIGHT - 1; y++) {
            for (int x = 1; x < GameConstants.MAZE_WIDTH - 1; x++) {
                // 只处理墙
                if (maze[y][x] == 0) {
                    // 检查是否应该打通这面墙
                    int adjacentPaths = 0;

                    // 检查上下左右四个方向
                    if (maze[y-1][x] == 1) adjacentPaths++;
                    if (maze[y+1][x] == 1) adjacentPaths++;
                    if (maze[y][x-1] == 1) adjacentPaths++;
                    if (maze[y][x+1] == 1) adjacentPaths++;

                    // 如果这面墙连接两个通路，有几率打通
                    if (adjacentPaths >= 2 && random.nextFloat() < chance) {
                        maze[y][x] = 1;
                        pathsAdded++;
                    }
                }
            }
        }

        Logger.debug("Added " + pathsAdded + " additional paths");
    }

    private void validateMaze(int[][] maze) {
        // 简单验证：确保玩家起点可达
        if (!isPathReachable(maze, 1, 1, GameConstants.MAZE_WIDTH - 2, GameConstants.MAZE_HEIGHT - 2)) {
            Logger.warning("Maze may not be fully connected, regenerating problematic areas");
            fixMazeConnectivity(maze);
        }
    }

// MazeGenerator.java - 在添加额外墙层的代码处
    /**
     * 确保顶部有足够的墙层以解决渲染问题
     * 问题原因：相机视口/坐标系导致顶部墙可能显示不完全
     * 解决方案：在迷宫顶部添加额外墙层确保视觉完整性
     */
    private void ensureTopWalls(int[][] maze) {
        int topLayers = 2; // 要确保的墙层数

        for (int layer = 0; layer < topLayers; layer++) {
            int y = GameConstants.MAZE_HEIGHT - 1 - layer;
            for (int x = 0; x < GameConstants.MAZE_WIDTH; x++) {
                maze[y][x] = 0; // 强制设为墙
            }
        }

        Logger.debug("Ensured " + topLayers + " top wall layers");
    }

    private boolean isPathReachable(int[][] maze,int startX, int startY, int endX, int endY) {
        boolean[][] visited = new boolean[GameConstants.MAZE_HEIGHT][GameConstants.MAZE_WIDTH];
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

                if (nx >= 0 && nx < GameConstants.MAZE_WIDTH &&
                    ny >= 0 && ny < GameConstants.MAZE_HEIGHT &&
                    !visited[ny][nx] && maze[ny][nx] == 1) {
                    visited[ny][nx] = true;
                    queue.offer(new int[]{nx, ny});
                }
            }
        }

        return false;
    }

    private void fixMazeConnectivity(int[][] maze) {
        // 尝试打通一些关键路径
        for (int y = 1; y < GameConstants.MAZE_HEIGHT - 1; y++) {
            for (int x = 1; x < GameConstants.MAZE_WIDTH - 1; x++) {
                if (maze[y][x] == 0) {
                    // 检查是否连接两个区域
                    int connectedRegions = 0;
                    if (maze[y-1][x] == 1) connectedRegions++;
                    if (maze[y+1][x] == 1) connectedRegions++;
                    if (maze[y][x-1] == 1) connectedRegions++;
                    if (maze[y][x+1] == 1) connectedRegions++;

                    if (connectedRegions >= 2) {
                        maze[y][x] = 1;
                        Logger.debug("Fixed connectivity at (" + x + ", " + y + ")");
                        return;
                    }
                }
            }
        }
    }

    public static boolean isValidPosition(int[][] maze, int x, int y) {
        return x >= 0 && x < GameConstants.MAZE_WIDTH &&
            y >= 0 && y < GameConstants.MAZE_HEIGHT &&
            maze[y][x] == 1;
    }


    public static void printMazeForDebug(int[][] maze) {
        StringBuilder sb = new StringBuilder();
        sb.append("\n=== MAZE DEBUG ===\n");
        for (int y = GameConstants.MAZE_HEIGHT - 1; y >= 0; y--) {
            for (int x = 0; x < GameConstants.MAZE_WIDTH; x++) {
                sb.append(maze[y][x] == 1 ? "  " : "██");
            }
            sb.append("\n");
        }
        sb.append("=================\n");
        Logger.debug(sb.toString());
    }
}
