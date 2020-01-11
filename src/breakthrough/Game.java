package breakthrough;

import breakthrough.game.Board;
import framework.AIPlayer;
import framework.Options;
import mcts.test.POTester;
import mcts.uct.UCTPlayer;

import java.util.Arrays;

public class Game {

    public static void main(String[] args) {
        Board b = new Board();
        b.initialize();
        Options.debug = true;

        AIPlayer aiPlayer1 = new UCTPlayer();
        Options options1 = new Options();
        options1.fixedSimulations = true;
        options1.nSimulations = 50000;
        options1.heuristics = true;
        //options1.MAST = true;
        // options1.RAVE = true;
        aiPlayer1.setOptions(options1);

        AIPlayer aiPlayer2 = new UCTPlayer();
        Options options2 = new Options();
        options2.fixedSimulations = true;
        options2.nSimulations = 50000;
        options2.heuristics = true;
        aiPlayer2.setOptions(options2);

        AIPlayer aiPlayer;
        int[] m;
        //
        while (b.checkWin() == Board.NONE_WIN) {
            int player = b.getPlayerToMove();
            System.out.println(b.toString());
            aiPlayer = (b.getPlayerToMove() == 1 ? aiPlayer1 : aiPlayer2);
            System.gc();
            aiPlayer.getMove(b.clone());
            m = aiPlayer.getBestMove();
            b.doMove(m);

            System.out.println(":: Player " + player + " moved " + b.getMoveString(m));
            System.out.println(":: Evaluation P1: " + b.evaluate(1) + " P2: " + b.evaluate(2));
        }
        System.out.println(":: Winner is " + b.checkWin());
    }

}


