package framework;

import java.util.List;

public interface IBoard {
    /**
     * Win/loss/draw and player definitions, don't change these
     */
    int NONE_WIN = 0, P1_WIN = 1, P2_WIN = 2, P3_WIN = 3, P4_WIN = 4, DRAW = 100;
    int P1 = 1, P2 = 2;

    String getMoveString(int[] move);

    /**
     * Check whether the game is in a win-loss state
     *
     * @return NONE_WIN = 0, P1_WIN = 1, P2_WIN = 2, DRAW = 3
     */
    int checkWin();

    /**
     * Get the player that is currently active (to move)
     *
     * @return current player
     */
    int getPlayerToMove();

    /**
     * Initialize the startingposition of the board. i.e. reset the game
     */
    void initialize();

    /**
     * Returns a long value corresponding to the Zobrist hash for this state
     */
    long hash();

    IBoard clone();

    void doMove(int[] move);

    int evaluate(int player);

    MoveList getPlayoutMoves(boolean heuristics);

    MoveList getExpandMoves(MoveList captures);
}