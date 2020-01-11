package checkers.game;

import framework.IMove;

public class Move extends IMove {
    public static int P_SLIDE = 1, K_SLIDE = 2, P_CAP = 3, K_CAP = 4;
    //
    public final int[] move, captures;
    public final int type;
    public boolean promotion = false;
    public int kMovesBefore;

    public Move(int[] move, int[] captures, boolean king, boolean capture) {
        this.move = move;
        this.captures = captures;
        int tp = 0;
        if (king)
            tp += 2;
        else
            tp += 1;
        if (capture)
            tp += 2;
        this.type = tp;
    }

    public int[] getCaptures() {
        return captures;
    }


    public int[] getMove() {
        return move;
    }


    public int getType() {
        return type;
    }


    public boolean equals(IMove mv) {

        if (mv.getMove().length != move.length)
            return false;

        for (int i = 0; i < move.length; i++) {
            if (mv.getMove()[i] != move[i])
                return false;
        }

        return true;
    }


    public int getUniqueId() {
        return ((move[1] * 8) + move[0]) + 64 * ((move[3] * 8) + move[2]);
    }


    public boolean isChance() {
        return false;
    }

    @Override
    public boolean isInteresting() {
        return captures != null && captures.length > 0;
    }

    @Override
    public String toString() {
        return "(" + (move[1] * 8 + move[0]) + ") -> (" + (move[3] * 8 + move[2]) + ")";
    }
}
