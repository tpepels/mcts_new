package framework;

public interface AIPlayer extends Runnable {
    void getMove(IBoard board);

    void setOptions(Options options);

    int[] getBestMove();

    void setMoveCallback(MoveCallback moveCallback);

    void setBoard(IBoard board);
}

