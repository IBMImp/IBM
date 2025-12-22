package model;

public class DiastolicParameters {

    private final double pd;
    private final double kd;
    private final int notchIndex;

    public DiastolicParameters(double pd, double kd, int notchIndex) {
        if (pd <= 0) {
            throw new IllegalArgumentException("Diastolic pressure must be positive");
        }
        if (kd <= 0) {
            throw new IllegalArgumentException("Diastolic decay constant must be positive");
        }
        if (notchIndex < 0) {
            throw new IllegalArgumentException("Notch index must be non-negative");
        }

        this.pd = pd;
        this.kd = kd;
        this.notchIndex = notchIndex;
    }

    public double getPd() {
        return pd;
    }

    public double getKd() {
        return kd;
    }

    public int getNotchIndex() {
        return notchIndex;
    }
}
