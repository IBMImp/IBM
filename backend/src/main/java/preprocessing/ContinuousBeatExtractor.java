package preprocessing;

import model.PressureSignal;

import java.util.List;
import java.util.Objects;

public final class ContinuousBeatExtractor implements BeatExtractor {

    private final LocalMinimaDetector minimaDetector;

    public ContinuousBeatExtractor(LocalMinimaDetector minimaDetector) {
        this.minimaDetector = Objects.requireNonNull(minimaDetector);
    }

    public int[] findMinima(PressureSignal input) {
        List<Integer> minima = minimaDetector.findLocalMinimaIndices(input);
        if (minima.size() < 4) {
            throw new IllegalArgumentException("Need at least 2 minima to extract a beat (found " + minima.size() + ")");
        }

        System.out.println(minima.size());

        for (int i = 0; i < minima.size()-2; ++i) {
            if(
                    input.getPressure()[minima.get(i)] < input.getPressure()[minima.get(i+1)] &&
                    input.getPressure()[minima.get(i+2)] < input.getPressure()[minima.get(i+1)]
            ) {
                return new int[] {minima.get(i), minima.get(i+2)};
            }
        }

        return new int[] {0,0};

    }

    public PressureSignal extract(int[] bounds, PressureSignal input) {

        // choose first complete beat for now (simple and deterministic)
        int start = bounds[0];
        int end = bounds[1];

        double[] p = input.getPressure();
        double[] beat = new double[end - start + 1];
        System.arraycopy(p, start, beat, 0, beat.length);

        double beatDuration = (beat.length - 1) * input.getTimeStep();
        PressureSignal oneBeat = new PressureSignal(beat, beatDuration);

        // Align extracted beat so Pd at index 0 (same alignment rule as MATLAB)
        return new SingleBeatExtractor().extract(oneBeat);
    }

    @Override
    public PressureSignal extract(PressureSignal input) {

        int[] bounds = findMinima(input);

        // choose first complete beat for now (simple and deterministic)
        int start = bounds[0];
        int end = bounds[1];

        double[] p = input.getPressure();
        double[] beat = new double[end - start + 1];
        System.arraycopy(p, start, beat, 0, beat.length);

        double beatDuration = (beat.length - 1) * input.getTimeStep();
        PressureSignal oneBeat = new PressureSignal(beat, beatDuration);

        // Align extracted beat so Pd at index 0 (same alignment rule as MATLAB)
        return new SingleBeatExtractor().extract(oneBeat);
    }
}