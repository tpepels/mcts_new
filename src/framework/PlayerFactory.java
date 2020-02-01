package framework;

import mcts.uct.UCTPlayer;

public class PlayerFactory {

    public static AIPlayer getPlayer(int p, String game) {
        Options.debug = true;
        AIPlayer aiPlayer = new UCTPlayer();
        Options options = new Options();
        aiPlayer.setOptions(options);
        options.setGame(game);
        //
        if(p == 1) {
            options.fixedSimulations = false;
            options.nSimulations = 10000;
            options.regression = true;
        } else if (p == 2) {
            options.fixedSimulations = false;
            options.nSimulations = 10000;
        }
        //
        return aiPlayer;
    }
}
