package preprocessing;

import model.DiastolicParameters;
import model.PressureSignal;

import java.util.ArrayList;
import java.util.List;

/**
 * Applies beat regularisation based on diastolic pressure comparisons.
 */
public class ContinuousBeatRegulariser implements BeatRegulariser {

    private static final double EPSILON = 1e-9;

    @Override
    public PressureSignal regularise(
            PressureSignal signal,
            DiastolicParameters diastolicParams,
            LocalMinimaDetector minimaDetector) {

        double[] pressure = signal.getPressure();
        double dt = signal.getTimeStep();
        List<Integer> minimaIndices = minimaDetector.findLocalMinimaIndices(signal);

        // Safety guard - use SingleBeatExtractor instead!
        if (minimaIndices.size() < 2) {
            // Not enough minima to define a full beat-to-beat segment.
            // Treat the signal as a single beat and return it unchanged.
            return signal;
        }

        double[] pd = new double[minimaIndices.size()];

        for (int i = 0; i < minimaIndices.size(); i++) {
            int index = minimaIndices.get(i);
            pd[i] = pressure[index];
        }

        List<Double> adjusted = new ArrayList<>();
        boolean firstBeat = true;

        for (int k = 0; k < minimaIndices.size() - 1; k++) {
            int startIndex = minimaIndices.get(k);
            int endIndex = minimaIndices.get(k + 1);

            double pdCurrent = pd[k];
            double pdNext = pd[k + 1];

            if (Math.abs(pdNext - pdCurrent) <= EPSILON) {
                appendSegment(pressure, adjusted, startIndex, endIndex, firstBeat);
            } else if (pdNext < pdCurrent) {
                int truncateIndex = findTruncationIndex(pressure, startIndex, endIndex, pdCurrent);
                appendSegment(pressure, adjusted, startIndex, truncateIndex, firstBeat);
            } else {
                appendSegment(pressure, adjusted, startIndex, endIndex, firstBeat);
                extendBeat(adjusted, dt, endIndex, diastolicParams, pdCurrent, pressure);
            }

            firstBeat = false;
        }

        int lastMinIndex = minimaIndices.get(minimaIndices.size() - 1);
        if (lastMinIndex > minimaIndices.get(minimaIndices.size() - 2)) {
            adjusted.add(pressure[lastMinIndex]);
        }

        double[] adjustedArray = adjusted.stream().mapToDouble(Double::doubleValue).toArray();
        double adjustedDuration = (adjustedArray.length - 1) * dt;

        return new PressureSignal(adjustedArray, adjustedDuration);
    }

    private void appendSegment(
            double[] pressure,
            List<Double> target,
            int startIndex,
            int endIndex,
            boolean includeStart) {

        int from = includeStart ? startIndex : startIndex + 1;
        for (int i = from; i <= endIndex; i++) {
            target.add(pressure[i]);
        }
    }

    private int findTruncationIndex(
            double[] pressure,
            int startIndex,
            int endIndex,
            double pdCurrent) {

        for (int i = startIndex + 1; i <= endIndex; i++) {
            if (pressure[i] <= pdCurrent) {
                int previous = i - 1;
                if (previous < startIndex) {
                    return i;
                }
                double previousDelta = Math.abs(pressure[previous] - pdCurrent);
                double currentDelta = Math.abs(pressure[i] - pdCurrent);
                return previousDelta <= currentDelta ? previous : i;
            }
        }
        return endIndex;
    }

    private void extendBeat(
            List<Double> adjusted,
            double dt,
            int endIndex,
            DiastolicParameters params,
            double pdCurrent,
            double[] pressure) {

        double kd = params.getKd();
        double pn = pressure[params.getNotchIndex()];

        double timeAtEnd = (endIndex - params.getNotchIndex()) * dt;
        double targetTime = Math.log(pn / pdCurrent) / kd;

        if (targetTime <= timeAtEnd) {
            return;
        }

        double relativeTarget = targetTime - timeAtEnd;
        int floorSamples = (int) Math.floor(relativeTarget / dt);
        int ceilSamples = (int) Math.ceil(relativeTarget / dt);
        int additionalSamples;
        if (floorSamples <= 0) {
            additionalSamples = 1;
        } else {
            double floorTime = timeAtEnd + floorSamples * dt;
            double ceilTime = timeAtEnd + ceilSamples * dt;
            double floorValue = pn * Math.exp(-kd * floorTime);
            double ceilValue = pn * Math.exp(-kd * ceilTime);
            double floorDelta = Math.abs(floorValue - pdCurrent);
            double ceilDelta = Math.abs(ceilValue - pdCurrent);
            additionalSamples = floorDelta <= ceilDelta ? floorSamples : ceilSamples;
        }

        for (int i = 1; i <= additionalSamples; i++) {
            double t = timeAtEnd + i * dt;
            double extrapolated = pn * Math.exp(-kd * t);
            adjusted.add(extrapolated);
        }
    }
}
