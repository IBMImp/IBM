package preprocessing;

import model.PressureSignal;

public final class IdentityBeatExtractor implements BeatExtractor {
    @Override
    public PressureSignal extract(PressureSignal input) {
        return input;
    }
}
