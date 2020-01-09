package pentalath.game;

import framework.IBoard;
import framework.MoveList;

import java.util.ArrayList;
import java.util.Random;

public class Board implements IBoard {
    // @formatter:off
    // This is only to check whether a cell is part of the board (1).
    public static final short[] occupancy =
            {0, 0, 1, 1, 1, 1, 1, 0, 0,
                    0, 1, 1, 1, 1, 1, 1, 0, 0,
                    0, 1, 1, 1, 1, 1, 1, 1, 0,
                    1, 1, 1, 1, 1, 1, 1, 1, 0,
                    1, 1, 1, 1, 1, 1, 1, 1, 1,
                    1, 1, 1, 1, 1, 1, 1, 1, 0,
                    0, 1, 1, 1, 1, 1, 1, 1, 0,
                    0, 1, 1, 1, 1, 1, 1, 0, 0,
                    0, 0, 1, 1, 1, 1, 1, 0, 0};
    // @formatter:on
    public static final int FREE = 0;
    public static final int SIZE = 81, REAL_SIZE = 61, WIDTH = 9;
    public static final int NUM_NEIGHBOURS = 6, ROW_SIZE = 5;
    public static final MoveList moves = new MoveList(REAL_SIZE);
    public static final MoveList poMoves = new MoveList(REAL_SIZE);
    //
    private static final int[] N_VECTOR_ODD = {-9, -8, +1, +10, +9, -1}, N_VECTOR_EVEN = {-10, -9, +1, +9, +8, -1};

    // set the bit in this position to 1 if a stone is black.
    private final int BLACK_BIT = 128;
    private final int P_INF = 2000000;
    public Field[] board;
    // Hashing stuff
    private static long[][] zobristPositions;
    private static long whiteHash, blackHash;
    private long zobristHash = 0;
    //
    public int freeSquares, winner, nPieces1, nPieces2, currentPlayer = P1, numCapture = 0, lastMove;

    private void hashCurrentPlayer() {
        if (currentPlayer == Board.P1) {
            zobristHash ^= blackHash;
            zobristHash ^= whiteHash;
        } else {
            zobristHash ^= whiteHash;
            zobristHash ^= blackHash;
        }
    }

    public int[] playerCaps = new int[2];
    //
    private boolean isEnd = false;
    private int[] capturePositions;
    private int captureI = 0, firstCaptureI = 0, nMoves = 0;
    // For the win check, this array holds the positions that have been investigated
    private boolean[] seen;
    private boolean[] closedrow = new boolean[3], extrafreedom = new boolean[3];
    private int[] rowLength = new int[3], freedom = new int[6], totFreedom = new int[6];
    // Weights for the features
    // [0] Captures  (not used yet, TODO)
    // [1] my longest row,
    // [2] min. freedom of my pieces,
    // [3] min. freedom of opponent's pieces,
    // [4] longest opponent's row,
    // [5] pieces capped by opponent.
    // [6] my largest group
    // [7] opponent's largest group
    // private int[] weights = {800, 50, 5, -5, -50, -800, 10, -10, 5, -5};
    private int[] weights = {10, 3, 1, -1, -3, -10, 0, 0, 0, 0};
    private boolean[] seenFree, visited;
    private ArrayList<Field> checkedFree = new ArrayList<Field>(Board.SIZE);
    private int groupSize = 0;
    // This value is set if there exists a row of length 4 and freedom 2
    private int winByForce1 = 0, winByForce2 = 0, totalfreedom = 0;

    //
    public void Board() {
        nPieces1 = 0;
        nPieces2 = 0;
        board = new Field[SIZE];
        // Initialize the empty fields
        for (int i = 0; i < SIZE; i++) {
            // Only use fields that are part of the board
            if (occupancy[i] == 0)
                continue;
            //
            freeSquares++;
            board[i] = new Field(i);
        }
        int[] nVector;
        // Set the neighbours of each field.
        for (int i = 0; i < SIZE; i++) {
            // First, check if the field is part of the board
            if (occupancy[i] == 0)
                continue;
            // Even or odd row
            if ((i / WIDTH) % 2 == 0)
                nVector = N_VECTOR_EVEN;
            else
                nVector = N_VECTOR_ODD;

            // Get the neighbours for the cell
            for (int j = 0; j < nVector.length; j++) {
                int nField = nVector[j] + i;
                if (Math.abs(nVector[j]) == 1) {
                    // Make sure the west-east fields don't go to a different row.
                    if (nField / WIDTH != i / WIDTH)
                        continue;
                } else {
                    // Make sure the other fields always go to a different row.
                    if (nField / WIDTH == i / WIDTH)
                        continue;
                }
                // Check if in field
                if (nField >= 0 && nField < SIZE) {
                    if (occupancy[nField] == 1) {
                        board[i].numNeighbours++;
                        board[i].neighbours[j] = board[nField];
                    }
                }
            }
        }
    }

