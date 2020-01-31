package atarigo;

import atarigo.game.Board;
import framework.AIPlayer;
import framework.Options;
import framework.PlayerFactory;
import mcts.uct.UCTPlayer;

public class Game {

    public static void main(String[] args) {
        Board b = new Board(11);
        b.initialize();
        Options.debug = true;

        AIPlayer aiPlayer1 = PlayerFactory.getPlayer(1, "atarigo");
        AIPlayer aiPlayer2 = PlayerFactory.getPlayer(2, "atarigo");

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


