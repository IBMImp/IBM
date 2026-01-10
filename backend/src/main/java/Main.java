// src/main/java/Main.java
import data.PressureSignalSource;
import data.PwdbCsvPressureSignalSource;
import model.ArterySite;
import model.PressureSignal;
import model.ReservoirResult;
import preprocessing.*;
import service.ReservoirComputationPipeline;
import calculation.ReservoirCalculator;
import estimation.DiastolicParameterEstimator;
import estimation.SystolicParameterEstimator;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Main {

    private static final Logger LOGGER = Logger.getLogger(Main.class.getName());

    public static void main(String[] args) {

        System.out.println("Working directory = " + System.getProperty("user.dir"));
        // --- 1) Data location ---
        Path csvDir = Paths.get(System.getProperty("user.dir"))
                .getParent()   // move from backend → IBM_copy
                .resolve("virtualPatientData/pwdb/PWs/CSV")
                .toAbsolutePath();

        // --- 2) Choose case ---
        String patientId = "1631";
        ArterySite site = ArterySite.AorticRoot;

        // --- 3) Sampling rate (must match the assumptions we use for parity) ---
        double sampleRateHz = 1000.0;

        // --- 4) Load waveform ---
        PressureSignalSource source = new PwdbCsvPressureSignalSource(csvDir, sampleRateHz);

        PressureSignal raw;
        try {
            raw = source.load(patientId, site);
        } catch (RuntimeException e) {
            LOGGER.log(Level.SEVERE,
                    "Failed to load PWDB pressure waveform (patient={0}, site={1}, dir={2}): {3}",
                    new Object[]{patientId, site, csvDir, e.getMessage()});
            e.printStackTrace();
            return;
        }

        System.out.println("Loaded PWDB waveform");
        System.out.println("Patient: " + patientId);
        System.out.println("Site: " + site);
        System.out.println("Samples: " + raw.getPressure().length);
        System.out.println("Beat duration (s): " + raw.getBeatDuration());
        System.out.println("Pressure min/max: " +
                Arrays.stream(raw.getPressure()).min().orElse(Double.NaN) + " / " +
                Arrays.stream(raw.getPressure()).max().orElse(Double.NaN));

        // --- 5) Wire components ---
        // For PWDB parity runs: single-beat extractor + identity post-regulariser
        BeatExtractor extractor = new SingleBeatExtractor();
        BeatRegulariser postRegulariser = new IdentityBeatRegulariser();

        // Notch/minima detection
        SignalSmoother smoother = new SavitzkyGolaySmoother(5, 5, 2);
        DicroticNotchDetector detector = new DicroticNotchDetector(smoother);
        NotchLocator notchLocator = detector;
        LocalMinimaDetector minimaDetector = detector;

        DiastolicParameterEstimator estimator = new DiastolicParameterEstimator();

        SystolicParameterEstimator systolicEstimator = new SystolicParameterEstimator();
        ReservoirCalculator reservoirCalculator = new ReservoirCalculator();

        ReservoirComputationPipeline pipeline = new ReservoirComputationPipeline(
                extractor,
                notchLocator,
                minimaDetector,
                postRegulariser,
                estimator,
                systolicEstimator,
                reservoirCalculator
        );

        // --- 6) Run ---
        ReservoirResult result;
        try {
            result = pipeline.run(raw);
        } catch (RuntimeException e) {
            LOGGER.log(Level.SEVERE,
                    "Pipeline failed (patient={0}, site={1}): {2}",
                    new Object[]{patientId, site, e.getMessage()});
            e.printStackTrace();
            return;
        }

        // --- 7) Output sanity ---
        double[] pr = result.getReservoirPressure();
        double[] pe = result.getExcessPressure();

        System.out.println();
        System.out.println("Pipeline ran successfully");
        System.out.println("Pr length: " + pr.length);
        System.out.println("Pe length: " + pe.length);
        System.out.println("Mean pressure: " + mean(raw.getPressure()));
        System.out.println("Mean Pr: " + mean(pr));
        System.out.println("Mean Pe: " + mean(pe));
    }

    private static double mean(double[] x) {
        return Arrays.stream(x).average().orElse(Double.NaN);
    }
}