    @Override
    public void initialize() {
        nMoves = 0;
        if (zobristPositions == null) {
            zobristPositions = new long[SIZE][];
            // Use the same seed every time
            Random r = new Random();
            for (int i = 0; i < SIZE; i++) {
                if (occupancy[i] == 0)
                    continue;
                // Generate a random number for each possible occupation
                zobristPositions[i] = new long[2];
                zobristPositions[i][P1 - 1] = r.nextLong();
                zobristPositions[i][P2 - 1] = r.nextLong();
            }
            whiteHash = r.nextLong();
            blackHash = r.nextLong();
        }
        zobristHash = 0;
        currentPlayer = P1;
        zobristHash ^= whiteHash;
        lastMove = -1;
    }

    @Override
    public Board clone() {
        Board newBoard = new Board();
        for (int i = 0; i < SIZE; i++) {
            if (occupancy[i] == 0)
                continue;
            //
            if (board[i].occupant != FREE) {
                // No need to take undomove into account here,
                // since we will not go back further than the current gamestate!
                newBoard.board[i].occupant = board[i].occupant;
            }
        }
        //
        newBoard.zobristHash = zobristHash;
        newBoard.nPieces1 = nPieces1;
        newBoard.nPieces2 = nPieces2;
        newBoard.freeSquares = freeSquares;
        newBoard.isEnd = isEnd;
        newBoard.currentPlayer = currentPlayer;
        newBoard.nMoves = nMoves;
        newBoard.winner = winner;
        newBoard.lastMove = lastMove;
        return newBoard;
    }

    @Override
    public void doMove(int[] move) {
        assert board[move[0]].occupant == FREE;

        board[move[0]].occupant = currentPlayer;
        if (currentPlayer == P2) {
            nPieces2++;
        } else {
            nPieces1++;
        }
        freeSquares--;
        zobristHash ^= zobristPositions[move[0]][currentPlayer - 1];
        currentPlayer = 3 - currentPlayer;
        hashCurrentPlayer();
        nMoves++;
        lastMove = move[0];
        // TODO Check how to deal with captures here
    }

    @Override
    public MoveList getExpandMoves() {
        int count = freeSquares;
        int c = 0;
        moves.clear();
        //
        for (int i = 0; i < SIZE; i++) {
            if (board[i] == null)
                continue;
            // Check if position is free and add it to the free moves
            if (board[i].occupant == 0) {
                moves.add(i, 0);
                c++;
                if (c == count)
                    break;
            }
        }
        return moves;
    }

    @Override
    public int getMoveId(int[] move) {
        return move[0];
    }

    @Override
    public int getMaxMoveId() {
        return SIZE * 2;
    }

    @Override
    public MoveList getPlayoutMoves(boolean heuristics) { //TODO, I think this still returns illegal moves
        int count = freeSquares;
        poMoves.clear();
        int c = 0;
        // Add the moves from the spiral ordering
        for (int i = 0; i < SIZE; i++) {
            if (board[i] == null)
                continue;
            // Check if position is free and add it to the free moves
            if (board[i].occupant == 0) {
                poMoves.add(i, 0);
                if (heuristics) {
                    // Prefer the highly connected positions
                    if (board[i].numNeighbours > 4)
                        poMoves.add(i, 0);
                }
                // No need to look further
                if (++c == count)
                    break;
            }
        }
        return poMoves;
    }

