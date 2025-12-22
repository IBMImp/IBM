package model;

public class PressureSignal {

    private final double[] pressure;
    private final double beatDuration;

    public PressureSignal(double[] pressure, double beatDuration) {
        if (pressure == null || pressure.length < 2) {
            throw new IllegalArgumentException("Pressure array must contain at least two points");
        }
        if (beatDuration <= 0) {
            throw new IllegalArgumentException("Beat duration must be positive");
        }

        this.pressure = pressure;
        this.beatDuration = beatDuration;
    }

    public double[] getPressure() {
        return pressure;
    }

    public double getBeatDuration() {
        return beatDuration;
    }

    public int getNumSamples() {
        return pressure.length;
    }

    public double getTimeStep() {
        return beatDuration / (pressure.length - 1);
    }
}
