package nogo.game;

import framework.IBoard;
import framework.MoveList;

import java.util.Random;

public class Board implements IBoard {
    public static int SIZE = 10, EMPTY = 0, BLACK = P1, WHITE = P2;
    private static MoveList moveList = new MoveList(SIZE * SIZE);
    //
    private int[][] board, freedom;
    private int nMoves = 0, currentPlayer = BLACK;

    static long[][] zbnums = null;
    static long blackHash, whiteHash;
    private long zbHash = 0;

    public Board() {
        this.board = new int[SIZE][SIZE];
    }

    @Override
    public void initialize() {
        nMoves = 0;
        currentPlayer = BLACK;
        for (int i = 0; i < SIZE; i++) {
            for (int j = 0; j < SIZE; j++) {
                board[i][j] = EMPTY;
            }
        }
        // initialize the zobrist numbers
        if (zbnums == null) {
            // init the zobrist numbers
            Random rng = new Random();

            // SIZE locations, 3 states for each location
            zbnums = new long[SIZE * SIZE][3];

            for (int i = 0; i < zbnums.length; i++) {
                zbnums[i][0] = rng.nextLong();
                zbnums[i][1] = rng.nextLong();
                zbnums[i][2] = rng.nextLong();
            }

            whiteHash = rng.nextLong();
            blackHash = rng.nextLong();
        }
        // now build the initial hash
        zbHash = 0;
        for (int r = 0; r < SIZE * SIZE; r++) {
            zbHash ^= zbnums[r][EMPTY];
        }
        // Initial position
        board[(SIZE / 2)-1][(SIZE / 2)-1] = P1;
        board[(SIZE / 2)][(SIZE / 2)-1] = P2;
        board[(SIZE / 2)-1][(SIZE / 2)] = P2;
        board[(SIZE / 2)][(SIZE / 2)] = P1;
        //
        currentPlayer = P1;
        zbHash ^= blackHash;
    }

    @Override
    public MoveList getPlayoutMoves(boolean heuristics) {
        return getExpandMoves();
    }

