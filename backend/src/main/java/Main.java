import model.ReservoirResult;
import service.BackendService;

import java.util.Arrays;

public class Main {

    public static void main(String[] args) {

        // --- 1. Create a test pressure waveform (single cardiac cycle) ---
        int n = 256;
        double beatDuration = 0.8; // seconds
        double[] pressure = new double[n];

        for (int i = 0; i < n; i++) {
            double t = i * beatDuration / (n - 1);

            // Simple synthetic arterial-like waveform
            pressure[i] = 80
                    + 40 * Math.exp(-5 * t)      // systolic upstroke + decay
                    + 5 * Math.sin(8 * Math.PI * t); // small oscillatory component
        }

        // --- 2. Create backend service ---
        // ks chosen as a reasonable initial value
        BackendService backend = new BackendService(1.0);

        // --- 3. Run full Parker pipeline ---
        ReservoirResult result =
                backend.runPipeline(pressure, beatDuration);

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

    // Simple helper for readability
    private static double mean(double[] x) {
        return Arrays.stream(x).average().orElse(Double.NaN);
    }
}
