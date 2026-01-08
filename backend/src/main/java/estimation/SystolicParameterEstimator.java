package estimation;

import calculation.FFTService;
import model.DiastolicParameters;
import model.PressureSignal;
import org.apache.commons.math3.complex.Complex;

/**
 * Estimates systolic rate constant ks following the MATLAB reference script.
 */
public class SystolicParameterEstimator {

    private final FFTService fftService;

    public SystolicParameterEstimator() {
        this.fftService = new FFTService();
    }

    public double estimate(PressureSignal signal, DiastolicParameters params) {
        double[] pressure = signal.getPressure();
        int n = pressure.length;
        if (n < 2) {
            throw new IllegalArgumentException("Pressure array must contain at least two points");
        }

        double pd = params.getPd();
        double kd = params.getKd();
        double sampleRate = (n - 1) / signal.getBeatDuration();

        double[] p = new double[n];
        for (int i = 0; i < n; i++) {
            p[i] = pressure[i] - pd;
        }

        Complex[] spectrum = fftService.fft(p);
        int spectrumLength = spectrum.length;
        double[] omega = new double[spectrumLength];
        for (int k = 0; k < spectrumLength; k++) {
            omega[k] = 2.0 * Math.PI * sampleRate * k / spectrumLength;
        }

        double bestKs = selectInitialKs(p, spectrum, omega, kd, fftService);

        return refineKs(p, spectrum, omega, kd, bestKs, fftService);
    }

    private static double selectInitialKs(
            double[] p,
            Complex[] spectrum,
            double[] omega,
            double kd,
            FFTService fftService) {
        double bestKs = kd * 5.0;
        double bestArea = Double.NEGATIVE_INFINITY;

        for (int ratio = 5; ratio <= 25; ratio += 2) {
            double ks = kd * ratio;
            double area = loopArea(p, spectrum, omega, kd, ks, fftService);
            if (area > bestArea) {
                bestArea = area;
                bestKs = ks;
            }
        }

        return bestKs;
    }

    private static double refineKs(
            double[] p,
            Complex[] spectrum,
            double[] omega,
            double kd,
            double seed,
            FFTService fftService) {
        double dk = 2.0;
        int iter = 0;
        int iterMax = 30;

        double ka = Math.max(1.0, seed - dk);
        double kb = ka + dk;

        double areaA = loopArea(p, spectrum, omega, kd, ka, fftService);
        double areaB = loopArea(p, spectrum, omega, kd, kb, fftService);

        while (areaB > areaA && iter < iterMax) {
            iter++;
            ka = kb;
            kb = ka + dk;
            areaA = areaB;
            areaB = loopArea(p, spectrum, omega, kd, kb, fftService);
        }

        double lower = Math.max(1e-6, ka - dk);
        double upper = kb;

        return goldenSectionMax(p, spectrum, omega, kd, lower, upper, fftService);
    }

    private static double goldenSectionMax(
            double[] p,
            Complex[] spectrum,
            double[] omega,
            double kd,
            double lower,
            double upper,
            FFTService fftService) {

        double phi = (Math.sqrt(5.0) - 1.0) / 2.0;
        double c = upper - phi * (upper - lower);
        double d = lower + phi * (upper - lower);

        double areaC = loopArea(p, spectrum, omega, kd, c, fftService);
        double areaD = loopArea(p, spectrum, omega, kd, d, fftService);

        for (int i = 0; i < 50; i++) {
            if (areaC < areaD) {
                lower = c;
                c = d;
                areaC = areaD;
                d = lower + phi * (upper - lower);
                areaD = loopArea(p, spectrum, omega, kd, d, fftService);
            } else {
                upper = d;
                d = c;
                areaD = areaC;
                c = upper - phi * (upper - lower);
                areaC = loopArea(p, spectrum, omega, kd, c, fftService);
            }
        }

        return (lower + upper) / 2.0;
    }

    private static double loopArea(
            double[] p,
            Complex[] spectrum,
            double[] omega,
            double kd,
            double ks,
            FFTService fftService) {
        Complex[] prSpectrum = new Complex[spectrum.length];
        for (int i = 0; i < spectrum.length; i++) {
            Complex denominator = new Complex(ks + kd, omega[i]);
            prSpectrum[i] = spectrum[i].multiply(ks).divide(denominator);
        }

        double[] pr = fftService.ifft(prSpectrum);
        double offset = pr[0];
        for (int i = 0; i < pr.length; i++) {
            pr[i] -= offset;
        }

        double[] pe = new double[p.length];
        for (int i = 0; i < p.length; i++) {
            pe[i] = p[i] - pr[i];
        }

        double area = 0.0;
        for (int i = 0; i < p.length - 1; i++) {
            double prMid = (pr[i] + pr[i + 1]) / 2.0;
            double dPe = pe[i + 1] - pe[i];
            area -= prMid * dPe;
        }

        return area;
    }
}
