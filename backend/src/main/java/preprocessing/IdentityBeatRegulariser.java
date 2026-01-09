package preprocessing;

import model.DiastolicParameters;
import model.PressureSignal;

public final class IdentityBeatRegulariser implements BeatRegulariser {
    @Override
    public PressureSignal regularise(PressureSignal beat,
                                     DiastolicParameters diastolicParams,
                                     LocalMinimaDetector minimaDetector) {
        return beat;
    }
}