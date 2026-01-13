package preprocessing;

import model.PressureSignal;

import java.util.ArrayList;
import java.util.List;

public class ContinuousMinimaDetector implements LocalMinimaDetector {

    public ContinuousMinimaDetector() {

    }

    @Override
    public List<Integer> findLocalMinimaIndices(PressureSignal signal) {
        double[] pressure = signal.getPressure();
        if (pressure.length < 3) {
            throw new IllegalArgumentException("Pressure array must contain at least three points to locate minima");
        }

        List<Integer> minima = new ArrayList<>();

        if (pressure[0] <= pressure[1]) {
            minima.add(0);
        }

        for (int i = 1; i < pressure.length - 1; i++) {
            double prev = pressure[i - 1];
            double current = pressure[i];
            double next = pressure[i + 1];

            if (current <= prev && current < next) {
                minima.add(i);
            }
        }

        int lastIndex = pressure.length - 1;
        if (pressure[lastIndex] <= pressure[lastIndex - 1]) {
            minima.add(lastIndex);
        }

        if (minima.size() < 2) {
            throw new IllegalArgumentException("Detected fewer than two local minima in the pressure waveform");
        }

        return minima;
    }

}
