package deployment;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import service.FrontendService;
import model.ArterySite;
import model.PressureSignal;
import model.ReservoirResult;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class ApiController {
    private final DataSource dataSource;
    private final FrontendService frontendService;

    public ApiController(DataSource dataSource, FrontendService frontendService) {
        this.dataSource = dataSource;
        this.frontendService = frontendService;
    }

    @GetMapping("/health")
    public Map<String, String> health() {
        Map<String, String> payload = new HashMap<>();
        payload.put("status", "ok");
        return payload;
    }

    @GetMapping("/db")
    public ResponseEntity<Map<String, String>> databaseStatus() {
        Map<String, String> payload = new HashMap<>();
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement("SELECT 1");
             ResultSet resultSet = statement.executeQuery()) {
            if (resultSet.next()) {
                payload.put("status", "ok");
                payload.put("result", String.valueOf(resultSet.getInt(1)));
                return ResponseEntity.ok(payload);
            }
        } catch (SQLException exception) {
            payload.put("status", "error");
            payload.put("message", exception.getMessage());
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(payload);
        }
        payload.put("status", "error");
        payload.put("message", "No response from database.");
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(payload);
    }

    @GetMapping("/compute")
    public ComputationResponse compute(@RequestParam("patientId") String patientId,
                                       @RequestParam(value = "dataPath", required = false) String dataPath,
                                       @RequestParam(value = "arterySite", required = false) String arterySite,
                                       @RequestParam(value = "sampleRateHz", required = false) Double sampleRateHz) {
        ArterySite site = resolveSite(arterySite);
        Double normalizedSampleRateHz = normalizeSampleRate(sampleRateHz);
        FrontendService.ComputationOutput output =
                frontendService.compute(
                        dataPath == null ? null : java.nio.file.Path.of(dataPath),
                        patientId,
                        site,
                        normalizedSampleRateHz
                );
        PressureSignal raw = output.raw();
        ReservoirResult result = output.result();
        double responseSampleRateHz = normalizedSampleRateHz == null
                ? frontendService.getSampleRateHz()
                : normalizedSampleRateHz;
        return new ComputationResponse(
                raw.getPressure(),
                result.getReservoirPressure(),
                result.getExcessPressure(),
                raw.getBeatDuration(),
                responseSampleRateHz
        );
    }

    public record ComputationResponse(
            double[] pressure,
            double[] reservoirPressure,
            double[] excessPressure,
            double beatDurationSeconds,
            double sampleRateHz
    ) {}

    private static ArterySite resolveSite(String arterySite) {
        if (arterySite == null || arterySite.isBlank()) {
            return ArterySite.AorticRoot;
        }
        return ArterySite.fromToken(arterySite);
    }

    private static Double normalizeSampleRate(Double sampleRateHz) {
        if (sampleRateHz == null) {
            return null;
        }
        if (sampleRateHz <= 0) {
            throw new IllegalArgumentException("sampleRateHz must be positive");
        }
        return sampleRateHz;
    }
}
