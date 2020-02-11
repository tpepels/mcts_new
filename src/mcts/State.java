package mcts;

import framework.CUSUMChangeDetector;
import org.apache.commons.math3.stat.regression.SimpleRegression;

import java.text.DecimalFormat;

public class State {
    public static final DecimalFormat df2 = new DecimalFormat("###,##0.000");
    public static final DecimalFormat df0 = new DecimalFormat("###,##0");
    public long hash;
    public int visits = 0, lastVisit = 0;
    public short solvedPlayer = 0;
    public boolean visited = false;
    public SimpleRegression regressor;
    private CUSUMChangeDetector cSum = new CUSUMChangeDetector();
    private double[][] MA = new double[10][2];
    private int resultC = 0;
    public State next = null;
    private double imValue = Integer.MIN_VALUE;
    private double sum = 0;

    public State(long hash) {
        this.hash = hash;
    }

    public void updateStats(double[] result, int n, boolean regression) {
        assert !this.isSolved() : "UpdateStats called on solved position!";

        visited = true;
        sum += result[0];
        visits += n;

        resultC++;
        MA[resultC % MA.length][1] = sum / visits;
        MA[resultC % MA.length][0] = visits;

        if (regression) {
            if(regressor == null)
                regressor = new SimpleRegression();
            if (cSum.update(sum / visits)) {
                regressor.clear();
                cSum.reset();
            }
            regressor.addData(visits, sum / visits);
        }
    }

    public double getRegValue(int steps, int player) {
        if (regressor.getN() > 1)
            return ((player == 1) ? 1 : -1) * regressor.predict(visits + steps);  // WARN Visits + steps is correct :)
        else
            return Integer.MIN_VALUE; // Value is captured in calling method
    }

    public double getMean(int player) {
        visited = true;
        if (solvedPlayer == 0) { // Position is not solved, return mean
            if (visits > 0)
                return (((player == 1) ? 1 : -1) * sum) / visits;
            else
                return 0;
        } else    // Position is solved, return inf
            return (player == solvedPlayer) ? Integer.MAX_VALUE : Integer.MIN_VALUE;
    }

    public double getMean(int player, int regSteps, double regAlpha) {
        visited = true;
        if (solvedPlayer == 0) { // Position is not solved, return mean

            if(regressor == null || regressor.getN() < 25 || regressor.getRSquare() < .6)
                return getMean(player);

            double regVal = regressor.predict(visits + regSteps);
            double R2 = regressor.getRSquare() * regAlpha;
            if(!Double.isNaN(regVal) && !Double.isNaN(R2)) {
                if (visits > 0)
                    return ((player == 1) ? 1 : -1) * ((1. - R2) * (sum / visits) + (R2 * regVal));
                else
                    return 0;
            } else {
                return getMean(player);
            }
        } else    // Position is solved, return inf
            return (player == solvedPlayer) ? Integer.MAX_VALUE : Integer.MIN_VALUE;
    }

    public double getImValue(int player) {
        return ((player == 1) ? 1 : -1) * imValue;
    }

    public void setImValue(double val, int player) {
        imValue = ((player == 1) ? 1 : -1) * val;
    }

    public boolean isSolved() {
        return solvedPlayer != 0;
    }

    public void setSolved(int player) {
        visited = true;
        if (solvedPlayer > 0 && player != solvedPlayer)
            throw new RuntimeException("setSolved with different player!");
        this.solvedPlayer = (short) player;
    }

    public int getVisits() {
        return visits;
    }

    public String toString() {
        if (solvedPlayer == 0) {
            String str = "val_p1: " + df2.format(getMean(1)) + "\tn: " + df0.format(visits);
            if (imValue != Integer.MIN_VALUE)
                str += "\t :: im_p1: " + df2.format(imValue);
            if (regressor != null) {
                str += "\t :: reg1_p1: " + df2.format(getRegValue(1, 1));
                str += "\t :: reg5_p1: " + df2.format(getRegValue(5, 1));
                str += "\t :: reg_R2: " + df2.format(regressor.getRSquare());
                str += "\t :: reg_N: " + df0.format(regressor.getN());
            }
            return str;
        } else
            return "solved win P" + solvedPlayer;
    }
}
