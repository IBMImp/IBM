package application;

import data.PressureSignalSource;
import data.PwdbCsvPressureSignalSource;
import data.PostgresPressureSignalSource;
import model.ArterySite;
import model.PressureSignal;
import model.ReservoirResult;
import preprocessing.*;
import calculation.ReservoirCalculator;
import estimation.DiastolicParameterEstimator;
import estimation.SystolicParameterEstimator;
import service.ReservoirComputationPipeline;

import java.nio.file.Path;
import java.util.Objects;

public final class ReservoirService {
    public enum DataSourceMode {
        AUTO,
        CSV,
        POSTGRES;

        public static DataSourceMode fromString(String raw) {
            if (raw == null || raw.isBlank()) {
                return AUTO;
            }
            return switch (raw.trim().toLowerCase()) {
                case "auto" -> AUTO;
                case "csv" -> CSV;
                case "postgres", "postgresql", "pg" -> POSTGRES;
                default -> throw new IllegalArgumentException("Unsupported data mode: " + raw);
            };
        }
    }

    private final double sampleRateHz;
    private static final ArterySite DEFAULT_SITE = ArterySite.AorticRoot;
    private static final String DATABASE_URL_ENV = "DATABASE_URL";
    private static final String PGHOST_ENV = "PGHOST";
    private static final String PGPORT_ENV = "PGPORT";
    private static final String PGDATABASE_ENV = "PGDATABASE";
    private static final String PGUSER_ENV = "PGUSER";
    private static final String PGPASSWORD_ENV = "PGPASSWORD";

    private final Path defaultDataPath;
    private final DataSourceMode dataSourceMode;
    private final ReservoirComputationPipeline pipeline;

    public double getSampleRateHz() {
        return sampleRateHz;
    }

    public ReservoirService(Path defaultDataPath, double sampleRateHz) {
        this(defaultDataPath, sampleRateHz, DataSourceMode.AUTO);
    }

    public ReservoirService(Path defaultDataPath, double sampleRateHz, DataSourceMode dataSourceMode) {
        this.sampleRateHz = sampleRateHz;
        this.defaultDataPath = Objects.requireNonNull(defaultDataPath, "defaultDataPath");
        this.dataSourceMode = Objects.requireNonNull(dataSourceMode, "dataSourceMode");
        BeatExtractor extractor = new SingleBeatExtractor();
        BeatRegulariser postRegulariser = new IdentityBeatRegulariser();

        SignalSmoother smoother = new SavitzkyGolaySmoother(5, 5, 2);
        DicroticNotchDetector detector = new DicroticNotchDetector(smoother);
        NotchLocator notchLocator = detector;
        LocalMinimaDetector minimaDetector = detector;

        var diastolicEstimator = new DiastolicParameterEstimator();
        var systolicEstimator = new SystolicParameterEstimator();
        var reservoirCalculator = new ReservoirCalculator();

        this.pipeline = new ReservoirComputationPipeline(
                extractor, notchLocator, minimaDetector, postRegulariser,
                diastolicEstimator, systolicEstimator, reservoirCalculator
        );
    }

    public ComputationOutput compute(Path dataPath, String patientId) {
        return compute(dataPath, patientId, DEFAULT_SITE);
    }

    public ComputationOutput compute(Path dataPath, String patientId, ArterySite site) {
        return compute(dataPath, patientId, site, null);
    }

    public ComputationOutput compute(Path dataPath, String patientId, ArterySite site, Double sampleRateOverrideHz) {
        double rateToUse = sampleRateOverrideHz == null ? sampleRateHz : sampleRateOverrideHz;
        Path dataPathToUse = dataPath == null ? defaultDataPath : dataPath;
        PressureSignalSource source = buildSource(dataPathToUse, rateToUse);
        PressureSignal raw = source.load(patientId, site);
        ReservoirResult result = pipeline.run(raw);
        return new ComputationOutput(raw, result);
    }

    public record ComputationOutput(PressureSignal raw, ReservoirResult result) {}

    private PressureSignalSource buildSource(Path dataPath, double sampleRateHz) {
        if (dataSourceMode == DataSourceMode.POSTGRES) {
            return buildPostgresSource(sampleRateHz);
        }
        if (dataSourceMode == DataSourceMode.CSV) {
            return new PwdbCsvPressureSignalSource(dataPath, sampleRateHz);
        }
        String name = dataPath.getFileName().toString().toLowerCase();
        if (name.endsWith(".csv") || java.nio.file.Files.isDirectory(dataPath)) {
            return new PwdbCsvPressureSignalSource(dataPath, sampleRateHz);
        }
        return buildPostgresSource(sampleRateHz);
    }

    private static PostgresPressureSignalSource buildPostgresSource(double sampleRateHz) {
        String databaseUrl = System.getenv(DATABASE_URL_ENV);
        if (databaseUrl != null && !databaseUrl.isBlank()) {
            return PostgresPressureSignalSource.fromDatabaseUrl(databaseUrl, sampleRateHz);
        }
        String host = System.getenv(PGHOST_ENV);
        String database = System.getenv(PGDATABASE_ENV);
        if (host == null || host.isBlank() || database == null || database.isBlank()) {
            throw new IllegalStateException(
                    "DATABASE_URL or PGHOST/PGDATABASE must be set for PostgreSQL waveform access");
        }
        String port = System.getenv(PGPORT_ENV);
        String username = System.getenv(PGUSER_ENV);
        String password = System.getenv(PGPASSWORD_ENV);
        String jdbcUrl = "jdbc:postgresql://" + host + ":" + normalizePort(port) + "/" + database;
        return new PostgresPressureSignalSource(jdbcUrl, username, password, sampleRateHz);
    }

    private static String normalizePort(String port) {
        if (port == null || port.isBlank()) {
            return "8888";
        }
        return port.trim();
    }
}
