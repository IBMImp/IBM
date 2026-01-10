package data;

import model.ArterySite;
import model.PressureSignal;

import java.io.*;
import java.nio.file.*;
import java.util.Arrays;
import java.util.Objects;

/**
 * Loads PWDB pressure waveforms from CSV.
 *
 * CSV format:
 *  - File: PWs_<Site>_P.csv
 *  - First column: patient id (pt1, pt2, ...)
 *  - Remaining columns: pressure samples
 */

public final class PwdbCsvPressureSignalSource implements PressureSignalSource {

    private final Path csvDir;
    private final double sampleRateHz;

    public PwdbCsvPressureSignalSource(Path csvDir, double sampleRateHz) {
        this.csvDir = Objects.requireNonNull(csvDir, "csvDir");
        this.sampleRateHz = sampleRateHz;
    }

    @Override
    public PressureSignal load(String patientId, ArterySite site) {
        Objects.requireNonNull(patientId, "patientId");
        Objects.requireNonNull(site, "site");

        Path file = csvDir;
        double[] pressure = readPatientRow(file, patientId);


        double beatDurationSeconds = (pressure.length - 1) / sampleRateHz;
        return new PressureSignal(pressure, beatDurationSeconds);
    }

    private static String fileNameFor(ArterySite site) {
        // Assumes enum names match file tokens exactly
        // e.g. AorticRoot -> PWs_AorticRoot_P.csv
        return "PWs_" + site.name() + "_P.csv";
    }

    private static double[] readPatientRow(Path file, String patientId) {
        try (BufferedReader br = Files.newBufferedReader(file)) {
            String line;

            while ((line = br.readLine()) != null) {
                if (line.isBlank()) continue;

                String[] parts = line.split(",");
                if (parts.length < 2) continue;

                String id = parts[0].trim();

                // Skip header orcomment rows defensively
                if (looksLikeHeader(id)) continue;

                if (!id.equals(patientId)) continue;

                double[] samples = new double[parts.length - 1];
                for (int i = 1; i < parts.length; i++) {
                    samples[i - 1] = Double.parseDouble(parts[i].trim());
                }
                return sanitizeSamples(samples, patientId, file);
            }

            throw new IllegalArgumentException(
                    "Patient '" + patientId + "' not found in " + file
            );

        } catch (IOException e) {
            throw new UncheckedIOException("Failed reading " + file, e);
        }
    }

    private static boolean looksLikeHeader(String cell) {
        String s = cell.toLowerCase();
        return s.equals("patient")
                || s.equals("patientid")
                || s.startsWith("#");
    }
    private static double[] sanitizeSamples(double[] samples, String patientId, Path file) {
        int lastValid = samples.length - 1;
        while (lastValid >= 0 && Double.isNaN(samples[lastValid])) {
            lastValid--;
        }
        if (lastValid < 1) {
            throw new IllegalArgumentException(
                    "Pressure signal for patient '" + patientId + "' in " + file
                            + " contains fewer than two valid samples"
            );
        }
        double[] trimmed = Arrays.copyOf(samples, lastValid + 1);
        for (double value : trimmed) {
            if (Double.isNaN(value)) {
                throw new IllegalArgumentException(
                        "Pressure signal for patient '" + patientId + "' in " + file
                                + " contains NaN samples"
                );
            }
        }
        return trimmed;
    }
}
