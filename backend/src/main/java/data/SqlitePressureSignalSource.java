package data;

import model.ArterySite;
import model.PressureSignal;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Objects;

/**
 * Loads pressure waveforms from a SQLite database.
 *
 * Expected schema:
 *  - table: pressure_waveforms
 *  - columns: patient_id (TEXT), site (TEXT), samples (TEXT)
 */
public final class SqlitePressureSignalSource implements PressureSignalSource {

    private static final String SQL = """
            SELECT samples
            FROM pressure_waveforms
            WHERE patient_id = ?
              AND site = ?
            """;

    private final String jdbcUrl;
    private final double sampleRateHz;

    public SqlitePressureSignalSource(Path databaseFile, double sampleRateHz) {
        Objects.requireNonNull(databaseFile, "databaseFile");
        this.jdbcUrl = "jdbc:sqlite:" + databaseFile.toAbsolutePath();
        this.sampleRateHz = sampleRateHz;
    }

    @Override
    public PressureSignal load(String patientId, ArterySite site) {
        Objects.requireNonNull(patientId, "patientId");
        Objects.requireNonNull(site, "site");

        String samples = fetchSamples(patientId, site.token());
        double[] pressure = parseSamples(samples, patientId, site);
        double beatDurationSeconds = (pressure.length - 1) / sampleRateHz;
        return new PressureSignal(pressure, beatDurationSeconds);
    }

    private String fetchSamples(String patientId, String siteToken) {
        try (Connection connection = DriverManager.getConnection(jdbcUrl);
             PreparedStatement statement = connection.prepareStatement(SQL)) {
            statement.setString(1, patientId);
            statement.setString(2, siteToken);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("samples");
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException(
                    "Failed to read pressure waveform from database: " + e.getMessage(), e);
        }
        throw new IllegalArgumentException(
                "Patient '" + patientId + "' with site '" + siteToken + "' not found in database");
    }

    private static double[] parseSamples(String samples, String patientId, ArterySite site) {
        if (samples == null || samples.isBlank()) {
            throw new IllegalArgumentException(
                    "Pressure signal for patient '" + patientId + "' at site '" + site.token()
                            + "' is empty");
        }
        String[] parts = samples.split(",");
        double[] values = new double[parts.length];
        for (int i = 0; i < parts.length; i++) {
            values[i] = Double.parseDouble(parts[i].trim());
        }
        return sanitizeSamples(values, patientId, site);
    }

    private static double[] sanitizeSamples(double[] samples, String patientId, ArterySite site) {
        int lastValid = samples.length - 1;
        while (lastValid >= 0 && Double.isNaN(samples[lastValid])) {
            lastValid--;
        }
        if (lastValid < 1) {
            throw new IllegalArgumentException(
                    "Pressure signal for patient '" + patientId + "' at site '" + site.token()
                            + "' contains fewer than two valid samples"
            );
        }
        double[] trimmed = Arrays.copyOf(samples, lastValid + 1);
        for (double value : trimmed) {
            if (Double.isNaN(value)) {
                throw new IllegalArgumentException(
                        "Pressure signal for patient '" + patientId + "' at site '" + site.token()
                                + "' contains NaN samples"
                );
            }
        }
        return trimmed;
    }
}
