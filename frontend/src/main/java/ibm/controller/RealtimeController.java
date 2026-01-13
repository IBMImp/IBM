package ibm.controller;

import application.ReservoirService;
import model.PressureSignal;
import model.ReservoirResult;

import javax.swing.*;
import java.nio.file.Path;
import java.util.function.Consumer;

public final class RealtimeController {
    private Path databaseFile;
    private double fsHz;
    private String patientId;

    private Timer timer;
    private int idx;

    private ReservoirService service;

    private double[] p, pr, pe;

    private Consumer<Frame> onFrameEdt;
    private Consumer<String> onStatusEdt;

    public void configure(Path databaseFile,
                          double fsHz,
                          String patientId,
                          Consumer<Frame> onFrameEdt,
                          Consumer<String> onStatusEdt) {
        reset();
        this.databaseFile = databaseFile;
        this.fsHz = fsHz;
        this.patientId = patientId;
        this.onFrameEdt = onFrameEdt;
        this.onStatusEdt = onStatusEdt;
        // IMPORTANT: this matches your current backend constructor
        service = new ReservoirService(databaseFile, fsHz);
    }

    public void prepare() {
        if (databaseFile == null || fsHz <= 0 || patientId == null || patientId.isBlank()) return;

        pause();
        onStatusEdt.accept("Computing...");

        SwingWorker<ReservoirService.ComputationOutput, Void> w = new SwingWorker<>() {
            @Override protected ReservoirService.ComputationOutput doInBackground() {
                ReservoirService service = new ReservoirService(databaseFile, fsHz);
                return service.compute(databaseFile, patientId);
            }
            @Override protected void done() {
                try {
                    var out = get();
                    p  = out.raw().getPressure();
                    pr = out.result().getReservoirPressure();
                    pe = out.result().getExcessPressure();
                    idx = 0;
                    onStatusEdt.accept("Ready");
                } catch (Exception ex) {
                    onStatusEdt.accept("Error: " + ex.getMessage());
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


    public record Frame(double tSec, double p, double pr, double pe) {}
}
