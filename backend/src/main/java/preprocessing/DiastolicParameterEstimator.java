package preprocessing;

import io.PressureSignal;

public class DiastolicParameterEstimator {

    public DiastolicParameters estimate(PressureSignal signal, int notchIndex) {

        double[] p = signal.getPressure();
        double dt = signal.getTimeStep();
        int n = p.length;

        if (notchIndex >= n - 2) {
            throw new IllegalArgumentException("Notch index too close to end of signal");
        }

        // 1. Estimate Pd as minimum pressure in late diastole
        double pd = Double.POSITIVE_INFINITY;
        for (int i = notchIndex + 1; i < n; i++) {
            if (p[i] < pd) {
                pd = p[i];
            }
        }

        // 2. Estimate kd from log-linear fit
        double sumT = 0;
        double sumY = 0;
        double sumTT = 0;
        double sumTY = 0;
        int count = 0;

        for (int i = notchIndex + 1; i < n; i++) {
            double dp = p[i] - pd;
            if (dp <= 0) continue; // avoid log problems

            double t = (i - notchIndex) * dt;
            double y = Math.log(dp);

            sumT += t;
            sumY += y;
            sumTT += t * t;
            sumTY += t * y;
            count++;
        }

        if (count < 2) {
            throw new IllegalArgumentException("Insufficient diastolic data for estimation");
        }

        double slope = (count * sumTY - sumT * sumY)
                / (count * sumTT - sumT * sumT);

        double kd = -slope;

        return new DiastolicParameters(pd, kd, notchIndex);
    }
}
