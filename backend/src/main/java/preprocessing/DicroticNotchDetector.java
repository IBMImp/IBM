package preprocessing;

import model.PressureSignal;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

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

        double[] smoothed = smoother.smooth(signal);

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
