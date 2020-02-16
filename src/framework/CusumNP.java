package framework;

/**
 * Non-parametric CUSUM change detection algorithm.
 * Source code: https://github.com/blockmon/blockmon
 *
 * @author Maycon Viana Bordin <mayconbordin@gmail.com>
 */
public class CusumNP {
    private int meanWindow;
    private double offset;
    private double[] lastValues;
    private int lastValueIndex = 0;
    private int lastValuesCount = 0;
    private double threshold, currentSum = 0;

    /**
     * Constructor
     *
     * @param threshold The minimum sum to throw an alarm
     * @param meanWindow Number of last values with which the mean should be evaluated
     * @param offset Value to add to the difference in the score computation
     */
    public CusumNP(double threshold, int meanWindow, double offset) {
        this.threshold = threshold;
        this.meanWindow = meanWindow;
        this.offset = offset;

        lastValues = new double[meanWindow];
    }

    /**
     * Check if the value raises an alarm.
     * Depend on previous values since last reset.
     *
     * @param value The value you want to watch
     * @return if an alarm just got raised
     */
    public boolean check(double value) {
        double score = computeScore(value);
        if (currentSum + score > 0)
            currentSum += score;
        else
            currentSum = 0;
        return currentSum >= threshold;
    }

    /**
     * Compute the score of this value.
     * May depend on previous values since last reset.
     *
     * @param value the value for which the score should be computed
     * @return the score
     */
    protected double computeScore(double value) {
        // compute the mean
        double mean;
        if (lastValuesCount > 0) {
            mean = 0;
            for (int i=0; i<lastValuesCount; i++)
                mean += lastValues[i];
            mean /= (double)lastValuesCount;
        } else {
            mean = value;
        }

        // compute the score
        double score = value - mean - offset;

        // Add the current value to compute the next mean
        lastValues[lastValueIndex] = value;
        lastValueIndex = (lastValueIndex + 1) % meanWindow;
        if (lastValuesCount < meanWindow)
            lastValuesCount++;

        return score;
    }

    /**
     * Reset the change detection algorithm.
     */
    public void reset() {
        currentSum = 0;
        resetScore();
    }

    protected void resetScore() {
        lastValueIndex  = 0;
        lastValuesCount = 0;
    }

}
