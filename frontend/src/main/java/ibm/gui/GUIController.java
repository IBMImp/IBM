package ibm.gui;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.FlatLightLaf;
import com.formdev.flatlaf.FlatPropertiesLaf;
import com.formdev.flatlaf.extras.FlatSVGIcon;
import ibm.controller.RealtimeController;
import ibm.gui.design.*;
import model.ArterySite;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Locale;
import java.util.Objects;
import java.util.StringJoiner;
import java.util.logging.*;


public class GUIController {
    private static final String DEFAULT_DATA_DIR = "../virtualPatientData/pwdb/PWs/CSV";
    private static final String DATA_PATH_ENV = "APP_DATA_PATH";
    private static final String DATA_PATH_PROPERTY = "app.data.path";

    private static Logger logger;
    private JPanel cards;
    private JFrame frame;
    private CardLayout cards_switcher;
    private final Dimension frameSize = new Dimension(800,600);
    private JMenuBar menuBar;
    private MonitorPage monitorPage;
    private SetupPage setupPage;
    private final ibm.controller.RealtimeController rt;

    //Sets default theme. True = Dark, False = Light (Not dark :) )
    private boolean dark = true;


    //Constructor. Doesn't create any swing components as those have to run on event thread.
    //Attaches Logger
    public GUIController(Logger logger, RealtimeController rt1) {
        GUIController.logger = logger;
        this.rt = rt1;
    }

    //Init for GUI creating gui. Called from event thread
    public boolean init(){
        boolean result = false;

        try {
            try {
                Image img = Toolkit.getDefaultToolkit()
                        .getImage(GUIController.class.getResource("/icons/app.png"));
                Taskbar taskbar = Taskbar.getTaskbar();
                if (taskbar.isSupported(Taskbar.Feature.ICON_IMAGE)) {
                    taskbar.setIconImage(img);
                }
            } catch (Exception ignored) {}
            //Setup Frame
            UIManager.put("defaultFont", new Font(".AppleSystemUIFont", Font.PLAIN, 12));

            //Apple Customization to make menu bar look native
            System.setProperty("apple.laf.useScreenMenuBar", "true");
            System.setProperty("com.apple.mrj.application.apple.menu.about.name", "MyApp");


            //Card layout for switching between pages
            cards = new JPanel(new CardLayout());
            //ibm.gui.Main Frame
            frame = new JFrame("Arterial Reservoir Pressure Monitor");
            //What switches the cards
            cards_switcher  = (CardLayout)cards.getLayout();

            this.frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            this.frame.getContentPane().add(cards);

            //Add ibm.gui.Main Pages
            createLandingPage();
            createSetupPage();
            createMonitorPage();
            this.frame.setJMenuBar(createMenuBar());

            //Apply theme after pages created
            applyTheme(dark);

            //Doesn't show menubar on landing page. Revealed based on page which helps with keyboard commands like save
            menuBar.setVisible(false);
            //Default page landing
            this.cards_switcher.show(cards, "landing");
            //Sets successful initialization
            result = true;
        }
        catch(Exception e) {
            logger.severe(e.getMessage() + "\n" + Arrays.toString(e.getStackTrace()));
        }

        return result;
    }



    //Shows GUI
    public boolean showGUI() {
        //Called After GUI initialized.
        this.frame.setSize(frameSize);
        this.frame.setVisible(true);
        return true;
    }

    //Shows SetupPage and shows relevant menus
    private boolean showSetupPage() {
        //Switches to startup page
        menuBar.setVisible(true);
        menuBar.getMenu(1).setVisible(false);
        menuBar.getMenu(0).setVisible(false);
        this.cards_switcher.show(cards, "setup");
        return true;
    }

    //Shows monitor and sets values from AppState to graphs. Also shows relevant menus
    private boolean showMonitorPage(){
        //Switches to monitor page and sets all values for graphing
        try {
            monitorPage.setSampleRate(AppState.currentSettings.sampleRate);
            menuBar.getMenu(0).setVisible(true);
            menuBar.getMenu(1).setVisible(true);
            monitorPage.setCustomFields(AppState.currentSettings.patientName, AppState.currentSettings.patientID, AppState.currentSettings.sampleRate);
            cards.revalidate();
            cards.repaint();
            this.cards_switcher.show(cards, "monitor");
        } catch (Exception e) {
            logger.warning(e.getMessage());
        }

        return true;
    }

