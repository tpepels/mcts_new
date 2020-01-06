package breakthrough;

import breakthrough.game.Board;
import framework.AIPlayer;
import framework.Options;
import mcts.uct.UCTPlayer;

import java.util.Arrays;

public class Game {

    public static void main(String[] args) {
        Board b = new Board();
        b.initialize();

        AIPlayer aiPlayer1 = new UCTPlayer();
        Options options1 = new Options();
        options1.fixedSimulations = true;
        options1.nSimulations = 30000;
        aiPlayer1.setOptions(options1);

        AIPlayer aiPlayer2 = new UCTPlayer();
        Options options2 = new Options();
        options2.fixedSimulations = true;
        options2.nSimulations = 30000;
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
            System.out.println(":: Evaluation P1" + b.evaluate(1) + " P2 " + b.evaluate(2));
        }

        System.out.println(":: Winner is " + b.checkWin());
    }

}


