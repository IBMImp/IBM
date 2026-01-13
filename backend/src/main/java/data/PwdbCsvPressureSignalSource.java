package data;

import model.ArterySite;
import model.PressureSignal;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Loads PWDB pressure waveforms from CSV.
 *
 * CSV format:
 *  - File: PWs_<Site>_P.csv
 *  - First column: patient number (1, 2, 3, ...) or pt<id>
 *  - Remaining columns: pressure samples
 */
public final class PwdbCsvPressureSignalSource implements PressureSignalSource {

    private static final Logger LOGGER = Logger.getLogger(PwdbCsvPressureSignalSource.class.getName());

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

        Path file = resolveCsvFile(site);
        double[] pressure = readPatientRow(file, patientId);

        double beatDurationSeconds = (pressure.length - 1) / sampleRateHz;
        return new PressureSignal(pressure, beatDurationSeconds);
    }

    private static String fileNameFor(ArterySite site) {
        return site.pressureFileName();
    }

    private Path resolveCsvFile(ArterySite site) {
        if (Files.isDirectory(csvDir)) {
            return csvDir.resolve(fileNameFor(site));
        }
        if (csvDir.toString().toLowerCase().endsWith(".csv")) {
            return csvDir;
        }
        throw new IllegalArgumentException("CSV path must be a directory or CSV file: " + csvDir);
    }

    private static double[] readPatientRow(Path file, String patientId) {
        String normalizedPatientId = normalizePatientId(patientId);

        try (BufferedReader br = Files.newBufferedReader(file)) {
            String line;
            int lineNumber = 0;

            while ((line = br.readLine()) != null) {
                lineNumber++;
                if (line.isBlank()) continue;

                String[] parts = line.split(",");
                if (parts.length < 2) continue;

                String id = normalizePatientId(parts[0]);
                if (!id.equals(normalizedPatientId)) continue;

                double[] samples = new double[parts.length - 1];
                for (int i = 1; i < parts.length; i++) {
                    String rawValue = parts[i].trim();
                    try {
                        samples[i - 1] = rawValue.isEmpty() ? Double.NaN : Double.parseDouble(rawValue);
                    } catch (NumberFormatException e) {
                        LOGGER.log(
                                Level.SEVERE,
                                "Invalid sample value for patient '{0}' at line {1}, column {2} in {3}: '{4}'",
                                new Object[]{patientId, lineNumber, i + 1, file, rawValue}
                        );
                        throw new IllegalArgumentException(
                                "Invalid sample value for patient '" + patientId + "' at line " + lineNumber
                                        + ", column " + (i + 1) + " in " + file + ": '" + rawValue + "'",
                                e
                        );
                    }
                }

                return sanitizeSamples(samples, patientId, file);
            }

            LOGGER.log(Level.WARNING, "Patient '{0}' not found in {1}", new Object[]{patientId, file});
            throw new IllegalArgumentException("Patient '" + patientId + "' not found in " + file);

        } catch (IOException e) {
            throw new UncheckedIOException("Failed reading " + file, e);
        }
    }

    private static String normalizePatientId(String raw) {
        String trimmed = raw.trim().toLowerCase();
        if (trimmed.isEmpty()) return trimmed;

        if (trimmed.startsWith("pt")) {
            String suffix = trimmed.substring(2).trim();
            return "pt" + suffix;
        }
        if (trimmed.matches("\\d+")) {
            return "pt" + trimmed;
        }
        return trimmed;
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