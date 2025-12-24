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

/**
 * Coordinates the reservoir computation workflow using injected strategies.
 */
public class ReservoirComputationPipeline {

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

        int notchIndex = notchLocator.locateNotch(signal);
        DiastolicParameters diastolicParameters = estimator.estimate(signal, notchIndex);

        PressureSignal regularisedSignal = beatRegularizer.regularise(
                signal,
                diastolicParameters,
                minimaDetector);

        return reservoirCalculator.compute(regularisedSignal, diastolicParameters);
    }
}
