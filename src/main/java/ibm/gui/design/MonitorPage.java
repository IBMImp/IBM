package ibm.gui.design;

import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;

import javax.swing.*;
import java.awt.*;

public class MonitorPage {

    private JPanel panel1;
    private JSplitPane graphsPane;
    private JPanel arppCont;
    private JPanel bppCont;
    private JTabbedPane tabbedPane1;
    private JButton playButton;
    private JButton stopButton;
    private JButton button3;
    private JButton button4;
    private JLabel patientNameLabel;

    private JPanel panel3;  // Individual reservoir tab
    private JPanel panel5;  // Individual blood pressure tab

    private int sampleRate = 100;   // default sample rate

    // ------------------------------------------
    // PUBLIC API (used by GUIController)
    // ------------------------------------------
    public JPanel getPanel() {
        return panel1;
    }

    public JSplitPane getSplitPane() {
        return graphsPane;
    }

    public void setSampleRate(int rate) {
        this.sampleRate = rate;
    }

    public void darkenBackground() {
        graphsPane.setBackground(panel1.getBackground());
        arppCont.setBackground(panel1.getBackground().darker());
        bppCont.setBackground(panel1.getBackground().darker());
    }

    public void setCustomFields(String patientName) {
        patientNameLabel.setText(patientName);
        panel1.revalidate();
        panel1.repaint();
    }

    // ------------------------------------------
    // CONSTRUCTOR
    // ------------------------------------------
    public MonitorPage() {
        $$$setupUI$$$();
        initButtons();
        darkenBackground();


        // Create graphs once
        WaveFormGraphPanel reservoirGraph = new WaveFormGraphPanel();
        WaveFormGraphPanel bloodGraph = new WaveFormGraphPanel();
        WaveFormGraphPanel reservoirGraph2 = new WaveFormGraphPanel();
        WaveFormGraphPanel bloodGraph2 = new WaveFormGraphPanel();

        JScrollPane scrollResBoth = new JScrollPane(reservoirGraph);
        JScrollPane scrollBloodBoth = new JScrollPane(bloodGraph);

        // Combined tab (left + right)
        arppCont.setLayout(new BorderLayout());
        bppCont.setLayout(new BorderLayout());
        arppCont.removeAll();
        bppCont.removeAll();
        arppCont.add(scrollResBoth, BorderLayout.CENTER);
        bppCont.add(scrollBloodBoth, BorderLayout.CENTER);

        // Individual tabs
        panel3.setLayout(new BorderLayout());
        panel5.setLayout(new BorderLayout());
        panel3.removeAll();
        panel5.removeAll();
        panel3.add(new JScrollPane(reservoirGraph2), BorderLayout.CENTER);
        panel5.add(new JScrollPane(bloodGraph2), BorderLayout.CENTER);

        // Test data
        Timer t = new Timer(10, e -> {
            double v = 80 + 30 * Math.sin(System.currentTimeMillis() * 0.002);
            reservoirGraph.addSample(v);
            bloodGraph.addSample(v + 10);
            reservoirGraph2.addSample(v);
            bloodGraph2.addSample(v + 10);
        });
        t.start();
    }

    // ------------------------------------------
    // Button icons
    // ------------------------------------------
    private void initButtons() {
        int size = 30;
        playButton.setIcon(new FlatSVGIcon("icons/play.svg", size, size));
        stopButton.setIcon(new FlatSVGIcon("icons/stop.svg", size + 4, size + 4));
        patientNameLabel.setIcon(new FlatSVGIcon("icons/patient.svg", size - 5, size - 5));
    }

    // ------------------------------------------
    // GENERATED GUI CODE (IntelliJ UI Designer)
    // ------------------------------------------
    private void $$$setupUI$$$() {
        panel1 = new JPanel();
        panel1.setLayout(new GridLayoutManager(2, 1, new Insets(0, 0, 0, 0), -1, -1));

        // Tabs
        tabbedPane1 = new JTabbedPane();
        panel1.add(tabbedPane1, new GridConstraints(
                1, 0, 1, 1,
                GridConstraints.ANCHOR_CENTER,
                GridConstraints.FILL_BOTH,
                GridConstraints.SIZEPOLICY_CAN_GROW,
                GridConstraints.SIZEPOLICY_CAN_GROW,
                null, null, null, 0, false
        ));

        // Combined graphs tab
        graphsPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        graphsPane.setDividerLocation(300);
        graphsPane.setDividerSize(4);
        graphsPane.setContinuousLayout(true);

        tabbedPane1.addTab("Both Charts", graphsPane);

        arppCont = new JPanel(new GridLayoutManager(1, 1));
        graphsPane.setLeftComponent(arppCont);

        bppCont = new JPanel(new GridLayoutManager(1, 1));
        graphsPane.setRightComponent(bppCont);

        // Individual tabs
        JPanel resPanel = new JPanel(new GridLayoutManager(1, 1));
        tabbedPane1.addTab("Reservoir Pressure Chart", resPanel);

        panel3 = new JPanel(new GridLayoutManager(1, 1));
        resPanel.add(panel3, new GridConstraints(
                0, 0, 1, 1,
                GridConstraints.ANCHOR_CENTER,
                GridConstraints.FILL_BOTH,
                GridConstraints.SIZEPOLICY_CAN_GROW,
                GridConstraints.SIZEPOLICY_CAN_GROW,
                null, null, null, 0, false
        ));

        JPanel bloodPanel = new JPanel(new GridLayoutManager(1, 1));
        tabbedPane1.addTab("Blood Pressure Chart", bloodPanel);

        panel5 = new JPanel(new GridLayoutManager(1, 1));
        bloodPanel.add(panel5, new GridConstraints(
                0, 0, 1, 1,
                GridConstraints.ANCHOR_CENTER,
                GridConstraints.FILL_BOTH,
                GridConstraints.SIZEPOLICY_CAN_GROW,
                GridConstraints.SIZEPOLICY_CAN_GROW,
                null, null, null, 0, false
        ));

        // Toolbar
        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);
        panel1.add(toolBar, new GridConstraints(
                0, 0, 1, 1,
                GridConstraints.ANCHOR_CENTER,
                GridConstraints.FILL_HORIZONTAL,
                GridConstraints.SIZEPOLICY_CAN_GROW,
                GridConstraints.SIZEPOLICY_FIXED,
                null, null, null, 0, false
        ));

        patientNameLabel = new JLabel("Patient Name");
        toolBar.add(patientNameLabel);

        toolBar.add(Box.createHorizontalGlue());

        playButton = new JButton();
        toolBar.add(playButton);

        stopButton = new JButton();
        toolBar.add(stopButton);

        toolBar.addSeparator();

        button3 = new JButton("Button");
        button3.setEnabled(false);
        toolBar.add(button3);

        button4 = new JButton("Button");
        button4.setEnabled(false);
        toolBar.add(button4);
    }

    public JComponent $$$getRootComponent$$$() {
        return panel1;
    }
}
