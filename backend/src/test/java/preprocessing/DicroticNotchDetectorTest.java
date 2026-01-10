package preprocessing;

import model.PressureSignal;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DicroticNotchDetectorTest {

    @Test
    void matchesSavitzkyGolayDerivativeMinimum() {
        double[] pressure = new double[]{100, 110, 120, 80, 70, 60, 65, 70, 75};
        PressureSignal signal = new PressureSignal(pressure, 0.8);

        SavitzkyGolaySmoother smoother = new SavitzkyGolaySmoother(2, 2, 2);
        DicroticNotchDetector detector = new DicroticNotchDetector(smoother);

        int notchIndex = detector.locateNotch(signal);
        int expected = expectedNotchIndex(pressure, 2, 2, 0.1);

        assertEquals(expected, notchIndex);
    }

    @Test
    void rejectsSignalsTooShortForSavitzkyGolayWindow() {
        double[] pressure = new double[]{100, 98, 96, 95};
        PressureSignal signal = new PressureSignal(pressure, 0.3);

        SavitzkyGolaySmoother smoother = new SavitzkyGolaySmoother(2, 2, 2);
        DicroticNotchDetector detector = new DicroticNotchDetector(smoother);

        assertThrows(IllegalArgumentException.class, () -> detector.locateNotch(signal));
    }

    private static int expectedNotchIndex(double[] pressure, int nl, int nr, double dt) {
        double[] coeffs = new double[]{-0.2, -0.1, 0.0, 0.1, 0.2};
        int window = nl + nr + 1;
        int n = pressure.length;
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