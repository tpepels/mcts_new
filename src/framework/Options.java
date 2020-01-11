package framework;

import java.util.LinkedList;
import java.util.List;
import java.util.Random;

public class Options {
    public static final Random r = new Random();
    public static boolean debug = false;

    // Various experimental variables
    public int nSamples = 1; // The number of samples per UCT simulation
    public double c = 0.8; // The UCT constant
    public boolean fixedSimulations = false; // Whether to do a timed run or a run limited by a number of simulations
    public int time = 5000; // Time in ms to run UCT
    public int nSimulations = 10000; // The number of UCT simulations per turn
    public boolean heuristics = true; // Whether to use improved heuristics in playouts

    // Parameters for early playout termination
    public boolean earlyTerm = false; // Whether to terminate playouts before the end of the game and return a value
    public int termDepth = 4; // Terminate playout after termDepth steps
    public int etT = 20; // The threshold of the evaluation function > = win < = loss
    public double etWv = 1.3;

    // Parameters for Implicit Minimax
    public boolean imm = false; // Whether to use implicit minimax
    public double imAlpha = 0.1; // The weight of the implicit minimax value

    // Parameters for regression
    public boolean regression = false; // Whether to include the regression term in the UCT value
    public int regForecastSteps = 1; // Number of regression steps to forecast into the future
    public double regAlpha = 0.1;
    //
    public boolean RAVE = false;
    public double k = 10;
    public int[][] RAVEMoves;

    public void resetRAVE(int maxId) {
        RAVEMoves = new int[2][maxId];
    }

    public void addRAVEMove(int player, int id, int depth) {
        RAVEMoves[player - 1][id] = depth;
    }

    public boolean isRAVEMove(int player, int id, int depth) {
        return RAVEMoves[player - 1][id] > depth;
    }
    //
    private double[][] histVal;
    private int[][] histVis;
    private List<Integer>[] moveLists;
    public boolean MAST = false;
    public double epsilon = .05;

    public void resetMAST(int maxId) {
        histVal = new double[2][maxId];
        histVis = new int[2][maxId];
        moveLists = new LinkedList[2];
        moveLists[0] = new LinkedList<>();
        moveLists[1] = new LinkedList<>();
    }

    public double getMASTValue(int player, int moveId) {
        return histVal[player - 1][moveId] / (double)histVis[player - 1][moveId];
    }

    public void addMASTMove(int player, int moveId) {
        moveLists[player - 1].add(moveId);
    }

    public void updateMASTMoves(double[] values) {
        for(int k = 0; k < 2; k++) {
            for (int i = 0; i < moveLists[k].size(); i++) {
                histVal[k][moveLists[k].get(i)] += values[k];
                histVis[k][moveLists[k].get(i)]++;
            }
            moveLists[k].clear();
        }
    }
}
