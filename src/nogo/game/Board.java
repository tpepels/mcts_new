package nogo.game;

import framework.IBoard;
import framework.MoveList;

import java.util.Random;

public class Board implements IBoard {
    private MoveList moveList;
    static long[][] zbnums = null;
    static long blackHash, whiteHash;
    //
    public int[][] board, liberty;
    public int nMoves = 0, cPlayer = P1, winner, size;
    private long zbHash = 0;
    private long[][] seen;
    private long seenI = Long.MIN_VALUE;

    public Board(int size) {
        this.size = size;
        this.board = new int[size][size];
        this.liberty = new int[size][size];
        this.moveList = new MoveList(size * size);
        this.seen = new long[size][size];
    }

    @Override
    public void initialize() {
        nMoves = 0;
        cPlayer = 1;
        winner = 0;

        // initialize the zobrist numbers
        if (zbnums == null) {
            // init the zobrist numbers
            Random rng = new Random();

            // SIZE locations, 3 states for each location
            zbnums = new long[size * size][3];

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
        for (int r = 0; r < size * size; r++) {
            zbHash ^= zbnums[r][0];
        }
        //
        cPlayer = P1;
        zbHash ^= blackHash;
    }

    @Override
    public MoveList getPlayoutMoves(boolean heuristics) {
        return getExpandMoves();
    }

    @Override
    public MoveList getExpandMoves() {
        moveList.clear();
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                if (board[y][x] == 0) {
                    moveList.add(x, y);
                }
            }
        }
        return moveList;
    }

    @Override
    public IBoard clone() {
        Board newBoard = new Board(size);
        newBoard.nMoves = nMoves;
        newBoard.cPlayer = cPlayer;
        newBoard.winner = winner;
        newBoard.zbHash = zbHash;
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                newBoard.board[j][i] = board[j][i];
                newBoard.liberty[j][i] = liberty[j][i];
            }
        }

        return newBoard;
    }

    @Override
    public String getMoveString(int[] move) {
        char cc = (char) (move[0] + 97);
        return String.format("%c%d", cc, move[1]);
    }

    @Override
    public int checkWin() {
        if (winner == P1 || winner == P2)
            return winner;
        if (nMoves == size * size)
            return DRAW;
        else
            return NONE_WIN;
    }

    @Override
    public int getPlayerToMove() {
        return cPlayer;
    }

    private void hashCurrentPlayer() {
        if (cPlayer == Board.P2) {
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
        assert winner == NONE_WIN : "Game already decided";

        if (winner != NONE_WIN)
            return;

        int pos = (move[1] * size) + move[0];
        zbHash ^= zbnums[pos][0];
        board[move[1]][move[0]] = cPlayer;
        seenI++;

        int lib = updateOppLiberty(move[0], move[1], 3 - cPlayer, size * size);
        if (lib == 0)
            winner = cPlayer;
        else { // In case of suicide capture, the capture counts, not the suicide
            lib = updateGroup(move[0], move[1]);
            updateMyLiberty(move[0], move[1], lib, cPlayer);
            if (lib == 0)
                winner = (3 - cPlayer);
        }
        seenI++;

        zbHash ^= zbnums[pos][cPlayer];
        cPlayer = (3 - cPlayer);
        hashCurrentPlayer();
        nMoves++;
    }

    private int updateOppLiberty(int x, int y, int color, int min) {
        if (board[y][x] == color) {
            liberty[y][x]--;
            seen[y][x] = seenI;
            min = Math.min(min, liberty[y][x]);
        }

        if (x + 1 < size && seen[y][x + 1] != seenI && board[y][x + 1] == color)
            min = Math.min(min, updateOppLiberty(x + 1, y, color, min));
        if (x - 1 >= 0 && seen[y][x - 1] != seenI && board[y][x - 1] == color)
            min = Math.min(min, updateOppLiberty(x - 1, y, color, min));
        if (y + 1 < size && seen[y + 1][x] != seenI && board[y + 1][x] == color)
            min = Math.min(min, updateOppLiberty(x, y + 1, color, min));
        if (y - 1 >= 0 && seen[y - 1][x] != seenI && board[y - 1][x] == color)
            min = Math.min(min, updateOppLiberty(x, y - 1, color, min));

        return min;
    }

    private void updateMyLiberty(int x, int y, int liberty, int color) {
        this.liberty[y][x] = liberty;
        seen[y][x]--; // reduce my seenindex so I don't get visited twice. Because in this method we're updating all positions belonging to the group with seenI
        if (x + 1 < size && seen[y][x + 1] == seenI && board[y][x + 1] == color) {
            updateMyLiberty(x + 1, y, liberty, color);
        }
        if (x - 1 >= 0 && seen[y][x - 1] == seenI && board[y][x - 1] == color) {
            updateMyLiberty(x - 1, y, liberty, color);
        }
        if (y + 1 < size && seen[y + 1][x] == seenI && board[y + 1][x] == color) {
            updateMyLiberty(x, y + 1, liberty, color);
        }
        if (y - 1 >= 0 && seen[y - 1][x] == seenI && board[y - 1][x] == color) {
            updateMyLiberty(x, y - 1, liberty, color);
        }
    }

    private int updateGroup(int x, int y) {
        seen[y][x] = seenI;
        int color = board[y][x], liberty = 0;

        if (x + 1 < size && seen[y][x + 1] != seenI && board[y][x + 1] == 0) {
            liberty++;
            seen[y][x + 1] = seenI;
        }
        if (x - 1 >= 0 && seen[y][x - 1] != seenI && board[y][x - 1] == 0) {
            liberty++;
            seen[y][x - 1] = seenI;
        }
        if (y + 1 < size && seen[y + 1][x] != seenI && board[y + 1][x] == 0) {
            liberty++;
            seen[y + 1][x] = seenI;
        }
        if (y - 1 >= 0 && seen[y - 1][x] != seenI && board[y - 1][x] == 0) {
            liberty++;
            seen[y - 1][x] = seenI;
        }

        if (x + 1 < size && seen[y][x + 1] != seenI && board[y][x + 1] == color) {
            liberty += updateGroup(x + 1, y);
        }
        if (x - 1 >= 0 && seen[y][x - 1] != seenI && board[y][x - 1] == color) {
            liberty += updateGroup(x - 1, y);
        }
        if (y + 1 < size && seen[y + 1][x] != seenI && board[y + 1][x] == color) {
            liberty += updateGroup(x, y + 1);
        }
        if (y - 1 >= 0 && seen[y - 1][x] != seenI && board[y - 1][x] == color) {
            liberty += updateGroup(x, y - 1);
        }

        return liberty;
    }

    @Override
    public int evaluate(int player) {
        if (nMoves < 2)
            return 0;
        int[] minLiberty = {100, 100};
        int pl, lib;
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                if (board[y][x] != 0) {
                    pl = board[y][x];
                    lib = liberty[y][x];
                    minLiberty[pl - 1] = Math.min(minLiberty[pl - 1], lib);
                }
            }
        }

        if (player == 1)
            return minLiberty[0] - minLiberty[1];
        else
            return minLiberty[1] - minLiberty[0];
    }

    @Override
    public String toString() {
        String str = "";
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                if (board[y][x] == 0)
                    str += ".";
                if (board[y][x] == P1)
                    str += "b";
                if (board[y][x] == P2)
                    str += "w";
            }
            str += "\n\n";
        }
        str += "p1Eval: " + evaluate(0) + " p2Eval: " + evaluate(1);
        return str;
    }
}
