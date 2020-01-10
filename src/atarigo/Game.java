package atarigo;

import framework.AIPlayer;
import framework.Options;
import mcts.uct.UCTPlayer;
import atarigo.game.Board;

public class Game {

    public static void main(String[] args) {
        Board b = new Board(9);
        b.initialize();
        Options.debug = true;
        Options options1 = new Options();
        AIPlayer aiPlayer1 = new UCTPlayer();
        aiPlayer1.setOptions(options1);
        options1.fixedSimulations = true;
        options1.nSimulations = 25000;

        Options options2 = new Options();
        AIPlayer aiPlayer2 = new UCTPlayer();
        aiPlayer2.setOptions(options2);
        options2.fixedSimulations = true;
        options2.nSimulations = 25000;

        AIPlayer aiPlayer;
        int[] m = null;
        while (b.checkWin() == Board.NONE_WIN) {
            int player = b.getPlayerToMove();
            System.out.println(b.toString());

            aiPlayer = (b.getPlayerToMove() == 1 ? aiPlayer1 : aiPlayer2);
            System.gc();
            aiPlayer.getMove(b.clone());
            m = aiPlayer.getBestMove();
            b.doMove(m);
            System.out.println("Player " + player + " played " + b.getMoveString(m));
        }

        System.out.println("Winner is " + b.checkWin());
    }

}


