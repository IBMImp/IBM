package preprocessing;

import model.DiastolicParameters;
import model.PressureSignal;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertEquals;

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
                new double[]{5, 4, 3, 4, 2, 3, 2},
                0.6
        );

        PressureSignal result = regulariser.regularise(
                signal,
                new DiastolicParameters(1.0, 1.0, 0),
                input -> List.of(0, 3, 6)
        );

        assertEquals(4, result.getNumSamples());
        assertEquals(0.3, result.getBeatDuration(), 1e-9);
        assertEquals(5.0, result.getPressure()[0], 1e-9);
        assertEquals(2.0, result.getPressure()[result.getNumSamples() - 1], 1e-9);
    }

    @Test
    void continuousRegulariserExtendsWhenPdRises() {
        ContinuousBeatRegulariser regulariser = new ContinuousBeatRegulariser();
        PressureSignal signal = new PressureSignal(
                new double[]{1, 4, 3, 2, 1, 1.5, 1},
                0.6
        );

        PressureSignal result = regulariser.regularise(
                signal,
                new DiastolicParameters(1.0, 1.0, 1),
                input -> List.of(0, 3, 6)
        );

        assertEquals(18, result.getNumSamples());
        assertEquals(1.7, result.getBeatDuration(), 1e-9);
        assertEquals(1.0, result.getPressure()[result.getNumSamples() - 1], 1e-9);
    }

    @Test
    void continuousRegulariserKeepsPdWhenNoExtensionNeeded() {
        ContinuousBeatRegulariser regulariser = new ContinuousBeatRegulariser();
        PressureSignal signal = new PressureSignal(
                new double[]{2, 3, 2.5, 2, 1.2, 1, 0.8},
                0.6
        );

        PressureSignal result = regulariser.regularise(
                signal,
                new DiastolicParameters(0.8, 2.0, 1),
                input -> List.of(0, 3, 6)
        );

        assertEquals(7, result.getNumSamples());
        assertEquals(0.6, result.getBeatDuration(), 1e-9);
        assertEquals(2.0, result.getPressure()[0], 1e-9);
        assertEquals(0.8, result.getPressure()[result.getNumSamples() - 1], 1e-9);
    }
}