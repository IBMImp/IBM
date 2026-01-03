package service;

import calculation.ReservoirCalculator;
import preprocessing.DicroticNotchDetector;
import preprocessing.DiastolicParameterEstimator;
import preprocessing.DiastolicParameters;
import io.PressureSignal;
import io.ReservoirResult;

public class BackendService {

    private final double ks;
    private final ReservoirComputationPipeline pipeline;

    private static final Logger LOGGER = Logger.getLogger(BackendService.class.getName());

    // Backwards-compatible constructor (keeps old behavior)
//    public BackendService(double ks) {
//        this(ks, new ContinuousBeatExtractor(), new ContinuousBeatRegulariser());
//    }

    public BackendService(double ks, BeatExtractor beatExtractor, BeatRegulariser beatRegulariser) {
        if (ks <= 0) {
            throw new IllegalArgumentException("ks must be positive");
        }
        this.ks = ks;

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
                new ReservoirCalculator(ks));
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

        // 1. Wrap raw inputs into PressureSignal io
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