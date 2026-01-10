package application;

import data.PwdbCsvPressureSignalSource;
import model.PressureSignal;
import model.ReservoirResult;
import preprocessing.*;
import calculation.ReservoirCalculator;
import estimation.DiastolicParameterEstimator;
import estimation.SystolicParameterEstimator;
import service.ReservoirComputationPipeline;

import java.nio.file.Path;

public final class ReservoirService {
    private final double sampleRateHz;

    private final ReservoirComputationPipeline pipeline;

    public ReservoirService(Path csvDir, double sampleRateHz) {
        this.sampleRateHz = sampleRateHz;

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
        var source = new PwdbCsvPressureSignalSource(pwdbCsvFile, sampleRateHz);
        PressureSignal raw = source.load(patientId, null); // site unused
        ReservoirResult result = pipeline.run(raw);
        return new ComputationOutput(raw, result);
    }

    public record ComputationOutput(PressureSignal raw, ReservoirResult result) {}
}