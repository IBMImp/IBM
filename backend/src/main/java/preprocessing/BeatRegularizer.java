package preprocessing;

import model.DiastolicParameters;
import model.PressureSignal;

/**
 * Contract for algorithms that adjust beat durations to achieve consistent diastolic levels.
 */
public interface BeatRegularizer {

    /**
     * Regularises a pressure beat based on diastolic comparisons between successive minima.
     *
     * @param signal          pressure signal for a single beat
     * @param diastolicParams diastolic parameters for the beat
     * @param minimaDetector  detector used to identify minima across the beat
     * @return a new PressureSignal with a regularised beat duration
     */
    PressureSignal regularise(
            PressureSignal signal,
            DiastolicParameters diastolicParams,
            LocalMinimaDetector minimaDetector);
}
