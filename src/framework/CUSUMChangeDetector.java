package framework;

public class CUSUMChangeDetector {

    private final static double DEFAULT_THRESHOLD = .45;
    private final static long DEFAULT_READY_AFTER = 50;

    private double pCusump = 0, pCusumn = 0;
    private double cusump, cusumn;
    private double threshold;
    private long readyAfter;

    private long observationCount = 0;
    private double runningMean = 0.0;

    private boolean change = false;

    public CUSUMChangeDetector(double threshold, long readyAfter) {
        this.readyAfter = readyAfter;
        this.threshold = threshold;
    }

    public CUSUMChangeDetector() {
        this(DEFAULT_THRESHOLD, DEFAULT_READY_AFTER);
    }

    public boolean update(Double xi) {
        // Instead of providing the target mean as a parameter as
        // we would in an offline test, we calculate it as we go to create a target of normality.
        runningMean = runningMean + ((xi - runningMean) / (++observationCount));
        cusump = Math.max(0, pCusump + xi - runningMean);
        cusumn = -Math.min(0, pCusumn - xi + runningMean);
        if (isReady()) {
            change = cusump > threshold;
            if (!change)
                change = cusumn > threshold;
        }
        pCusump = cusump;
        pCusumn = cusumn;
        return change;
    }

    public boolean isReady() {
        return this.observationCount >= readyAfter;
    }

    public void reset() {
        this.pCusump = 0;
        this.cusump = 0;
        this.pCusumn = 0;
        this.cusumn = 0;
        this.runningMean = 0;
        this.observationCount = 0;
        this.change = false;
    }

    public double getPosCusum() {
        return pCusump;
    }

    public double getNegCusum() {
        return pCusumn;
    }

    public double getMean() {
        return runningMean;
    }
}
