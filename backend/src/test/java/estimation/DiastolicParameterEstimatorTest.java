package estimation;

import model.DiastolicParameters;
import model.PressureSignal;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DiastolicParameterEstimatorTest {

    @Test
    void estimatesPdAndKdFromLastHalf() {
        double kd = 2.0;
        double dt = 0.1;
        int n = 11;
        double[] pressure = new double[n];
        for (int i = 0; i < n; i++) {
            double t = i * dt;
            pressure[i] = Math.exp(-kd * t);
        }

        PressureSignal signal = new PressureSignal(pressure, dt * (n - 1));
        DiastolicParameterEstimator estimator = new DiastolicParameterEstimator();

        DiastolicParameters params = estimator.estimate(signal, 0);

        assertEquals(pressure[n - 1], params.getPd(), 1e-9);
        assertEquals(kd, params.getKd(), 1e-3);
    }
}
