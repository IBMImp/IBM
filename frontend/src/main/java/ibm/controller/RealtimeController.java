package ibm.controller;

import application.ReservoirService;
import com.fasterxml.jackson.databind.ObjectMapper;
import model.ArterySite;

import javax.swing.*;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.net.ConnectException;
import java.util.function.Consumer;

public final class RealtimeController {
    private static final String BACKEND_URL_ENV = "BACKEND_BASE_URL";
    private static final String BACKEND_URL_PROPERTY = "backend.base.url";
    private static final String DEFAULT_BACKEND_BASE_URL = "http://localhost:8080";

    private Path databaseFile;
    private double fsHz;
    private String patientId;
    private ArterySite arterySite;

    private Timer timer;
    private int idx;

    private double[] p, pr, pe;

    private Consumer<Frame> onFrameEdt;
    private Consumer<String> onStatusEdt;
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final String backendBaseUrl = resolveBackendBaseUrl();

    public void configure(Path databaseFile,
                          double fsHz,
                          String patientId,
                          ArterySite arterySite,
                          Consumer<Frame> onFrameEdt,
                          Consumer<String> onStatusEdt) {
        reset();
        this.databaseFile = databaseFile;
        this.fsHz = fsHz;
        this.patientId = patientId;
        this.arterySite = arterySite;
        this.onFrameEdt = onFrameEdt;
        this.onStatusEdt = onStatusEdt;
    }

    public void prepare() {
        if (fsHz <= 0 || patientId == null || patientId.isBlank()) return;

        pause();
        onStatusEdt.accept("Computing...");

        SwingWorker<WaveformPayload, Void> w = new SwingWorker<>() {
            @Override protected WaveformPayload doInBackground() throws Exception {
                ArterySite site = arterySite == null ? ArterySite.AorticRoot : arterySite;
                if (databaseFile != null) {
                    ReservoirService localService = new ReservoirService(databaseFile, fsHz);
                    ReservoirService.ComputationOutput output = localService.compute(databaseFile, patientId, site);
                    return new WaveformPayload(
                            output.raw().getPressure(),
                            output.result().getReservoirPressure(),
                            output.result().getExcessPressure(),
                            fsHz
                    );
                }
                ComputeResponse response = fetchRemoteCompute(site);
                return new WaveformPayload(
                        response.pressure(),
                        response.reservoirPressure(),
                        response.excessPressure(),
                        response.sampleRateHz()
                );
            }
            @Override protected void done() {
                try {
                    WaveformPayload payload = get();
                    p = payload.pressure();
                    pr = payload.reservoirPressure();
                    pe = payload.excessPressure();
                    if (payload.sampleRateHz() > 0) {
                        fsHz = payload.sampleRateHz();
                    }
                    idx = 0;
                    onStatusEdt.accept("Ready");
                } catch (Exception ex) {
                    onStatusEdt.accept(buildErrorMessage(ex));
                }
            }
        };
        w.execute();
    }


    public void start() {
        if (p == null || pr == null || pe == null) { onStatusEdt.accept("Not ready"); return; }

        if (timer != null && timer.isRunning()) return; // already running
        onStatusEdt.accept("Running...");

        int periodMs = 20;
        int step = Math.max(1, (int)Math.round(fsHz * (periodMs / 1000.0)));

        timer = new Timer(periodMs, event-> {
            if (idx >= p.length) { onStatusEdt.accept("Complete"); pause(); return; }

            int end = Math.min(p.length, idx + step);
            for (int i = idx; i < end; i++) {
                double t = i / fsHz;
                onFrameEdt.accept(new Frame(t, p[i], pr[i], pe[i]));
            }
            idx = end;
        });
        timer.start();
    }


    public void pause() {
        if (timer != null) { timer.stop(); timer = null; }
    }

    public void reset() {
        pause();
        idx = 0;
    }

    private ComputeResponse fetchRemoteCompute(ArterySite site) throws Exception {
        URI uri = buildComputeUri(site);
        HttpRequest request = HttpRequest.newBuilder(uri).GET().build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            String body = response.body();
            String detail = (body == null || body.isBlank()) ? "" : " - " + body.trim();
            throw new IllegalStateException("Backend compute failed: HTTP " + response.statusCode() + detail);
        }
        return objectMapper.readValue(response.body(), ComputeResponse.class);
    }

    private URI buildComputeUri(ArterySite site) {
        String base = backendBaseUrl.endsWith("/") ? backendBaseUrl.substring(0, backendBaseUrl.length() - 1) : backendBaseUrl;
        StringBuilder query = new StringBuilder();
        appendQueryParam(query, "patientId", patientId);
        appendQueryParam(query, "arterySite", site.token());
        appendQueryParam(query, "sampleRateHz", Double.toString(fsHz));
        return URI.create(base + "/api/compute?" + query);
    }

    private String buildErrorMessage(Exception ex) {
        Throwable cause = ex instanceof ConnectException ? ex : ex.getCause();
        if (cause instanceof ConnectException) {
            return "Error: Unable to reach backend at " + backendBaseUrl
                    + ". Set BACKEND_BASE_URL or -Dbackend.base.url.";
        }
        return "Error: " + ex.getMessage();
    }

    private static void appendQueryParam(StringBuilder query, String name, String value) {
        if (query.length() > 0) {
            query.append('&');
        }
        query.append(URLEncoder.encode(name, StandardCharsets.UTF_8));
        query.append('=');
        query.append(URLEncoder.encode(value, StandardCharsets.UTF_8));
    }

    private static String resolveBackendBaseUrl() {
        String fromProperty = System.getProperty(BACKEND_URL_PROPERTY);
        if (fromProperty != null && !fromProperty.isBlank()) {
            return fromProperty;
        }
        String fromEnv = System.getenv(BACKEND_URL_ENV);
        if (fromEnv != null && !fromEnv.isBlank()) {
            return fromEnv;
        }
        return DEFAULT_BACKEND_BASE_URL;
    }

    private record WaveformPayload(double[] pressure, double[] reservoirPressure, double[] excessPressure,
                                   double sampleRateHz) {}

    private record ComputeResponse(double[] pressure, double[] reservoirPressure, double[] excessPressure,
                                   double beatDurationSeconds, double sampleRateHz) {}

    public record Frame(double tSec, double p, double pr, double pe) {}
}
