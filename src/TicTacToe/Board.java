package TicTacToe;

import framework.IBoard;
import framework.MoveList;

import java.util.Random;

public class Board implements IBoard {
    // Zobrist stuff
    private static long[][][] zbnums = null;
    private static long crossHash, naughtHash;
    private final char P1_SIGN = 'X', P2_SIGN = 'O';
    int nMoves = 0;
    private long zbHash;
    //
    private int playerToMove = P1;
    private int winner = NONE_WIN;
    private int[][] board;
    private boolean[][] seen;
    private int size = 3;

    public void setSize(int size) {
        this.size = size;
    }

    @Override
    public int checkWin() {
        int win = 0;
        seen = new boolean[size][size];
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                if (!seen[i][j] && board[i][j] != 0) {
                    win = checkWin(i, j, board[i][j]);
                    if (win != 0)
                        return win;
                    seen[i][j] = true;
                }
            }
        }
        if (nMoves == (size * size))
            return DRAW;
        return win;
    }

    private int checkWin(int i, int j, int pl) {
        int c = 1;
        for (int k = i + 1; k < size; k++) {
            if (board[k][j] == pl) {
                c++;
                if (c == size)
                    return pl;
            } else
                break;
        }
        c = 1;
        for (int k = i + 1, l = j + 1; k < size && l < size; k++, l++) {
            if (board[k][l] == pl) {
                c++;
                if (c == size)
                    return pl;
            } else
                break;
        }

        c = 1;
        for (int k = i + 1, l = j - 1; k < size && l >= 0; k++, l--) {
            if (board[k][l] == pl) {
                c++;
                if (c == size)
                    return pl;
            } else
                break;
        }

        c = 1;
        for (int l = j + 1; l < size; l++) {
            if (board[i][l] == pl) {
                c++;
                if (c == size)
                    return pl;
            } else
                break;
        }
        return 0;
    }

    @Override
    public int getPlayerToMove() {
        return playerToMove;
    }

    @Override
    public void initialize() {
        board = new int[size][size];
        winner = NONE_WIN;
        playerToMove = P1;
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
            crossHash = rng.nextLong();
            naughtHash = rng.nextLong();
        }
        // now build the initial hash
        zbHash = 0;
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                zbHash ^= zbnums[i][j][0];
            }
        }
        // Hash in the first player
        zbHash ^= crossHash;
    }

    @Override
    public long hash() {
        return zbHash;
    }

    @Override
    public IBoard clone() {
        Board b = new Board();
        b.board = new int[size][size];
        b.size = size;

        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                b.board[i][j] = board[i][j];
            }
        }

        b.winner = this.winner;
        b.playerToMove = this.playerToMove;
        b.zbHash = zbHash;
        b.nMoves = nMoves;
        return b;
    }

    @Override
    public void doMove(int[] move) {
        assert board[move[0]][move[1]] == 0 : "Illegal move.";
        board[move[0]][move[1]] = playerToMove;

        zbHash ^= zbnums[move[0]][move[1]][0]; // Hash out the empty
        zbHash ^= zbnums[move[0]][move[1]][playerToMove]; // Hash in the player

        // Switch players
        if (playerToMove == P1) {
            zbHash ^= crossHash;
            zbHash ^= naughtHash;
        } else {
            zbHash ^= naughtHash;
            zbHash ^= crossHash;
        }

        playerToMove = 3 - playerToMove;
        nMoves++;
    }

    @Override
    public double evaluate(int player) { // WARN This will only work for larger boards
        int maxMe = 0, maxOpp = 0, n = 0;
        seen = new boolean[size][size];
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                if (!seen[i][j] && board[i][j] != 0) {
                    n = checkLongest(i, j, player);
                    if (board[i][j] == player) {
                        if (n > maxMe)
                            maxMe = n;
                    } else {
                        if (n > maxOpp)
                            maxOpp = n;
                    }
                }
            }
        }

        // Correct for the player with less pieces on the board
        if (playerToMove == player)
            maxMe++;
        else
            maxOpp++;

        return maxMe - maxOpp;
    }

    private int checkLongest(int i, int j, int pl) {
        int c = 1, max = 1;
        for (int k = i + 1; k < size; k++) {
            if (board[k][j] == pl) {
                c++;
                if (c > max)
                    max = c;
            } else
                break;
        }
        c = 1;
        for (int k = i + 1, l = j + 1; k < size && l < size; k++, l++) {
            if (board[k][l] == pl) {
                c++;
                if (c > max)
                    max = c;
            } else
                break;
        }
        c = 1;
        for (int l = j + 1; l < size; l++) {
            if (board[i][l] == pl) {
                c++;
                if (c > max)
                    max = c;
            } else
                break;

        }
        return max;
    }


    @Override
    public MoveList getPlayoutMoves(boolean heuristics) {
        return getMoves();
    }

    @Override
    public MoveList getExpandMoves() {
        return getMoves();
    }

    @Override
    public int getMoveId(int[] move) {
        return move[0];
    }

    @Override
    public int getMaxMoveId() {
        return size;
    }

    private MoveList getMoves() {
        MoveList moveList = new MoveList(size * size);
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                if (board[i][j] == 0)
                    moveList.add(i, j);
            }
        }
        return moveList;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("  ");
        for (int i = 0; i < size; i++) {
            sb.append(i + 1).append(" ");
        }
        sb.append("\n");
        for (int i = 0; i < size; i++) {
            sb.append((char) (97 + i)).append(" ");
            for (int j = 0; j < size; j++) {

                if (board[i][j] == 0)
                    sb.append(" ");
                if (board[i][j] == 1)
                    sb.append(P1_SIGN);
                if (board[i][j] == 2)
                    sb.append(P2_SIGN);

                if (j < size - 1)
                    sb.append("|");
            }
            sb.append("\n");
        }
        return sb.toString();
    }

    @Override
    public String getMoveString(int[] move) {
        return (char) (97 + move[0]) + "" + (move[1] + 1);
    }
}