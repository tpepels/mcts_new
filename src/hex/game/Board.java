package hex.game;

import framework.IBoard;
import framework.MoveList;

import java.util.ArrayList;
import java.util.Random;

public class Board implements IBoard {
    // Zobrist stuff
    private static long[][][] zbnums = null;
    private static long p1Hash, p2Hash;
    private long zbHash;
    //
    public int[][] board;
    private long[][] seen;
    private long seenI = Long.MIN_VALUE;
    private int size, currentPlayer, winner, nMoves;
    private MoveList moveList;

    public Board(int size) {
        this.size = size;
        moveList = new MoveList(size*size);
    }

    @Override
    public void initialize() {
        board = new int[size][size];
        seen = new long[size][size];
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
        checkWin();
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
    public int evaluate(int player) {
        return 0;
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

    public int checkWin() {

        if (winner != NONE_WIN)
            return winner;

        if (nMoves == (size * size))
            return DRAW;

        if (((nMoves + 2) / 2) < size)
            return NONE_WIN;


        int[][] floodMap = new int[size][size];

        int c = 1;
        seenI++;
        // Player 1 plays along the x axis, 2 along y
        for (int x = 0; x < size; x++) {
            for (int y = 0; y < size; y++) {
                if (floodMap[x][y] == 0 && board[x][y] == currentPlayer) {
                    floodMap = floodFill(x, y, c, floodMap);
                    c++;
                }
            }
        }
        boolean[] beginList = new boolean[c];

        if (currentPlayer == 1) {
            for (int y = 0; y < size; y++) {
                if (floodMap[0][y] > 0 && !beginList[floodMap[0][y]]) {
                    beginList[floodMap[0][y]] = true;
                }
            }
            for (int y = 0; y < size; y++) {
                if (floodMap[size - 1][y] > 0 && beginList[floodMap[size - 1][y]]) {
                    winner = P1_WIN;
                    return P1_WIN;
                }
            }
        } else {
            for (int x = 0; x < size; x++) {
                if (floodMap[x][0] > 0 && !beginList[floodMap[x][0]]) {
                    beginList[floodMap[x][0]] = true;
                }
            }

            for (int x = 0; x < size; x++) {
                if (floodMap[x][size - 1] > 0 && beginList[floodMap[x][size - 1]]) {
                    winner = P2_WIN;
                    return P2_WIN;
                }
            }
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
        b.seen = new long[size][size];
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
        return b;
    }

    private int[][] floodFill(int x, int y, int value, int[][] floodMap) {
        floodMap[x][y] = value;
        seen[x][y] = seenI;
        //check whether the connected hexagons exist, have not been visited
        //already and are occupied by the player. If so, continue the recursion
        //on those fields.
        if (x > 0 && y > 0 && seen[x - 1][y - 1] != seenI && board[x - 1][y - 1] == currentPlayer) {
            floodMap = floodFill(x - 1, y - 1, value, floodMap);
        }
        if (x > 0 && seen[x - 1][y] != seenI && board[x - 1][y] == currentPlayer) {
            floodMap = floodFill(x - 1, y, value, floodMap);
        }
        if (y > 0 && seen[x][y - 1] != seenI && board[x][y - 1] == currentPlayer) {
            floodMap = floodFill(x, y - 1, value, floodMap);
        }
        if (x < (size - 1) && y < (size - 1) && seen[x + 1][y + 1] != seenI && board[x + 1][y + 1] == currentPlayer) {
            floodMap = floodFill(x + 1, y + 1, value, floodMap);
        }
        if (x < (size - 1) && seen[x + 1][y] != seenI && board[x + 1][y] == currentPlayer) {
            floodMap = floodFill(x + 1, y, value, floodMap);
        }
        if (y < (size - 1) && seen[x][y + 1] != seenI && board[x][y + 1] == currentPlayer) {
            floodMap = floodFill(x, y + 1, value, floodMap);
        }
        return floodMap;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                if(board[i][j] != 0)
                sb.append(board[i][j]);
                else
                    sb.append(" ");
            }
            sb.append("\n");
        }
        return sb.toString();
    }
}
