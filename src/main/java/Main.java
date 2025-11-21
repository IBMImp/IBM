
import ibm.gui.GUIController;

import javax.swing.*;
import java.awt.*;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import com.formdev.flatlaf.*;

private static final Logger guiLogger = Logger.getLogger(GUIController.class.getName());

void main() {
    setupLogger(guiLogger);
    GUIController gui = new GUIController(guiLogger);
    SwingUtilities.invokeLater(() -> {
        try {
            if (!gui.init()) throw new ExceptionInInitializerError("GUI Initialization Error");
            gui.showGUI();
        } catch (Exception e) {
            IO.print("Failed to Initialize GUI");
        }
    });

}

private static void setupLogger(Logger logger) {
    ConsoleHandler handler = new ConsoleHandler();
    handler.setLevel(Level.ALL);              // logs everything to console

    logger.addHandler(handler);            // attach handler to logger
    logger.setUseParentHandlers(false);    // avoid duplicate console output
    logger.setLevel(Level.ALL);            // accept all log levels
}
