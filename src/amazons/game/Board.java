package amazons.game;

import framework.IBoard;
import framework.MoveList;

import java.util.Random;

public class Board implements IBoard {
    // The board size
    public static final int SIZE = 8, B_SIZE = SIZE * SIZE, N_QUEENS = 4;
    // Board occupants
    public static final int EMPTY = 0, WHITE_Q = P1, BLACK_Q = P2, ARROW = 3;
    // Zobrist stuff
    static long[][] zbnums = null;
    static long whiteHash, blackHash;
    private long zbHash = 0;
    //
    private static final Random r = new Random();
    private static final MoveList moves = new MoveList(128);
    private static final MoveList playoutMoves = new MoveList(128);
    // Initial queen positions
    private static final int[][] initPositions = {{58, 61, 40, 47}, {2, 5, 16, 23}};
    public final int[][] queens = new int[2][4];
    // Board is public for fast access
    public final int[] board;
    public int[] bcopy;
    private final int[] ALL_MOVE_INT = {9, -9, 7, -7, 8, -8, -1, 1};
    private final int[] possibleMoves = new int[40], possibleShots = new int[40];
    private int nMoves = 0;
    private int currentPlayer;
    private int winner = NONE_WIN;

    /**
     * Initialise the board using the default size
     */
    public Board() {
        board = new int[SIZE * SIZE];
        bcopy = new int[SIZE * SIZE];
        currentPlayer = P1;
    }

    @Override
    public Board clone() {
        Board newBoard = new Board();
        // Copy the board data
        System.arraycopy(board, 0, newBoard.board, 0, board.length);
        System.arraycopy(queens[0], 0, newBoard.queens[0], 0,
                queens[0].length);
        System.arraycopy(queens[1], 0, newBoard.queens[1], 0,
                queens[1].length);
        newBoard.currentPlayer = currentPlayer;
        newBoard.nMoves = nMoves;
        newBoard.winner = winner;
        newBoard.bcopy = new int[SIZE * SIZE];
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
                undoQueenMove(currentPlayer); // TODO When is a queen moved in this method?
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
        playoutMoves.clear();
        int from, moveCount, shotCount;
        if (heuristics) {
            int start = r.nextInt(N_QUEENS);
            int c = 0;
            while (playoutMoves.isEmpty() && c < N_QUEENS) {
                // Select the location to move from, ie the queen to move
                from = queens[currentPlayer - 1][start];
                moveCount = getPossibleMovesFrom(from, possibleMoves);
                // Move count holds the possible number of moves possible from this position
                for (int j = 0; j < moveCount; j++) {
                    moveQueen(from, possibleMoves[j], currentPlayer); // WARN Here
                    // Iterate through the possible shots
                    shotCount = getPossibleMovesFrom(possibleMoves[j], possibleShots);
                    for (int k = 0; k < shotCount; k++) {
                        playoutMoves.add(new Move(from, possibleMoves[j], possibleShots[k]));
                    }
                    undoQueenMove(currentPlayer); // TODO When is a queen moved here?
                }
                // Next queen, in case of no moves
                start = (start == N_QUEENS - 1) ? 0 : start + 1;
                c++;
            }
        }
        //
        if (playoutMoves.isEmpty()) {
            for (int i = 0; i < queens[currentPlayer - 1].length; i++) {
                // Select the location to move from, ie the queen to move
                from = queens[currentPlayer - 1][i];
                moveCount = getPossibleMovesFrom(from, possibleMoves);
                // Move count holds the possible number of moves possible from this position
                for (int j = 0; j < moveCount; j++) {
                    moveQueen(from, possibleMoves[j], currentPlayer); // WARN Here
                    // Iterate through the possible shots
                    shotCount = getPossibleMovesFrom(possibleMoves[j], possibleShots);
                    for (int k = 0; k < shotCount; k++) {
                        playoutMoves.add(new Move(from, possibleMoves[j], possibleShots[k]));
                    }
                    undoQueenMove(currentPlayer); // TODO When is a queen moved here?
                }
            }
        }
        return playoutMoves;
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
        currentPlayer = 3 - currentPlayer;
        hashCurrentPlayer();
        nMoves++;
    }

