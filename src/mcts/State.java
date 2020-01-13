package mcts;

import org.apache.commons.math3.stat.regression.SimpleRegression;

import java.text.DecimalFormat;

public class State {
    public static final DecimalFormat df2 = new DecimalFormat("###,##0.00");
    public long hash;
    public int visits = 0, lastVisit = 0;
    public short solvedPlayer = 0;
    public boolean visited = false;
    public SimpleRegression[] simpleRegression = new SimpleRegression[2];
    public State next = null;
    private double[] imValue = {Integer.MIN_VALUE, Integer.MIN_VALUE};
    private double[] sums = {0, 0};

    public State(long hash) {
        this.hash = hash;
        simpleRegression[0] = new SimpleRegression();
        simpleRegression[1] = new SimpleRegression();
    }

    public void updateStats(double[] results, boolean regression) {
        assert !this.isSolved() : "UpdateStats called on solved position!";

        visited = true;
        sums[0] += results[0];
        sums[1] += results[1];
        visits++;

        if (regression) {
            if (visits % 100 == 0) { // TODO Check this number or put it in options
                simpleRegression[0].clear();
                simpleRegression[1].clear();
            }

            simpleRegression[0].addData(visits, getMean(0));
            simpleRegression[1].addData(visits, getMean(1));
        }
    }

    public double getRegressionValue(int steps, int player) {
        if (simpleRegression[player - 1].getN() > 5)
            return simpleRegression[player - 1].predict(visits + steps); // WARN Visits + steps is correct :)
        else
            return 0; // Value is captured in calling method
    }

    public double getMean(int player) {
        visited = true;
        if (solvedPlayer == 0) { // Position is not solved, return mean
            if (visits > 0)
                return sums[player - 1] / visits;
            else
                return 0;
        } else    // Position is solved, return inf
            return (player == solvedPlayer) ? Integer.MAX_VALUE : Integer.MIN_VALUE;
    }

    public double[] getImValue() {
        return imValue;
    }

    public void setImValue(double[] val) {
        imValue[0] = val[0];
        imValue[1] = val[1];
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
            String str = "val1: " + df2.format(getMean(1)) + " val2: " +
                    df2.format(getMean(2)) + " n: " + visits;
            if (imValue[0] != Integer.MIN_VALUE && imValue[1] != Integer.MIN_VALUE) {
                str += "\t :: im1: " + df2.format(imValue[0]);
                str += "  im2: " + df2.format(imValue[1]);
            }
            if (simpleRegression != null) {
                str += "\t :: reg1: " + df2.format(getRegressionValue(1, 1));
                str += "  reg2: " + df2.format(getRegressionValue(1, 2));
            }
            return str;
        } else
            return "solved win P" + solvedPlayer;
    }
}
