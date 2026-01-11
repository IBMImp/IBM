package preprocessing;

import model.PressureSignal;

public final class SingleBeatExtractor implements BeatExtractor {

    @Override
    public PressureSignal extract(PressureSignal input) {
        double[] p = input.getPressure();
        int n = p.length;
        if (n == 0) return input;

        int idxMin = 0;
        for (int i = 1; i < n; i++) {
            if (p[i] < p[idxMin]) idxMin = i;
        }

        double[] shifted = new double[n];

        System.out.println(idxMin);


        System.arraycopy(p, idxMin, shifted, 0, n - idxMin);
        System.arraycopy(p, 0, shifted, n - idxMin, idxMin);

        return new PressureSignal(shifted, input.getBeatDuration());
    }
}