    private void moveQueen(int from, int to, int player) {
        board[to] = board[from];
        board[from] = EMPTY;
        queens[player - 1][board[to] % 10] = to;
    }

    @Override
    public String getMoveString(int[] move) {
        return null;
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

    @Override
    public int getPlayerToMove() {
        return currentPlayer;
    }

    @Override
    public void initialize() {
        nMoves = 0;
        for (int i = 0; i < board.length; i++) {
            board[i] = EMPTY;
        }

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

            whiteHash = rng.nextLong();
            blackHash = rng.nextLong();
        }
        // now build the initial hash
        zbHash = 0;
        for (int r = 0; r < SIZE * SIZE; r++) {
            zbHash ^= zbnums[r][EMPTY];
        }
        currentPlayer = P1;
        zbHash ^= whiteHash;

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

    private void hashCurrentPlayer() {
        if (currentPlayer == Board.P1) {
            zbHash ^= blackHash;
            zbHash ^= whiteHash;
        } else {
            zbHash ^= whiteHash;
            zbHash ^= blackHash;
        }
    }

    private int getZbId(int p) {
        int id = p * 4;
        if (board[p] / 10 == WHITE_Q)
            id += 1;
        else if (board[p] / 10 == BLACK_Q)
            id += 2;
        else if (board[p] == ARROW)
            id += 3;
        return id;
    }

    public String toString() {
        System.arraycopy(board, 0, bcopy, 0, board.length);

        for (int i = 0; i < 4; i++)
            bcopy[queens[0][i]] = WHITE_Q;

        for (int i = 0; i < 4; i++)
            bcopy[queens[1][i]] = BLACK_Q;

        String str = "";

        for (int r = 0; r < SIZE; r++) {
            for (int c = 0; c < SIZE; c++) {

                int x = r * SIZE + c;

                if (bcopy[x] == WHITE_Q)
                    str += "W";
                else if (bcopy[x] == BLACK_Q)
                    str += "B";
                else if (bcopy[x] == ARROW)
                    str += "a";
                else if (bcopy[x] == EMPTY)
                    str += ".";
                else {
                    //str += "?";
                    System.out.println("WTF!" + bcopy[x]);
                    System.exit(-1);
                }
            }

            str += "\n";
        }

        return str;
    }

    @Override
    public int evaluate(int player) {

        if (true) { // Todo wtf is this?
            double val = evaluate_eggcelent(player);
            return Math.tanh(val);
        }
        if (player > 2) //TODO wtf is this?
            return evaluate_tree(player - 2);

        // clear bcopy
        System.arraycopy(board, 0, bcopy, 0, board.length);

        for (int i = 0; i < 4; i++)
            bcopy[queens[0][i]] = WHITE_Q;

        for (int i = 0; i < 4; i++)
            bcopy[queens[1][i]] = BLACK_Q;

        boolean change = true;
        int pass = 0;

        int whiteCount = 0;
        int blackCount = 0;

        while (change) {
            change = false;
            pass++;

            whiteCount = 0;
            blackCount = 0;

            for (int r = 0; r < SIZE; r++)
                for (int c = 0; c < SIZE; c++) {
                    int x = r * SIZE + c;

                    if (bcopy[x] == WHITE_Q || (bcopy[x] >= 100 && bcopy[x] < 200))
                        whiteCount++;
                    if (bcopy[x] == BLACK_Q || (bcopy[x] >= 200 && bcopy[x] < 300))
                        blackCount++;

                    if (bcopy[x] == WHITE_Q
                            || bcopy[x] == BLACK_Q
                            || bcopy[x] == (100 + pass - 1)
                            || bcopy[x] == (200 + pass - 1)) {
                        int[] coords = new int[8];
                        coords[0] = (r - 1) * SIZE + c;
                        coords[1] = (r + 1) * SIZE + c;
                        coords[2] = r * SIZE + (c - 1);
                        coords[3] = r * SIZE + (c + 1);
                        coords[4] = (r - 1) * SIZE + (c - 1);
                        coords[5] = (r - 1) * SIZE + (c + 1);
                        coords[6] = (r + 1) * SIZE + (c - 1);
                        coords[7] = (r + 1) * SIZE + (c + 1);

                        int myInfluence = 0;

                        if (bcopy[x] == WHITE_Q || (bcopy[x] >= 100 && bcopy[x] < 200)) {
                            myInfluence = 100 + pass;
                        } else if (bcopy[x] == BLACK_Q || (bcopy[x] >= 200 && bcopy[x] < 300)) {
                            myInfluence = 200 + pass;
                        }
                        for (int dir = 0; dir < 8; dir++) {
                            int coord = coords[dir];
                            if (coord >= 0 && coord < SIZE * SIZE) {
                                if (bcopy[coord] == EMPTY) {
                                    bcopy[coord] = myInfluence;
                                    change = true;
                                } else if (bcopy[coord] == 100 + pass || bcopy[coord] == 200 + pass) {
                                    if (myInfluence == 100 + pass && bcopy[coord] == 200 + pass) {
                                        bcopy[coord] = 300;
                                        change = true;
                                    } else if (myInfluence == 200 + pass && bcopy[coord] == 100 + pass) {
                                        bcopy[coord] = 300;
                                        change = true;
                                    }
                                }
                            }
                        }
                    }
                }

        }
        double diff = whiteCount - blackCount;
        double p1eval = Math.tanh(diff / 10.0);

        if (player == 1)
            return p1eval;
        else
            return -p1eval;
    }

    public double evaluate_simple(int player, int version) {
        // clear bcopy
        //System.arraycopy(board, 0, bcopy, 0, board.length);

        for (int i = 0; i < 4; i++)
            board[queens[0][i]] = WHITE_Q;

        for (int i = 0; i < 4; i++)
            board[queens[1][i]] = BLACK_Q;

        int rad = 2;

        // count white blocks
        int whiteBlocks = 0, blackBlocks = 0;

        for (int i = 0; i < 4; i++) {
            int coord = queens[0][i];
            int r = coord / SIZE;
            int c = coord % SIZE;

            for (int rp = r - rad; rp <= r + rad; rp++)
                for (int cp = c - rad; cp <= c + rad; cp++) {
                    if (rp >= 0 && rp < SIZE && cp >= 0 && cp < SIZE) {
                        int x = rp * SIZE + cp;
                        if (board[x] != EMPTY)
                            whiteBlocks++;
                    }
                }
        }

        for (int i = 0; i < 4; i++) {
            int coord = queens[1][i];
            int r = coord / SIZE;
            int c = coord % SIZE;

            for (int rp = r - rad; rp <= r + rad; rp++)
                for (int cp = c - rad; cp <= c + rad; cp++) {
                    if (rp >= 0 && rp < SIZE && cp >= 0 && cp < SIZE) {
                        int x = rp * SIZE + cp;
                        if (board[x] != EMPTY)
                            blackBlocks++;
                    }
                }
        }

        for (int i = 0; i < 4; i++)
            board[queens[0][i]] = EMPTY;

        for (int i = 0; i < 4; i++)
            board[queens[1][i]] = EMPTY;

        double diff = blackBlocks - whiteBlocks;
        //System.out.println(diff);
        double p1eval = Math.tanh(diff / 5.0);
        if (player == 1)
            return p1eval;
        else
            return -p1eval;
    }

    public double evaluate_tree(int player) {
        double freediff = getFreedom(1) - getFreedom(2);
        //System.out.println(freediff);
        double p1eval = Math.tanh(freediff / 100.0);
        if (player == 1)
            return p1eval;
        else
            return -p1eval;
    }

    public double evaluate_eggcelent(int player) {
        double count = getFreedom(player) - getFreedom(3 - player);
        // The more available moves the winning player has, the better
        return count / (16. * (nMoves / 4.0));  // 16. assumes endgame
    }

    @Override
    public long hash() {
        return zbHash;
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
}
