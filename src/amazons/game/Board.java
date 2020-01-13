package amazons.game;

import framework.IBoard;
import framework.MoveList;
import framework.Options;

import java.util.Arrays;
import java.util.Random;

public class Board implements IBoard {
    // The board size
    public static final int SIZE = 8, B_SIZE = SIZE * SIZE;
    public static final int EMPTY = 0, WHITE_Q = P1, BLACK_Q = P2, ARROW = 3;
    private static final int[][] initPositions = {{58, 61, 40, 47}, {2, 5, 16, 23}};
    private static final int[] ALL_MOVE_INT = {9, -9, 7, -7, 8, -8, -1, 1};
    // Zobrist stuff
    static long[][] zbnums = null;
    static long p1Hash, p2Hash;
    //
    public final int[][] queens = new int[2][4];
    public final int[] board, possibleMoves = new int[40], possibleShots = new int[40];
    // Movelists
    private final MoveList moves = new MoveList(2048, 3);
    private long zbHash = 0;
    private int nMoves = 0, winner = NONE_WIN;
    public int currentPlayer = P1;

    public Board() {
        board = new int[SIZE * SIZE];
        currentPlayer = P1;
    }

    @Override
    public void initialize() {
        nMoves = 0;
        Arrays.fill(board, EMPTY);

        if (zbnums == null) {
            // init the zobrist numbers
            Random rng = new Random();

            // 64 locations, 4 states for each location = 192
            zbnums = new long[SIZE * SIZE][4];

            for (int i = 0; i < zbnums.length; i++) {
                zbnums[i][0] = rng.nextLong();
                zbnums[i][1] = rng.nextLong();
                zbnums[i][2] = rng.nextLong();
                zbnums[i][3] = rng.nextLong();
            }

            p1Hash = rng.nextLong();
            p2Hash = rng.nextLong();
        }
        // now build the initial hash
        zbHash = 0;
        for (int r = 0; r < SIZE * SIZE; r++) {
            zbHash ^= zbnums[r][EMPTY];
        }
        currentPlayer = P1;
        zbHash ^= p1Hash;

        // Setup initial positions
        for (int i = 0; i < initPositions[0].length; i++) {
            board[initPositions[0][i]] = WHITE_Q * 10 + i;
            zbHash ^= zbnums[initPositions[0][i]][P1];
            queens[0][i] = initPositions[0][i];
            board[initPositions[1][i]] = BLACK_Q * 10 + i;
            zbHash ^= zbnums[initPositions[1][i]][P2];
            queens[1][i] = initPositions[1][i];
        }
    }

    @Override
    public Board clone() {
        Board newBoard = new Board();
        // Copy the board data
        System.arraycopy(board, 0, newBoard.board, 0, board.length);
        System.arraycopy(queens[0], 0, newBoard.queens[0], 0, queens[0].length);
        System.arraycopy(queens[1], 0, newBoard.queens[1], 0, queens[1].length);
        newBoard.currentPlayer = currentPlayer;
        newBoard.nMoves = nMoves;
        newBoard.winner = winner;
        newBoard.zbHash = zbHash;
        return newBoard;
    }

    @Override
    public MoveList getExpandMoves() {
        moves.clear();
        int from, moveCount, shotCount;
        for (int i = 0; i < queens[currentPlayer - 1].length; i++) {
            // Select the location to move from, ie the queen to move
            from = queens[currentPlayer - 1][i];
            moveCount = getPossibleMovesFrom(from, possibleMoves);
            // Move count holds the possible number of moves possible from this position
            for (int j = 0; j < moveCount; j++) {
                moveQueen(from, possibleMoves[j], currentPlayer); // WARN Here!
                // Iterate through the possible shots
                shotCount = getPossibleMovesFrom(possibleMoves[j], possibleShots);
                for (int k = 0; k < shotCount; k++) {
                    moves.add(from, possibleMoves[j], possibleShots[k]);
                }
                moveQueen(possibleMoves[j], from, currentPlayer); // TODO When is a queen moved in this method?
            }
        }
        return moves;
    }

    @Override
    public int getMoveId(int[] move) {
        return move[0] + 64 * move[1] + 128 * move[3];
    }

    @Override
    public int getMaxMoveId() {
        return 646465;
    }

    @Override
    public MoveList getPlayoutMoves(boolean heuristics) {
        moves.clear();
        int from, moveCount, shotCount;
        int start = Options.r.nextInt(4);
        int c = 0;
        while (moves.isEmpty() && c < 4) {
            // Select the location to move from, ie the queen to move
            from = queens[currentPlayer - 1][start % 4];
            moveCount = getPossibleMovesFrom(from, possibleMoves);
            // Move count holds the possible number of moves possible from this position
            for (int j = 0; j < moveCount; j++) {
                moveQueen(from, possibleMoves[j], currentPlayer);
                // Iterate through the possible shots
                shotCount = getPossibleMovesFrom(possibleMoves[j], possibleShots);
                for (int k = 0; k < shotCount; k++) {
                    moves.add(from, possibleMoves[j], possibleShots[k]);
                }
                moveQueen(possibleMoves[j], from, currentPlayer);
            }
            // Next queen, in case of no moves
            start++;
            c++;
        }
        return moves;
    }

