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

        if (isPowerOfTwo(signal.length)) {
            return transformer.transform(signal, TransformType.FORWARD);
        }

        return dft(signal);
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

        if (isPowerOfTwo(spectrum.length)) {
            Complex[] timeDomain = transformer.transform(
                    spectrum, TransformType.INVERSE
            );

            double[] result = new double[timeDomain.length];
            for (int i = 0; i < timeDomain.length; i++) {
                result[i] = timeDomain[i].getReal();
            }
            return result;
        }
        return idft(spectrum);
    }

    private static boolean isPowerOfTwo(int n) {
        return (n & (n - 1)) == 0;
    }

    private static Complex[] dft(double[] signal) {
        int n = signal.length;
        Complex[] spectrum = new Complex[n];
        for (int k = 0; k < n; k++) {
            double real = 0.0;
            double imag = 0.0;
            for (int t = 0; t < n; t++) {
                double angle = -2.0 * Math.PI * k * t / n;
                real += signal[t] * Math.cos(angle);
                imag += signal[t] * Math.sin(angle);
            }
            spectrum[k] = new Complex(real, imag);
        }
        return spectrum;
    }

    private static double[] idft(Complex[] spectrum) {
        int n = spectrum.length;
        double[] signal = new double[n];
        for (int t = 0; t < n; t++) {
            double real = 0.0;
            for (int k = 0; k < n; k++) {
                double angle = 2.0 * Math.PI * k * t / n;
                Complex term = spectrum[k].multiply(new Complex(Math.cos(angle), Math.sin(angle)));
                real += term.getReal();
            }
            signal[t] = real / n;
        }
        return signal;
    }
}