package service;

import data.PressureSignalSource;
import data.PwdbCsvPressureSignalSource;
import data.PostgresPressureSignalSource;
import model.ArterySite;
import model.PressureSignal;
import model.ReservoirResult;
import preprocessing.*;
import calculation.ReservoirCalculator;
import estimation.DiastolicParameterEstimator;
import estimation.SystolicParameterEstimator;

import java.nio.file.Path;
import java.util.Objects;

public final class FrontendService {
    private final double sampleRateHz;
    private static final ArterySite DEFAULT_SITE = ArterySite.AorticRoot;
    private static final String DATABASE_URL_ENV = "DATABASE_URL";

    private final Path defaultDataPath;
    private final ReservoirComputationPipeline pipeline;

    public double getSampleRateHz() {
        return sampleRateHz;
    }

    public FrontendService(Path defaultDataPath, double sampleRateHz) {
        this.sampleRateHz = sampleRateHz;
        this.defaultDataPath = Objects.requireNonNull(defaultDataPath, "defaultDataPath");
        BeatExtractor extractor = new SingleBeatExtractor();
        BeatRegulariser postRegulariser = new IdentityBeatRegulariser();

        SignalSmoother smoother = new SavitzkyGolaySmoother(5, 5, 2);
        DicroticNotchDetector detector = new DicroticNotchDetector(smoother);
        NotchLocator notchLocator = detector;
        LocalMinimaDetector minimaDetector = detector;

        var diastolicEstimator = new DiastolicParameterEstimator();
        var systolicEstimator = new SystolicParameterEstimator();
        var reservoirCalculator = new ReservoirCalculator();

        this.pipeline = new ReservoirComputationPipeline(
                extractor, notchLocator, minimaDetector, postRegulariser,
                diastolicEstimator, systolicEstimator, reservoirCalculator
        );
    }

    public ComputationOutput compute(Path dataPath, String patientId) {
        return compute(dataPath, patientId, DEFAULT_SITE);
    }

    public ComputationOutput compute(Path dataPath, String patientId, ArterySite site) {
        return compute(dataPath, patientId, site, null);
    }

    public ComputationOutput compute(Path dataPath, String patientId, ArterySite site, Double sampleRateOverrideHz) {
        double rateToUse = sampleRateOverrideHz == null ? sampleRateHz : sampleRateOverrideHz;
        Path dataPathToUse = dataPath == null ? defaultDataPath : dataPath;
        PressureSignalSource source = buildSource(dataPathToUse, rateToUse);
        PressureSignal raw = source.load(patientId, site);
        ReservoirResult result = pipeline.run(raw);
        return new ComputationOutput(raw, result);
    }

    public record ComputationOutput(PressureSignal raw, ReservoirResult result) {}

    private PressureSignalSource buildSource(Path dataPath, double sampleRateHz) {
        String name = dataPath.getFileName().toString().toLowerCase();
        if (name.endsWith(".csv")) {
            return new PwdbCsvPressureSignalSource(dataPath, sampleRateHz);
        }
        String databaseUrl = System.getenv(DATABASE_URL_ENV);
        if (databaseUrl == null || databaseUrl.isBlank()) {
            throw new IllegalStateException(
                    "DATABASE_URL must be set for PostgreSQL waveform access");
        }
        return PostgresPressureSignalSource.fromDatabaseUrl(databaseUrl, sampleRateHz);
    }
}
