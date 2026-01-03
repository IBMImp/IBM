package preprocessing;

import model.PressureSignal;

public interface BeatExtractor {
    /**
     * @param input raw signal (either already one beat or continuous)
     * @return exactly one beat, aligned as needed
     */
    PressureSignal extract(PressureSignal input);
}