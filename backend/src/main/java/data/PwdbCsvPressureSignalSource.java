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
 *  - Header row: patient ids (pt1, pt2, ...), with pressure samples in columns
 *  - First column: sample index or time values
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
            String headerLine = nextNonBlankLine(br);
            if (headerLine == null) {
                throw new IllegalArgumentException("CSV file is empty: " + file);
            }

            String[] headerParts = headerLine.split(",");
            int patientColumnIndex = findPatientColumnIndex(headerParts, normalizedPatientId);
            if (patientColumnIndex < 0) {
                throw new IllegalArgumentException(
                        "Patient '" + patientId + "' not found in header of " + file
                );
            }

            double[] samples = readPatientColumn(br, patientColumnIndex);
            return sanitizeSamples(samples, patientId, file);

        } catch (IOException e) {
            throw new UncheckedIOException("Failed reading " + file, e);
        }
    }

    private static String nextNonBlankLine(BufferedReader br) throws IOException {
        String line;
        while ((line = br.readLine()) != null) {
            if (!line.isBlank()) {
                return line;
            }
        }
        return null;
    }

    private static int findPatientColumnIndex(String[] headerParts, String normalizedPatientId) {
        for (int i = 0; i < headerParts.length; i++) {
            String headerId = normalizePatientId(headerParts[i]);
            if (headerId.equals(normalizedPatientId)) {
                return i;
            }
        }
        return -1;
    }

    private static double[] readPatientColumn(BufferedReader br, int columnIndex) throws IOException {
        double[] buffer = new double[1024];
        int count = 0;
        String line;
        while ((line = br.readLine()) != null) {
            if (line.isBlank()) {
                continue;
            }
            String[] parts = line.split(",");
            if (parts.length <= columnIndex) {
                continue;
            }
            String rawValue = parts[columnIndex].trim();
            double value = rawValue.isEmpty() ? Double.NaN : Double.parseDouble(rawValue);
            if (count == buffer.length) {
                buffer = Arrays.copyOf(buffer, buffer.length * 2);
            }
            buffer[count++] = value;
        }
        return Arrays.copyOf(buffer, count);
    }

    private static String normalizePatientId(String raw) {
        String trimmed = raw.trim().toLowerCase();
        if (trimmed.isEmpty()) {
            return trimmed;
        }
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
