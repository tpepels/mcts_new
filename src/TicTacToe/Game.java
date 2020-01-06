package TicTacToe;

import framework.AIPlayer;
import framework.IBoard;
import framework.Options;
import mcts.test.POTester;
import mcts.uct.UCTPlayer;

public class Game {

    public static void main(String[] args) {
        Board b = new Board();
        b.setSize(4);
        b.initialize();

        Options.debug = true;

        AIPlayer aiPlayer1 = new UCTPlayer();
        Options options1 = new Options();
        options1.fixedSimulations = true;
        options1.nSimulations = 30000;
        options1.imm = true;
        aiPlayer1.setOptions(options1);

        AIPlayer aiPlayer2 = new POTester();
        Options options2 = new Options();
        options2.fixedSimulations = true;
        options2.nSimulations = 30000;
        aiPlayer2.setOptions(options2);

        AIPlayer aiPlayer;
        int[] m;
        int nm = 0;
        while (b.checkWin() == IBoard.NONE_WIN) {
            int player = b.getPlayerToMove();

            aiPlayer = (b.getPlayerToMove() == 1 ? aiPlayer1 : aiPlayer2);
            System.gc();
            aiPlayer.getMove(b.clone());
            m = aiPlayer.getBestMove();
            b.doMove(m);
            System.out.println("--------------" + ++nm + "--------------");
            System.out.println(b);
            System.out.println(":: Player " + player + " played " + b.getMoveString(m));
            System.out.println(":: Evaluation P1: " + b.evaluate(1) + " P2: " + b.evaluate(2));
            System.out.println("-------" + nm + "--------------");
        }
        System.out.println(":: Winner is " + b.checkWin());
    }
}
