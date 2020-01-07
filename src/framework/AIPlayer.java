package framework;

public interface AIPlayer {
    void getMove(IBoard board);

    void setOptions(Options options);

    int[] getBestMove();

    void setMoveCallback(MoveCallback moveCallback);
}

