package ibm.gui;

import ibm.gui.design.*;

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
        //menuBar.getMenu(1).getItem(0).setVisible(true);
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
            ConfirmDialog newSetupConfirm = new ConfirmDialog("Confirm Setup");
            newSetupConfirm.confirmButton.addActionListener(_->{
                showSetupPage();
                //TODO Clear Graph and allow new setup
                newSetupConfirm.dialog.dispose();
                newSetupConfirm.dispose();
            });
            newSetupConfirm.dialog.setVisible(true);
        });
        setupMenu.add(setupItem);
        menuBar.add(setupMenu);

        JMenu monitorMenu = createMonitorMenu();
        monitorMenu.setVisible(false);

        menuBar.add(monitorMenu);

        JMenu helpMenu = createHelpMenu();
        menuBar.add(helpMenu);
        return menuBar;
    }

    private JMenu createMonitorMenu() {
        JMenu monitorMenu = new JMenu("Graph Options");
        /*JMenuItem monitorItem = new JMenuItem("Monitor");
        monitorItem.addActionListener(_ -> showMonitorPage());
        monitorMenu.add(monitorItem);*/
        JMenuItem graphClearItem = new JMenuItem("Clear all Graphs");
        //graphClearItem.setVisible(false);
        graphClearItem.addActionListener(_ -> {
            ConfirmDialog clearConfirm = new ConfirmDialog("Clear");
            clearConfirm.confirmButton().addActionListener(_ -> {
                //TODO add clear graphs
            });
            clearConfirm.dialog.setVisible(true);
        });
        monitorMenu.add(graphClearItem);
        return monitorMenu;
    }

    private class ConfirmDialog extends JDialog{
        private final JButton confirmButton;
        private final JButton cancelButton;
        private final JDialog dialog;
        //Creates and returns a JDialog to confirm action
        private ConfirmDialog(String action) {
            dialog = new JDialog();
            dialog.setTitle(action);
            dialog.setLocationRelativeTo(frame);
            Font f = new Font(".AppleSystemUIFont", Font.PLAIN, 14);

            confirmButton = new JButton("Confirm");
            confirmButton.setFont(f);
            confirmButton.setFocusPainted(false);
            cancelButton = new JButton("Cancel");
            cancelButton.setFont(f);
            cancelButton.setFocusPainted(false);
            JPanel round = new RoundedPanel(5, 0);
            round.setBackground(Color.DARK_GRAY);

            JPanel confirm = new RoundedPanel(5, 0);
            dialog.setUndecorated(true);
            confirmButton.setBorderPainted(false);

            confirm.setBackground(Color.GREEN);
            confirm.add(confirmButton);

            JPanel cancel = new RoundedPanel(5, 0);
            cancelButton.setBorderPainted(false);

            cancel.add(cancelButton);
            cancel.setBackground(Color.RED);

            dialog.setLayout(new GridLayout(1, 0));

            dialog.setModal(true);
            dialog.setBackground(new Color(0, 0, 0, 0));

            dialog.setContentPane(round);

            dialog.add(cancel);
            dialog.add(confirm);

            dialog.pack();
            dialog.setLocationRelativeTo(null);
            cancelButton.addActionListener(_ -> {
                dialog.dispose();
                this.dispose();
            });
        }
        public JButton confirmButton(){
            return confirmButton;
        }
    }
    
    private JMenu createHelpMenu() {
        JMenu helpMenu = new JMenu("Help");
        JMenuItem helpItem = new JMenuItem("Help");
        helpItem.addActionListener(_ ->{
            JDialog helpDialog = new JDialog(frame, "Help", true);
            Dimension dialogSize = new  Dimension(400,400);
            helpDialog.setSize(dialogSize);
            helpDialog.setLocationRelativeTo(null);
            helpDialog.setResizable(false);
            helpDialog.setTitle("Help");
            helpDialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
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
