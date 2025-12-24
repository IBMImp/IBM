package preprocessing;

import model.PressureSignal;

/**
 * Abstraction for locating the dicrotic notch within a pressure beat.
 */
public interface NotchLocator {

    /**
     * Determines the index of the dicrotic notch.
     *
     * @param signal pressure signal representing a single beat
     * @return sample index of the detected notch
     */
    int locateNotch(PressureSignal signal);
}
