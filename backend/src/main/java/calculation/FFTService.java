package calculation;

import org.apache.commons.math3.complex.Complex;
import org.apache.commons.math3.transform.DftNormalization;
import org.apache.commons.math3.transform.FastFourierTransformer;
import org.apache.commons.math3.transform.TransformType;

import java.util.Arrays;

public class FFTService {

    private final FastFourierTransformer transformer;

    public FFTService() {
        this.transformer = new FastFourierTransformer(DftNormalization.STANDARD);
    }

    /**
     * Computes the forward FFT of a real-valued time-domain signal.
     * Zero-pads the signal to the next power of two if necessary.
     *
     * @param signal real-valued signal in the time domain
     * @return complex frequency spectrum
     */
    public Complex[] fft(double[] signal) {

        if (signal == null || signal.length < 2) {
            throw new IllegalArgumentException(
                    "Signal must contain at least two samples for FFT"
            );
        }

        int paddedLength = nextPowerOfTwo(signal.length);
        double[] paddedSignal = Arrays.copyOf(signal, paddedLength);

        return transformer.transform(paddedSignal, TransformType.FORWARD);
    }

    /**
     * Computes the inverse FFT and trims padding to recover
     * the original signal length.
     *
     * @param spectrum complex frequency-domain representation
     * @return reconstructed real-valued signal
     */
    public double[] ifft(Complex[] spectrum) {

        if (spectrum == null || spectrum.length < 2) {
            throw new IllegalArgumentException(
                    "Spectrum must contain at least two frequency components"
            );
        }

        Complex[] timeDomain = transformer.transform(
                spectrum, TransformType.INVERSE
        );

        double[] result = new double[timeDomain.length];
        for (int i = 0; i < timeDomain.length; i++) {
            result[i] = timeDomain[i].getReal();
        }

        return result;
    }

    /**
     * Computes the smallest power of two >= n.
     */
    private int nextPowerOfTwo(int n) {
        int pow = 1;
        while (pow < n) {
            pow <<= 1;
        }
        return pow;
    }
}