    public boolean capturePieces(int pos) {
        Field[] nb = board[pos].neighbours;
        int player = board[pos].occupant;
        int opponent = 3 - player;
        // To mark positions we have already seen.
        seen = new boolean[SIZE];
        boolean suicide = false, capture = false;
        Field capturePos = null;
        // Check if this move is a suicide move.
        if (!checkFree(board[pos])) {
            suicide = true;
        }
        // Check for capture condition (around the placed stone only!)
        for (Field f : nb) {
            if (f == null)
                continue;
            // This position was already checked or is free
            if (f.occupant == FREE)
                continue;
            seen = new boolean[SIZE];
            // Check the position's freedom.
            if (!checkFree(f)) {
                if (f.occupant == opponent) {
                    capture = true;
                    capturePos = f;
                    firstCaptureI = 0;
                    break;
                } else {
                    suicide = true;
                }
            }
        }
        numCapture = 0;
        captureI = 0;
        if (suicide && capture) { // Freedom suicide
            // Will now store all positions to be eliminated.
            seen = new boolean[SIZE];
            numCapture++;
            checkCapturePositions(capturePos);
            capturePositions = new int[numCapture];
            capturePositions();
            freeSquares += numCapture;
        } else if (capture) { // Capture!
            numCapture++;
            // Will now store all positions to be eliminated.
            seen = new boolean[SIZE];
            checkCapturePositions(capturePos);
            capturePositions = new int[numCapture];
            capturePositions();
            freeSquares += numCapture;
        } else if (suicide) {
            zobristHash ^= zobristPositions[pos][board[pos].occupant - 1];
            // Not allowed!
            board[pos].occupant = FREE;
            if (player == P1)
                nPieces1--;
            else
                nPieces2--;
            freeSquares++;
            currentPlayer = player;
            hashCurrentPlayer();
            return false;
        }
        //
        if (numCapture == 0)
            capturePositions = new int[0];
        //
        return true;
    }

    private void capturePositions() {
        // We can start capturing at the lowest index.
        for (int i = firstCaptureI; i < SIZE; i++) {
            if (seen[i]) {
                capturePositions[captureI] = setColor(board[i].position, board[i].occupant == Board.P2);
                captureI++;
                zobristHash ^= zobristPositions[i][board[i].occupant - 1];
                playerCaps[board[i].occupant - 1]++;
                if (board[i].occupant == P1) {
                    nPieces1--;
                } else {
                    nPieces2--;
                }
                board[i].occupant = FREE;
                // stop after all positions are captured
                if (captureI == numCapture)
                    return;
            }
        }
        System.out.println("HOHOHO!");
    }

    private void checkCapturePositions(Field field) {
        Field[] nb = field.neighbours;
        seen[field.position] = true;
        int player = field.occupant;
        for (Field f : nb) {
            if (f == null)
                continue;
            // Position was previously investigated
            if (seen[f.position])
                continue;
            // check if this position should be cleared
            if (f.occupant == player) {
                seen[f.position] = true;
                // Remember where to start the loop for capturing
                if (f.position < firstCaptureI)
                    firstCaptureI = f.position;
                //
                numCapture++;
                checkCapturePositions(f);
            }
        }
    }

    private boolean checkFree(Field field) {
        Field[] nb = field.neighbours;
        boolean free = false;
        for (Field f : nb) {
            if (f == null)
                continue;
            // A neighbour is free --> freedom!
            if (f.occupant == FREE) {
                return true;
            }
            // This position was already checked
            if (seen[f.position] || f.occupant != field.occupant)
                continue;
            // Don't check this position again!
            seen[f.position] = true;
            //
            free = checkFree(f);
            if (free)
                return true;
        }
        // None of the neighbours is free
        return free;
    }

    @Override
    public String getMoveString(int[] move) {
        return null;
    }

    @Override
    public int checkWin() {
        winner = NONE_WIN;

        Field lastPosition = board[lastMove];
        // No need to check if there are less than 8 pieces on the board
        if ((Board.REAL_SIZE - freeSquares) < (ROW_SIZE * 2) - 2)
            return NONE_WIN;
        int player = lastPosition.occupant;
        // Each row is of at least length 1 :)
        Field currentField;
        int[] rowLength = new int[3];
        for (int i = 0; i < rowLength.length; i++) {
            // The current stone
            rowLength[i]++;
        }
        // Check once in each direction.
        for (int j = 0; j < NUM_NEIGHBOURS; j++) {
            currentField = lastPosition.neighbours[j];
            //
            if (currentField == null)
                continue;
            // Check for a row of 5 in each direction.
            while (currentField != null && currentField.occupant == player) {
                rowLength[j % 3]++;
                // One of the players has won!
                if (rowLength[j % 3] == ROW_SIZE) {
                    isEnd = true;
                    winner = player;
                    return player;
                }
                currentField = currentField.neighbours[j];
            }
        }
        // There are fewer free squares on the board than needed to build a row
        if (freeSquares == 0) {
            isEnd = true;
            winner = DRAW;
            return DRAW;
        }
        // None of the players win, continue the game
        return NONE_WIN;
    }

