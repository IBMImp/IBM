package preprocessing;

import model.PressureSignal;

import java.util.List;
import java.util.Objects;

public final class ContinuousBeatExtractor implements BeatExtractor {

    private final LocalMinimaDetector minimaDetector;

    public ContinuousBeatExtractor(LocalMinimaDetector minimaDetector) {
        this.minimaDetector = Objects.requireNonNull(minimaDetector);
    }

    @Override
    public PressureSignal extract(PressureSignal input) {
        List<Integer> minima = minimaDetector.findLocalMinimaIndices(input);
        if (minima.size() < 2) {
            throw new IllegalArgumentException("Need at least 2 minima to extract a beat (found " + minima.size() + ")");
        }

        // choose first complete beat for now (simple and deterministic)
        int start = minima.get(0);
        int end = minima.get(1);

        double[] p = input.getPressure();
        double[] beat = new double[end - start + 1];
        System.arraycopy(p, start, beat, 0, beat.length);

        double beatDuration = (beat.length - 1) * input.getTimeStep();
        PressureSignal oneBeat = new PressureSignal(beat, beatDuration);

        // Align extracted beat so Pd at index 0 (same alignment rule as MATLAB)
        return new SingleBeatExtractor().extract(oneBeat);
    }
}