package preprocessing;

import model.PressureSignal;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DicroticNotchDetectorTest {

    private static final SignalSmoother IDENTITY_SMOOTHER = signal -> signal.getPressure();

    @Test
    void excludesEdgesWhenLocatingNotch() {
        double[] pressure = new double[]{100, 105, 110, 90, 10};
        PressureSignal signal = new PressureSignal(pressure, 0.4);

        DicroticNotchDetector detector = new DicroticNotchDetector(IDENTITY_SMOOTHER);
        int notchIndex = detector.locateNotch(signal);

        assertTrue(notchIndex > 0);
        assertTrue(notchIndex < pressure.length - 1);
        assertEquals(3, notchIndex);
    }

    @Test
    void rejectsSignalsTooShortForNotchDetection() {
        double[] pressure = new double[]{100, 98};
        PressureSignal signal = new PressureSignal(pressure, 0.1);

        DicroticNotchDetector detector = new DicroticNotchDetector(IDENTITY_SMOOTHER);

        assertThrows(IllegalArgumentException.class, () -> detector.locateNotch(signal));
    }
}
