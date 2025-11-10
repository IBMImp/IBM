package ibm.gui;

import ibm.gui.design.LandingForm;
import ibm.gui.design.MonitorPage;
import ibm.gui.design.SetupPage;

import javax.swing.*;
import java.awt.*;
import java.util.Arrays;
import java.util.logging.*;

public class GUIController {

    private static Logger logger;
    private final JPanel cards = new JPanel(new CardLayout());
    private final JFrame frame = new JFrame("Arterial Reservoir Pressure Monitor");
    private final CardLayout cards_switcher = (CardLayout)cards.getLayout();
    private final Dimension frameSize = new Dimension(1500,1000);


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
            createLandingPage();System.out.print("here3");
            createSetupPage(); System.out.print("here2");
            createMonitorPage();
            this.frame.setJMenuBar(createMenuBar());

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
        this.cards_switcher.show(cards, "setup");
        return true;
    }

    public boolean showMonitorPage(){
        //Switches to startup page
        this.cards_switcher.show(cards, "monitor");

        return true;
    }

    private JMenuBar createMenuBar(){
        JMenuBar menuBar = new JMenuBar();
        JMenu settingsMenu = new JMenu("Settings");
        JMenuItem settingsItem = new JMenuItem("Settings");
        settingsItem.addActionListener(_ -> showSetupPage());
        settingsMenu.add(settingsItem);
        menuBar.add(settingsMenu);

        JMenu monitorMenu = new JMenu("Monitor");
        JMenuItem monitorItem = new JMenuItem("Monitor");
        monitorItem.addActionListener(_ -> showMonitorPage());
        monitorMenu.add(monitorItem);
        menuBar.add(monitorMenu);

        JMenu helpMenu = getHelpMenu();
        menuBar.add(helpMenu);

        return menuBar;
    }

    private JMenu getHelpMenu() {
        JMenu helpMenu = new JMenu("Help");
        JMenuItem helpItem = new JMenuItem("Help");
        helpItem.addActionListener(_ ->{
            JDialog helpDialog = new JDialog(frame, "Help", true);
            Dimension dialogSize = new  Dimension(400,400);
            helpDialog.setSize(dialogSize);
            Dimension frame_size = frame.getSize();
            helpDialog.setLocationRelativeTo(frame);
            helpDialog.setLocation(frame_size.width/2-dialogSize.width/2,frame_size.height/2-dialogSize.height/2);
            helpDialog.setResizable(false);
            helpDialog.setTitle("Help");
            helpDialog.setVisible(true);
        });
        helpMenu.add(helpItem);
        return helpMenu;
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
        setupPage.getButton_finish_setup().addActionListener(_ -> {
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

}