    @Override
    public MoveList getExpandMoves() {
        boolean free;
        int opp = 3 - currentPlayer;
        moveList.clear();
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                if (board[y][x] == EMPTY) {
                    board[y][x] = currentPlayer;
                    seenI++;
                    // Check the freedom of the position
                    free = checkFree(x, y, currentPlayer);
                    // Check the freedom of the surrounding positions
                    if (free && x + 1 < SIZE && seen[y][x + 1] != seenI && board[y][x + 1] == opp) {
                        free = checkFree(x + 1, y, board[y][x + 1]);
                    }
                    if (free && x - 1 >= 0 && seen[y][x - 1] != seenI && board[y][x - 1] == opp) {
                        free = checkFree(x - 1, y, board[y][x - 1]);
                    }
                    if (free && y + 1 < SIZE && board[y + 1][x] == opp) {
                        free = checkFree(x, y + 1, board[y + 1][x]);
                    }
                    if (free && y - 1 >= 0 && seen[y - 1][x] != seenI && board[y - 1][x] == opp) {
                        free = checkFree(x, y - 1, board[y - 1][x]);
                    }
                    if (free)
                        moveList.add(x, y);
                    board[y][x] = EMPTY;
                }
            }
        }
        return moveList;
    }

    private long seen[][] = new long[SIZE][SIZE];
    private long seenI = Long.MIN_VALUE;

    private boolean checkFree(int x, int y, int color) {
        seen[y][x] = seenI;
        if (x + 1 < SIZE && board[y][x + 1] == EMPTY)
            return true;
        if (x - 1 >= 0 && board[y][x - 1] == EMPTY)
            return true;
        if (y + 1 < SIZE && board[y + 1][x] == EMPTY)
            return true;
        if (y - 1 >= 0 && board[y - 1][x] == EMPTY)
            return true;
        if (x + 1 < SIZE && seen[y][x + 1] != seenI && board[y][x + 1] == color)
            if (checkFree(x + 1, y, color))
                return true;
        if (x - 1 >= 0 && seen[y][x - 1] != seenI && board[y][x - 1] == color)
            if (checkFree(x - 1, y, color))
                return true;
        if (y + 1 < SIZE && seen[y + 1][x] != seenI && board[y + 1][x] == color)
            if (checkFree(x, y + 1, color))
                return true;
        if (y - 1 >= 0 && seen[y - 1][x] != seenI && board[y - 1][x] == color)
            if (checkFree(x, y - 1, color))
                return true;

        return false;
    }

    private boolean countFree(int x, int y, int color) {
        seen[y][x] = seenI;
        if (x + 1 < SIZE && board[y][x + 1] == EMPTY)
            return true;
        if (x - 1 >= 0 && board[y][x - 1] == EMPTY)
            return true;
        if (y + 1 < SIZE && board[y + 1][x] == EMPTY)
            return true;
        if (y - 1 >= 0 && board[y - 1][x] == EMPTY)
            return true;
        if (x + 1 < SIZE && seen[y][x + 1] != seenI && board[y][x + 1] == color)
            if (checkFree(x + 1, y, color))
                return true;
        if (x - 1 >= 0 && seen[y][x - 1] != seenI && board[y][x - 1] == color)
            if (checkFree(x - 1, y, color))
                return true;
        if (y + 1 < SIZE && seen[y + 1][x] != seenI && board[y + 1][x] == color)
            if (checkFree(x, y + 1, color))
                return true;
        if (y - 1 >= 0 && seen[y - 1][x] != seenI && board[y - 1][x] == color)
            if (checkFree(x, y - 1, color))
                return true;

        return false;
    }


    @Override
    public IBoard clone() {
        Board newBoard = new Board();
        newBoard.nMoves = nMoves;
        newBoard.currentPlayer = currentPlayer;
        for (int i = 0; i < SIZE; i++) {
            for (int j = 0; j < SIZE; j++) {
                newBoard.board[i][j] = board[i][j];
            }
        }
        newBoard.zbHash = zbHash;
        return newBoard;
    }

    @Override
    public String getMoveString(int[] move) {
        char cc = (char) (move[0] + 97);
        return String.format("%c%d", cc, move[1]);
    }

    @Override
    public int checkWin() {
        getPlayoutMoves(false);
        if (moveList.isEmpty())
            return 3 - currentPlayer;
        else
            return NONE_WIN;
    }

    @Override
    public int getPlayerToMove() {
        return currentPlayer;
    }

    private void hashCurrentPlayer() {
        if (currentPlayer == Board.P2) {
            zbHash ^= blackHash;
            zbHash ^= whiteHash;
        } else {
            zbHash ^= whiteHash;
            zbHash ^= blackHash;
        }
    }

    @Override
    public long hash() {
        return zbHash;
    }

    @Override
    public void doMove(int[] move) {
        int pos = (move[1] * SIZE) + move[0];
        zbHash ^= zbnums[pos][EMPTY];
        board[move[1]][move[0]] = currentPlayer;
        zbHash ^= zbnums[pos][currentPlayer];
        currentPlayer = 3 - currentPlayer;
        hashCurrentPlayer();
        nMoves++;
    }

    @Override
    public int evaluate(int player) {
        boolean free;
        int opp = 3 - currentPlayer;
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                if (board[y][x] == EMPTY) {
                    board[y][x] = currentPlayer;
                    seenI++;
                    // Check the freedom of the position
                    free = checkFree(x, y, currentPlayer);
                    // Check the freedom of the surrounding positions
                    if (free && x + 1 < SIZE && seen[y][x + 1] != seenI && board[y][x + 1] == opp) {
                        free = checkFree(x + 1, y, board[y][x + 1]);
                    }
                    if (free && x - 1 >= 0 && seen[y][x - 1] != seenI && board[y][x - 1] == opp) {
                        free = checkFree(x - 1, y, board[y][x - 1]);
                    }
                    if (free && y + 1 < SIZE && board[y + 1][x] == opp) {
                        free = checkFree(x, y + 1, board[y + 1][x]);
                    }
                    if (free && y - 1 >= 0 && seen[y - 1][x] != seenI && board[y - 1][x] == opp) {
                        free = checkFree(x, y - 1, board[y - 1][x]);
                    }
                    board[y][x] = EMPTY;
                }
            }
        }
    }

    @Override
    public String toString() {
        String str = "";
        for (int r = 0; r < SIZE; r++) {
            for (int c = 0; c < SIZE; c++) {
                if (board[r][c] == EMPTY)
                    str += ".";
                if (board[r][c] == BLACK)
                    str += "b";
                if (board[r][c] == WHITE)
                    str += "w";
            }
            str += "\n";
        }
        return str;
    }
}
