package preprocessing;

import model.PressureSignal;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.SingularValueDecomposition;
/**
 * Detector responsible for locating the dicrotic notch and broader minima features.
 */
public class DicroticNotchDetector implements LocalMinimaDetector, NotchLocator {

    private final SignalSmoother smoother;

    public DicroticNotchDetector(SignalSmoother smoother) {
        this.smoother = Objects.requireNonNull(smoother, "Signal smoother cannot be null");
    }

    @Override
    public int locateNotch(PressureSignal signal) {
        double[] pressures = signal.getPressure();
        double beatTime = signal.getBeatDuration();

        if (beatTime <= 0) {
            throw new IllegalArgumentException("Beat Time must be more than zero");
        }


        int n = pressures.length;
        double dt = beatTime / (n - 1);

        double[] dp;
        if (smoother instanceof SavitzkyGolaySmoother sgSmoother) {
            dp = savitzkyGolayDerivative(pressures, dt, sgSmoother);
        } else {
            double[] smoothed = smoother.smooth(signal);
            dp = finiteDifferenceDerivative(smoothed, dt);

        }

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

    private static double[] finiteDifferenceDerivative(double[] smoothed, double dt) {
        int n = smoothed.length;
        if (n < 3) {
            throw new IllegalArgumentException("Pressure array must contain at least three points to locate notch");
        }
        double[] dp = new double[n];
        dp[0] = (smoothed[1] - smoothed[0]) / dt;
        for (int i = 1; i < n - 1; i++) {
            dp[i] = (smoothed[i + 1] - smoothed[i - 1]) / (2 * dt);
        }
        dp[n - 1] = (smoothed[n - 1] - smoothed[n - 2]) / dt;
        return dp;
    }

    private static double[] savitzkyGolayDerivative(double[] pressure, double dt, SavitzkyGolaySmoother smoother) {
        int n = pressure.length;
        int nl = smoother.getNl();
        int nr = smoother.getNr();
        int polyOrder = smoother.getPolynomialOrder();
        int window = nl + nr + 1;

        if (n < window) {
            throw new IllegalArgumentException("Pressure array shorter than Savitzky-Golay window");
        }
        if (polyOrder < 1) {
            throw new IllegalArgumentException("Polynomial order must be at least 1 to compute derivatives");
        }

        double[] coeffs = savitzkyGolayCoefficients(nl, nr, polyOrder, 1);
        double[] dp = new double[n];
        for (int i = nl; i < n - nr; i++) {
            double sum = 0.0;
            int start = i - nl;
            for (int j = 0; j < window; j++) {
                sum += coeffs[j] * pressure[start + j];
            }
            dp[i] = sum / dt;
        }

        for (int i = 0; i < nl; i++) {
            dp[i] = dp[nl];
        }
        for (int i = n - nr; i < n; i++) {
            dp[i] = dp[n - nr - 1];
        }

        return dp;
    }

    private static double[] savitzkyGolayCoefficients(
            int nl,
            int nr,
            int polyOrder,
            int derivativeOrder) {

        int window = nl + nr + 1;
        double[][] design = new double[window][polyOrder + 1];
        for (int i = 0; i < window; i++) {
            int k = i - nl;
            double value = 1.0;
            for (int j = 0; j <= polyOrder; j++) {
                design[i][j] = value;
                value *= k;
            }
        }

        RealMatrix a = new Array2DRowRealMatrix(design);
        RealMatrix ata = a.transpose().multiply(a);
        RealMatrix pinv = new SingularValueDecomposition(ata).getSolver().solve(a.transpose());

        double scale = factorial(derivativeOrder);
        return pinv.getRowVector(derivativeOrder).mapMultiply(scale).toArray();
    }

    private static double factorial(int n) {
        double result = 1.0;
        for (int i = 2; i <= n; i++) {
            result *= i;
        }
        return result;
    }

    @Override
    public List<Integer> findLocalMinimaIndices(PressureSignal signal) {
        double[] pressure = signal.getPressure();
        if (pressure.length < 3) {
            throw new IllegalArgumentException("Pressure array must contain at least three points to locate minima");
        }

        List<Integer> minima = new ArrayList<>();

        if (pressure[0] <= pressure[1]) {
            minima.add(0);
        }

        for (int i = 1; i < pressure.length - 1; i++) {
            double prev = pressure[i - 1];
            double current = pressure[i];
            double next = pressure[i + 1];

            if (current <= prev && current < next) {
                minima.add(i);
            }
        }

        int lastIndex = pressure.length - 1;
        if (pressure[lastIndex] <= pressure[lastIndex - 1]) {
            minima.add(lastIndex);
        }

        if (minima.size() < 2) {
            throw new IllegalArgumentException("Detected fewer than two local minima in the pressure waveform");
        }

        return minima;
    }

}
