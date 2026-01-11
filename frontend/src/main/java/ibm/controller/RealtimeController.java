package ibm.controller;

import application.ReservoirService;

import javax.swing.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Path;
import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;

public final class RealtimeController {
    private Path csvFile;
    private double fsHz;
    private String patientId;

    private Timer timer;
    private int idx;

    private ReservoirService service;

    private HttpClient client;
    private volatile WebSocket socket;

    private double[] p, pr, pe;

    private PressureBuffer pressureBuffer;
    private ByteBuffer packetBuffer;

    private Consumer<Frame> onFrameEdt;
    private Consumer<String> onStatusEdt;

    public void configure(Path csvFile,
                          double fsHz,
                          String patientId,
                          Consumer<Frame> onFrameEdt,
                          Consumer<String> onStatusEdt) {
        reset();
        this.csvFile = csvFile;
        this.fsHz = fsHz;
        this.patientId = patientId;
        this.onFrameEdt = onFrameEdt;
        this.onStatusEdt = onStatusEdt;
        // IMPORTANT: this matches your current backend constructor
        service = new ReservoirService(csvFile, fsHz);
    }

    public void prepare() {
        if (csvFile == null || fsHz <= 0 || patientId == null || patientId.isBlank()) return;

        pause();
        onStatusEdt.accept("Computing...");

        SwingWorker<ReservoirService.ComputationOutput, Void> w = new SwingWorker<>() {
            @Override protected ReservoirService.ComputationOutput doInBackground() {
                ReservoirService service = new ReservoirService(csvFile, fsHz);
                return service.compute(csvFile, patientId);
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

        int periodMs = 10;
        int step = Math.max(1, (int)Math.round(fsHz * (periodMs / 1000.0)));

        timer = new Timer(periodMs, e -> {

            if (pressureBuffer.length() > 100) {
                for (int i = 0; i < 10; ++i) {
                    pressureBuffer.popBack();
                }
                var a = pressureBuffer.popBack();

                onFrameEdt.accept(new Frame(a.t(), a.p(), 0, 0));

            }

            /*
            if (idx >= p.length) { onStatusEdt.accept("Complete"); pause(); return; }

            int end = Math.min(p.length, idx + step);
            for (int i = idx; i < end; i++) {
                double t = i / fsHz;
                onFrameEdt.accept(new Frame(t, p[i], pr[i], pe[i]));
            }

            idx = end;
            */
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

    public void connect() {
        client = HttpClient.newHttpClient();

        int sendRate = 60;
        int fs = 100;

        int packetSize = (fs / sendRate) + 1;

        packetBuffer = ByteBuffer
                .allocate(2*packetSize * Double.BYTES)
                .order(ByteOrder.BIG_ENDIAN);

        pressureBuffer = new PressureBuffer(10000);

        try {
            WebSocket.Listener listener = new WebSocket.Listener() {
                @Override
                public void onOpen(WebSocket ws) {
                    System.out.println("Connected to server");
                    ws.request(1);

                    // test text packet
                    ws.sendText("hello from client", true);

                    socket = ws;
                }

                @Override
                public CompletionStage<?> onText(WebSocket ws, CharSequence data, boolean last) {
                    System.out.println("CLIENT GOT TEXT: " + data);
                    ws.request(1);
                    return null;
                }

                @Override
                public CompletionStage<?> onBinary(WebSocket ws, ByteBuffer data, boolean last) {
                    byte[] bytes = new byte[data.remaining()];
                    data.get(bytes);

                  //  System.out.println("length: "+ bytes.length);



                    packetBuffer.put(bytes);

                   // System.out.println("test1");

                    if(packetBuffer.remaining() == 0) {
                        pressureBuffer.appendAsBytes(packetBuffer);
                        packetBuffer.clear();
                    }

                    //double p = buffer.getDouble();
                    //double t = buffer.getDouble();


                    System.out.println(pressureBuffer.length());




                    ws.request(1);
                    return null;
                }

                @Override
                public CompletionStage<?> onClose(WebSocket ws, int statusCode, String reason) {
                    System.out.println("Closed: " + statusCode + " " + reason);

                    socket = null;
                    return null;
                }

                @Override
                public void onError(WebSocket ws, Throwable error) {
                    error.printStackTrace();
                    socket = null;
                }
            };

            client.newWebSocketBuilder()
                    .buildAsync(URI.create("ws://localhost:8080/ws"), listener)
                    .join();
        } catch (Exception e){
            throw e;
        }

    }

    public void disconnect() {

        if (socket == null) {
            return;
        }

        socket.sendClose(WebSocket.NORMAL_CLOSURE, "client disconnect").join();

    }

    public boolean isConnected() {
        return socket != null;
    }



    public record Frame(double tSec, double p, double pr, double pe) {}
}
