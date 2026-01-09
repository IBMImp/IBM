package preprocessing;

import model.PressureSignal;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ContinuousBeatExtractorTest {

    @Test
    void extractsFirstBeatBetweenMinimaAndAlignsPd() {
        LocalMinimaDetector minimaDetector = signal -> List.of(2, 5);
        ContinuousBeatExtractor extractor = new ContinuousBeatExtractor(minimaDetector);

        double[] pressure = new double[]{5, 4, 3, 2, 1, 2, 4, 6};
        PressureSignal input = new PressureSignal(pressure, 0.7);

        PressureSignal beat = extractor.extract(input);

        assertEquals(4, beat.getNumSamples());
        assertEquals(1.0, beat.getPressure()[0], 1e-9);
        assertEquals(0.3, beat.getBeatDuration(), 1e-9);
    }

    @Test
    void rejectsSignalsWithoutEnoughMinima() {
        LocalMinimaDetector minimaDetector = signal -> List.of(1);
        ContinuousBeatExtractor extractor = new ContinuousBeatExtractor(minimaDetector);

        double[] pressure = new double[]{5, 4, 3, 2};
        PressureSignal input = new PressureSignal(pressure, 0.3);

        assertThrows(IllegalArgumentException.class, () -> extractor.extract(input));
    }
}
