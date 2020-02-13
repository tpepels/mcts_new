package hex.game;

import java.util.Arrays;

public class ShortestPath {
    static final int INF = 99999;
    static final int[] xn = {-1, -1, 0, 0, +1, +1}, yn = {-1, 0, -1, +1, +1, 0};
    public int[][] dist;
    private long[][] seen;
    private int V, player;
    private long seenI = Long.MIN_VALUE;

    public ShortestPath(int V) {
        this.V = V;
        seen = new long[V][V];
        dist = new int[V][V];
    }

    // Function for Dijkstra's Algorithm
    public int dijkstra(int[][] board, int player) {
        this.player = player;
        seenI++;

        for (int i = 0; i < V; i++)
            Arrays.fill(dist[i], INF);

        int count = 0;
        if (player == 2) {
            for (int i = 0; i < V; i++) {
                if (board[i][0] == 3 - player)
                    continue;
                dist[i][0] = (board[i][0] == player) ? 0 : 1;
                count++;
            }
        } else {
            for (int i = 0; i < V; i++) {
                if (board[0][i] == 3 - player)
                    continue;
                dist[0][i] = (board[0][i] == player) ? 0 : 1;
                count++;
            }
        }

        int min = INF;
        int[] n;
        while (count < V * V) {
            // remove the minimum distance node from the priority queue
            n = minDistance();
            // adding the node whose distance is finalized
            seen[n[0]][n[1]] = seenI;
            e_Neighbours(board, n[0], n[1]);

            count++;

            if (player == 2 && n[1] == V - 1) {
                min = Math.min(dist[n[0]][n[1]], min);
            } else if (player == 1 && n[0] == V - 1) {
                min = Math.min(dist[n[0]][n[1]], min);
            }
        }
        return min;
    }

    private int[] minDistance() {
        // Initialize min value
        int min = Integer.MAX_VALUE;
        int[] min_node = new int[2];

        for (int x = 0; x < V; x++) {
            for (int y = 0; y < V; y++) {
                if (seen[x][y] != seenI && dist[x][y] < min) {
                    min = dist[x][y];
                    min_node[0] = x;
                    min_node[1] = y;
                }
            }
        }
        return min_node;
    }

    // Function to process all the neighbours of the passed node
    private void e_Neighbours(int[][] board, int xs, int ys) {
        int newDistance, x, y;
        for (int i = 0; i < xn.length; i++) {
            x = xn[i] + xs;
            y = yn[i] + ys;

            if (!inBounds(x, y) || seen[x][y] == seenI || board[x][y] == (3 - player))
                continue;

            newDistance = dist[xs][ys] + ((board[x][y] == player) ? 0 : 1);

            if (newDistance < dist[x][y])
                dist[x][y] = newDistance;
        }
    }

    private boolean inBounds(int r, int c) {
        return (r >= 0 && c >= 0 && r < V && c < V);
    }

    public static void main(String args[]) {
        int[][] board = {{0, 1, 0, 0, 0},
                {0, 1, 0, 1, 1},
                {0, 1, 2, 0, 1},
                {0, 2, 1, 1, 0},
                {0, 0, 0, 0, 0}};
        ShortestPath dijk = new ShortestPath(board.length);
        System.out.println(dijk.dijkstra(board, 2));

        for (int i = 0; i < dijk.dist.length; i++) {
            for (int j = 0; j < dijk.dist[i].length; j++) {
                if (dijk.dist[i][j] == ShortestPath.INF)
                    System.out.print("i");
                else
                    System.out.print(dijk.dist[i][j]);
            }
            System.out.println();
        }
    }
}

