package service;

import calculation.ReservoirCalculator;
import dicroticnotch.DicroticNotchDetector;
import estimation.DiastolicParameterEstimator;
import model.DiastolicParameters;
import model.PressureSignal;
import model.ReservoirResult;

public class BackendService {

    // Fixed systolic rate constant (can be tuned later)
    private final double ks;

    private final DiastolicParameterEstimator estimator;
    private final ReservoirCalculator reservoirCalculator;

    public BackendService(double ks) {
        if (ks <= 0) {
            throw new IllegalArgumentException("ks must be positive");
        }

        this.ks = ks;
        this.estimator = new DiastolicParameterEstimator();
        this.reservoirCalculator = new ReservoirCalculator(ks);
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

        // 1. Wrap raw inputs into PressureSignal model
        PressureSignal signal = new PressureSignal(pressure, beatDuration);

        // 2. Detect dicrotic notch
        DicroticNotchDetector notchDetector =
                new DicroticNotchDetector(
                        signal.getPressure(),
                        signal.getBeatDuration()
                );

        int notchIndex = notchDetector.getNotchIndex();

        // 3. Estimate diastolic parameters (Pd, kd)
        DiastolicParameters params =
                estimator.estimate(signal, notchIndex);

        // 4. Compute reservoir and excess pressure
        return reservoirCalculator.compute(signal, params);
    }
}