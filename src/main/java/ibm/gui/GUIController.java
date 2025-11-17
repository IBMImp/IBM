package ibm.gui;

import ibm.gui.design.*;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.*;
import java.util.Arrays;
import java.util.StringJoiner;
import java.util.logging.*;

public class GUIController {

    private static Logger logger;
    private final JPanel cards = new JPanel(new CardLayout());
    private final JFrame frame = new JFrame("Arterial Reservoir Pressure Monitor");
    private final CardLayout cards_switcher = (CardLayout)cards.getLayout();
    private final Dimension frameSize = new Dimension(1500,1000);
    private final JMenuBar menuBar = new JMenuBar();

    public GUIController(Logger logger) {
        GUIController.logger = logger;
    }

    public boolean init(){
        boolean result = false;

        try {
            //Setup Frame
            this.frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            this.frame.getContentPane().add(cards);

            //Add Main Pages
            createLandingPage();
            createSetupPage();
            createMonitorPage();
            this.frame.setJMenuBar(createMenuBar());
            menuBar.setVisible(false);
            this.cards_switcher.show(cards, "landing");
            result = true;
        }
        catch(Exception e) {
            logger.severe(e.getMessage() + "\n" + Arrays.toString(e.getStackTrace()));
        }
        return result;
    }
    public boolean showGUI() {
        //Called After GUI initialized.
        this.frame.setSize(frameSize);
        this.frame.setVisible(true);
        return true;
    }




    public boolean showSetupPage() {
        //Switches to startup page
        menuBar.setVisible(true);
        menuBar.getMenu(1).setVisible(false);
        menuBar.getMenu(0).setVisible(false);
        //menuBar.getMenu(1).getItem(1).setVisible(false);
        this.cards_switcher.show(cards, "setup");
        return true;
    }

    public boolean showMonitorPage(){
        //Switches to startup page
        menuBar.getMenu(0).setVisible(true);
        menuBar.getMenu(1).setVisible(true);
        this.cards_switcher.show(cards, "monitor");

        return true;
    }

    private JMenuBar createMenuBar(){
        Font f = new Font(".AppleSystemUIFont", Font.PLAIN, 14);
        UIManager.put("MenuBar.font", f);
        UIManager.put("Menu.font", f);
        UIManager.put("MenuItem.font", f);
        JMenu setupMenu = new JMenu("Setup");
        JMenuItem setupItem = new JMenuItem("New Setup");
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

        JMenuItem helpMenu = createJMenuItem(f);
        menuBar.add(helpMenu);
        return menuBar;
    }

    private JMenuItem createJMenuItem(Font f) {
        JMenuItem helpMenu = new JMenuItem("Help");
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

    private JMenu createMonitorMenu() {
        JMenu monitorMenu = new JMenu("Monitor");
        JMenuItem graphClearItem = new JMenuItem("Clear all Graphs");
        //graphClearItem.setVisible(false);
        graphClearItem.addActionListener(_ -> {
            if(JOptionPane.showConfirmDialog(null, "Clear all graphs ?", "Warning", JOptionPane.YES_NO_OPTION) == 0) {
                //TODO Implement Clear Graph
            }
        });
        monitorMenu.add(graphClearItem);
        JMenuItem saveGraph = new JMenuItem("Save Graph");
        saveGraph.addActionListener(_-> saveFileDialog());
        monitorMenu.add(saveGraph);

        JMenuItem loadGraph = new JMenuItem("Load Graph");
        loadGraph.addActionListener(_-> {
            if (JOptionPane.showConfirmDialog(null, "Do you want to load a new file? \nThis will clear all graphs.", "Warning", JOptionPane.YES_NO_OPTION) == 0)
                loadFileDialog();
        });
        monitorMenu.add(loadGraph);

        monitorMenu.setVisible(false);
        return monitorMenu;
    }

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
    private void loadFile(File file){
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String settingsLine = br.readLine();
            String[] settings = settingsLine.split(",");
            int numWaveForms = Integer.parseInt(settings[0]);
            int sampleRate = Integer.parseInt(settings[1]);
            AppState.currentSettings = new SetupSettings(numWaveForms, sampleRate);
            //TODO ADD File data
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }
    private void saveFileDialog(){
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Save As");
        chooser.setSelectedFile(new File("untitled.csv"));
        if(chooser.showSaveDialog(null) == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
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
    private void writeFile(File file){
        try {
            BufferedWriter bw = new BufferedWriter(new FileWriter(file));
            StringJoiner setupLine = new StringJoiner(",");
            setupLine.add("" + AppState.currentSettings.numWaveForms);
            setupLine.add("" + AppState.currentSettings.sampleRate);
            setupLine.add("-->");
            bw.write(setupLine.toString());
            bw.close();
            //TODO ADD SAVE GRAPH
            System.out.println("Saved file:" + file.getName() + "  To:" + file.getAbsolutePath());
        }
        catch (IOException e) {
            logger.log(Level.WARNING, "Could not save file: " + file.getName(), e);
        }
    }

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

    private void createSetupPage(){
        //Create and add the setup page
        SetupPage setupPage = new SetupPage();
        cards.add(setupPage.getPanel(), "setup");

        setupPage.getButton_loadSetup().addActionListener(_ -> {
            if(JOptionPane.showConfirmDialog(null, "Would you like to load a pre-existing file?", "Load File?", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                loadFileDialog();
                showMonitorPage();
            }
        });
        setupPage.getButton_finish_setup().addActionListener(_ -> {
            AppState.currentSettings = new SetupSettings(setupPage.getNumWaveForms(), setupPage.getSampleRate());
            if(!this.showMonitorPage()) {
                try {
                    throw new Exception("Switch to Monitor Page Failed");
                } catch (Exception ex) {
                    logger.severe(ex.getMessage());
                }
            }
        });
    }

    private void createMonitorPage(){
        //Create and add the monitor Page
        MonitorPage monitorPage = new MonitorPage();
        monitorPage.getGraphsPane().setDividerLocation(frameSize.height/2-100);
        cards.add(monitorPage.getPanel(), "monitor");
    }

    public static class AppState {
        public static SetupSettings currentSettings;
    }
    public record SetupSettings(int numWaveForms, int sampleRate) {}
}
