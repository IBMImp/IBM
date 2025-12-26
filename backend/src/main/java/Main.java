import model.ReservoirResult;
import service.BackendService;

import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Main {

    private static final Logger LOGGER = Logger.getLogger(Main.class.getName());

    public static void main(String[] args) {

        // --- 1. Create a test pressure waveform (single cardiac cycle) ---
        int n = 256;
        double beatDuration = 0.8; // seconds
        double[] pressure = createArterialWaveform(n, beatDuration);

        // --- 2. Create backend service ---
        // ks chosen as a reasonable initial value
        BackendService backend = new BackendService(1.0);

        // --- 3. Run full Parker pipeline ---
        ReservoirResult result;
        try {
            result = backend.runPipeline(pressure, beatDuration);
        } catch (RuntimeException e) {
            LOGGER.log(Level.SEVERE,
                    "Pipeline failed for synthetic waveform (samples={0}, beatDuration={1}s): {2}",
                    new Object[]{pressure.length, beatDuration, e.getMessage()});
            e.printStackTrace();
            return;
        }

        // --- 4. Basic sanity checks ---
        double[] pr = result.getReservoirPressure();
        double[] pe = result.getExcessPressure();

        System.out.println("Pipeline ran successfully");
        System.out.println("Signal length: " + pressure.length);
        System.out.println("Reservoir length: " + pr.length);
        System.out.println("Excess length: " + pe.length);

        System.out.println();
        System.out.println("Mean pressure: " + mean(pressure));
        System.out.println("Mean reservoir pressure: " + mean(pr));
        System.out.println("Mean excess pressure: " + mean(pe));

        System.out.println();
        System.out.println("Reservoir min / max: "
                + Arrays.stream(pr).min().getAsDouble() + " / "
                + Arrays.stream(pr).max().getAsDouble());

        System.out.println("Excess min / max: "
                + Arrays.stream(pe).min().getAsDouble() + " / "
                + Arrays.stream(pe).max().getAsDouble());
    }

    private static double[] createArterialWaveform(int n, double beatDuration) {
        double baseline = 80.0; // diastolic baseline
        double systolicPeakTime = 0.22 * beatDuration;
        double notchTime = 0.45 * beatDuration;
        double diastolicTau = 0.65 * beatDuration;

        double[] pressure = new double[n];
        for (int i = 0; i < n; i++) {
            double t = i * beatDuration / (n - 1);

            // Fast systolic upstroke followed by a gentle decay
            double systolicRise = 55 * Math.pow(t / systolicPeakTime, 2)
                    * Math.exp(2 * (1 - (t / systolicPeakTime)));
            systolicRise = Math.max(0, systolicRise);

            // Exponential diastolic runoff starting at the notch
            double runoffTime = Math.max(0, t - notchTime);
            double diastolicRunoff = 28 * Math.exp(-runoffTime / diastolicTau);

            // Localised dicrotic notch dip mid-beat
            double notchDip = -8 * Math.exp(-Math.pow((t - notchTime) / (0.04 * beatDuration), 2));

            // Small reflective and respiratory oscillations
            double reflectedWave = 5 * Math.sin(2 * Math.PI * 6 * t) * Math.exp(-4 * t);
            double respiratorySwing = 1.2 * Math.sin(2 * Math.PI * t);

            pressure[i] = baseline + systolicRise + diastolicRunoff + notchDip
                    + reflectedWave + respiratorySwing;
        }
        return pressure;
    }

    // Simple helper for readability
    private static double mean(double[] x) {
        return Arrays.stream(x).average().orElse(Double.NaN);
    }
}