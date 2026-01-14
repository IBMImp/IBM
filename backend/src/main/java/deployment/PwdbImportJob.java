package deployment;

import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.zip.GZIPInputStream;

@Component
public class PwdbImportJob implements CommandLineRunner {

    private final JdbcTemplate jdbc;

    public PwdbImportJob(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void run(String... args) throws Exception {
        if (!"true".equalsIgnoreCase(System.getenv("PWDB_IMPORT"))) {
            return; // do nothing on normal starts
        }

        String importUrl = System.getenv("PWDB_IMPORT_URL");
        if (importUrl == null || importUrl.isBlank()) {
            throw new IllegalStateException("PWDB_IMPORT_URL not set");
        }

        // Schema (idempotent)
        jdbc.execute("""
            CREATE TABLE IF NOT EXISTS pressure_waveforms (
              patient_id TEXT NOT NULL,
              site       TEXT NOT NULL,
              samples    TEXT NOT NULL,
              PRIMARY KEY (patient_id, site)
            )
        """);

        System.out.println("PWDB import starting. URL=" + importUrl);

        try (InputStream raw = new URL(importUrl).openStream();
             InputStream in = importUrl.endsWith(".gz") ? new GZIPInputStream(raw) : raw;
             BufferedReader br = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
             Connection conn = jdbc.getDataSource().getConnection()) {

            conn.setAutoCommit(false);

            try (PreparedStatement ps = conn.prepareStatement("""
                INSERT INTO pressure_waveforms(patient_id, site, samples)
                VALUES (?, ?, ?)
                ON CONFLICT (patient_id, site) DO UPDATE SET samples = EXCLUDED.samples
            """)) {

                final int batchSize = 500;
                int count = 0;

                String line;
                while ((line = br.readLine()) != null) {
                    if (line.isBlank()) continue;

                    // Your export format: patient_id,site,samples
                    // samples contains commas, so split only at first two commas:
                    int c1 = line.indexOf(',');
                    int c2 = line.indexOf(',', c1 + 1);
                    if (c1 < 0 || c2 < 0) continue;

                    String patientId = line.substring(0, c1).trim();
                    String site = line.substring(c1 + 1, c2).trim();
                    String samples = line.substring(c2 + 1).trim();

                    ps.setString(1, patientId);
                    ps.setString(2, site);
                    ps.setString(3, samples);
                    ps.addBatch();

                    if (++count % batchSize == 0) {
                        ps.executeBatch();
                        conn.commit();
                        System.out.println("Imported rows: " + count);
                    }
                }

                ps.executeBatch();
                conn.commit();
                System.out.println("PWDB import complete. Total rows: " + count);
            } catch (Exception e) {
                conn.rollback();
                throw e;
            }
        }
    }
}