    /*Used to apply the selected theme. If dark true then dark mode if false then light mode
    Uses custom themes defined in themes*/
    private void applyTheme(boolean dark) {
        try {
            //If dark mode true then setups dark theme, else setup light mode
            if (dark) {
                FlatDarkLaf.setup();
                FlatLaf.setup(new FlatPropertiesLaf(
                        "MyDarkTheme",
                        Objects.requireNonNull(getClass().getClassLoader().getResourceAsStream("themes/my_dark.properties"))
                ));
            } else {
                FlatLightLaf.setup();
                FlatLaf.setup(new FlatPropertiesLaf(
                        "MyLightTheme",
                        Objects.requireNonNull(getClass().getClassLoader().getResourceAsStream("themes/my_light.properties"))
                ));
            }

            /*Because the UI is reloaded and updated, some settings and custom details get overridden therefore
            they're all recalled and rewrote.*/

            for (java.awt.Window w : java.awt.Window.getWindows()) {
                javax.swing.SwingUtilities.updateComponentTreeUI(w);
                w.repaint();
            }
            setupPage.darkenBackground();
            monitorPage.darkenBackground();
            monitorPage.refreshChartTheme();
        }
        catch(Exception e) {
            logger.warning(e.getMessage() + "\n" + Arrays.toString(e.getStackTrace()));

        }
    }

    //Creates the menubar at the top of the page
    private JMenuBar createMenuBar(){
        menuBar = new JMenuBar();

        Font f = new Font(".AppleSystemUIFont", Font.PLAIN, 14);
        UIManager.put("MenuBar.font", f);
        UIManager.put("Menu.font", f);
        UIManager.put("MenuItem.font", f);
        JMenu setupMenu = new JMenu("Setup");
        JMenuItem setupItem = new JMenuItem("New Setup");
        setupItem.setIcon(new FlatSVGIcon("icons/newsetup.svg",16,16));
        setupItem.addActionListener(event ->{
            if(JOptionPane.showConfirmDialog(null, "Start a New Setup?\nThis will clear all graphs", "Warning", JOptionPane.YES_NO_OPTION) == 0) {
                showSetupPage();
                monitorPage.clearGraphs();
            }
        });
        setupMenu.add(setupItem);
        menuBar.add(setupMenu);

        JMenu monitorMenu = createMonitorMenu();
        menuBar.add(monitorMenu);

        JMenuItem lookMenu = createLookMenu();
        menuBar.add(lookMenu);

        JMenu helpMenu = createHelpMenu(f);
        menuBar.add(helpMenu);
        return menuBar;
    }

    //Creates the help menu with all the help text in HTML format for text wrapping
    private JMenu createHelpMenu(Font f) {
        JMenu helpMenu = new JMenu("Help");
        helpMenu.setIcon(new FlatSVGIcon("icons/help.svg",16,16));

        JMenuItem showHelpItem = new JMenuItem("Show Help");
        showHelpItem.addActionListener(event -> SwingUtilities.invokeLater(() -> showHelpDialog(f)));
        helpMenu.add(showHelpItem);
        return helpMenu;
    }

    private void showHelpDialog(Font f) {
        JOptionPane helpMenuOptionPane = new JOptionPane();

        helpMenuOptionPane.setMessage("<html><body><p style='width: 280px;'>" +
                "<strong>Important information:</strong><br>" +
                "The systole\u2013diastole dashed line is identified using waveform features (inflection points). " +
                "For very smooth or low-feature data, where these features cannot be reliably detected, the software " +
                "may not display the dashed line.<br><br>" +
                "If you encounter any other issues or unexpected behavior, please contact " +
                "ibmgroup.help@gmail.com." +
                "</p></body></html>");
        helpMenuOptionPane.setFont(f);
        JOptionPane.showMessageDialog(helpMenuOptionPane,helpMenuOptionPane.getMessage(),"Help",JOptionPane.INFORMATION_MESSAGE);
    }

