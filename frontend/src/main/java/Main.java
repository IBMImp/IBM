import ibm.gui.GUIController;
import javax.swing.*;
import java.util.logging.*;

public class Main {

    private static final Logger guiLogger =
            Logger.getLogger(GUIController.class.getName());

    public static void main(String[] args) {
        setupLogger(guiLogger);
        SwingUtilities.invokeLater(() -> {
            try {
                GUIController gui = new GUIController(guiLogger);
                if (!gui.init()) throw new ExceptionInInitializerError("GUI Initialization Error");
                gui.showGUI();
            } catch (Exception e) {
                IO.print("Failed to Initialize GUI");
            }
        });
    }

    private static void setupLogger(Logger logger) {
        ConsoleHandler handler = new ConsoleHandler();
        handler.setLevel(Level.ALL);
        logger.addHandler(handler);
        logger.setUseParentHandlers(false);
        logger.setLevel(Level.ALL);
    }
}
