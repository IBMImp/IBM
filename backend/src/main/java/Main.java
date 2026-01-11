// src/main/java/Main.java
import data.ContinuousWaveformGenerator;
import data.PwdbCsvPressureSignalSource;
import model.ArterySite;
import model.PressureSignal;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.websocket.server.WebSocketUpgradeHandler;

import websocket.EchoEndpoint;

public class Main {

    private static final Logger LOGGER = Logger.getLogger(Main.class.getName());

    public static final Set<EchoEndpoint> CLIENTS = ConcurrentHashMap.newKeySet();

    public static void main(String[] args) throws Exception {

        int fs = 100;

        Server server = new Server(8080);
        ContextHandler context = new ContextHandler("/");
        server.setHandler(context);

        WebSocketUpgradeHandler wsHandler =
                WebSocketUpgradeHandler.from(server, context, container -> {
                    container.addMapping("/ws", (req, res, cb) -> {
                        EchoEndpoint ep = new EchoEndpoint();
                        CLIENTS.add(ep);
                        return ep;
                    });
                });

        context.setHandler(wsHandler);
        server.start();







        Path csvDir = Paths.get(System.getProperty("user.dir"))
                .getParent()   // move from backend → IBM_copy
                .resolve("virtualPatientData/pwdb/PWs/CSV/PWs_AbdAorta_P.csv")
                .toAbsolutePath();

        //System.out.println(csvDir);

        var source = new PwdbCsvPressureSignalSource(csvDir, fs);
        PressureSignal beat = source.load("1631", ArterySite.AorticRoot);


       // PressureBuffer buf = new PressureBuffer(500);

        ContinuousWaveformGenerator dat = new ContinuousWaveformGenerator(beat, 0);


        int sendRate = 60;

        int packetSize = (fs / sendRate)+1;


        while(true) {

            //package
            ByteBuffer bb = ByteBuffer
                    .allocate(2*packetSize * Double.BYTES)
                    .order(ByteOrder.BIG_ENDIAN);

            for (int i =0; i < packetSize; ++i) {
                var a = dat.getPoint();
                bb.putDouble(a.p())
                        .putDouble(a.t());
            }

            //System.out.println(bb.array().length / 16);


            for (EchoEndpoint ep : Main.CLIENTS) {
                ep.send(bb.array());
            }

           // System.out.println(bytes);

            try {
                Thread.sleep((long) (1000 / sendRate));
            } catch (InterruptedException e) {
                break;
            }

        }

        server.join();




        return;
        /*





        return;

        System.out.println("Working directory = " + System.getProperty("user.dir"));
        // --- 1) Data location ---
        Path csvDir = Paths.get(System.getProperty("user.dir"))
                .getParent()   // move from backend → IBM_copy
                .resolve("virtualPatientData/pwdb/PWs/CSV")
                .toAbsolutePath();

        // --- 2) Choose case ---
        String patientId = "1631";
        ArterySite site = ArterySite.AorticRoot;

        // --- 3) Sampling rate (must match the assumptions we use for parity) ---
        double sampleRateHz = 1000.0;

        // --- 4) Load waveform ---
        PressureSignalSource source = new PwdbCsvPressureSignalSource(csvDir, sampleRateHz);

        PressureSignal raw;
        try {
            raw = source.load(patientId, site);
        } catch (RuntimeException e) {
            LOGGER.log(Level.SEVERE,
                    "Failed to load PWDB pressure waveform (patient={0}, site={1}, dir={2}): {3}",
                    new Object[]{patientId, site, csvDir, e.getMessage()});
            e.printStackTrace();
            return;
        }

        System.out.println("Loaded PWDB waveform");
        System.out.println("Patient: " + patientId);
        System.out.println("Site: " + site);
        System.out.println("Samples: " + raw.getPressure().length);
        System.out.println("Beat duration (s): " + raw.getBeatDuration());
        System.out.println("Pressure min/max: " +
                Arrays.stream(raw.getPressure()).min().orElse(Double.NaN) + " / " +
                Arrays.stream(raw.getPressure()).max().orElse(Double.NaN));

        // --- 5) Wire components ---
        // For PWDB parity runs: single-beat extractor + identity post-regulariser
        BeatExtractor extractor = new SingleBeatExtractor();
        BeatRegulariser postRegulariser = new IdentityBeatRegulariser();

        // Notch/minima detection
        SignalSmoother smoother = new SavitzkyGolaySmoother(5, 5, 2);
        DicroticNotchDetector detector = new DicroticNotchDetector(smoother);
        NotchLocator notchLocator = detector;
        LocalMinimaDetector minimaDetector = detector;

        DiastolicParameterEstimator estimator = new DiastolicParameterEstimator();

        SystolicParameterEstimator systolicEstimator = new SystolicParameterEstimator();
        ReservoirCalculator reservoirCalculator = new ReservoirCalculator();

        ReservoirComputationPipeline pipeline = new ReservoirComputationPipeline(
                extractor,
                notchLocator,
                minimaDetector,
                postRegulariser,
                estimator,
                systolicEstimator,
                reservoirCalculator
        );

        // --- 6) Run ---
        ReservoirResult result;
        try {
            result = pipeline.run(raw);
        } catch (RuntimeException e) {
            LOGGER.log(Level.SEVERE,
                    "Pipeline failed (patient={0}, site={1}): {2}",
                    new Object[]{patientId, site, e.getMessage()});
            e.printStackTrace();
            return;
        }

        // --- 7) Output sanity ---
        double[] pr = result.getReservoirPressure();
        double[] pe = result.getExcessPressure();

        System.out.println();
        System.out.println("Pipeline ran successfully");
        System.out.println("Pr length: " + pr.length);
        System.out.println("Pe length: " + pe.length);
        System.out.println("Mean pressure: " + mean(raw.getPressure()));
        System.out.println("Mean Pr: " + mean(pr));
        System.out.println("Mean Pe: " + mean(pe));

 */
    }

    private static double mean(double[] x) {
        return Arrays.stream(x).average().orElse(Double.NaN);
    }
}