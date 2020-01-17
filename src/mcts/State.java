package mcts;

import org.apache.commons.math3.stat.regression.SimpleRegression;

import java.text.DecimalFormat;

public class State {
    public static final DecimalFormat df2 = new DecimalFormat("###,##0.000");
    public long hash;
    public int visits = 0, lastVisit = 0;
    public short solvedPlayer = 0;
    public boolean visited = false;
    public SimpleRegression shortRegression = new SimpleRegression();
    public SimpleRegression longRegression = new SimpleRegression();
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

        if (regression) {
            if (shortRegression.getN() % 200 == 0) // TODO Check this number or put it in options
                shortRegression.clear();
            if (longRegression.getN() % 1000 == 0)  // TODO Check this number or put it in options
                longRegression.clear();

            shortRegression.addData(visits, sum / visits);
            longRegression.addData(visits, sum / visits);
        }
    }

    public double getRegressionValue(int steps, int player) {
        if (shortRegression.getN() > 10)
            return ((player == 1) ? 1 : -1) * .5 * (shortRegression.predict(visits + steps) + longRegression.predict(visits + steps)); // WARN Visits + steps is correct :)
        else
            return 0; // Value is captured in calling method
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
            String str = "val_p1: " + df2.format(getMean(1)) + " n: " + visits;
            if (imValue != Integer.MIN_VALUE)
                str += "\t :: im_p1: " + df2.format(imValue);
            if (shortRegression != null)
                str += "\t :: reg_p1: " + df2.format(getRegressionValue(1, 1));
            return str;
        } else
            return "solved win P" + solvedPlayer;
    }
}
