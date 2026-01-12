package application;

import data.PwdbCsvPressureSignalSource;
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

    private final Path csvDir;
    private final ReservoirComputationPipeline pipeline;

    public ReservoirService(Path csvDir, double sampleRateHz) {
        this.sampleRateHz = sampleRateHz;
        this.csvDir = Objects.requireNonNull(csvDir, "csvDir");
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

    public ComputationOutput compute(Path pwdbCsvFile, String patientId) {
        Path csvDirToUse = pwdbCsvFile == null ? csvDir : pwdbCsvFile;
        var source = new PwdbCsvPressureSignalSource(csvDirToUse, sampleRateHz);
        PressureSignal raw = source.load(patientId, DEFAULT_SITE);
        ReservoirResult result = pipeline.run(raw);
        return new ComputationOutput(raw, result);
    }

    public record ComputationOutput(PressureSignal raw, ReservoirResult result) {}
}