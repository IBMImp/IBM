package ibm.gui;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.FlatLightLaf;
import com.formdev.flatlaf.FlatPropertiesLaf;
import com.formdev.flatlaf.extras.FlatSVGIcon;
import ibm.gui.design.*;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.io.*;
import java.util.Arrays;
import java.util.Objects;
import java.util.StringJoiner;
import java.util.logging.*;


public class GUIController {

    private static Logger logger;
    private JPanel cards;
    private JFrame frame;
    private CardLayout cards_switcher;
    private final Dimension frameSize = new Dimension(800,600);
    private JMenuBar menuBar;
    private MonitorPage monitorPage;
    private SetupPage setupPage;

    //Sets default theme. True = Dark, False = Light (Not dark :) )
    private boolean dark = true;


    //Constructor. Doesn't create any swing components as those have to run on event thread.
    //Attaches Logger
    public GUIController(Logger logger) {
        GUIController.logger = logger;
    }

    //Init for GUI creating gui. Called from event thread
    public boolean init(){
        boolean result = false;

        try {
            //Setup Frame
            UIManager.put("defaultFont", new Font(".AppleSystemUIFont", Font.PLAIN, 12));

            //Apple Customization to make menu bar look native
            System.setProperty("apple.laf.useScreenMenuBar", "true");
            System.setProperty("com.apple.mrj.application.apple.menu.about.name", "MyApp");


            //Card layout for switching between pages
            cards = new JPanel(new CardLayout());
            //Main Frame
            frame = new JFrame("Arterial Reservoir Pressure Monitor");
            //What switches the cards
            cards_switcher  = (CardLayout)cards.getLayout();

            this.frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            this.frame.getContentPane().add(cards);

            //Add Main Pages
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
            monitorPage.setCustomFields(AppState.currentSettings.patientName);
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
        setupItem.addActionListener(_ ->{
            if(JOptionPane.showConfirmDialog(null, "Start a New Setup?\nThis will clear all graphs", "Warning", JOptionPane.YES_NO_OPTION) == 0) {
                showSetupPage();
                //TODO Clear Graph and allow new Setup
            }
        });
        setupMenu.add(setupItem);
        menuBar.add(setupMenu);

        JMenu monitorMenu = createMonitorMenu();
        menuBar.add(monitorMenu);

        JMenuItem lookMenu = createLookMenu();
        menuBar.add(lookMenu);

        JMenuItem helpMenu = createHelpMenu(f);
        menuBar.add(helpMenu);
        return menuBar;
    }

    //Creates the help menu with all the help text in HTML format for text wrapping
    private JMenuItem createHelpMenu(Font f) {
        JMenuItem helpMenu = new JMenuItem("Help");
        helpMenu.setIcon(new FlatSVGIcon("icons/help.svg",16,16));
        helpMenu.addActionListener(_->{
           JOptionPane helpMenuOptionPane = new JOptionPane();

           helpMenuOptionPane.setMessage("<html><body><p style='width: 200px;'>"+"There ain't no help where you're looking and now this is just testing if the " +
                   "thing will wrap because it should but im not 100% sure it will because Ive never tried this before really and it would be very cool if it did." +
                   " Obviously this is yappery and absolutely useless but ehhhhhh"+"</p></body></html>"); //TODO Actual HELP
            helpMenuOptionPane.setFont(f);
           JOptionPane.showMessageDialog(helpMenuOptionPane,helpMenuOptionPane.getMessage(),"Help",JOptionPane.INFORMATION_MESSAGE);
        });
        return helpMenu;
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
        saveGraph.addActionListener(_-> saveFileDialog());
        saveGraph.setIcon(new FlatSVGIcon("icons/save.svg",16,16));
        monitorMenu.add(saveGraph);

        JMenuItem loadGraph = new JMenuItem("Load Graph");
        loadGraph.setIcon(new FlatSVGIcon("icons/load.svg",16,16));
        loadGraph.addActionListener(_-> {
            if (JOptionPane.showConfirmDialog(null, "Do you want to load a new file? \nThis will clear all graphs.", "Warning", JOptionPane.YES_NO_OPTION) == 0)
                loadFileDialog();
        });
        monitorMenu.add(loadGraph);
        JMenuItem graphClearItem = new JMenuItem("Clear all Graphs");
        graphClearItem.setIcon(new FlatSVGIcon("icons/clear.svg",16,16));
        graphClearItem.addActionListener(_ -> {
            if(JOptionPane.showConfirmDialog(null,
                    "Clear all graphs ?",
                    "Warning",
                    JOptionPane.YES_NO_OPTION) == 0) {
                //TODO Implement Clear Graph
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
        if(dark) lookItem.setIcon(new FlatSVGIcon("icons/dark.svg",16,16));
        else lookItem.setIcon(new FlatSVGIcon("icons/light.svg",16,16));
        //When switching themes, apply opposite them and update icon
        lookItem.addActionListener(_ -> {
            dark = !dark;
            applyTheme(dark);
            if(!dark) lookItem.setIcon(new FlatSVGIcon("icons/light.svg",16,16));
            else lookItem.setIcon(new FlatSVGIcon("icons/dark.svg",16,16));
        });
        lookMenu.add(lookItem);
        return lookMenu;
    }

    //Opens a file chooser for loading new files
    private void loadFileDialog(){
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Load Graph");
        chooser.setFileFilter(new FileNameExtensionFilter("CSV Files","csv"));
        if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            if (file.exists() && !file.isDirectory() && file.canRead() && file.getName().toLowerCase().endsWith(".csv")) {
                loadFile(file);

            } else {
                JOptionPane.showMessageDialog(null, "Please Select a CSV File", "Warning", JOptionPane.WARNING_MESSAGE);
                loadFileDialog();
            }
        }

    }
    //Loads file with settings selected by load file dialog
    private void loadFile(File file){
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String settingsLine = br.readLine();
            String[] settings = settingsLine.split(",");
            String patientName = settings[0].trim();
            int numWaveForms = Integer.parseInt(settings[1]);
            int sampleRate = Integer.parseInt(settings[2]);
            AppState.currentSettings = new SetupSettings(patientName, numWaveForms, sampleRate);
            showMonitorPage();
            //TODO ADD File data
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    //Opens a file chooser for saving files
    private void saveFileDialog(){
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Save As");
        chooser.setFileFilter(new FileNameExtensionFilter("CSV Files","csv"));
        chooser.setSelectedFile(new File(AppState.currentSettings.patientName + " ARP Plots"));

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
        landingForm.getStart_button().addActionListener(_ -> {
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
        setupPage.getButton_loadSetup().addActionListener(_ -> {
            if(JOptionPane.showConfirmDialog(null, "Would you like to load " +
                    "a pre-existing file?", "Load File?",
                    JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                loadFileDialog();
            }
        });
        //Finish Setup Button, Writes all setup settings into AppState.currentSettings Record
        setupPage.getButton_finish_setup().addActionListener(_ -> {
            try {
                AppState.currentSettings = new SetupSettings(setupPage.getPatientName().trim(),
                        setupPage.getNumWaveForms(),
                        setupPage.getSampleRate());
                if(!this.showMonitorPage()) {
                    try {
                        throw new Exception("Switch to Monitor Page Failed");
                    } catch (Exception ex) {
                        logger.severe(ex.getMessage());
                    }
                }
            } catch (SetupValueException e) {
                logger.warning("The value entered for Sample Rate is Invalid. Ensure sample rate is a Positive" +
                        "Integer.");
                JOptionPane.showMessageDialog(null, "Sample Rate must be a Positive Integer", "Sample Rate " +
                        "Error", JOptionPane.ERROR_MESSAGE);
            }

        });
    }

    //Init for monitor Page
    private void createMonitorPage(){
        //Create and add the monitor Page
        monitorPage = new MonitorPage();
        //Sets divider location at middle of the page
        monitorPage.getSplitPane().setDividerLocation(frameSize.width/2);
        cards.add(monitorPage.getPanel(), "monitor");
    }

    //Static Class for storing App State. Used for accessing and setting current settings
    private static class AppState {
        private static SetupSettings currentSettings;
    }
    //Storage for apps settings, selected either at setup or by loading files
    private record SetupSettings(String patientName, int numWaveForms, int sampleRate) {}
}
