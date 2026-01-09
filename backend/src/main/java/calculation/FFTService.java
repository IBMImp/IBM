package calculation;

import org.apache.commons.math3.complex.Complex;
import org.apache.commons.math3.transform.DftNormalization;
import org.apache.commons.math3.transform.FastFourierTransformer;
import org.apache.commons.math3.transform.TransformType;

public class FFTService {

    // FFT engine from Apache Commons Math
    private final FastFourierTransformer transformer;

    public FFTService() {
        /*
         * STANDARD normalisation is used so that forward + inverse FFT
         * preserves the original signal amplitude. This matches the
         * assumptions made in Parker’s FFT-based derivation.
         */
        this.transformer = new FastFourierTransformer(DftNormalization.STANDARD);
    }

    /**
     * Computes the forward FFT of a real-valued time-domain signal.
     *
     * @param signal real-valued signal in the time domain (e.g. p(t) - Pd)
     * @return complex frequency spectrum with the same length as the input
     */
    public Complex[] fft(double[] signal) {

        if (signal == null || signal.length < 2) {
            throw new IllegalArgumentException(
                    "Signal must contain at least two samples for FFT"
            );
        }

        /*
         * The FFT library handles conversion from real input to
         * complex frequency components internally.
         */
        return transformer.transform(signal, TransformType.FORWARD);
    }

    /**
     * Computes the inverse FFT to recover a real-valued time-domain signal.
     *
     * @param spectrum complex frequency-domain representation
     * @return reconstructed real-valued signal in the time domain
     */
    public double[] ifft(Complex[] spectrum) {

        if (spectrum == null || spectrum.length < 2) {
            throw new IllegalArgumentException(
                    "Spectrum must contain at least two frequency components"
            );
        }

        /*
         * Inverse FFT returns complex values due to numerical error.
         * For a physically real signal, the imaginary part should be ~0
         * and can be safely discarded.
         */
        Complex[] timeDomain = transformer.transform(
                spectrum, TransformType.INVERSE
        );

        double[] result = new double[timeDomain.length];
        for (int i = 0; i < timeDomain.length; i++) {
            result[i] = timeDomain[i].getReal();
        }

        return result;
    }
}