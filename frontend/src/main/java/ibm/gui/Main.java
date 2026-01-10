package ibm.gui;

import application.ReservoirService;
import ibm.controller.RealtimeController;

import javax.swing.*;
import java.util.logging.*;

public class Main {

    private static final Logger guiLogger =
            Logger.getLogger(GUIController.class.getName());


    public static void main(String[] args) {
        setupLogger(guiLogger);

        RealtimeController rt = new RealtimeController();
        SwingUtilities.invokeLater(() -> {
            try {
                GUIController gui = new GUIController(guiLogger, rt);
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