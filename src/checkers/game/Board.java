package checkers.game;

import framework.IBoard;
import framework.IMove;
import framework.MoveList;
import framework.util.FastTanh;
import framework.util.StatCounter;

import java.util.*;

public class Board implements IBoard {
    public final static int EMPTY = 0, W_PIECE = P1, B_PIECE = P2, W_KING = W_PIECE * 10, B_KING = B_PIECE * 10;
    private final static long[] seen = new long[64];
    private final static int[] n = {-1, 1}, p1n = {-1}, p2n = {1};
    private static long seenI = Long.MIN_VALUE;
    //
    public final MoveList slideMoves = new MoveList(1000), jumpMoves = new MoveList(1000);
    private MoveList moves;
    private final Stack<Move> pastMoves = new Stack<Move>();
    private final List<Integer> captures = new ArrayList<Integer>();
    //
    public int[][] board = new int[8][8];
    private int nMoves, currentPlayer, nPieces1, nPieces2, nKings1, nKings2, winner = NONE_WIN;
    public int kingMoves = 0; // For the 25 move draw-rule

    @Override
    public boolean doAIMove(IMove move, int player) {
        int fromX = move.getMove()[0], fromY = move.getMove()[1];
        int toX = move.getMove()[2], toY = move.getMove()[3];
        // Move the piece
        int piece = board[fromY][fromX];
        board[fromY][fromX] = Board.EMPTY;
        board[toY][toX] = piece;
        // Check for captures
        Move mv = (Move) move;
        if (mv.getCaptures() != null) {
            for (int i : mv.getCaptures()) {
                if (player == P1) {
                    nPieces2--;
                    if (i > 100) {
                        nKings2--;
                        i /= 100;
                    }
                } else {
                    nPieces1--;
                    if (i > 100) {
                        nKings1--;
                        i /= 100;
                    }
                }
                board[i / 8][i % 8] = Board.EMPTY;
                // System.out.println("1 " + nPieces1 + " 2 " + nPieces2);
            }
        }
        // Check for promotion
        if (player == P1 && toY == 0 && board[toY][toX] < 10) {
            board[toY][toX] = W_KING;
            nKings1++;
        }
        if (player == P2 && toY == 7 && board[toY][toX] < 10) {
            board[toY][toX] = B_KING;
            nKings2++;
        }
        // Remember the number of king-moves before this one
        mv.kMovesBefore = kingMoves;

        if (move.getType() == Move.K_SLIDE)
            kingMoves++;
        else
            kingMoves = 0;

        pastMoves.push(mv);
        currentPlayer = getOpponent(currentPlayer);
        nMoves++;
        return true;
    }

    private boolean canPlayerMakeMove(int player) {
        int piece, opp = getOpponent(player), seen = 0;
        int maxSeen = (player == P1) ? nPieces1 : nPieces2;
        for (int i = 0; i < board.length; i++) {
            for (int j = 0; j < board.length; j++) {
                piece = board[i][j];
                if (piece == player || piece == (10 * player)) {
                    if (canPieceMakeMove(j, i, player, opp, piece == (10 * player)))
                        return true;
                    seen++;
                    if (seen == maxSeen)
                        break;
                }
                if (seen == maxSeen)
                    break;
            }
        }

        return false;
    }

