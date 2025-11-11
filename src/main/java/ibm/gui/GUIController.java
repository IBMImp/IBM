package ibm.gui;

import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
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
            ConfirmDialog newSetupConfirm = new ConfirmDialog("Create a New Setup?");
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
        JMenuItem saveGraph = new JMenuItem("Save Graph");
        saveGraph.addActionListener(_->{
            SaveGraph saveDialog = new SaveGraph();
        });
        monitorMenu.add(saveGraph);

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
            ConfirmDialog clearConfirm = new ConfirmDialog("Clear all Graphs?");
            clearConfirm.confirmButton().addActionListener(_ -> {
                //TODO add clear graphs
            });
            clearConfirm.dialog.setVisible(true);
        });
        monitorMenu.add(graphClearItem);
        return monitorMenu;
    }

    private class SaveGraph extends JDialog{
        private final JDialog dialog;
        private final JTextField nameField;
        private final JTextField locField;
        private final JButton save;
        private SaveGraph(){
            dialog = new  JDialog();
            dialog.setLocationRelativeTo(null);
            dialog.setLayout(new BorderLayout());
            save = new JButton("Save");
            JPanel filePropertiesPane = new RoundedPanel(5,0);
            filePropertiesPane.setOpaque(true);
            filePropertiesPane.getBackground().darker();

            filePropertiesPane.setLayout(new GridLayout(0,1));
            filePropertiesPane.setSize(300,200);

            JPanel fileNamePane = new JPanel();
            fileNamePane.setSize(300,100);
            fileNamePane.setLayout(new GridLayout(1,0));
            fileNamePane.setBackground(new Color(0,0,0,0));
            fileNamePane.setOpaque(false);
            nameField = new JTextField();
            nameField.setSize(150,25);
            JLabel nameLabel = new JLabel("Enter File Name:");
            fileNamePane.add(nameLabel);
            fileNamePane.add(nameField);
            filePropertiesPane.add(fileNamePane);

            JPanel fileLocationPane = new JPanel();
            fileLocationPane.setLayout(new GridLayout(1,0));
            fileLocationPane.setBackground(new Color(0,0,0,0));
            fileLocationPane.setOpaque(false);

            locField = new JTextField();
            JLabel locLabel = new JLabel("Enter File Location:");
            fileLocationPane.add(locLabel);
            fileLocationPane.add(locField);
            filePropertiesPane.add(fileLocationPane);

            dialog.add(filePropertiesPane,BorderLayout.CENTER);
            dialog.add(save, BorderLayout.SOUTH);



            dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
            save.addActionListener(_ -> {
                //TODO Add Save Featah
                dialog.dispose();
                this.dispose();
            });
            dialog.setSize(300,300);
            dialog.setVisible(true);

        }
    }

    private class ConfirmDialog extends JDialog{
        private final JButton confirmButton;
        private final JButton cancelButton;
        private final JDialog dialog;
        //Creates and returns a JDialog to confirm action
        private ConfirmDialog(String action) {
            dialog = new JDialog();
            dialog.setTitle(action);
            dialog.setUndecorated(true);
            Font f = new Font(".AppleSystemUIFont", Font.PLAIN, 14);

            confirmButton = new JButton("Confirm");
            confirmButton.setFont(f);
            confirmButton.setFocusPainted(false);

            cancelButton = new JButton("Cancel");
            cancelButton.setFont(f);
            cancelButton.setFocusPainted(false);

            //Create New Round Panel and Set a Margin
            JPanel round = new RoundedPanel(5, 1);
            round.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
            round.setOpaque(true);

            //Create a Round Panel to House Confrim Button and add margin.
            JPanel confirmWrapper = new JPanel();
            confirmWrapper.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
            confirmWrapper.setOpaque(false);
            confirmWrapper.setBackground(new Color(0,0,0,0));
            JPanel confirm = new RoundedPanel(5, 0);
            //Removes Button Border
            confirmButton.setBorderPainted(false);
            confirm.setBackground(Color.GREEN);
            confirm.add(confirmButton);
            confirmWrapper.add(confirm);

            JPanel cancelWrapper =  new JPanel();
            cancelWrapper.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
            cancelWrapper.setOpaque(false);
            cancelWrapper.setBackground(new Color(0,0,0,0));
            JPanel cancel = new RoundedPanel(5, 0);
            cancelButton.setBorderPainted(false);
            cancel.setBackground(Color.RED);
            cancel.add(cancelButton);
            cancelWrapper.add(cancel);

            JPanel buttonsLocator = new  JPanel();
            buttonsLocator.setOpaque(false);
            buttonsLocator.setBackground(new Color(0, 0, 0, 0));
            buttonsLocator.setLayout(new BorderLayout());
            buttonsLocator.add(cancelWrapper,BorderLayout.WEST);
            buttonsLocator.add(confirmWrapper,BorderLayout.EAST);
            buttonsLocator.setSize(new Dimension(200,30));
            //cancelButton.setBackground(Color.RED);

            Font f2 = new Font(".AppleSystemUIFont", Font.BOLD, 14);
            JLabel actionLabel = new JLabel(action, SwingConstants.CENTER);
            actionLabel.setOpaque(false);
            actionLabel.setFont(f2);



            dialog.setModal(true);
            dialog.setBackground(new Color(0, 0, 0, 0));

            dialog.setContentPane(round);
            dialog.setLayout(new BorderLayout());
            dialog.add(actionLabel, BorderLayout.NORTH);
            dialog.add(buttonsLocator, BorderLayout.SOUTH);
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
