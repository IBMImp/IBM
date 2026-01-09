package preprocessing;

import model.DiastolicParameters;
import model.PressureSignal;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BeatRegulariserTest {

    @Test
    void identityRegulariserReturnsSameSignal() {
        IdentityBeatRegulariser regulariser = new IdentityBeatRegulariser();
        PressureSignal signal = new PressureSignal(new double[]{1, 2, 3}, 0.2);

        PressureSignal result = regulariser.regularise(
                signal,
                new DiastolicParameters(1.0, 1.0, 0),
                input -> List.of(0, 2)
        );

        assertSame(signal, result);
    }

    @Test
    void continuousRegulariserKeepsSignalWhenMinimaMissing() {
        ContinuousBeatRegulariser regulariser = new ContinuousBeatRegulariser();
        PressureSignal signal = new PressureSignal(new double[]{3, 2, 1}, 0.2);

        PressureSignal result = regulariser.regularise(
                signal,
                new DiastolicParameters(1.0, 1.0, 0),
                input -> List.of(1)
        );

        assertSame(signal, result);
    }

    @Test
    void continuousRegulariserTruncatesWhenPdDrops() {
        ContinuousBeatRegulariser regulariser = new ContinuousBeatRegulariser();
        PressureSignal signal = new PressureSignal(
                new double[]{3.0, 3.4, 3.2, 3.05, 2.9, 2.0},
                0.5
        );

        PressureSignal result = regulariser.regularise(
                signal,
                new DiastolicParameters(1.0, 1.0, 0),
                input -> List.of(0, 5)
        );

        assertEquals(5, result.getNumSamples());
        assertEquals(0.4, result.getBeatDuration(), 1e-9);
        assertEquals(3.0, result.getPressure()[0], 1e-9);
        assertEquals(2.0, result.getPressure()[result.getNumSamples() - 1], 1e-9);
    }

    @Test
    void continuousRegulariserExtendsWhenPdRises() {
        ContinuousBeatRegulariser regulariser = new ContinuousBeatRegulariser();
        PressureSignal signal = new PressureSignal(
                new double[]{1, 4, 3, 2, 1, 1.5, 1},
                0.6
        );
        int originalSamples = signal.getNumSamples();

        PressureSignal result = regulariser.regularise(
                signal,
                new DiastolicParameters(1.0, 1.0, 1),
                input -> List.of(0, 3, 6)
        );

        assertTrue(result.getNumSamples() > originalSamples);
        assertTrue(result.getBeatDuration() > signal.getBeatDuration());
        assertEquals(1.0, result.getPressure()[result.getNumSamples() - 1], 1e-9);
    }

    @Test
    void continuousRegulariserKeepsPdWhenNoExtensionNeeded() {
        ContinuousBeatRegulariser regulariser = new ContinuousBeatRegulariser();
        PressureSignal signal = new PressureSignal(
                new double[]{2, 3, 2.5, 2.1, 2.05, 2.0, 2.0},
                0.6
        );

        PressureSignal result = regulariser.regularise(
                signal,
                new DiastolicParameters(0.8, 2.0, 1),
                input -> List.of(0, 6)
        );

        assertEquals(8, result.getNumSamples());
        assertEquals(0.7, result.getBeatDuration(), 1e-9);
        assertEquals(2.0, result.getPressure()[0], 1e-9);
        assertEquals(2.0, result.getPressure()[result.getNumSamples() - 1], 1e-9);
    }

    @Test
    void continuousRegulariserTruncatesAtNearestSample() {
        ContinuousBeatRegulariser regulariser = new ContinuousBeatRegulariser();
        PressureSignal signal = new PressureSignal(
                new double[]{4.0, 4.3, 4.1, 4.02, 3.95, 3.5},
                0.5
        );

        PressureSignal result = regulariser.regularise(
                signal,
                new DiastolicParameters(1.0, 1.0, 0),
                input -> List.of(0, 5)
        );

        assertEquals(5, result.getNumSamples());
        assertEquals(0.4, result.getBeatDuration(), 1e-9);
        assertEquals(4.0, result.getPressure()[0], 1e-9);
        assertEquals(3.5, result.getPressure()[result.getNumSamples() - 1], 1e-9);
    }
}
