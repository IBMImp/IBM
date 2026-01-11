package data;

import model.PressureSignal;

public class ContinuousWaveformGenerator {

    private int i;
    private double t = 0.0;
    private PressureSignal beat;

    public ContinuousWaveformGenerator(PressureSignal beat, int fs) {
        this.beat = beat;

    }

    public PressurePoint getPoint() {
        return new PressurePoint(
                beat.getPressure()[++i % beat.getNumSamples()],
                t+=beat.getTimeStep()
        );

    }

    public record PressurePoint(double p, double t) {};

}
