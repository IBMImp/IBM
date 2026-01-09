package preprocessing;

import model.PressureSignal;

/**
 * Defines a smoothing strategy for pressure signals, allowing alternative filters to be swapped.
 */
public interface SignalSmoother {

    /**
     * Smooths the provided pressure signal and returns a new array of samples.
     *
     * @param signal pressure signal to be smoothed
     * @return smoothed pressure values, same length as the input signal
     */
    double[] smooth(PressureSignal signal);
}
