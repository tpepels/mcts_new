package framework;

public class FastLog {
    // Stores a number of pre-computed logarithms
    private final static int N_LOGS = 200000;
    private final static double[] logs = new double[N_LOGS];

    static {
        for (int i = 0; i < logs.length; i++)
            logs[i] = Math.log(i);
    }

    public static double log(double i) {
        if (i >= N_LOGS)
            return Math.log(i);
        return logs[(int) i];
    }
}