    private int setColor(int number, boolean black) {
        if (black)
            number += BLACK_BIT;
        return number;
    }

    @Override
    public int getPlayerToMove() {
        return currentPlayer;
    }

    @Override
    public int evaluate(int player) {
        // early termination (mcts_pd0 and mcts_pd3) is losing by a lot against vanilla mcts
        // I suspect something is still wrong with this
        //
        seenFree = new boolean[Board.SIZE];
        int minFreeOpp = P_INF, minFreeMe = P_INF, currentFree, count = 0, score = 0;
        int maxRowOpp = 0, maxRowMe = 0, currentMax, maxGroupMe = 0, maxGroupOpp = 0, maxTotalFreeMe = 0, maxTotalFreeOpp = 0;
        boolean isOpp, myTurn;
        score += weights[0] * playerCaps[(3 - player) - 1];
        // The number of my pieces captured by the opponent
        score += weights[5] * playerCaps[player - 1];
        // Check minimal freedom, longest rows etc.
        for (int i = 0; i < board.length; i++) {
            // Check if position is part of the board
            if (board[i] == null || board[i].occupant == Board.FREE)
                continue;
            isOpp = board[i].occupant != player;
            myTurn = currentPlayer == board[i].occupant;
            // Check if longest row.
            currentMax = checkRowLength(board[i], currentPlayer == board[i].occupant);
            // Check if row of 4 with 2 freedom
            if (winByForce1 > 0) {
                score = (isOpp) ? -5000 : 5000;
                return score;
            } else if (winByForce2 > 0) {
                // System.out.println("Force move win for: " + board.board[i].occupant);
                // good or bad :)
                score = (isOpp) ? -6000 : 6000;
                return score;
            }
            // Check if row length is higher than current highest
            if (isOpp && currentMax > maxRowOpp) {
                maxRowOpp = currentMax;
            } else if (!isOpp && currentMax > maxRowMe) {
                maxRowMe = currentMax;
            }
            // Check the maximum total freedom in every direction
            if (isOpp && totalfreedom > maxTotalFreeOpp) {
                maxTotalFreeOpp = totalfreedom;
            } else if (!isOpp && totalfreedom > maxTotalFreeMe) {
                maxTotalFreeMe = totalfreedom;
            }
            // Check for minimal freedom.
            checkedFree.clear();
            visited = new boolean[Board.SIZE];
            if (myTurn) // Be pessimistic about group-size if not my turn
                groupSize = 1;
            else
                groupSize = 0;
            //
            assert (board[i] != null);
            currentFree = checkFreedom(board[i], 0);
            for (Field f : checkedFree) {
                f.freedom = currentFree;
            }
            // Check the largest group
            if (isOpp && groupSize > maxGroupOpp) {
                maxGroupOpp = groupSize;
            } else if (!isOpp && groupSize > maxGroupMe) {
                maxGroupMe = groupSize;
            }
            // There should be at least two pieces on the board or no use to compare freedom
            if (Board.REAL_SIZE - freeSquares > 2) {
                // Check if freedom is lower than current lowest.
                if (isOpp && currentFree < minFreeOpp) {
                    minFreeOpp = currentFree;
                } else if (!isOpp && currentFree < minFreeMe) {
                    minFreeMe = currentFree;
                }
            }
            count++;
            if (count == Board.REAL_SIZE - freeSquares)
                break;
        }
        // Final scoring
        score += weights[1] * maxRowMe;
        score += weights[2] * minFreeMe;
        score += weights[3] * minFreeOpp;
        score += weights[4] * maxRowOpp;
        score += weights[8] * maxTotalFreeMe;
        score += weights[9] * maxTotalFreeOpp;

        return score;
    }

    @Override
    public long hash() {
        return zobristHash;
    }

    /**
     * Set/get the freedom of a field
     *
     * @param f The current field
     * @return The freedom of the field
     */
    private int checkFreedom(Field f, int current) {
        // This field was checked before, return its freedom.
        if (seenFree[f.position])
            return f.freedom;

        visited[f.position] = true;
        //
        Field[] nb = f.neighbours;
        for (Field n : nb) {
            if (n == null)
                continue;
            // For each free neighbor increase the current freedom.
            if (n.occupant == Board.FREE && !visited[n.position]) {
                current++;
                // Count each free position only once!
                visited[n.position] = true;
            } else if (n.occupant == f.occupant && !visited[n.position]) {
                // Check similarly occupied neighbors
                groupSize++;
                current = checkFreedom(n, current);
                checkedFree.add(n);
            }
        }
        seenFree[f.position] = true;
        return current;
    }

