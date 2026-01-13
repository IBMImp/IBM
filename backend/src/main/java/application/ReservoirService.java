package application;

import data.PressureSignalSource;
import data.PwdbCsvPressureSignalSource;
import data.SqlitePressureSignalSource;
import model.ArterySite;
import model.PressureSignal;
import model.ReservoirResult;
import preprocessing.*;
import calculation.ReservoirCalculator;
import estimation.DiastolicParameterEstimator;
import estimation.SystolicParameterEstimator;
import service.ReservoirComputationPipeline;

import java.nio.file.Path;
import java.util.Objects;

public final class ReservoirService {
    private final double sampleRateHz;
    private static final ArterySite DEFAULT_SITE = ArterySite.AorticRoot;

    private final Path defaultDataPath;
    private final ReservoirComputationPipeline pipeline;

    public ReservoirService(Path defaultDataPath, double sampleRateHz) {
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
        Path dataPathToUse = dataPath == null ? defaultDataPath : dataPath;
        PressureSignalSource source = buildSource(dataPathToUse);
        PressureSignal raw = source.load(patientId, DEFAULT_SITE);
        ReservoirResult result = pipeline.run(raw);
        return new ComputationOutput(raw, result);
    }

    public record ComputationOutput(PressureSignal raw, ReservoirResult result) {}

    private PressureSignalSource buildSource(Path dataPath) {
        String name = dataPath.getFileName().toString().toLowerCase();
        if (name.endsWith(".db")) {
            return new SqlitePressureSignalSource(dataPath, sampleRateHz);
        }
        return new PwdbCsvPressureSignalSource(dataPath, sampleRateHz);
    }
}
