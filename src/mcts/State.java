package mcts;

import org.apache.commons.math3.stat.regression.SimpleRegression;

import java.text.DecimalFormat;

public class State {
    public static final int REG_PLAYER = 1; // Regression line is always in view of this player

    public static float INF = 999999;
    public long hash;
    public int visits = 0, lastVisit = 0, imValue = Integer.MIN_VALUE;
    private double[] sums = {0, 0};
    public short solvedPlayer = 0;
    public boolean visited = false;
    public SimpleRegression[] simpleRegression;
    //
    public State next = null;

    public State(long hash) {
        this.hash = hash;
    }

    public void updateStats(double[] results, boolean regression) {
        visited = true;
        if (solvedPlayer != 0)
            throw new RuntimeException("updateStats called on solved position!");

        getSums()[0] += results[0];
        getSums()[1] += results[1];

        this.visits++;

        if (regression) {
            // Start to build regression after n steps, after every N visits reset the regression
            if (simpleRegression == null || visits % 1000 == 10) {
                simpleRegression[0] = new SimpleRegression();
                simpleRegression[1] = new SimpleRegression();
            }
            if (simpleRegression != null) {
                simpleRegression[0].addData(visits, getMean(0));
                simpleRegression[1].addData(visits, getMean(1));
            }
        }
    }

    public double getRegressionValue(int steps, int player) {
        return simpleRegression[player].predict(visits + steps);
    } // TODO Check if it should be visits + steps or just steps...

    public double getMean(int player) {
        visited = true;
        if (solvedPlayer == 0) { // Position is not solved, return mean
            if (visits > 0)
                return getSums()[player] / visits;
            else
                return 0;
        } else    // Position is solved, return inf
            return (player == solvedPlayer) ? INF : -INF;
    }

    public void setImValue(int imValue) {
        this.imValue = imValue;
    }

    public void setSolved(int player) {
        visited = true;
        if (solvedPlayer > 0 && player != solvedPlayer)
            throw new RuntimeException("setSolved with different player!");
        this.solvedPlayer = (short) player;
    }

    public boolean isSolved() {
        return solvedPlayer != 0;
    }

    public int getVisits() {
        return visits;
    }

    private static final DecimalFormat df2 = new DecimalFormat("###,##0.000");

    public String toString(int player) {
        if (solvedPlayer == 0) {
            String str = df2.format(getMean(player)) + "\tn:" + visits;
            if (imValue > Integer.MIN_VALUE)
                str += "\tim: " + imValue;
            if (simpleRegression != null)
                str += "\treg: " + getRegressionValue(1, player);
            return str;
        } else
            return "solved win P" + solvedPlayer;
    }

    public double[] getSums() {
        return sums;
    }
}
