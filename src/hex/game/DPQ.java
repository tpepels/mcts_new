package hex.game;

import java.util.Arrays;
import java.util.PriorityQueue;

public class DPQ {
    static final int INF = 99999;
    static final int[] xn = {-1, -1, 0, 0, +1, +1}, yn = {-1, 0, -1, +1, +1, 0};
    public int dist[][];
    private long[][] seen;
    private PriorityQueue<Node> pq;
    private int V, player;
    private long seenI = Long.MIN_VALUE;

    public DPQ(int V) {
        this.V = V;
        seen = new long[V][V];
        pq = new PriorityQueue<>(V);
    }

    // Function for Dijkstra's Algorithm
    public void dijkstra(int[][] board, int[] src, int player) {
        pq.clear();
        dist = new int[V][V];
        this.player = player;
        seenI++;
        for (int i = 0; i < V; i++)
            Arrays.fill(dist[i], INF);
        int initDist = (board[src[0]][src[1]] == player) ? 0 : ((board[src[0]][src[1]] == 0) ? 1 : INF);
        // Add source node to the priority queue
        pq.add(new Node(src, initDist));
        // Distance to the source is 0
        dist[src[0]][src[1]] = initDist;
        while (!pq.isEmpty()) {
            // remove the minimum distance node from the priority queue
            int[] u = pq.remove().node;
            // adding the node whose distance is finalized
            seen[u[0]][u[1]] = seenI;
            e_Neighbours(board, u);
        }
    }

    // Function to process all the neighbours of the passed node
    private void e_Neighbours(int[][] board, int[] u) {
        int newDistance, x, y;
        boolean playerpos = board[u[0]][u[1]] == player;
        for (int i = 0; i < xn.length; i++) {
            x = xn[i] + u[0];
            y = yn[i] + u[1];

            if (!inBounds(x, y))
                continue;

            if (board[x][y] != player && board[x][y] != 0)
                continue;

            if (seen[x][y] != seenI) {
                newDistance = dist[u[0]][u[1]] + ((board[x][y] == player && playerpos) ? 0 : 1);
                dist[x][y] = Math.min(newDistance, dist[x][y]);
                pq.add(new Node(new int[]{x, y}, dist[x][y]));
            }
        }
    }

    private boolean inBounds(int r, int c) {
        return (r >= 0 && c >= 0 && r < V && c < V);
    }

    public static void main(String args[]) {
        int[][] board ={{0,0,0,0,0},
                        {1,0,0,0,1},
                        {1,0,0,0,1},
                        {1,0,0,1,0},
                        {0,0,0,0,0}};
        DPQ dijk = new DPQ(board.length);
        dijk.dijkstra(board, new int[]{1,0}, 1);

        for(int i = 0; i < dijk.dist.length; i++) {
            for(int j = 0; j < dijk.dist[i].length; j++) {
                if(dijk.dist[i][j] == DPQ.INF)
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
    public int[] node;
    public int cost;

    public Node(int[] node, int cost) {
        this.node = node;
        this.cost = cost;
    }

    @Override
    public boolean equals(Object obj) {
        return node[0] == ((Node)obj).node[0] && node[1] == ((Node)obj).node[1];
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

