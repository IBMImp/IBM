package service;

import calculation.ReservoirCalculator;
import estimation.DiastolicParameterEstimator;
import model.ReservoirResult;
import preprocessing.BeatRegularisation;
import preprocessing.DicroticNotchDetector;
import preprocessing.LocalMinimaDetector;
import preprocessing.NotchLocator;
import preprocessing.SavitzkyGolaySmoother;
import preprocessing.SignalSmoother;

import java.util.logging.Level;
import java.util.logging.Logger;

public class BackendService {

    // Fixed systolic rate constant (can be tuned later)
    private final double ks;

    private final ReservoirComputationPipeline pipeline;

    private static final Logger LOGGER = Logger.getLogger(BackendService.class.getName());

    public BackendService(double ks) {
        if (ks <= 0) {
            throw new IllegalArgumentException("ks must be positive");
        }

        this.ks = ks;

        SignalSmoother smoother = new SavitzkyGolaySmoother(3, 3, 2);
        DicroticNotchDetector detector = new DicroticNotchDetector(smoother);
        NotchLocator notchLocator = detector;
        LocalMinimaDetector minimaDetector = detector;

        this.pipeline = new ReservoirComputationPipeline(
                notchLocator,
                minimaDetector,
                new BeatRegularisation(),
                new DiastolicParameterEstimator(),
                new ReservoirCalculator(ks));
    }

    /**
     * Runs the full reservoir pressure pipeline for one cardiac cycle.
     *
     * @param pressure     raw arterial pressure waveform
     * @param beatDuration duration of the cardiac cycle (seconds)
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