package preprocessing;

import model.PressureSignal;

import java.util.List;

/**
 * Abstraction for locating sequential local minima in a pressure waveform.
 */
public interface LocalMinimaDetector {

    /**
     * Identifies the indices of local minima within a pressure signal.
     *
     * @param signal pressure signal representing a single beat
     * @return indices of detected minima, ordered from earliest to latest
     */
    List<Integer> findLocalMinimaIndices(PressureSignal signal);
}
