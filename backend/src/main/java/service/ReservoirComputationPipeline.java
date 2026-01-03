// src/main/java/service/ReservoirComputationPipeline.java
package service;

import calculation.ReservoirCalculator;
import estimation.DiastolicParameterEstimator;
import model.DiastolicParameters;
import model.PressureSignal;
import model.ReservoirResult;
import preprocessing.BeatExtractor;
import preprocessing.BeatRegulariser;
import preprocessing.LocalMinimaDetector;
import preprocessing.NotchLocator;

import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Coordinates the reservoir computation workflow using injected strategies.
 *
 * Contract:
 * - Input can be a single-beat signal OR a continuous signal.
 * - BeatExtractor is responsible for producing exactly one beat.
 * - BeatRegulariser is responsible for optional post-processing of that beat (after diastolic params exist).
 */
public class ReservoirComputationPipeline {

    private static final Logger LOGGER = Logger.getLogger(ReservoirComputationPipeline.class.getName());

    private final BeatExtractor beatExtractor;
    private final NotchLocator notchLocator;
    private final LocalMinimaDetector minimaDetector;
    private final BeatRegulariser beatRegulariser;
    private final DiastolicParameterEstimator estimator;
    private final ReservoirCalculator reservoirCalculator;

    public ReservoirComputationPipeline(
            BeatExtractor beatExtractor,
            NotchLocator notchLocator,
            LocalMinimaDetector minimaDetector,
            BeatRegulariser beatRegulariser,
            DiastolicParameterEstimator estimator,
            ReservoirCalculator reservoirCalculator) {

        this.beatExtractor = Objects.requireNonNull(beatExtractor, "Beat extractor cannot be null");
        this.notchLocator = Objects.requireNonNull(notchLocator, "Notch locator cannot be null");
        this.minimaDetector = Objects.requireNonNull(minimaDetector, "Minima detector cannot be null");
        this.beatRegulariser = Objects.requireNonNull(beatRegulariser, "Beat regulariser cannot be null");
        this.estimator = Objects.requireNonNull(estimator, "Diastolic estimator cannot be null");
        this.reservoirCalculator = Objects.requireNonNull(reservoirCalculator, "Reservoir calculator cannot be null");
    }

    /** Executes the pipeline for an input array + duration. */
    public ReservoirResult run(double[] pressure, double beatDuration) {
        return run(new PressureSignal(pressure, beatDuration));
    }

    /** Executes the pipeline for an input PressureSignal. */
    public ReservoirResult run(PressureSignal input) {
        Objects.requireNonNull(input, "input signal cannot be null");

        // (1) Extract exactly one beat (single-beat or continuous input)
        PressureSignal beat;
        try {
            beat = beatExtractor.extract(input);
        } catch (RuntimeException e) {
            LOGGER.log(Level.SEVERE,
                    "Beat extraction failed (samples={0}, beatDuration={1}s): {2}",
                    new Object[]{input.getNumSamples(), input.getBeatDuration(), e.getMessage()});
            throw e;
        }

        // (2) Locate notch on extracted beat
        int notchIndex;
        try {
            notchIndex = notchLocator.locateNotch(beat);
        } catch (RuntimeException e) {
            LOGGER.log(Level.SEVERE,
                    "Failed to locate dicrotic notch (samples={0}, beatDuration={1}s): {2}",
                    new Object[]{beat.getNumSamples(), beat.getBeatDuration(), e.getMessage()});
            throw e;
        }

        // (3) Estimate diastolic parameters on extracted beat
        DiastolicParameters diastolicParameters;
        try {
            diastolicParameters = estimator.estimate(beat, notchIndex);
        } catch (RuntimeException e) {
            LOGGER.log(Level.SEVERE,
                    "Diastolic estimation failed (notchIndex={0}, samples={1}, beatDuration={2}s): {3}",
                    new Object[]{notchIndex, beat.getNumSamples(), beat.getBeatDuration(), e.getMessage()});
            throw e;
        }

        // (4) Optional post-processing regularisation (mainly for continuous mode)
        PressureSignal regularisedBeat;
        try {
            regularisedBeat = beatRegulariser.regularise(beat, diastolicParameters, minimaDetector);
        } catch (RuntimeException e) {
            LOGGER.log(Level.SEVERE,
                    "Beat regularisation failed (pd={0}, kd={1}, notchIndex={2}): {3}",
                    new Object[]{diastolicParameters.getPd(), diastolicParameters.getKd(),
                            diastolicParameters.getNotchIndex(), e.getMessage()});
            throw e;
        }

        // (5) Reservoir computation
        try {
            return reservoirCalculator.compute(regularisedBeat, diastolicParameters);
        } catch (RuntimeException e) {
            LOGGER.log(Level.SEVERE,
                    "Reservoir calculation failed (samples={0}, beatDuration={1}s): {2}",
                    new Object[]{regularisedBeat.getNumSamples(), regularisedBeat.getBeatDuration(), e.getMessage()});
            throw e;
        }
    }
}