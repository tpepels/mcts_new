package gomoku.game;

import framework.IBoard;
import framework.MoveList;
import framework.Options;

import java.util.Random;

public class Board implements IBoard {
    static long[][] zbnums = null;
    static long blackHash, whiteHash;
    //
    public int[][] board;
    public int nMoves = 0, cPlayer = P1, winner, size;
    private MoveList moveList;
    private long zbHash = 0;

    public Board(int size) {
        this.size = size;
        this.board = new int[size][size];
        this.moveList = new MoveList(size * size * size);
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
    public int getMoveId(int[] move) {
        return move[0] + (move[1] * size);
    }

    @Override
    public int getMaxMoveId() {
        return size * size;
    }

    @Override
    public IBoard clone() {
        Board newBoard = new Board(size);
        newBoard.nMoves = nMoves;
        newBoard.cPlayer = cPlayer;
        newBoard.winner = winner;
        newBoard.zbHash = zbHash;
        for (int i = 0; i < size; i++)
            for (int j = 0; j < size; j++)
                newBoard.board[j][i] = board[j][i];
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
        int x = move[0], y = move[1];

        winner = checkWinAfterMove(x, y, cPlayer);
        zbHash ^= zbnums[pos][cPlayer];
        cPlayer = (3 - cPlayer);
        hashCurrentPlayer();
        nMoves++;
    }

    private int checkWinAfterMove(int i, int j, int pl) {
        int c = 1;
        // x - 1 y - 1
        for (int x = i - 1, y = j - 1; x > 0 && y > 0; y--, x--) {
            if (board[y][x] == pl) {
                c++;
                if (c == 5)
                    return pl;
            } else
                break;
        }
        // x + 1 y + 1
        for (int x = i + 1, y = j + 1; x < size && y < size; y++, x++) {
            if (board[y][x] == pl) {
                c++;
                if (c == 5)
                    return pl;
            } else
                break;
        }
        c = 1;
        // x + 1 y
        for (int x = i + 1; x < size; x++) {
            if (board[j][x] == pl) {
                c++;
                if (c == 5)
                    return pl;
            } else
                break;
        }
        // x - 1 y
        for (int x = i - 1; x > 0; x--) {
            if (board[j][x] == pl) {
                c++;
                if (c == 5)
                    return pl;
            } else
                break;
        }
        c = 1;
        // x y + 1
        for (int y = j + 1; y < size; y++) {
            if (board[y][i] == pl) {
                c++;
                if (c == 5)
                    return pl;
            } else
                break;
        }
        // x y - 1
        for (int y = j - 1; y > 0; y--) {
            if (board[y][i] == pl) {
                c++;
                if (c == 5)
                    return pl;
            } else
                break;
        }
        c = 1;
        // x + 1 y - 1
        for (int x = i + 1, y = j - 1; x < size && y > 0; y--, x++) {
            if (board[y][x] == pl) {
                c++;
                if (c == 5)
                    return pl;
            } else
                break;
        }
        // x - 1 y + 1
        for (int x = i - 1, y = j + 1; x > 0 && y < size; y++, x--) {
            if (board[y][x] == pl) {
                c++;
                if (c == 5)
                    return pl;
            } else
                break;
        }

        if (nMoves == size * size)
            return DRAW;

        return NONE_WIN;
    }

    @Override
    public double evaluate(int player) {
        if (nMoves < 2)
            return 0;

        double maxMe = 0, maxOpp = 0;
        double n;
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                if (board[y][x] != 0) {
                    n = checkLongest(x, y, board[y][x]);
                    if (board[y][x] == player) {
                        if (n > maxMe) {
                            maxMe = n;
                        }
                    } else {
                        if (n > maxOpp) {
                            maxOpp = n;
                        }
                    }
                }
            }
        }
        Options.maxEval = Math.max(Math.abs(maxMe - maxOpp), Options.maxEval);
        return Math.tanh((maxMe - maxOpp) / (0.5 * Options.maxEval));
    }

    private double checkLongest(int i, int j, int pl) {
        int c = 1, f = 0, max = 1;
        boolean stop = false;
        // x - 1 y - 1
        for (int x = i - 1, y = j - 1; x > 0 && y > 0; y--, x--) {
            if (!stop && board[y][x] == pl) {
                c++;
            } else if (board[y][x] == 0) {
                f++;
                stop = true;
            } else
                break;
        }
        stop = false;
        // x + 1 y + 1
        for (int x = i + 1, y = j + 1; x < size && y < size; y++, x++) {
            if (!stop && board[y][x] == pl) {
                c++;
            } else if (board[y][x] == 0) {
                f++;
                stop = true;
            } else
                break;
        }
        if(c + f >= 5)
            max = Math.max(c, max);
        stop = false;
        c = 1;
        f = 0;
        // x + 1 y
        for (int x = i + 1; x < size; x++) {
            if (!stop && board[j][x] == pl) {
                c++;
            } else if (board[j][x] == 0) {
                f++;
                stop = true;
            } else
                break;
        }
        stop = false;
        // x - 1 y
        for (int x = i - 1; x > 0; x--) {
            if (!stop && board[j][x] == pl) {
                c++;
            } else if (board[j][x] == 0) {
                f++;
                stop = true;
            } else
                break;
        }
        if(c + f >= 5)
            max = Math.max(c, max);
        stop = false;
        c = 1;
        f = 0;
        // x y + 1
        for (int y = j + 1; y < size; y++) {
            if (!stop && board[y][i] == pl) {
                c++;
            } else if (board[y][i] == 0) {
                f++;
                stop = true;
            } else
                break;
        }
        stop = false;
        // x y - 1
        for (int y = j - 1; y > 0; y--) {
            if (!stop && board[y][i] == pl) {
                c++;
            } else if (board[y][i] == 0) {
               f++;
               stop = true;
            } else
                break;
        }
        if(c + f >= 5)
            max = Math.max(c, max);
        stop = false;
        f = 0;
        c = 1;
        // x + 1 y - 1
        for (int x = i + 1, y = j - 1; x < size && y > 0; y--, x++) {
            if (!stop && board[y][x] == pl) {
                c++;
            } else if (board[y][x] == 0) {
                f++;
                stop = true;
            } else
                break;
        }
        stop = false;
        // x - 1 y + 1
        for (int x = i - 1, y = j + 1; x > 0 && y < size; y++, x--) {
            if (!stop && board[y][x] == pl) {
                c++;
            } else if (board[y][x] == 0) {
                f++;
                stop = true;
            } else
                break;
        }
        if(c + f >= 5)
            max = Math.max(c, max);
        
        return max;
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
