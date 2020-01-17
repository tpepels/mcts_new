package gomoku.game;

import java.util.Arrays;
import java.util.PriorityQueue;

public class DPQ {
    static final int INF = 99999;
    static final int[] xn = {-1, -1, 0, 0, +1, +1, +1, -1}, yn = {-1, 0, -1, +1, +1, 0, -1, +1};
    public int dist[][];
    private long[][] seen;
    private PriorityQueue<Node> pq;
    private int V, player;
    private long seenI = Long.MIN_VALUE;

    public DPQ(int V) {
        this.V = V;
        seen = new long[V][V];
        pq = new PriorityQueue<>(V);
        dist = new int[V][V];
    }

    // Function for Dijkstra's Algorithm
    public int dijkstra(int[][] board, int player) {
        this.player = player;
        seenI++;
        for (int i = 0; i < V; i++)
            Arrays.fill(dist[i], INF);
        pq.clear();
        int p = board.length / 2;
        dist[p][p] = 0;
        // Add source node to the priority queue
        pq.add(new Node(p, p, dist[p][p]));
        int min = 0;
        Node n;
        while (!pq.isEmpty()) {
            // remove the minimum distance node from the priority queue
            n = pq.remove();
            // adding the node whose distance is finalized
            seen[n.x][n.y] = seenI;
            e_Neighbours(board, n.x, n.y);
            if (n.x == 0 || n.y == 0 || n.x == V - 1 || n.y == V - 1)
                min += dist[n.x][n.y];
        }
        return min;
    }

    // Function to process all the neighbours of the passed node
    private void e_Neighbours(int[][] board, int xs, int ys) {
        int newDistance, x, y;

        for (int i = 0; i < xn.length; i++) {

            x = xn[i] + xs;
            y = yn[i] + ys;

            if (!inBounds(x, y) || seen[x][y] == seenI)
                continue;
            if (board[x][y] != player && board[x][y] != 0)
                continue;
            newDistance = dist[xs][ys] + ((board[x][y] == player) ? 0 : 1);
            if (newDistance < dist[x][y]) {
                dist[x][y] = newDistance;
                Node n = new Node(x, y, dist[x][y]);

                if (!pq.contains(n))
                    pq.add(n);
            }
        }
    }

    private boolean inBounds(int r, int c) {
        return (r >= 0 && c >= 0 && r < V && c < V);
    }

    public static void main(String args[]) {
        int[][] board = {{0, 0, 0, 0, 0, 0},
                {1, 0, 1, 1, 1, 0},
                {0, 1, 2, 2, 1, 1},
                {1, 0, 1, 1, 2, 2},
                {0, 0, 0, 0, 2, 2},
                {0, 0, 0, 0, 2, 1}};
        DPQ dijk = new DPQ(board.length);
        System.out.println(dijk.dijkstra(board, 1));
        System.out.println(dijk.dijkstra(board, 2));
        System.out.println(Math.pow(board.length, 2) + Math.pow(board.length-2, 2));
        for (int i = 0; i < dijk.dist.length; i++) {
            for (int j = 0; j < dijk.dist[i].length; j++) {
                if (dijk.dist[i][j] == DPQ.INF)
                    System.out.print("i");
                else
                    System.out.print(dijk.dist[i][j]);
            }
            System.out.println();
        }
    }
}

// Class to represent a node in the graph
class Node implements Comparable<Node> {
    public int x, y;
    public int cost;

    public Node(int x, int y, int cost) {
        this.x = x;
        this.y = y;
        this.cost = cost;
    }

    @Override
    public boolean equals(Object obj) {
        return x == ((Node) obj).x && y == ((Node) obj).y;
    }

    @Override
    public int compareTo(Node node) {
        if (cost < node.cost)
            return -1;
        if (cost > node.cost)
            return 1;
        return 0;
    }
}
