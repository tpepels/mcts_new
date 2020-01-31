package breakthrough;

import breakthrough.game.Board;
import framework.AIPlayer;
import framework.Options;
import framework.PlayerFactory;
import mcts.uct.UCTPlayer;

public class Game {

    public static void main(String[] args) {
        Board b = new Board();
        b.initialize();
        Options.debug = true;

        AIPlayer aiPlayer1 = PlayerFactory.getPlayer(1, "breakthrough");
        AIPlayer aiPlayer2 = PlayerFactory.getPlayer(2, "breakthrough");

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


