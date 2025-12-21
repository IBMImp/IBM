import mr.go.sgfilter.SGFilter;

public class DicroticNotchDetector {

    private double[] pressures;
    private double beatTime;

    public DicroticNotchDetector(double[] pressures, double beatTime){
        this.pressures = pressures;
        this.beatTime = beatTime;
    }

    public int getNotchIndex() {
        if (this.pressures == null) {
            throw new IllegalArgumentException("Pressure array is null");
        }

        int windowSize = 7;
        if (this.pressures.length < windowSize) {
            throw new IllegalArgumentException("pressures length must be >= windowSize");
        }

        if (this.beatTime <= 0){
            throw new IllegalArgumentException("Beat Time must be more than zero");
        }


        int nI = windowSize % 2;
        int nr = windowSize % 2;
        int polOrder = 2;
        double[] coeffs = SGFilter.computeSGCoefficients(nI, nr, polOrder);

        SGFilter sg = new SGFilter(nI, nr);
        double[] smoothed = sg.smooth(this.pressures, coeffs);

        int n = pressures.length;
        double[] dp = new double[n];
        double dt = beatTime / (n - 1);

        dp[0] = (smoothed[1] - smoothed[0]) / dt;
        for (int i = 1; i < n - 1; i++) {
            dp[i] = (smoothed[i + 1] - smoothed[i - 1]) / (2 * dt);
        }
        dp[n - 1] = (smoothed[n - 1] - smoothed[n - 2]) / dt;

        int notchIndex = 0;
        double minDerivative = Double.POSITIVE_INFINITY;

        for (int i = 0; i < n; i++) {
            if (dp[i] < minDerivative) {
                minDerivative = dp[i];
                notchIndex = i;
            }
        }
        return notchIndex;
    }

}

