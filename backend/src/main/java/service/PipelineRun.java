package service;

import calculation.ReservoirCalculator;
import estimation.DiastolicParameterEstimator;
import estimation.SystolicParameterEstimator;
import model.ReservoirResult;
import preprocessing.*;

import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

public class PipelineRun {

    private final ReservoirComputationPipeline pipeline;

    private static final Logger LOGGER = Logger.getLogger(PipelineRun.class.getName());

    public PipelineRun(BeatExtractor beatExtractor, BeatRegulariser beatRegulariser) {

        Objects.requireNonNull(beatExtractor, "beatExtractor cannot be null");
        Objects.requireNonNull(beatRegulariser, "beatRegulariser cannot be null");


        SignalSmoother smoother = new SavitzkyGolaySmoother(5, 6, 2); // Window size:11, Pol Order:2
        DicroticNotchDetector detector = new DicroticNotchDetector(smoother);
        NotchLocator notchLocator = detector;
        LocalMinimaDetector minimaDetector = detector;

        this.pipeline = new ReservoirComputationPipeline(
                beatExtractor,
                notchLocator,
                minimaDetector,
                beatRegulariser,
                new DiastolicParameterEstimator(),
                new SystolicParameterEstimator(),
                new ReservoirCalculator());
    }

    /**
     * Runs the full reservoir pressure pipeline for one cardiac cycle.
     *
     * @param pressure     raw arterial pressure waveform
     * @param beatDuration duration of the cardiac cycle (seconds) for continuous signal inferred from sampling rate provided
     * @return ReservoirResult containing reservoir and excess pressure
     */
    public ReservoirResult runPipeline(
            double[] pressure,
            double beatDuration) {

        try {
            return pipeline.run(pressure, beatDuration);
        } catch (RuntimeException e) {
            LOGGER.log(Level.SEVERE,
                    "Pipeline execution failed (samples={0}, beatDuration={1}s): {2}",
                    new Object[]{pressure.length, beatDuration, e.getMessage()});
            throw e;
        }
    }
}