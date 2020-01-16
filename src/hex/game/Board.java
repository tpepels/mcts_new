package hex.game;

import framework.IBoard;
import framework.MoveList;

import java.util.Random;

public class Board implements IBoard {
    // Zobrist stuff
    private static long[][][] zbnums = null;
    private static long p1Hash, p2Hash;
    //
    public int[][] board;
    private long zbHash;
    private int size, currentPlayer, winner, nMoves;
    private MoveList moveList;
    public boolean realPlay = true;
    private final DPQ dpq;

    public Board(int size) {
        this.size = size;
        moveList = new MoveList(size * size);
        dpq = new DPQ(size);
    }

    @Override
    public void initialize() {
        board = new int[size][size];
        winner = NONE_WIN;
        currentPlayer = P1;
        nMoves = 0;

        // initialize the zobrist numbers
        if (zbnums == null) {
            // init the zobrist numbers
            Random rng = new Random();
            // 64 locations, 3 states for each location = 192
            zbnums = new long[size][size][3];
            for (int i = 0; i < size; i++) {
                for (int j = 0; j < size; j++) {
                    zbnums[i][j][0] = rng.nextLong();
                    zbnums[i][j][1] = rng.nextLong();
                    zbnums[i][j][2] = rng.nextLong();
                }
            }
            p1Hash = rng.nextLong();
            p2Hash = rng.nextLong();
        }
        // now build the initial hash
        zbHash = 0;
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                zbHash ^= zbnums[i][j][0];
            }
        }
        // Hash in the first player
        zbHash ^= p1Hash;
    }

    @Override
    public void doMove(int[] move) {
        if (winner != NONE_WIN) {
            return;
        }

        zbHash ^= zbnums[move[0]][move[1]][0]; // Hash out the empty
        zbHash ^= zbnums[move[0]][move[1]][currentPlayer]; // Hash in the player

        //perform the actual move on the board
        board[move[0]][move[1]] = currentPlayer;
        nMoves++;
        // In the real game
        if(!playout)
            winner = checkWinAfterMove();

        if (winner == NONE_WIN) {
            // Switch players
            if (currentPlayer == P1) {
                zbHash ^= p1Hash;
                zbHash ^= p2Hash;
            } else {
                zbHash ^= p2Hash;
                zbHash ^= p1Hash;
            }
            currentPlayer = 3 - currentPlayer;
        }
    }

    @Override
    public double evaluate(int player) {
        if(nMoves < 4)
            return 0;
        return dpq.dijkstra(board, player) - dpq.dijkstra(board, 3 - player);
    }

    @Override
    public MoveList getPlayoutMoves(boolean heuristics) {
        return getExpandMoves();
    }

    @Override
    public MoveList getExpandMoves() {
        moveList.clear();
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                if (board[i][j] == 0)
                    moveList.add(i, j);
            }
        }
        return moveList;
    }

    @Override
    public int getMoveId(int[] move) {
        return move[0] + (size * move[1]);
    }

    @Override
    public int getMaxMoveId() {
        return size * size;
    }

    @Override
    public String getMoveString(int[] move) {
        return (char) (97 + move[0]) + "" + (move[1] + 1);
    }

    private int checkWinAfterMove() {
        if (winner != NONE_WIN)
            return winner;
        if (nMoves == (size * size))
            return DRAW;
        if (((nMoves + 2) / 2) < size)
            return NONE_WIN;

        if(dpq.dijkstra(board, currentPlayer) == 0)
            return currentPlayer;

        return NONE_WIN;
    }

    private boolean playout = false;

    @Override
    public void startPlayout() {
        playout = true;
    }

    public int checkWin() {
        if(playout) {
            if(nMoves == (size * size)) {

                if (dpq.dijkstra(board, 1) == 0)
                    return P1_WIN;

                if (dpq.dijkstra(board, 2) == 0)
                    return P2_WIN;

                return DRAW;
            }
        } else {
            if (winner != NONE_WIN)
                return winner;
            if (nMoves == (size * size))
                return DRAW;
            if (((nMoves + 2) / 2) < size)
                return NONE_WIN;
        }
        return NONE_WIN;
    }

    @Override
    public int getPlayerToMove() {
        return currentPlayer;
    }

    @Override
    public long hash() {
        return zbHash;
    }

    @Override
    public IBoard clone() {
        Board b = new Board(size);
        b.board = new int[size][size];
        b.size = size;
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                b.board[i][j] = board[i][j];
            }
        }
        b.winner = winner;
        b.currentPlayer = currentPlayer;
        b.zbHash = zbHash;
        b.nMoves = nMoves;
        b.realPlay = false; // for algorithms the board will be completely filled before checking for a win
        return b;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                if (board[i][j] != 0)
                    sb.append(board[i][j]);
                else
                    sb.append(" ");
            }
            sb.append("\n");
        }
        return sb.toString();
    }
}
