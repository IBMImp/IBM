package model;

public class ReservoirResult {

    private final double[] reservoirPressure;
    private final double[] excessPressure;
    private final DiastolicParameters diastolicParameters;

    public ReservoirResult(double[] reservoirPressure,
                           double[] excessPressure,
                           DiastolicParameters diastolicParameters) {

        if (reservoirPressure == null || excessPressure == null) {
            throw new IllegalArgumentException("Pressure arrays cannot be null");
        }
        if (reservoirPressure.length != excessPressure.length) {
            throw new IllegalArgumentException("Pressure arrays must have same length");
        }
        if (diastolicParameters == null) {
            throw new IllegalArgumentException("Diastolic parameters cannot be null");
        }

        this.reservoirPressure = reservoirPressure;
        this.excessPressure = excessPressure;
        this.diastolicParameters = diastolicParameters;
    }

    public double[] getReservoirPressure() {
        return reservoirPressure;
    }

    public double[] getExcessPressure() {
        return excessPressure;
    }

    public DiastolicParameters getDiastolicParameters() {
        return diastolicParameters;
    }
}