    /**
     * Check the longest row that this field is part of
     *
     * @param f The current field
     * @return The length of the longest row
     */
    private int checkRowLength(Field f, boolean myTurn) {
        winByForce1 = 0;
        winByForce2 = 0;
        totalfreedom = 0;
        //
        int opp = (f.occupant == Board.P2) ? Board.P1 : Board.P2;
        // Each row is of at least length 1
        for (int i = 0; i < rowLength.length; i++) {
            // The current stone
            rowLength[i] = 1;
            closedrow[i] = true;
            extrafreedom[i] = false;
            //
            freedom[i] = 0;
            freedom[i + 3] = 0;
            totFreedom[i] = 0;
            totFreedom[i + 3] = 0;
        }
        int longestRow = 0;
        boolean prevFree, rowfinished;
        Field currentField;
        // Check once in each direction.
        for (int j = 0; j < Board.NUM_NEIGHBOURS; j++) {
            prevFree = false;
            rowfinished = false;
            currentField = f.neighbours[j];
            // If we've already seen this position, the current row will not be longer than when
            // we saw it before.
            if (currentField == null)
                continue;

            // Check for a row of 5 in each direction.
            while (currentField != null && currentField.occupant != opp) {
                if (!rowfinished) {
                    // Is part of the row, increase
                    if (currentField.occupant == f.occupant) {
                        if (prevFree) {
                            closedrow[j % 3] = false; // a gap
                        } else {
                            prevFree = false;
                            rowLength[j % 3]++;
                        }
                    } else if (!prevFree) {
                        totalfreedom++;
                        // The row has some freedom in this direction
                        prevFree = true;
                        freedom[j]++;
                        totFreedom[j]++;
                    } else if (prevFree) {
                        extrafreedom[j % 3] = true;
                        // Two free squares == no longer part of a row
                        rowfinished = true;
                        totalfreedom++;
                        totFreedom[j]++;
                        // Total freedom is not considered later in the game
                        if (totFreedom[j % 3] + rowLength[j % 3] >= Board.ROW_SIZE)
                            break;
                    }
                } else {
                    // Keep counting the free squares in this direction
                    totalfreedom++;
                    totFreedom[j]++;
                }
                //
                currentField = currentField.neighbours[j];
            }
        }
        //
        int longestrowi = -1;
        for (int i = 0; i < rowLength.length; i++) {
            // Check for the longest row, only if it can be extended to a row of 5
            if (rowLength[i] > longestRow
                    && rowLength[i] + totFreedom[i] + totFreedom[i + 3] >= Board.ROW_SIZE) {
                longestRow = rowLength[i];
                longestrowi = i;
            }
        }
        // If not player's turn, be pessimistic about freedom
        if (!myTurn) {
            if (longestrowi >= 0) {
                // Assume the opponent will block the longest row
                freedom[longestrowi]--;
                // Assume the opponent will cut of row with most freedom
                if (totFreedom[longestrowi] > totFreedom[longestrowi + 3]) {
                    totalfreedom -= totFreedom[longestrowi];
                    totFreedom[longestrowi] = 0;
                } else {
                    totalfreedom -= totFreedom[longestrowi + 3];
                    totFreedom[longestrowi + 3] = 0;
                }
                // Re-check if still the longest row.
                for (int i = 0; i < rowLength.length; i++) {
                    // Check for the longest row
                    if (rowLength[i] > longestRow
                            && rowLength[i] + totFreedom[i] + totFreedom[i + 3] >= Board.ROW_SIZE) {
                        longestRow = rowLength[i];
                    }
                }
            }
        }
        for (int i = 0; i < rowLength.length; i++) {
            // This condition always leads to a win, closed row of 4, freedom on both sides
            // Or, if myTurn, closed row of three with freedom on both sides, and one extra freedom.
            if (rowLength[i] == 4 && freedom[i] == 2 && closedrow[i]) {
                winByForce1 = f.occupant;
                return longestRow;
            } else if (myTurn && rowLength[i] == 3 && freedom[i] == 2 && closedrow[i]
                    && extrafreedom[i]) {
                winByForce2 = f.occupant;
            } else if (myTurn && rowLength[i] == 4 && freedom[i] >= 1 && closedrow[i]) {
                winByForce2 = f.occupant;
            }
        }
        return longestRow;
    }
}
