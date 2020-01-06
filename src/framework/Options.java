package framework;

import java.util.Random;

public class Options {
    public static final Random r = new Random();
    public static boolean debug = false;

    // Various experimental variables
    public int nSamples = 1; // The number of samples per UCT simulation
    public double c = 0.4; // The UCT constant
    public boolean fixedSimulations = false; // Whether to do a timed run or a run limited by a number of simulations
    public int time = 5000; // Time in ms to run UCT
    public int nSimulations = 10000; // The number of UCT simulations per turn
    public boolean heuristics = false; // Whether to use improved heuristics in playouts

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
}