package preprocessing;

import model.PressureSignal;
import mr.go.sgfilter.SGFilter;

import java.util.Objects;

/**
 * Savitzky-Golay implementation of {@link SignalSmoother}.
 */
public class SavitzkyGolaySmoother implements SignalSmoother {

    private final int nl;
    private final int nr;
    private final int polynomialOrder;

    public SavitzkyGolaySmoother(int nl, int nr, int polynomialOrder) {
        if (nl <= 0 || nr <= 0) {
            throw new IllegalArgumentException("nl and nr must be positive to form a smoothing window");
        }
        if (polynomialOrder < 0) {
            throw new IllegalArgumentException("Polynomial order must be non-negative");
        }
        this.nl = nl;
        this.nr = nr;
        this.polynomialOrder = polynomialOrder;
    }

    @Override
    public double[] smooth(PressureSignal signal) {
        Objects.requireNonNull(signal, "Pressure signal cannot be null");
        double[] pressures = signal.getPressure();
        if (pressures.length < nl + nr + 1) {
            throw new IllegalArgumentException("Pressure signal shorter than smoothing window");
        }

        double[] coeffs = SGFilter.computeSGCoefficients(nl, nr, polynomialOrder);
        SGFilter sgFilter = new SGFilter(nl, nr);
        return sgFilter.smooth(pressures, coeffs);
    }


    public int getNl() {
        return nl;
    }

    public int getNr() {
        return nr;
    }

    public int getPolynomialOrder() {
        return polynomialOrder;
    }
}
