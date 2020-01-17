package experiments;

import breakthrough.game.Board;
import framework.AIPlayer;
import framework.FastLog;
import framework.Options;
import mcts.uct.UCTPlayer;

/**
 * Runs a single experiment. Options are sent by command-line.
 */

public class SimGame {

    private String p1label, p2label;
    private AIPlayer player1, player2;
    private int[] timeLimit;
    private long seed;
    private boolean printBoard, mctsDebug;

    public SimGame() {
        p1label = "none specified";
        p2label = "none specified";
        player1 = null;
        player2 = null;
        timeLimit = new int[]{10000, 10000};
        printBoard = false;
        mctsDebug = false;
        FastLog.log(1);
        seed = System.currentTimeMillis();
    }

    public static void main(String[] args) {
        SimGame sim = new SimGame();
        sim.parseArgs(args);
        sim.run();
    }

    public void parseArgs(String[] args) {
        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("--p1")) {
                i++;
                p1label = args[i];
            } else if (args[i].equals("--p2")) {
                i++;
                p2label = args[i];
            } else if (args[i].equals("--timelimit")) {
                i++;
                timeLimit[0] = Integer.parseInt(args[i]);
                timeLimit[1] = Integer.parseInt(args[i]);
            } else if (args[i].equals("--seed")) {
                i++;
                seed = Long.parseLong(args[i]);
                Options.r.setSeed(seed);
            } else if (args[i].equals("--printboard")) {
                printBoard = true;
            } else if (args[i].equals("--game")) {
                i++;
            } else {
                throw new RuntimeException("Unknown option: " + args[i]);
            }
        }
    }

    public void loadPlayer(int player, String label) {
        AIPlayer playerRef;

        String[] parts = label.split("_");
        Options options = new Options();
        Options.debug = mctsDebug; // false by default
        options.nSimulations = timeLimit[player - 1];

        if (parts[0].equals("uct")) {
            playerRef = new UCTPlayer();
        } else {
            throw new RuntimeException("Unrecognized player: " + label);
        }

        // now, parse the tags
        for (int i = 1; i < parts.length; i++) {
            String tag = parts[i];
            if (tag.equals("nh")) {
                options.heuristics = false;
            } else if (tag.equals("s")) {
                options.nSimulations = Integer.parseInt(tag.substring(1));
            } else if (tag.equals("f")) {
                options.fixedSimulations = true;
            } else if (tag.startsWith("c")) {
                options.c = Double.parseDouble(tag.substring(1));
                // Early termination
            } else if (tag.startsWith("et")) {
                options.earlyTerm = true;
                if (tag.length() > 2)
                    options.termDepth = Integer.parseInt(tag.substring(2));
            } else if (tag.startsWith("ert")) {
                options.earlyTerm = true;
                if (tag.length() > 3)
                    options.etT = Integer.parseInt(tag.substring(3));
            } else if (tag.startsWith("wv")) {
                options.etWv = Float.parseFloat(tag.substring(2));
                // Implicit minimax
            } else if (tag.startsWith("imm")) {
                options.imm = true;
                if (tag.length() > 3)
                    options.imAlpha = Double.parseDouble(tag.substring(3));
            } else if (tag.startsWith("reg")) {
                options.regression = true;
                if (tag.length() > 3)
                    options.regAlpha = Double.parseDouble(tag.substring(3));
                // Regression
            } else if (tag.startsWith("rs")) {
                options.regression = true;
                options.regForecastSteps = Integer.parseInt(tag.substring(2));
                // MAST
            } else if (tag.startsWith("M")) {
                options.MAST = true;
            } else if (tag.startsWith("UM")) {
                options.UCBMast = true;
            } else if (tag.equals("R")) {
                options.RAVE = true;
                if (tag.length() > 1)
                    options.k = Integer.parseInt(tag.substring(1));
            } else {
                throw new RuntimeException("Unrecognized tag: " + tag);
            }
        }
        // Now, set the player
        if (player == 1) {
            player1 = playerRef;
            player1.setOptions(options);
        } else if (player == 2) {
            player2 = playerRef;
            player2.setOptions(options);
        }
    }

    public void run() {
        System.out.println("Starting game.");
        System.out.println("P1: " + p1label);
        System.out.println("P2: " + p2label);
        System.out.println("");

        Board board = new Board();
        board.initialize();

        loadPlayer(1, p1label);
        loadPlayer(2, p2label);

        while (board.checkWin() == Board.NONE_WIN) {
            if (printBoard)
                System.out.println(board);
            AIPlayer aiPlayer = (board.getPlayerToMove() == 1 ? player1 : player2);
            System.gc();
            aiPlayer.getMove(board.clone());
            board.doMove(aiPlayer.getBestMove());
        }
        // Do not change the format of this line. Used by results aggregator scripts/parseres.perl
        System.out.println("Game over. Winner is " + board.checkWin());
    }
}