    //Creates the monitor menu with save graph, load graph, and clear graph
    private JMenu createMonitorMenu() {
        JMenu monitorMenu = new JMenu("Monitor");


        JMenuItem saveGraph = new JMenuItem("Save Graph");

        KeyStroke saveKey = KeyStroke.getKeyStroke(
                KeyEvent.VK_S,
                Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()
        );

        saveGraph.setAccelerator(saveKey);
        saveGraph.addActionListener(event-> saveFileDialog());
        saveGraph.setIcon(new FlatSVGIcon("icons/save.svg",16,16));
        monitorMenu.add(saveGraph);

        JMenuItem loadGraph = new JMenuItem("Load Graph");
        loadGraph.setIcon(new FlatSVGIcon("icons/load.svg",16,16));
        loadGraph.addActionListener(event-> {
            if (JOptionPane.showConfirmDialog(null, "Do you want to load a new file? \nThis will clear all graphs.", "Warning", JOptionPane.YES_NO_OPTION) == 0) {
                monitorPage.clearGraphs();
                loadFileDialog();
            }
        });
        monitorMenu.add(loadGraph);
        JMenuItem graphClearItem = new JMenuItem("Clear all Graphs");
        graphClearItem.setIcon(new FlatSVGIcon("icons/clear.svg",16,16));
        graphClearItem.addActionListener(event -> {
            if(JOptionPane.showConfirmDialog(null,
                    "Clear all graphs ?",
                    "Warning",
                    JOptionPane.YES_NO_OPTION) == 0) {
                monitorPage.clearGraphs();
            }
        });
        monitorMenu.add(graphClearItem);

        monitorMenu.setVisible(false);
        return monitorMenu;
    }

    //Creates the appearance menu to change the theme of the app
    private JMenu createLookMenu() {
        JMenu lookMenu = new JMenu("Appearance");
        JMenuItem lookItem = new JMenuItem("Theme");
        //Set icon at runtime
        if (dark)
            lookItem.setIcon(new FlatSVGIcon("icons/light.svg", 16, 16));
        else
            lookItem.setIcon(new FlatSVGIcon("icons/dark.svg", 16, 16));

        //When switching themes, apply opposite them and update icon
        lookItem.addActionListener(event -> {
            dark = !dark;
            applyTheme(dark);
            if (dark)
                lookItem.setIcon(new FlatSVGIcon("icons/light.svg", 16, 16));
            else
                lookItem.setIcon(new FlatSVGIcon("icons/dark.svg", 16, 16));

        });
        lookMenu.add(lookItem);
        return lookMenu;
    }