    @Override
    public void doMove(int[] move) {
        // remove zobrist nums from hash of the squares that are changing
        zbHash ^= zbnums[move[0]][currentPlayer];
        zbHash ^= zbnums[move[1]][EMPTY];
        zbHash ^= zbnums[move[2]][EMPTY];

        board[move[1]] = board[move[0]];
        board[move[0]] = EMPTY;
        queens[currentPlayer - 1][board[move[1]] % 10] = move[1];
        // Shoot the arrow (after moving the queen!)
        board[move[2]] = ARROW;
        //
        // remove zobrist nums from hash of the squares that are changing
        zbHash ^= zbnums[move[0]][EMPTY];
        zbHash ^= zbnums[move[1]][currentPlayer];
        zbHash ^= zbnums[move[2]][ARROW];
        //
        if (currentPlayer == Board.P1) {
            zbHash ^= p1Hash;
            zbHash ^= p2Hash;
        } else {
            zbHash ^= p2Hash;
            zbHash ^= p1Hash;
        }
        currentPlayer = 3 - currentPlayer;
        nMoves++;
    }

    public int getPossibleMovesFrom(int from, int[] moves) {
        int count = 0, position;
        int col, row, direction, min, max;
        for (int i = 0; i < ALL_MOVE_INT.length; i++) {
            // Select a random direction.
            direction = ALL_MOVE_INT[i];
            col = from % SIZE;
            row = from / SIZE;
            //
            if (direction == -(SIZE + 1)) {
                min = from + (Math.min(col, row) * direction);
                max = (B_SIZE - 1);
            } else if (direction == (SIZE + 1)) {
                col = (SIZE - 1) - col;
                row = (SIZE - 1) - row;
                max = from + (Math.min(col, row) * direction);
                min = -1;
            } else if (direction == (SIZE - 1)) {
                row = (SIZE - 1) - row;
                max = from + (Math.min(col, row) * direction);
                min = -1;
            } else if (direction == -(SIZE - 1)) {
                col = (SIZE - 1) - col;
                min = from + (Math.min(col, row) * direction);
                max = B_SIZE;
            } else if (direction == SIZE || direction == -SIZE) {
                max = (B_SIZE - 1);
                min = 0;
            } else {
                max = row * SIZE + (SIZE - 1);
                min = row * SIZE;
            }

            position = from + direction;
            // Select a position along the chosen direction
            while (position <= max && position >= min
                    && board[position] == Board.EMPTY) {
                //
                if (moves != null)
                    moves[count] = position;

                count++;
                position += direction;
            }
        }
        // Returns the number of moves found
        return count;
    }

    private void moveQueen(int from, int to, int player) {
        board[to] = board[from];
        board[from] = EMPTY;
        queens[player - 1][board[to] % 10] = to;
    }

    @Override
    public String getMoveString(int[] move) {
        return move[0] + (char) (97 + move[1]) + "A" + move[2];
    }

    @Override
    public int checkWin() {
        boolean[] can = {false, false};
        for (int i = P1; i <= P2; i++) {
            for (int j = 0; j < queens[i - 1].length; j++) {
                // Check if player can make a move from position.
                if (canMakeMoveFrom(queens[i - 1][j])) {
                    can[i - 1] = true;
                    break;
                }
            }
        }
        if (!can[0]) {
            winner = P2_WIN;
            return P2_WIN;
        }
        if (!can[1]) {
            winner = P1_WIN;
            return P1_WIN;
        }
        winner = NONE_WIN;
        return NONE_WIN;
    }

    private boolean canMakeMoveFrom(int from) {
        int col, row, direction, min, max, position;
        for (int i = 0; i < ALL_MOVE_INT.length; i++) {
            // Select a random direction.
            direction = ALL_MOVE_INT[i];
            col = from % SIZE;
            row = from / SIZE;
            //
            if (direction == -(SIZE + 1)) {
                min = from + (Math.min(col, row) * direction);
                max = (B_SIZE - 1);
            } else if (direction == (SIZE + 1)) {
                col = (SIZE - 1) - col;
                row = (SIZE - 1) - row;
                max = from + (Math.min(col, row) * direction);
                min = -1;
            } else if (direction == (SIZE - 1)) {
                row = (SIZE - 1) - row;
                max = from + (Math.min(col, row) * direction);
                min = -1;
            } else if (direction == -(SIZE - 1)) {
                col = (SIZE - 1) - col;
                min = from + (Math.min(col, row) * direction);
                max = B_SIZE;
            } else if (direction == SIZE || direction == -SIZE) {
                max = (B_SIZE - 1);
                min = 0;
            } else {
                max = row * SIZE + (SIZE - 1);
                min = row * SIZE;
            }

            position = from + direction;
            // Select a position along the chosen direction
            if (position <= max && position >= min
                    && board[position] == Board.EMPTY) {
                //
                return true;
            }
        }
        return false;
    }

    @Override
    public double evaluate(int player) {
        double count = getFreedom(player) - getFreedom(3 - player);
        // The more available moves the player has, the better
        return count / (16. * (nMoves / 4.0));  // 16. assumes endgame
    }

    private int getFreedom(int player) {
        int from, total = 0;
        for (int i = 0; i < queens[player - 1].length; i++) {
            // Select the location to move from, ie the queen to move
            from = queens[player - 1][i];
            total += getPossibleMovesFrom(from, null);
        }
        return total;
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
    public String toString() {
        String str = "";
        for (int r = 0; r < SIZE; r++) {
            for (int c = 0; c < SIZE; c++) {
                int x = r * SIZE + c;
                if (board[x] / 10 == WHITE_Q)
                    str += "W";
                else if (board[x] / 10 == BLACK_Q)
                    str += "B";
                else if (board[x] / 10 == ARROW)
                    str += "a";
                else if (board[x] / 10 == EMPTY)
                    str += ".";
            }
            str += "\n";
        }
        return str;
    }
}
