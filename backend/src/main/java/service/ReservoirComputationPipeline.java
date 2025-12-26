package service;

import calculation.ReservoirCalculator;
import estimation.DiastolicParameterEstimator;
import model.DiastolicParameters;
import model.PressureSignal;
import model.ReservoirResult;
import preprocessing.BeatRegularizer;
import preprocessing.LocalMinimaDetector;
import preprocessing.NotchLocator;

import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Coordinates the reservoir computation workflow using injected strategies.
 */
public class ReservoirComputationPipeline {

    private static final Logger LOGGER = Logger.getLogger(ReservoirComputationPipeline.class.getName());

    private final NotchLocator notchLocator;
    private final LocalMinimaDetector minimaDetector;
    private final BeatRegularizer beatRegularizer;
    private final DiastolicParameterEstimator estimator;
    private final ReservoirCalculator reservoirCalculator;

    public ReservoirComputationPipeline(
            NotchLocator notchLocator,
            LocalMinimaDetector minimaDetector,
            BeatRegularizer beatRegularizer,
            DiastolicParameterEstimator estimator,
            ReservoirCalculator reservoirCalculator) {

        this.notchLocator = Objects.requireNonNull(notchLocator, "Notch locator cannot be null");
        this.minimaDetector = Objects.requireNonNull(minimaDetector, "Minima detector cannot be null");
        this.beatRegularizer = Objects.requireNonNull(beatRegularizer, "Beat regularizer cannot be null");
        this.estimator = Objects.requireNonNull(estimator, "Diastolic estimator cannot be null");
        this.reservoirCalculator = Objects.requireNonNull(reservoirCalculator, "Reservoir calculator cannot be null");
    }

    /**
     * Executes the reservoir-pressure pipeline for a single beat.
     */
    public ReservoirResult run(double[] pressure, double beatDuration) {
        PressureSignal signal = new PressureSignal(pressure, beatDuration);

        int notchIndex;
        try {
            notchIndex = notchLocator.locateNotch(signal);
        } catch (RuntimeException e) {
            LOGGER.log(Level.SEVERE,
                    "Failed to locate dicrotic notch (samples={0}, beatDuration={1}s)",
                    new Object[]{signal.getNumSamples(), signal.getBeatDuration()});
            throw e;
        }

        DiastolicParameters diastolicParameters;
        try {
            diastolicParameters = estimator.estimate(signal, notchIndex);
        } catch (RuntimeException e) {
            LOGGER.log(Level.SEVERE,
                    "Diastolic estimation failed (notchIndex={0}, samples={1}, beatDuration={2}s)",
                    new Object[]{notchIndex, signal.getNumSamples(), signal.getBeatDuration()});
            throw e;
        }

        PressureSignal regularisedSignal;
        try {
            regularisedSignal = beatRegularizer.regularise(
                    signal,
                    diastolicParameters,
                    minimaDetector);
        } catch (RuntimeException e) {
            LOGGER.log(Level.SEVERE,
                    "Beat regularisation failed (pd={0}, kd={1}, notchIndex={2})",
                    new Object[]{diastolicParameters.getPd(), diastolicParameters.getKd(), diastolicParameters.getNotchIndex()});
            throw e;
        }

        try {
            return reservoirCalculator.compute(regularisedSignal, diastolicParameters);
        } catch (RuntimeException e) {
            LOGGER.log(Level.SEVERE,
                    "Reservoir calculation failed (samples={0}, beatDuration={1}s)",
                    new Object[]{regularisedSignal.getNumSamples(), regularisedSignal.getBeatDuration()});
            throw e;
        }
    }
}