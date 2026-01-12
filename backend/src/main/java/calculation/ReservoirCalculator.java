package calculation;

import model.PressureSignal;
import model.DiastolicParameters;
import model.ReservoirResult;
import org.apache.commons.math3.complex.Complex;

/**
 * ReservoirCalculator
 *
 * Computes reservoir pressure and excess pressure using
 * Parker's FFT-based reservoir pressure formulation.
 *
 * This class contains the physiological modelling logic and
 * uses FFTService purely for numerical transforms.
 */
public class ReservoirCalculator {

    // Numerical FFT handler
    private final FFTService fftService;

    public ReservoirCalculator() {
        this.fftService = new FFTService();
    }

    /**
     * Computes reservoir and excess pressure for a single cardiac cycle.
     *
     * @param signal PressureSignal containing time-domain pressure and beat duration
     * @param params DiastolicParameters containing Pd and kd
     * @param ks systolic rate constant
     * @return ReservoirResult containing reservoir pressure and excess pressure
     */
    public ReservoirResult compute(
            PressureSignal signal,
            DiastolicParameters params,
            double ks) {

            if (ks <= 0) {
                throw new IllegalArgumentException("ks must be positive");
            }

        double[] P = signal.getPressure();
        int n = P.length;
        double T = signal.getBeatDuration();
        double sampleRate = (n - 1) / T;

        double Pd = params.getPd();
        double kd = params.getKd();

        // Debug: Print estimated parameters
        System.out.println("=== Parameter Estimates ===");
        System.out.println("Estimated Pd: " + Pd + " mmHg");
        System.out.println("Estimated kd: " + kd + " s^-1");
        System.out.println("Estimated ks: " + ks + " s^-1");
        System.out.println("Ratio ks/kd: " + (ks/kd));
        System.out.println("========================");

        /*
         * 1. Reference pressure to diastolic pressure:
         *    p(t) = P(t) - Pd
         */
        double[] p = new double[n];
        for (int i = 0; i < n; i++) {
            p[i] = P[i] - Pd;
        }

        /*
         * 2. FFT of referenced pressure signal
         */
        Complex[] spectrum = fftService.fft(p);

        /*
         * 3. Apply Parker transfer function in frequency domain:
         *
         *    Pr(ω) = [ ks / (ks + kd + iω) ] * P(ω)
         *
         *    ω_k = 2πk / T
         */
        for (int k = 0; k < spectrum.length; k++) {

            double omega = 2.0 * Math.PI * sampleRate * k / n;

            Complex denominator = new Complex(ks + kd, omega);

            spectrum[k] = spectrum[k]
                    .multiply(ks)
                    .divide(denominator);
        }

        /*
         * 4. Inverse FFT to recover reservoir pressure (referenced to Pd)
         */
        double[] prReferenced = fftService.ifft(spectrum);

        /*
         * 5. Add Pd back to obtain absolute reservoir pressure
         */
        double[] reservoirPressure = new double[n];
        for (int i = 0; i < n; i++) {
            reservoirPressure[i] = prReferenced[i] + Pd;
        }
        
        /*
         * 5b. Enforce diastolic boundary condition: Pr = P after valve closure
         *     Physiology requires Qin = 0 in diastole, therefore Pr(t) = P(t)
         */
        int notchIndex = params.getNotchIndex();
        if (notchIndex >= 0 && notchIndex < n) {
            for (int i = notchIndex; i < n; i++) {
                reservoirPressure[i] = P[i];
            }
        }
        
        /*
         * 6. Excess pressure:
         *    Pe(t) = P(t) - Pr(t)
         */
        double[] excessPressure = new double[n];
        for (int i = 0; i < n; i++) {
            excessPressure[i] = P[i] - reservoirPressure[i];
        }

        // Debug: Check diastolic equality
        if (notchIndex > 0 && notchIndex < n) {
            double maxDiastolicDiff = 0.0;
            for (int i = notchIndex; i < n; i++) {
                double diff = Math.abs(P[i] - reservoirPressure[i]);
                if (diff > maxDiastolicDiff) {
                    maxDiastolicDiff = diff;
                }
            }
            System.out.println("Max diastolic difference |P - Pr|: " + maxDiastolicDiff + " mmHg");
            System.out.println("At notch: P = " + P[notchIndex] + ", Pr = " + reservoirPressure[notchIndex]);
        }

        int ni = notchIndex;

        System.out.println("----- DIAGNOSTIC -----");
        System.out.println("kd = " + kd);
        System.out.println("ks = " + ks);
        System.out.println("notchIndex = " + ni);
        System.out.println("P[notch]  = " + P[ni]);
        System.out.println("Pr[notch] = " + reservoirPressure[ni]);
        
        double maxErr = 0.0;
        for (int i = ni; i < n; i++) {
            maxErr = Math.max(maxErr, Math.abs(reservoirPressure[i] - P[i]));
        }
        System.out.println("Max |Pr - P| in diastole = " + maxErr);
        System.out.println("----------------------");

        
        return new ReservoirResult(
                reservoirPressure,
                excessPressure,
                params
        );
    }
}
