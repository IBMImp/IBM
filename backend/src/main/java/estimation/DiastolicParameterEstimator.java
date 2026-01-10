package estimation;

import model.PressureSignal;
import model.DiastolicParameters;

public class DiastolicParameterEstimator {

    public DiastolicParameters estimate(PressureSignal signal, int notchIndex) {

        double[] p = signal.getPressure();
        double dt = signal.getTimeStep();
        int n = p.length;

        if (n < 2) {
            throw new IllegalArgumentException("Pressure array must contain at least two points");
        }
        // 1. Estimate Pd as minimum pressure in the beat (MATLAB: Pd = min(Pdata))
        double pd = Double.POSITIVE_INFINITY;
        for (double value : p) {
            if (value < pd) {
                pd = value;
            }
        }

        // 2. Estimate kd from log-linear fit on the last half of the beat.
        int nHalf = (int) Math.round(n / 2.0);
        int startIndex = Math.max(nHalf - 1, 0);

        double sumT = 0.0;
        double sumY = 0.0;
        double sumTT = 0.0;
        double sumTY = 0.0;
        int count = 0;

        for (int i = startIndex; i < n; i++) {
            double value = p[i];
            if (value <= 0) {
                throw new IllegalArgumentException("Pressure values must be positive for log fit");
            }


            double t = (i - startIndex) * dt;
            double y = Math.log(value);

            sumT += t;
            sumY += y;
            sumTT += t * t;
            sumTY += t * y;
            count++;
        }

        if (count < 2) {
            throw new IllegalArgumentException("Insufficient diastolic data for estimation");
        }

        double denominator = count * sumTT - sumT * sumT;
        if (denominator == 0.0) {
            throw new IllegalArgumentException("Unable to fit diastolic decay constant");
        }

        double slope = (count * sumTY - sumT * sumY) / denominator;
        double kd = -slope;

        return new DiastolicParameters(pd, kd, notchIndex);
        }
    }
