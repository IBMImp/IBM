package data;

import model.ArterySite;
import model.PressureSignal;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PwdbCsvPressureSignalSourceTest {

    @Test
    void trimsTrailingNaNsFromSamples() throws IOException {
        Path tempDir = Files.createTempDirectory("pwdb-test");
        Path csv = tempDir.resolve("PWs_AorticRoot_P.csv");

        Files.writeString(csv, String.join(System.lineSeparator(),
                "patient,1,2,3",
                "pt1,1.0,2.0,NaN,NaN"
        ));

        PwdbCsvPressureSignalSource source = new PwdbCsvPressureSignalSource(tempDir, 100.0);
        PressureSignal signal = source.load("pt1", ArterySite.AorticRoot);

        assertEquals(2, signal.getNumSamples());
        assertEquals(1.0, signal.getPressure()[0]);
        assertEquals(2.0, signal.getPressure()[1]);
        assertEquals(0.01, signal.getBeatDuration(), 1e-6);
    }

    @Test
    void rejectsNaNsInsideSignal() throws IOException {
        Path tempDir = Files.createTempDirectory("pwdb-test");
        Path csv = tempDir.resolve("PWs_AorticRoot_P.csv");

        Files.writeString(csv, String.join(System.lineSeparator(),
                "patient,1,2,3",
                "pt1,1.0,NaN,2.0"
        ));

        PwdbCsvPressureSignalSource source = new PwdbCsvPressureSignalSource(tempDir, 100.0);

        assertThrows(IllegalArgumentException.class, () -> source.load("pt1", ArterySite.AorticRoot));
    }
}
