package data;

import model.ArterySite;
import model.PressureSignal;

import java.net.URI;
import java.net.URISyntaxException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Objects;
import java.util.Properties;

/**
 * Loads pressure waveforms from a PostgreSQL database.
 *
 * Expected schema:
 *  - table: pressure_waveforms
 *  - columns: patient_id (TEXT), site (TEXT), samples (TEXT)
 */
public final class PostgresPressureSignalSource implements PressureSignalSource {

    private static final String SQL = """
            SELECT samples
            FROM pressure_waveforms
            WHERE patient_id = ?
              AND site = ?
            """;

    private final String jdbcUrl;
    private final Properties connectionProps;
    private final double sampleRateHz;

    public PostgresPressureSignalSource(String jdbcUrl, String username, String password, double sampleRateHz) {
        this.jdbcUrl = Objects.requireNonNull(jdbcUrl, "jdbcUrl");
        this.sampleRateHz = sampleRateHz;
        this.connectionProps = new Properties();
        if (username != null && !username.isBlank()) {
            connectionProps.setProperty("user", username);
        }
        if (password != null && !password.isBlank()) {
            connectionProps.setProperty("password", password);
        }
    }

    public static PostgresPressureSignalSource fromDatabaseUrl(String databaseUrl, double sampleRateHz) {
        Objects.requireNonNull(databaseUrl, "databaseUrl");
        if (databaseUrl.startsWith("jdbc:")) {
            return new PostgresPressureSignalSource(databaseUrl, null, null, sampleRateHz);
        }
        try {
            URI uri = new URI(databaseUrl);
            String username = null;
            String password = null;
            if (uri.getUserInfo() != null) {
                String[] parts = uri.getUserInfo().split(":", 2);
                username = parts[0];
                if (parts.length > 1) {
                    password = parts[1];
                }
            }
            String host = uri.getHost();
            int port = uri.getPort() == -1 ? 5432 : uri.getPort();
            String path = uri.getPath();
            String jdbcUrl = "jdbc:postgresql://" + host + ":" + port + path;
            return new PostgresPressureSignalSource(jdbcUrl, username, password, sampleRateHz);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Invalid database URL: " + databaseUrl, e);
        }
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
        try (Connection connection = connectionProps.isEmpty()
                ? DriverManager.getConnection(jdbcUrl)
                : DriverManager.getConnection(jdbcUrl, connectionProps);
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
