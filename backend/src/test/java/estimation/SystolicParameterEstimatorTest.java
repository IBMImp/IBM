package estimation;

import calculation.FFTService;
import model.DiastolicParameters;
import model.PressureSignal;
import org.apache.commons.math3.complex.Complex;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SystolicParameterEstimatorTest {

    @Test
    void estimatesKsNearMaxArea() {
        int n = 64;
        double beatDuration = 1.0;
        double dt = beatDuration / (n - 1);
        double pd = 1.0;
        double kd = 2.0;

        double[] pressure = new double[n];
        for (int i = 0; i < n; i++) {
            double t = i * dt;
            pressure[i] = pd + 0.5 * Math.sin(2 * Math.PI * t) + 0.2 * Math.cos(4 * Math.PI * t);
        }

        PressureSignal signal = new PressureSignal(pressure, beatDuration);
        DiastolicParameters params = new DiastolicParameters(pd, kd, 0);
        SystolicParameterEstimator estimator = new SystolicParameterEstimator();

        double ks = estimator.estimate(signal, params);
        double expected = bruteForceMaxKs(signal, params, 0.1);

        assertEquals(expected, ks, 0.5);
    }

    private static double bruteForceMaxKs(PressureSignal signal, DiastolicParameters params, double step) {
        double bestKs = step;
        double bestArea = Double.NEGATIVE_INFINITY;
        for (double ks = step; ks <= 50.0; ks += step) {
            double area = loopArea(signal, params, ks);
            if (area > bestArea) {
                bestArea = area;
                bestKs = ks;
            }
        }
        return bestKs;
    }

    private static double loopArea(PressureSignal signal, DiastolicParameters params, double ks) {
        double[] pressure = signal.getPressure();
        int n = pressure.length;
        double sampleRate = (n - 1) / signal.getBeatDuration();

        double[] p = new double[n];
        for (int i = 0; i < n; i++) {
            p[i] = pressure[i] - params.getPd();
        }

        FFTService fftService = new FFTService();
        Complex[] spectrum = fftService.fft(p);

        int spectrumLength = spectrum.length;
        Complex[] prSpectrum = new Complex[spectrumLength];
        for (int k = 0; k < spectrumLength; k++) {
            double omega = 2.0 * Math.PI * sampleRate * k / spectrumLength;
            Complex denominator = new Complex(ks + params.getKd(), omega);
            prSpectrum[k] = spectrum[k].multiply(ks).divide(denominator);
        }

        double[] pr = fftService.ifft(prSpectrum);
        double offset = pr[0];
        for (int i = 0; i < n; i++) {
            pr[i] -= offset;
        }

        double[] pe = new double[n];
        for (int i = 0; i < n; i++) {
            pe[i] = p[i] - pr[i];
        }

        double area = 0.0;
        for (int i = 0; i < n - 1; i++) {
            double prMid = (pr[i] + pr[i + 1]) / 2.0;
            double dPe = pe[i + 1] - pe[i];
            area -= prMid * dPe;
        }
        return area;
    }
}