    private boolean canPieceMakeMove(int x, int y, int player, int opp, boolean king) {
        int[] dir;
        if (!king)
            dir = (player == W_PIECE) ? p1n : p2n;
        else
            dir = n;
        for (int j : dir) {
            for (int k : n) {
                if (inBounds(y + j, x + k)) {
                    if (board[y + j][x + k] == Board.EMPTY) {
                        //
                        return true;
                    } else if ((board[y + j][x + k] == opp || board[y + j][x + k] == (opp * 10)) &&
                            inBounds(y + (j * 2), x + (k * 2)) &&
                            board[y + (j * 2)][x + (k * 2)] == EMPTY) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    @Override
    public MoveList getExpandMoves() {
        jumpMoves.clear();
        slideMoves.clear();
        int piece, opp = getOpponent(currentPlayer), seen = 0;
        int maxSeen = (currentPlayer == P1) ? nPieces1 : nPieces2;
        boolean captureFound = false;
        for (int i = 0; i < board.length; i++) {
            for (int j = 0; j < board.length; j++) {
                piece = board[i][j];
                if (piece == currentPlayer || piece == (10 * currentPlayer)) {
                    seenI++;
                    captures.clear();
                    if (generateMovesForPiece(j, i, j, i, currentPlayer, 0, piece == (10 * currentPlayer), captureFound, opp)) {
                        captureFound = true;
                    }
                    seen++;
                    if (seen == maxSeen)
                        break;
                }
                if (seen == maxSeen)
                    break;
            }
        }
        //
        if (captureFound)
            moves = jumpMoves;
        else
            moves = slideMoves;
        return moves;
    }

    @Override
    public MoveList getPlayoutMoves(boolean heuristics) {
        return getExpandMoves();
    }

    private boolean generateMovesForPiece(int initX, int initY, int x, int y, int colour, int hops, boolean king, boolean captureOnly, int opp) {
        int[] dir;
        if (!king)
            dir = (colour == W_PIECE) ? p1n : p2n;
        else
            dir = n;
        boolean hopMove = false;
        int location;
        Move mv;
        for (int j : dir) {
            for (int k : n) {
                location = (8 * (y + j)) + (x + k);
                if (inBounds(y + j, x + k)) {
                    if (!captureOnly && hops == 0 && board[y + j][x + k] == Board.EMPTY) {
                        //
                        mv = new Move(new int[]{x, y, x + k, y + j}, null, king, false);
                        mv.promotion = board[initY][initX] < 10 && ((currentPlayer == P1 && y + j == 0) || (currentPlayer == P2 && y + j == 7));
                        slideMoves.add(mv);
                    } else if (seen[location] != seenI &&
                            (board[y + j][x + k] == opp || board[y + j][x + k] == (opp * 10)) &&
                            inBounds(y + (j * 2), x + (k * 2)) &&
                            board[y + (j * 2)][x + (k * 2)] == EMPTY) {

                        seen[location] = seenI;
                        hopMove = true;

                        // Encode the king
                        if (board[y + j][x + k] == (opp * 10))
                            location *= 100;

                        captures.add(new Integer(location));
                        //
                        if (!generateMovesForPiece(initX, initY, x + (k * 2), y + (j * 2), colour, hops + 1, king, captureOnly, opp)) {

                            mv = new Move(new int[]{initX, initY, x + (k * 2), y + (j * 2)}, convertIntegers(captures), king, true);
                            mv.promotion = board[initY][initX] < 10 && ((currentPlayer == P1 && y + (j * 2) == 0) || (currentPlayer == P2 && y + (j * 2) == 7));
                            // Add accordingly to the number of captures
                            jumpMoves.add(mv);
                        }
                        captures.remove(captures.size() - 1);
                    }
                }
            }
        }
        return hopMove;
    }

    @Override
    public int checkWin() {
        winner = NONE_WIN;
        if (nPieces1 == 0)
            winner = P2_WIN;
        else if (nPieces2 == 0)
            winner = P1_WIN;

        if (winner == NONE_WIN) {
            // Check if the current player can move
            // if the current player cannot make a move he loses
            if (!canPlayerMakeMove(currentPlayer))
                winner = 3 - currentPlayer;
        }
        if (winner == NONE_WIN) {
            // If only two kings left, draw
            if (nPieces1 == 1 && nPieces2 == 1 && nKings1 == 1 && nKings2 == 1)
                winner = DRAW;
        }
        // Tournament rule: 25 consecutive king moves without capture = draw
        if (winner == NONE_WIN) {
            if (kingMoves >= 25) {
                winner = DRAW;
            }
        }

        return winner;
    }

    @Override
    public int getPlayerToMove() {
        return currentPlayer;
    }

    @Override
    public int getMaxMoveId() {
        return 64 + (100 * 64);
    }

    @Override
    public void initialize() {
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                board[i][j] = EMPTY;
            }
        }

        for (int i = 0; i < 3; i++) {
            for (int j = 1; j <= 8; j += 2) {
                if (i % 2 == 1) {
                    board[i][j] = B_PIECE;
                    board[7 - i][j - 1] = W_PIECE;
                } else {
                    board[i][j - 1] = B_PIECE;
                    board[7 - i][j] = W_PIECE;
                }
            }
        }

        nPieces1 = 12;
        nPieces2 = 12;
        nKings1 = 0;
        nKings2 = 0;
        nMoves = 0;
        kingMoves = 0;
        currentPlayer = P1;
        winner = NONE_WIN;
    }

    public static int[] convertIntegers(List<Integer> integers) {
        int[] ret = new int[integers.size()];
        Iterator<Integer> iterator = integers.iterator();
        for (int i = 0; i < ret.length; i++) {
            ret[i] = iterator.next().intValue();
        }
        return ret;
    }

    private boolean inBounds(int y, int x) {
        return x >= 0 && x < 8 && y >= 0 && y < 8;
    }

    @Override
    public int evaluate(int player) {
        double diff = (nKings1 * 5 + nPieces1) - (nKings2 * 5 + nPieces2);
        double p1eval = Math.tanh(diff / 10.0);
        if (player == 1)
            return p1eval;
        else
            return -p1eval;
    }

    @Override
    public long hash() {
        return zbHash;
    }

    @Override
    public IBoard clone() {
        Board newBoard = new Board();
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                newBoard.board[i][j] = board[i][j];
            }
        }

        newBoard.currentPlayer = currentPlayer;
        newBoard.nPieces1 = nPieces1;
        newBoard.nPieces2 = nPieces2;
        newBoard.nKings1 = nKings1;
        newBoard.nKings2 = nKings2;
        newBoard.winner = winner;
        newBoard.nMoves = nMoves;
        newBoard.kingMoves = kingMoves;

        return newBoard;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                switch (board[i][j]) {
                    case EMPTY:
                        sb.append(".");
                        break;
                    case B_PIECE:
                        sb.append("b");
                        break;
                    case W_PIECE:
                        sb.append("w");
                        break;
                    case W_KING:
                        sb.append("W");
                        break;
                    case B_KING:
                        sb.append("B");
                        break;
                }
                sb.append(" ");
            }
            sb.append("\n");
        }
        sb.append("pcs1: " + nPieces1 + " pcs2: " + nPieces2 + "\n");
        sb.append("kings1: " + nKings1 + " kings2: " + nKings2 + "\n");
        return sb.toString();
    }
}
