package mr.go.sgfilter;

import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.QRDecomposition;
import org.apache.commons.math3.linear.RealMatrix;

/**
 * Minimal Savitzky-Golay filter implementation for smoothing signals.
 */
public class SGFilter {

    private final int nl;
    private final int nr;

    public SGFilter(int nl, int nr) {
        if (nl < 0 || nr < 0) {
            throw new IllegalArgumentException("nl and nr must be non-negative");
        }
        this.nl = nl;
        this.nr = nr;
    }

    /**
     * Computes the smoothing coefficients for a Savitzky-Golay filter.
     *
     * @param nl           number of points to the left of the central sample
     * @param nr           number of points to the right of the central sample
     * @param polynomial   polynomial order to fit
     * @return coefficients to convolve with the signal
     */
    public static double[] computeSGCoefficients(int nl, int nr, int polynomial) {
        if (nl < 0 || nr < 0) {
            throw new IllegalArgumentException("nl and nr must be non-negative");
        }
        if (polynomial < 0) {
            throw new IllegalArgumentException("polynomial order must be non-negative");
        }

        int samples = nl + nr + 1;
        int columns = polynomial + 1;

        double[][] aData = new double[samples][columns];
        for (int i = -nl; i <= nr; i++) {
            for (int j = 0; j <= polynomial; j++) {
                aData[i + nl][j] = Math.pow(i, j);
            }
        }

        RealMatrix a = MatrixUtils.createRealMatrix(aData);
        RealMatrix ata = a.transpose().multiply(a);
        RealMatrix ataInv = new QRDecomposition(ata).getSolver().getInverse();
        RealMatrix coefficients = ataInv.multiply(a.transpose()).getRowMatrix(0);
        return coefficients.getRow(0);
    }

    /**
     * Applies the filter to smooth the given series.
     *
     * @param values    raw signal values
     * @param coeffs    coefficients computed by {@link #computeSGCoefficients(int, int, int)}
     * @return smoothed signal
     */
    public double[] smooth(double[] values, double[] coeffs) {
        if (values == null || coeffs == null) {
            throw new IllegalArgumentException("values and coeffs must not be null");
        }
        double[] smoothed = new double[values.length];

        for (int i = 0; i < values.length; i++) {
            double sum = 0.0;
            for (int j = -nl; j <= nr; j++) {
                int index = clamp(i + j, 0, values.length - 1);
                sum += coeffs[j + nl] * values[index];
            }
            smoothed[i] = sum;
        }

        return smoothed;
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
