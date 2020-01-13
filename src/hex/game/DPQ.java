package hex.game;

// Java implementation of Dijkstra's Algorithm
// using Priority Queue

import java.util.Arrays;
import java.util.PriorityQueue;

public class DPQ {
    static final int[] xn = {-1, -1, 0, 0, +1, +1}, yn = {-1, 0, -1, +1, +1, 0};
    private int dist[][];
    private boolean[][] seen;
    private PriorityQueue<Node> pq;
    private int V, nNodes, player, settled;

    public DPQ(int V) {
        this.nNodes = V * V;
        this.V = V;
        dist = new int[V][V];
        seen = new boolean[V][V];
        pq = new PriorityQueue<Node>(V);
    }

    // Function for Dijkstra's Algorithm
    public void dijkstra(int[][] board, int[] src) {

        player = board[src[0]][src[1]];

        for (int i = 0; i < Math.sqrt(V); i++)
            Arrays.fill(dist[i], Integer.MAX_VALUE);

        // Add source node to the priority queue
        pq.add(new Node(src, 0));

        // Distance to the source is 0
        dist[src[0]][src[1]] = 0;
        while (settled != nNodes) {

            // remove the minimum distance node
            // from the priority queue
            int[] u = pq.remove().node;

            // adding the node whose distance is
            // finalized
            seen[u[0]][u[1]] = true;
            settled++;
            e_Neighbours(board, u);
        }
    }

    // Function to process all the neighbours of the passed node
    private void e_Neighbours(int[][] board, int[] u) {
        int edgeDistance = -1, newDistance = -1;

        // All the neighbors of v
        for (int i = 0; i < 6; i++) {
            int x = xn[i] + u[0], y = yn[i] + u[1];
            if (!inBounds(x, y))
                continue;
            int occ = board[x][y];
            // If current node hasn't already been processed
            if (!seen[x][y]) {
                edgeDistance = (occ == player) ? 0 : (occ == 0) ? 1 : 99999;
                newDistance = dist[u[0]][u[1]] + edgeDistance;

                // If new distance is cheaper in cost
                if (newDistance < dist[x][y])
                    dist[x][y] = newDistance;

                // Add the current node to the queue
                pq.add(new Node(new int[]{x, y}, dist[x][y]));
            }
        }
    }


    private boolean inBounds(int r, int c) {
        return (r >= 0 && c >= 0 && r < V && c < V);
    }

    // Driver code
    public static void main(String arg[]) {
        int[][] nodes = new int[][]
                {{1, 0, 2, 2, 2},
                        {0, 1, 0, 0, 0},
                        {0, 0, 1, 0, 0},
                        {0, 0, 0, 0, 0},
                        {0, 0, 0, 0, 0}};


        DPQ dpq = new DPQ(5);
        dpq.dijkstra(nodes, new int[]{0, 0});

        // Print the shortest path to all the nodes
        // from the source node
        System.out.println("The shorted path from node :");
        for (int i = 0; i < dpq.dist.length; i++) {
            for (int j = 0; j < dpq.dist.length; j++) {
                if (dpq.dist[i][j] == Integer.MAX_VALUE)
                    System.out.print("i");
                else
                    System.out.print(dpq.dist[i][j]);
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
    public int compareTo(Node node) {
        if (cost < node.cost)
            return -1;
        if (cost > node.cost)
            return 1;
        return 0;
    }
}

