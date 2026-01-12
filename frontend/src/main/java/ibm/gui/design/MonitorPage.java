package ibm.gui.design;

import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import ibm.gui.liveCharting.LiveChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.data.xy.XYSeries;

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
    private JButton connectButton;
    private JButton button4;
    private JLabel patientNameLabel;
    private JPanel RPPanel;
    private JPanel BPPanel;
    private JPanel OverlayPanel;
    private XYSeries reservoirSeries;
    private XYSeries pressureSeries;

    private int sampleRate = 100;   // default sample rate

    private int windowSeconds = 5;

    public JButton getPlayButton(){return playButton;}
    public JButton getStopButton(){return stopButton;}
    public JButton getConnectButton(){return connectButton;}

    public void clearGraphs() {
        if (reservoirSeries != null) reservoirSeries.clear();
        if (pressureSeries != null)  pressureSeries.clear();
    }


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

    private void installLiveGraphs() {
        // create series ONCE (shared across all tabs)
        reservoirSeries = new XYSeries("Reservoir Pressure (Pr)", false);
        pressureSeries  = new XYSeries("Blood Pressure (P)", false);

        // ===== Both Charts tab (split pane) =====
        ChartPanel reservoirChart = LiveChartFactory.createLiveChart(
                "Reservoir Pressure (Pr)", "Time (s)", "Pressure", reservoirSeries
        );
        ChartPanel pressureChart = LiveChartFactory.createLiveChart(
                "Blood Pressure (P)", "Time (s)", "Pressure", pressureSeries
        );

        arppCont.removeAll();
        arppCont.setLayout(new BorderLayout());
        arppCont.add(reservoirChart, BorderLayout.CENTER);

        bppCont.removeAll();
        bppCont.setLayout(new BorderLayout());
        bppCont.add(pressureChart, BorderLayout.CENTER);

        // ===== Reservoir tab =====
        RPPanel.removeAll();
        RPPanel.setLayout(new BorderLayout());
        RPPanel.add(LiveChartFactory.createLiveChart(
                "Reservoir Pressure (Pr)", "Time (s)", "Pressure", reservoirSeries
        ), BorderLayout.CENTER);

        // ===== Blood tab =====
        BPPanel.removeAll();
        BPPanel.setLayout(new BorderLayout());
        BPPanel.add(LiveChartFactory.createLiveChart(
                "Blood Pressure (P)", "Time (s)", "Pressure", pressureSeries
        ), BorderLayout.CENTER);

        // ===== Overlay tab (P + Pr on same plot) =====
        OverlayPanel.removeAll();
        OverlayPanel.setLayout(new BorderLayout());
        OverlayPanel.add(createOverlayChartPanel(), BorderLayout.CENTER);

        // refresh
        arppCont.revalidate(); arppCont.repaint();
        bppCont.revalidate();  bppCont.repaint();
        RPPanel.revalidate();  RPPanel.repaint();
        BPPanel.revalidate();  BPPanel.repaint();
        OverlayPanel.revalidate(); OverlayPanel.repaint();
    }

    // Helper: build one chart that overlays both series
    private ChartPanel createOverlayChartPanel() {
        var dataset = new org.jfree.data.xy.XYSeriesCollection();
        dataset.addSeries(pressureSeries);   // 0 = P
        dataset.addSeries(reservoirSeries);  // 1 = Pr

        var xAxis = new org.jfree.chart.axis.NumberAxis("Time (s)");
        var yAxis = new org.jfree.chart.axis.NumberAxis("Pressure");

        var renderer = new org.jfree.chart.renderer.xy.XYLineAndShapeRenderer(true, false);
        renderer.setSeriesStroke(0, new BasicStroke(2.5f));
        renderer.setSeriesStroke(1, new BasicStroke(2.5f));
        renderer.setSeriesPaint(0, new Color(0, 220, 180));   // P
        renderer.setSeriesPaint(1, new Color(255, 180, 0));   // Pr

        var plot = new org.jfree.chart.plot.XYPlot(dataset, xAxis, yAxis, renderer);

        // ===== monitor-style colors (match LiveChartFactory) =====
        Color bg   = new Color(10, 16, 28);
        Color grid = new Color(255, 255, 255, 22);
        Color axis = new Color(255, 255, 255, 60);
        Color text = new Color(230, 230, 230);

        plot.setBackgroundPaint(bg);
        plot.setOutlineVisible(false);
        plot.setDomainGridlinesVisible(true);
        plot.setRangeGridlinesVisible(true);
        plot.setDomainGridlinePaint(grid);
        plot.setRangeGridlinePaint(grid);

        styleAxis(xAxis, text, axis);
        styleAxis(yAxis, text, axis);

        var chart = new org.jfree.chart.JFreeChart(
                "Overlay: P vs Pr",
                org.jfree.chart.JFreeChart.DEFAULT_TITLE_FONT,
                plot,
                true
        );
        chart.setBackgroundPaint(bg);
        chart.getTitle().setPaint(Color.WHITE);
        if (chart.getLegend() != null) {
            chart.getLegend().setItemPaint(text);   // legend text
            chart.getLegend().setBackgroundPaint(bg);
        }

        ChartPanel panel = new ChartPanel(chart);
        panel.setMouseWheelEnabled(true);
        panel.setDomainZoomable(true);
        panel.setRangeZoomable(true);
        panel.setPopupMenu(null);
        panel.setBackground(bg);

        return panel;
    }

    private static void styleAxis(org.jfree.chart.axis.NumberAxis axis, Color labelText, Color axisLine) {
        axis.setLabelPaint(labelText);
        axis.setTickLabelPaint(labelText);
        axis.setAxisLinePaint(axisLine);
        axis.setTickMarkPaint(axisLine);

        axis.setAutoRange(true);
        axis.setAutoRangeIncludesZero(false);
    }


    public void addPoint(double tSec, double p, double pr, double pe) {
        if (reservoirSeries == null || pressureSeries == null) return;

        reservoirSeries.add(tSec, pr);
        pressureSeries.add(tSec, p);

        int maxPoints = Math.max(200, sampleRate * windowSeconds);
        while (reservoirSeries.getItemCount() > maxPoints) reservoirSeries.remove(0);
        while (pressureSeries.getItemCount() > maxPoints) pressureSeries.remove(0);
    }


    // ------------------------------------------
    // CONSTRUCTOR
    // ------------------------------------------
    public MonitorPage() {
        $$$setupUI$$$();
        initButtons();
        darkenBackground();
        installLiveGraphs();
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

    /**
     * Method generated by IntelliJ IDEA GUI Designer
     * >>> IMPORTANT!! <<<
     * DO NOT edit this method OR call it in your code!
     *
     * @noinspection ALL
     */
    private void $$$setupUI$$$() {
        panel1 = new JPanel();
        panel1.setLayout(new GridLayoutManager(2, 1, new Insets(0, 0, 0, 0), -1, -1));
        tabbedPane1 = new JTabbedPane();
        panel1.add(tabbedPane1, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, new Dimension(200, 200), null, 0, false));
        graphsPane = new JSplitPane();
        graphsPane.setContinuousLayout(true);
        graphsPane.setDividerLocation(250);
        graphsPane.setDividerSize(3);
        graphsPane.setEnabled(true);
        graphsPane.setName("Both Graphs");
        graphsPane.setOneTouchExpandable(true);
        graphsPane.setOrientation(1);
        tabbedPane1.addTab("Both Charts", graphsPane);
        arppCont = new JPanel();
        arppCont.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        graphsPane.setLeftComponent(arppCont);
        final JLabel label1 = new JLabel();
        label1.setText("Arterial Reservoir Pressure Plot");
        label1.setVerticalTextPosition(1);
        arppCont.add(label1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        bppCont = new JPanel();
        bppCont.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        graphsPane.setRightComponent(bppCont);
        final JLabel label2 = new JLabel();
        label2.setText("Blood Pressure Plot");
        label2.setVerticalTextPosition(1);
        bppCont.add(label2, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel2 = new JPanel();
        panel2.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        tabbedPane1.addTab("Reservoir Pressure Chart", panel2);
        RPPanel = new JPanel();
        RPPanel.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        panel2.add(RPPanel, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JPanel panel4 = new JPanel();
        panel4.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        tabbedPane1.addTab("Blood Pressure Chart", panel4);
        BPPanel = new JPanel();
        BPPanel.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        panel4.add(BPPanel, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JPanel panel6 = new JPanel();
        panel6.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        tabbedPane1.addTab("Overlay", panel6);
        OverlayPanel = new JPanel();
        OverlayPanel.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        panel6.add(OverlayPanel, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JToolBar toolBar1 = new JToolBar();
        panel1.add(toolBar1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(-1, 20), null, 0, false));
        patientNameLabel = new JLabel();
        patientNameLabel.setText("Patient Name");
        toolBar1.add(patientNameLabel);
        final Spacer spacer1 = new Spacer();
        toolBar1.add(spacer1);
        playButton = new JButton();
        playButton.setIconTextGap(2);
        playButton.setInheritsPopupMenu(true);
        playButton.setLabel("");
        playButton.setMaximumSize(new Dimension(25, 25));
        playButton.setMinimumSize(new Dimension(25, 25));
        playButton.setPreferredSize(new Dimension(25, 25));
        playButton.setText("");
        playButton.setToolTipText("Start Waveform Plotting");
        toolBar1.add(playButton);
        stopButton = new JButton();
        stopButton.setMaximumSize(new Dimension(25, 25));
        stopButton.setMinimumSize(new Dimension(25, 25));
        stopButton.setPreferredSize(new Dimension(25, 25));
        stopButton.setText("");
        stopButton.setToolTipText("Stop WaveForm Plotting");
        toolBar1.add(stopButton);
        final JToolBar.Separator toolBar$Separator1 = new JToolBar.Separator();
        toolBar1.add(toolBar$Separator1);
        connectButton = new JButton();
        connectButton.setEnabled(true);
        connectButton.setText("Connect");
        connectButton.setToolTipText("Connect to Server");
        toolBar1.add(connectButton);
        button4 = new JButton();
        button4.setEnabled(false);
        button4.setText("Button");
        toolBar1.add(button4);
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return panel1;
    }
}