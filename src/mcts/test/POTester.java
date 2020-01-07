package mcts.test;

import framework.*;

import java.text.DecimalFormat;
import java.util.*;

public class POTester implements AIPlayer {

    private int[] bestMove;
    private static final DecimalFormat df2 = new DecimalFormat("###,##0.0");
    private Options options;

    @Override
    public void getMove(IBoard board) {
        if (options == null)
            throw new RuntimeException("MCTS Options not set.");

        MoveList moves = board.getPlayoutMoves(options.heuristics);
        bestMove = moves.get(Options.r.nextInt(moves.size()));

        // show information on the best move
        if (options.debug) {
            System.out.println("-------- < PO Debug > ----------");
            System.out.println("- Player " + board.getPlayerToMove());
            System.out.println("- Selected move: " + board.getMoveString(bestMove));
            int[][] moveArr = moves.getArray();
            Arrays.sort(moveArr, new moveComparator());
            Map<int[], Double> moveProb = new HashMap<int[], Double>();
            int[] cMove = moveArr[0];
            double c = 1;
            for(int i = 1; i < moveArr.length; i++) {
                if(moveArr[i][0] == cMove[0] && moveArr[i][1] == cMove[1]) {
                    c++;
                } else {
                    moveProb.put(cMove, c);
                    cMove = moveArr[i];
                    c = 1;
                }
            }
            moveProb.put(cMove, c);
            double N = moveArr.length;
            for(int[] mv : moveProb.keySet()) {
                double prob = (moveProb.get(mv) / N) * 100.;
                System.out.println("- " + board.getMoveString(mv) + " moveProb: " + df2.format(prob) + "%" + " n: " + moveProb.get(mv));
                IBoard cBoard = board.clone();
                cBoard.doMove(mv);
                System.out.println("- " + board.getMoveString(mv) + " eval: " + cBoard.evaluate(board.getPlayerToMove()));
            }
            System.out.println("-------- </PO Debug > ----------");
        }
    }

    @Override
    public void setOptions(Options options) {
        this.options = options;
    }

    @Override
    public int[] getBestMove() {
        return bestMove;
    }

    public void setMoveCallback(MoveCallback moveCallback) {}

    private class moveComparator implements Comparator<int[]> {

        @Override
        public int compare(int[] o1, int[] o2) {
            if(o1[0] > o2[0])
                return 1;
            if(o1[1] > o2[1])
                return 1;
            if(o1[0] == o2[0] && o1[1] == o2[1])
                return 0;
            return -1;
        }
    }
}