    //Opens a file chooser for loading new files
    private void loadFileDialog() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Load Graph");
        chooser.setFileFilter(new FileNameExtensionFilter("CSV or SQLite Database","csv", "db"));
        if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            String lowerName = file.getName().toLowerCase();
            if (file.exists() && !file.isDirectory() && file.canRead()
                    && (lowerName.endsWith(".csv") || lowerName.endsWith(".db"))) {
                //loadFile(file);
                configureBackend(file.toPath());

            } else {
                JOptionPane.showMessageDialog(null, "Please Select a CSV or SQLite Database File", "Warning", JOptionPane.WARNING_MESSAGE);
                loadFileDialog();
            }
        }
    }
    //Loads file with settings selected by load file dialog
    private boolean loadFile(File file){
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String settingsLine = br.readLine();
            if (settingsLine == null || settingsLine.isBlank()) {
                logger.warning("File is missing setup settings: " + file.getAbsolutePath());
                return false;
            }
            String[] settings = settingsLine.split(",");
            if (settings.length < 3) {
                logger.warning("File has invalid setup settings: " + file.getAbsolutePath());
                return false;
            }
            String patientName = settings[0].trim();
            int numWaveForms = Integer.parseInt(settings[1]);
            int sampleRate = Integer.parseInt(settings[2]);
            int patientID = 0;
            ArterySite arterySite = ArterySite.AorticRoot;
            if (settings.length > 3) {
                try {
                    patientID = Integer.parseInt(settings[3].trim());
                } catch (NumberFormatException e) {
                    logger.warning("Invalid patient ID in setup settings: " + file.getAbsolutePath());
                }
            }
            if (settings.length > 4) {
                try {
                    arterySite = ArterySite.fromToken(settings[4].trim());
                } catch (IllegalArgumentException e) {
                    logger.warning("Invalid artery site in setup settings: " + file.getAbsolutePath());
                }
            }
            AppState.currentSettings = new SetupSettings(patientName, numWaveForms, sampleRate, patientID, arterySite);

            if (!showMonitorPage()) {
                logger.severe("Switch to Monitor Page Failed");
                return false;
            }
            return true;
            //TODO ADD File data
        } catch (Exception e) {
            logger.log(Level.WARNING, "Failed to load setup settings from file: " + file.getAbsolutePath(), e);
            return false;
        }
    }

    //Opens a file chooser for saving files
    private void saveFileDialog(){
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Save As");
        chooser.setFileFilter(new FileNameExtensionFilter("CSV Files","csv"));
        chooser.setSelectedFile(new File(AppState.currentSettings.patientName + " - " + AppState.currentSettings.patientID + " ARP Plots"));

        if(chooser.showSaveDialog(null) == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            if(!file.getName().toLowerCase().endsWith(".csv")){
                file = new File(file.getAbsolutePath() + ".csv");
            }
            if(!file.exists()){
                writeFile(file);
            }
            else {
                switch(JOptionPane.showConfirmDialog(null, "File already exists. "
                        + "\n Do you want to overwrite the file", "Warning", JOptionPane.YES_NO_CANCEL_OPTION)){
                    case JOptionPane.YES_OPTION:
                        try {
                            writeFile(file);
                            return;
                        } catch(Exception e) {
                            System.out.println(e.getMessage());
                        }
                        return;
                    case JOptionPane.NO_OPTION:
                        saveFileDialog();

                    case JOptionPane.CANCEL_OPTION:
                }
            }
        }
    }
    //Creates/Writes file selected from save file dialog
    private void writeFile(File file){
        try {
            BufferedWriter bw = new BufferedWriter(new FileWriter(file));
            StringJoiner setupLine = new StringJoiner(",");
            setupLine.add(AppState.currentSettings.patientName);
            setupLine.add("" + AppState.currentSettings.numWaveForms);
            setupLine.add("" + AppState.currentSettings.sampleRate);
            setupLine.add("" + AppState.currentSettings.patientID);
            setupLine.add(AppState.currentSettings.arterySite.token());
            setupLine.add("-->");
            bw.write(setupLine.toString());
            bw.close();
            //TODO ADD SAVE GRAPH
            logger.info("Saved file:" + file.getName() + "  To:" + file.getAbsolutePath());
        }
        catch (IOException e) {
            logger.log(Level.WARNING, "Could not save file: " + file.getName() + " To:" + file.getAbsolutePath(), e);
        }
    }

    //Init for landing page
    private void createLandingPage(){
        //Create and add the landing page
        LandingForm landingForm = new LandingForm();
        cards.add(landingForm.getPanel(), "landing");
        landingForm.getStart_button().addActionListener(event -> {
            if(!this.showSetupPage()) {
                try {
                    throw new Exception("Switch to Startup Page Failed");
                } catch (Exception ex) {
                    logger.severe(ex.getMessage());
                }
            }
        });
    }

    //Init for setup page
    private void createSetupPage(){
        //Create and add the setup page
        setupPage = new SetupPage();
        cards.add(setupPage.getPanel(), "setup");

        //Load File Button
        setupPage.getButton_loadSetup().addActionListener(event -> {
            if(JOptionPane.showConfirmDialog(null, "Would you like to load " +
                            "a pre-existing file?", "Load File?",
                    JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                loadFileDialog();
            }
        });
        //Finish Setup Button, Writes all setup settings into AppState.currentSettings Record
        setupPage.getButton_finish_setup().addActionListener(event -> {
            try {
                String patientName = setupPage.getPatientName().trim();
                int numWaveForms = setupPage.getNumWaveForms();
                int sampleRate = setupPage.getSampleRate();
                int patientId = setupPage.getPatientId();
                ArterySite arterySite = setupPage.getArterySite();
                validatePatientIdRange(resolveDefaultDatabaseFile(arterySite), patientId);
                AppState.currentSettings = new SetupSettings(patientName, numWaveForms, sampleRate, patientId, arterySite);
                if(!this.showMonitorPage()) {
                    try {
                        throw new Exception("Switch to Monitor Page Failed");
                    } catch (Exception ex) {
                        logger.severe(ex.getMessage());
                    }
                } else {
                    configureBackend(resolveDefaultDatabaseFile(AppState.currentSettings.arterySite));
                }
            } catch (SetupValueException e) {
                logger.warning("Setup validation failed: " + e.getMessage());
                JOptionPane.showMessageDialog(null, e.getMessage(), "Setup Error", JOptionPane.ERROR_MESSAGE);
            }

        });
    }

    //Init for monitor Page
    private void createMonitorPage(){
        //Create and add the monitor Page
        monitorPage = new MonitorPage();
        //Sets divider location at middle of the page
        monitorPage.getSplitPane().setDividerLocation(frameSize.width/2);
        monitorPage.getPlayButton().addActionListener(event->{rt.start();});
        monitorPage.getStopButton().addActionListener(event->{rt.pause();});
        cards.add(monitorPage.getPanel(), "monitor");

    }

    private void configureBackend(Path databaseFile) {
        try {
            validatePatientIdRange(databaseFile, AppState.currentSettings.patientID);
        } catch (SetupValueException e) {
            logger.warning("Setup validation failed: " + e.getMessage());
            JOptionPane.showMessageDialog(null, e.getMessage(), "Setup Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        rt.configure(
                databaseFile,
                AppState.currentSettings.sampleRate,
                String.valueOf(AppState.currentSettings.patientID),
                AppState.currentSettings.arterySite,
                frame -> {
                    monitorPage.addPoint(frame.tSec(), frame.p(), frame.pr(), frame.pe());


                },
                msg -> {
                    logger.info(msg);
                    // optional: monitorPage.setStatus(msg);
                }
        );
        rt.prepare();
    }

    private void validatePatientIdRange(Path databaseFile, int patientId) throws SetupValueException {
        if (databaseFile == null || patientId <= 0) {
            return;
        }
        String fileName = databaseFile.getFileName().toString().toLowerCase(Locale.ROOT);
        if (!fileName.endsWith(".csv")) {
            return;
        }
        int totalPatients = countPatientRows(databaseFile);
        if (totalPatients <= 0) {
            throw new SetupValueException("No patient rows found in " + databaseFile.getFileName() + ".");
        }
        if (patientId > totalPatients) {
            throw new SetupValueException("Patient ID is out of bounds. Enter a value between 1 and " + totalPatients + ".");
        }
    }

    private int countPatientRows(Path csvFile) throws SetupValueException {
        try (BufferedReader br = Files.newBufferedReader(csvFile)) {
            String line;
            int count = 0;
            while ((line = br.readLine()) != null) {
                if (line.isBlank()) {
                    continue;
                }
                String[] parts = line.split(",");
                if (parts.length < 2) {
                    continue;
                }
                String id = parts[0].trim();
                if (looksLikeHeader(id)) {
                    continue;
                }
                count++;
            }
            return count;
        } catch (IOException e) {
            throw new SetupValueException("Unable to read patient data from " + csvFile.getFileName() + ".");
        }
    }

    private boolean looksLikeHeader(String cell) {
        String normalized = cell.toLowerCase(Locale.ROOT);
        return normalized.equals("patient")
                || normalized.equals("patientid")
                || normalized.equals("subject number")
                || normalized.startsWith("#");
    }

    private Path resolveDefaultDatabaseFile(ArterySite arterySite) {
        ArterySite selectedSite = arterySite == null ? ArterySite.AorticRoot : arterySite;
        String fromProperty = System.getProperty(DATA_PATH_PROPERTY);
        if (fromProperty != null && !fromProperty.isBlank()) {
            Path path = Path.of(fromProperty);
            return resolveCsvPath(path, selectedSite);
        }
        String fromEnv = System.getenv(DATA_PATH_ENV);
        if (fromEnv != null && !fromEnv.isBlank()) {
            Path path = Path.of(fromEnv);
            return resolveCsvPath(path, selectedSite);
        }
        Path defaultDir = Path.of(DEFAULT_DATA_DIR);
        return resolveCsvPath(defaultDir, selectedSite);
    }

    private Path resolveCsvPath(Path basePath, ArterySite arterySite) {
        if (Files.isDirectory(basePath)) {
            Path candidate = basePath.resolve(arterySite.pressureFileName());
            return Files.exists(candidate) ? candidate : null;
        }
        return Files.exists(basePath) ? basePath : null;
    }


    //Static Class for storing App State. Used for accessing and setting current settings
    private static class AppState {
        private static SetupSettings currentSettings;
    }
    //Storage for apps settings, selected either at setup or by loading files
    private record SetupSettings(String patientName, int numWaveForms, int sampleRate, int patientID,
                                 ArterySite arterySite) {}
}
