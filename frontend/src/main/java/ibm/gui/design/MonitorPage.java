package ibm.gui.design;

import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import ibm.gui.liveCharting.LiveChartFactory;
import org.jfree.chart.*;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.data.xy.XYSeries;
import org.jfree.chart.plot.ValueMarker;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.chart.title.LegendTitle;
import org.jfree.chart.ui.RectangleEdge;
import org.jfree.data.xy.XYSeriesCollection;


import javax.swing.*;
import java.awt.*;
import java.awt.geom.Line2D;

public class MonitorPage {

    private JPanel panel1;
    private JSplitPane graphsPane;
    private JPanel arppCont;
    private JPanel bppCont;
    private JTabbedPane tabbedPane1;
    private JButton playButton;
    private JButton stopButton;
    private JButton connectButton;
    private JLabel patientNameLabel;
    private JPanel RPPanel;
    private JPanel BPPanel;
    private JPanel OverlayPanel;
    private JSpinner windowLengthSelect;
    private XYSeries reservoirSeries;
    private XYSeries pressureSeries;
    private ChartPanel bpTabChartPanel;
    private ChartPanel overlayChartPanel;
    private ChartPanel rpChartPanel;
    private ChartPanel bothPressureChartPanel;
    private ChartPanel bothReservoirChartPanel;

    // the actual vertical markers
    private ValueMarker sdMarkerBP;
    private ValueMarker sdMarkerOverlay;

    private boolean showSdLine = true;
    private Double lastSdTime = null;
    private JCheckBox sdToggleOverlay;
    private JCheckBox sdToggleBP;


    // update marker occasionally
    private int markerTick = 0;

    private int sampleRate = 1000;   // default sample rate

    private int windowSeconds = 5;

    public JButton getPlayButton() {
        return playButton;
    }

    public JButton getStopButton() {
        return stopButton;
    }

    public JButton getConnectButton() {
        return connectButton;
    }

    public void clearGraphs() {
        if (reservoirSeries != null) reservoirSeries.clear();
        if (pressureSeries != null) pressureSeries.clear();
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
        connectButton.setBackground(UIManager.getColor("Button.default.accent"));
    }

    public void refreshChartTheme() {
        applyChartTheme(bpTabChartPanel);
        applyChartTheme(overlayChartPanel);
        applyChartTheme(rpChartPanel);
        applyChartTheme(bothReservoirChartPanel);
        applyChartTheme(bothPressureChartPanel);

        if (overlayChartPanel != null) addSdLegendToChart(overlayChartPanel.getChart());
        if (bpTabChartPanel != null) addSdLegendToChart(bpTabChartPanel.getChart());

        updateSdMarker(bpTabChartPanel, false);
        updateSdMarker(overlayChartPanel, true);

        styleSdMarker(sdMarkerBP);
        styleSdMarker(sdMarkerOverlay);

        if (bpTabChartPanel != null) bpTabChartPanel.repaint();
        if (overlayChartPanel != null) overlayChartPanel.repaint();

        Color t = currentChartTheme().text;
        if (sdToggleBP != null) sdToggleBP.setForeground(t);
        if (sdToggleOverlay != null) sdToggleOverlay.setForeground(t);
    }

    private void setWindowSeconds() {
        //Initializes the Window length spinner at 1
        windowLengthSelect.setValue(5);
        //Sets minimum and Maximum Window Lengths
        windowLengthSelect.setModel(new SpinnerNumberModel(1, 1, 300, 1));
        //Adds a Listener to change the windowSeconds var when changed
        windowLengthSelect.addChangeListener(_ -> {
            windowSeconds =  Math.round((Float) windowLengthSelect.getValue());
        });
    }

    public void setCustomFields(String patientName, int patientId, int sampleRate) {
        patientNameLabel.setText(patientName + "  |  ID: " + patientId + "  |  Sample Rate: " + sampleRate + " Hz");
        panel1.revalidate();
        panel1.repaint();
    }

    private void installLiveGraphs() {
        reservoirSeries = new XYSeries("Reservoir Pressure (Pr)", false);
        pressureSeries = new XYSeries("Blood Pressure (P)", false);

        // Both charts tab
        bothReservoirChartPanel = LiveChartFactory.createLiveChart(
                "Reservoir Pressure (Pr)", "Time (s)", "Pressure", reservoirSeries
        );
        bothPressureChartPanel = LiveChartFactory.createLiveChart(
                "Blood Pressure (P)", "Time (s)", "Pressure", pressureSeries
        );

        applyChartTheme(bothReservoirChartPanel);
        applyChartTheme(bothPressureChartPanel);

        styleSingleSeriesChart(bothPressureChartPanel, BP_COLOR);
        styleSingleSeriesChart(bothReservoirChartPanel, RP_COLOR);

        arppCont.removeAll();
        arppCont.setLayout(new BorderLayout());
        arppCont.add(bothReservoirChartPanel, BorderLayout.CENTER);

        bppCont.removeAll();
        bppCont.setLayout(new BorderLayout());
        bppCont.add(bothPressureChartPanel, BorderLayout.CENTER);

        // Reservoir tab
        RPPanel.removeAll();
        RPPanel.setLayout(new BorderLayout());

        rpChartPanel = LiveChartFactory.createLiveChart(
                "Reservoir Pressure (Pr)", "Time (s)", "Pressure", reservoirSeries
        );
        styleSingleSeriesChart(rpChartPanel, RP_COLOR);
        RPPanel.add(rpChartPanel, BorderLayout.CENTER);
        applyChartTheme(rpChartPanel);

        // ===== Blood tab =====
        BPPanel.removeAll();
        BPPanel.setLayout(new BorderLayout());

        bpTabChartPanel = LiveChartFactory.createLiveChart(
                "Blood Pressure (P)", "Time (s)", "Pressure", pressureSeries
        );

        applyChartTheme(bpTabChartPanel);

        styleSingleSeriesChart(bpTabChartPanel, BP_COLOR);
        addSdLegendToChart(bpTabChartPanel.getChart());

        BPPanel.add(bpTabChartPanel, BorderLayout.CENTER);

        sdToggleBP = new JCheckBox("Show Systole/Diastole line", showSdLine);
        sdToggleBP.setOpaque(false);
        sdToggleBP.setForeground(currentChartTheme().text);
        sdToggleBP.addActionListener(e -> setShowSdLine(sdToggleBP.isSelected()));

        JPanel bpControls = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        bpControls.setOpaque(false);
        bpControls.add(sdToggleBP);

        BPPanel.add(bpControls, BorderLayout.SOUTH);

        // Overlay tab
        OverlayPanel.removeAll();
        OverlayPanel.setLayout(new BorderLayout());
        overlayChartPanel = createOverlayChartPanel();
        OverlayPanel.add(overlayChartPanel, BorderLayout.CENTER);

        sdToggleOverlay = new JCheckBox("Show Systole/Diastole line", showSdLine);
        sdToggleOverlay.setOpaque(false);
        sdToggleOverlay.setForeground(currentChartTheme().text);
        sdToggleOverlay.addActionListener(e -> setShowSdLine(sdToggleOverlay.isSelected()));

        JPanel overlayControls = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        overlayControls.setOpaque(false);
        overlayControls.add(sdToggleOverlay);

        OverlayPanel.add(overlayControls, BorderLayout.SOUTH);

        updateSdMarker(bpTabChartPanel, false);
        updateSdMarker(overlayChartPanel, true);

        // refresh
        arppCont.revalidate();
        arppCont.repaint();
        bppCont.revalidate();
        bppCont.repaint();
        RPPanel.revalidate();
        RPPanel.repaint();
        BPPanel.revalidate();
        BPPanel.repaint();
        OverlayPanel.revalidate();
        OverlayPanel.repaint();
    }

    // Helper: build one chart that overlays both series
    private ChartPanel createOverlayChartPanel() {
        var dataset = new XYSeriesCollection();
        dataset.addSeries(pressureSeries);   // 0 = P
        dataset.addSeries(reservoirSeries);  // 1 = Pr

        var xAxis = new NumberAxis("Time (s)");
        var yAxis = new NumberAxis("Pressure");

        var baseRenderer = new XYLineAndShapeRenderer(true, false);
        baseRenderer.setSeriesStroke(0, LINE_STROKE);
        baseRenderer.setSeriesStroke(1, LINE_STROKE);
        baseRenderer.setSeriesPaint(0, BP_COLOR);
        baseRenderer.setSeriesPaint(1, RP_COLOR);

        var plot = new XYPlot(dataset, xAxis, yAxis, baseRenderer);

        ChartTheme theme = currentChartTheme();

        plot.setBackgroundPaint(theme.bg);
        plot.setOutlineVisible(false);
        plot.setDomainGridlinesVisible(true);
        plot.setRangeGridlinesVisible(true);
        plot.setDomainGridlinePaint(theme.grid);
        plot.setRangeGridlinePaint(theme.grid);

        styleAxis(xAxis, theme.text, theme.axis);
        styleAxis(yAxis, theme.text, theme.axis);

        var chart = new JFreeChart(
                "Overlay: P vs Pr",
                JFreeChart.DEFAULT_TITLE_FONT,
                plot,
                true
        );

        addSdLegendToChart(chart);

        chart.setBackgroundPaint(theme.bg);
        chart.getTitle().setPaint(theme.text);
        if (chart.getLegend() != null) {
            chart.getLegend().setItemPaint(theme.text);
            chart.getLegend().setBackgroundPaint(theme.bg);
        }

        ChartPanel panel = new ChartPanel(chart);
        panel.setMouseWheelEnabled(true);
        panel.setDomainZoomable(true);
        panel.setRangeZoomable(true);
        panel.setPopupMenu(null);
        panel.setBackground(theme.bg);

        return panel;
    }

    private static void styleAxis(NumberAxis axis, Color labelText, Color axisLine) {
        axis.setLabelPaint(labelText);
        axis.setTickLabelPaint(labelText);
        axis.setAxisLinePaint(axisLine);
        axis.setTickMarkPaint(axisLine);

        axis.setAutoRange(true);
        axis.setAutoRangeIncludesZero(false);
    }

    public void setShowSdLine(boolean show) {
        this.showSdLine = show;

        // ---- BP chart ----
        if (bpTabChartPanel != null && bpTabChartPanel.getChart() != null) {
            XYPlot p = bpTabChartPanel.getChart().getXYPlot();

            // always remove old marker from plot (if any)
            if (sdMarkerBP != null) {
                p.removeDomainMarker(sdMarkerBP);
            }
            sdMarkerBP = null; // force recreate when showing

            if (show) {
                updateSdMarker(bpTabChartPanel, false); // will create+add marker
            }

            addSdLegendToChart(bpTabChartPanel.getChart());
            setLegendVisible(bpTabChartPanel.getChart(), show);

            bpTabChartPanel.getChart().fireChartChanged();
            bpTabChartPanel.repaint();
        }

        // Overlay chart
        if (overlayChartPanel != null && overlayChartPanel.getChart() != null) {
            XYPlot p = overlayChartPanel.getChart().getXYPlot();

            if (sdMarkerOverlay != null) {
                p.removeDomainMarker(sdMarkerOverlay);
            }
            sdMarkerOverlay = null; // force recreate when showing

            if (show) {
                updateSdMarker(overlayChartPanel, true);
            }

            addSdLegendToChart(overlayChartPanel.getChart());

            overlayChartPanel.getChart().fireChartChanged();
            overlayChartPanel.repaint();
        }

        // keep checkboxes synced
        if (sdToggleBP != null) sdToggleBP.setSelected(show);
        if (sdToggleOverlay != null) sdToggleOverlay.setSelected(show);
    }

    private void applyChartTheme(ChartPanel panel) {
        if (panel == null || panel.getChart() == null) return;

        var theme = currentChartTheme();
        var chart = panel.getChart();
        var plot = chart.getXYPlot();

        plot.setBackgroundPaint(theme.bg);
        plot.setDomainGridlinePaint(theme.grid);
        plot.setRangeGridlinePaint(theme.grid);

        // axes
        if (plot.getDomainAxis() instanceof NumberAxis dx) {
            styleAxis(dx, theme.text, theme.axis);
        }
        if (plot.getRangeAxis() instanceof NumberAxis ry) {
            styleAxis(ry, theme.text, theme.axis);
        }

        chart.setBackgroundPaint(theme.bg);
        chart.getTitle().setPaint(theme.text);

        // legend (if exists)
        if (chart.getLegend() != null) {
            chart.getLegend().setItemPaint(theme.text);
            chart.getLegend().setBackgroundPaint(theme.bg);
        }

        panel.setBackground(theme.bg);
    }


    private static final Color BP_COLOR = new Color(220, 60, 60);   // red
    private static final Color RP_COLOR = new Color(0, 0, 255);  // blue
    private static final BasicStroke LINE_STROKE = new BasicStroke(2.5f);

    private static class ChartTheme {
        final Color bg, grid, axis, text;

        ChartTheme(Color bg, Color grid, Color axis, Color text) {
            this.bg = bg;
            this.grid = grid;
            this.axis = axis;
            this.text = text;
        }
    }

    private ChartTheme currentChartTheme() {
        // use panel background to decide light vs dark
        Color uiBg = panel1 != null ? panel1.getBackground() : Color.DARK_GRAY;

        // brightness check
        int brightness = (uiBg.getRed() + uiBg.getGreen() + uiBg.getBlue()) / 3;
        boolean light = brightness > 140;

        if (light) {
            return new ChartTheme(
                    new Color(245, 245, 245),  // bg
                    new Color(0, 0, 0, 25),  // grid
                    new Color(0, 0, 0, 90),  // axis lines
                    new Color(20, 20, 20) // text
            );
        } else {
            return new ChartTheme(
                    new Color(10, 16, 28),
                    new Color(255, 255, 255, 22),
                    new Color(255, 255, 255, 60),
                    new Color(230, 230, 230)
            );
        }
    }

    private boolean isLightTheme() {
        ChartTheme theme = currentChartTheme();
        int b = (theme.bg.getRed() + theme.bg.getGreen() + theme.bg.getBlue()) / 3;
        return b > 140;
    }

    private Color sdColorForCurrentTheme(int alpha) {
        return isLightTheme()
                ? new Color(0, 0, 0, alpha)          // light
                : new Color(255, 255, 255, alpha);   // dark
    }

    private void styleSdMarker(ValueMarker marker) {
        if (marker == null) return;

        marker.setPaint(sdColorForCurrentTheme(220));
        marker.setStroke(new BasicStroke(
                3.2f,
                BasicStroke.CAP_BUTT,
                BasicStroke.JOIN_BEVEL,
                1.0f,
                new float[]{7f, 7f},
                0.0f
        ));
    }

    private void styleSingleSeriesChart(ChartPanel panel, Color color) {
        if (panel == null || panel.getChart() == null) return;
        XYPlot plot = panel.getChart().getXYPlot();
        if (plot.getRenderer() instanceof XYLineAndShapeRenderer r) {
            r.setSeriesPaint(0, color);
            r.setSeriesStroke(0, LINE_STROKE);
        }
    }

    private void addSdLegendToChart(JFreeChart chart) {
        if (chart == null) return;
        if (!showSdLine) {
            LegendTitle legend = chart.getLegend();
            if (legend != null) {
                legend.setSources(new LegendItemSource[]{chart.getPlot()});
            }
            return;
        }

        ChartTheme theme = currentChartTheme();
        Color text = theme.text;
        Color bg = theme.bg;

        // Dashed-line systole/diastole legend item
        final LegendItem sdItem = new LegendItem(
                "Systole/Diastole",
                null,
                null,
                null,
                new Line2D.Double(0, 0, 20, 0),
                new BasicStroke(
                        3.2f,
                        BasicStroke.CAP_BUTT,
                        BasicStroke.JOIN_BEVEL,
                        1.0f,
                        new float[]{7f, 7f},
                        0f
                ),
                sdColorForCurrentTheme(220)
        );

        LegendTitle legend = chart.getLegend();
        if (legend == null) {
            legend = new LegendTitle(chart.getPlot());
            chart.addSubtitle(legend);
        }

        // styling the legend
        legend.setItemPaint(text);      // white text
        legend.setBackgroundPaint(bg);  // dark background
        legend.setPosition(RectangleEdge.BOTTOM);  // below the plot

        // Combine dataset legend items and systole/diastole
        LegendItemSource combined = () -> {
            LegendItemCollection items = new LegendItemCollection();

            LegendItemCollection existing = chart.getPlot().getLegendItems();
            if (existing != null) {
                for (int i = 0; i < existing.getItemCount(); i++) {
                    items.add(existing.get(i));
                }
            }

            // prevent duplicates
            for (int i = 0; i < items.getItemCount(); i++) {
                if ("Systole/Diastole".equals(items.get(i).getLabel())) return items;
            }

            items.add(sdItem);
            return items;
        };

        legend.setSources(new LegendItemSource[]{combined});
    }


    // Systole/diastole line

    /**
     * Find the systole/diastole transition time using:
     * - last local systolic peak
     * - first local minimum after it (dicrotic notch candidate)
     * - inflection point before notch = max |2nd derivative| between peak and notch
     * <p>
     * Returns time in seconds (x-value), or null if not enough structure yet.
     */
    private Double findSdTransitionTime() {
        if (pressureSeries == null) return null;
        int n = pressureSeries.getItemCount();
        if (n < 20) return null;

        // work only on the most recent window
        int lookback = Math.min(n, Math.max(80, sampleRate * 3)); // ~3 seconds or at least 80 points
        int startIdx = n - lookback;

        double[] t = new double[lookback];
        double[] p = new double[lookback];
        for (int i = 0; i < lookback; i++) {
            t[i] = pressureSeries.getX(startIdx + i).doubleValue();
            p[i] = pressureSeries.getY(startIdx + i).doubleValue();
        }

        // light smoothing helps with derivatives and notch detection
        double[] ps = movingAverage(p, 5);

        // 1) find last local maximum (systolic peak)
        int peak = findLastLocalMax(ps);
        if (peak < 2 || peak > lookback - 3) return null;

        // 2) find first local minimum after the peak (dicrotic notch candidate)
        int notch = findFirstLocalMinAfter(ps, peak + 2);
        if (notch == -1 || notch <= peak + 3) return null;

        // 3) find inflection before notch: max abs 2nd derivative between peak and notch
        int best = -1;
        double bestScore = -1;

        for (int i = peak + 1; i <= notch - 1; i++) {
            if (i - 1 < 0 || i + 1 >= lookback) continue;
            double d2 = ps[i + 1] - 2.0 * ps[i] + ps[i - 1]; // discrete second derivative
            double score = Math.abs(d2);
            if (score > bestScore) {
                bestScore = score;
                best = i;
            }
        }

        if (best == -1) return null;
        return t[best];
    }

    private void updateSdMarker(ChartPanel panel, boolean overlay) {
        if (panel == null || panel.getChart() == null) return;
        if (!showSdLine) return;

        Double tSD = findSdTransitionTime();

        if (tSD == null) {
            if (lastSdTime == null) return;
            tSD = lastSdTime;
        } else {
            lastSdTime = tSD;
        }


        XYPlot plot = panel.getChart().getXYPlot();
        ValueMarker marker = overlay ? sdMarkerOverlay : sdMarkerBP;

        if (marker == null) {
            marker = new ValueMarker(tSD);
            styleSdMarker(marker);
            plot.addDomainMarker(marker);

            if (overlay) sdMarkerOverlay = marker;
            else sdMarkerBP = marker;
        } else {
            marker.setValue(tSD);
            styleSdMarker(marker);

            plot.removeDomainMarker(marker);
            plot.addDomainMarker(marker);
        }
    }

    // helpers

    private void setLegendVisible(JFreeChart chart, boolean visible) {
        if (chart == null) return;
        LegendTitle legend = chart.getLegend();
        if (legend != null) legend.setVisible(visible);
    }

    private static int findLastLocalMax(double[] a) {
        for (int i = a.length - 2; i >= 1; i--) {
            if (a[i] > a[i - 1] && a[i] >= a[i + 1]) return i;
        }
        return -1;
    }

    private static int findFirstLocalMinAfter(double[] a, int from) {
        for (int i = Math.max(from, 1); i < a.length - 1; i++) {
            if (a[i] < a[i - 1] && a[i] <= a[i + 1]) return i;
        }
        return -1;
    }

    private static double[] movingAverage(double[] a, int window) {
        if (window <= 1) return a.clone();
        int n = a.length;
        double[] out = new double[n];
        int w2 = window / 2;

        for (int i = 0; i < n; i++) {
            int lo = Math.max(0, i - w2);
            int hi = Math.min(n - 1, i + w2);
            double sum = 0.0;
            for (int k = lo; k <= hi; k++) sum += a[k];
            out[i] = sum / (hi - lo + 1);
        }
        return out;
    }


    public void addPoint(double tp, double tpr, double p, double pr, double pe) {
        if (reservoirSeries == null || pressureSeries == null) return;

        if (pr == 0.0 && tpr == 0) {

        } else {
            reservoirSeries.add(tpr, pr);
        }
        if (p == 0.0 && tp == 0) {

        } else {
            pressureSeries.add(tp, p);
        }


        //Remove Old Points based on the window width
        //Add offset for BP to have a continous bp
        float bpOffset = 0.20f; //seconds

        int maxPoints = (int) Math.round(sampleRate*windowSeconds);

        while (reservoirSeries.getItemCount() > maxPoints) reservoirSeries.remove(0);
        while (pressureSeries.getItemCount() > Math.round(maxPoints + sampleRate * bpOffset)) pressureSeries.remove(0);

        // update systole/diastole marker occasionally (not every sample for performance)
        markerTick++;
        if (markerTick % 10 == 0) {
            SwingUtilities.invokeLater(() -> {
                updateSdMarker(bpTabChartPanel, false);
                updateSdMarker(overlayChartPanel, true);
            });
        }
    }


    // ------------------------------------------
    // CONSTRUCTOR
    // ------------------------------------------
    public MonitorPage() {
        $$$setupUI$$$();
        initButtons();
        darkenBackground();
        setWindowSeconds();
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
        final JPanel panel3 = new JPanel();
        panel3.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        tabbedPane1.addTab("Blood Pressure Chart", panel3);
        BPPanel = new JPanel();
        BPPanel.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        panel3.add(BPPanel, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JPanel panel4 = new JPanel();
        panel4.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        tabbedPane1.addTab("Overlay", panel4);
        OverlayPanel = new JPanel();
        OverlayPanel.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        panel4.add(OverlayPanel, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
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
        connectButton.setActionCommand("Button");
        connectButton.setEnabled(true);
        connectButton.setHideActionText(false);
        connectButton.setLabel("Connect");
        connectButton.setText("Connect");
        toolBar1.add(connectButton);
        final JPanel panel5 = new JPanel();
        panel5.setLayout(new GridLayoutManager(2, 1, new Insets(0, 0, 0, 0), -1, -1));
        panel5.setMaximumSize(new Dimension(86, 100));
        panel5.setMinimumSize(new Dimension(86, 58));
        panel5.setOpaque(false);
        toolBar1.add(panel5);
        final JLabel label3 = new JLabel();
        label3.setText("Window Size [s]");
        panel5.add(label3, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        windowLengthSelect = new JSpinner();
        windowLengthSelect.setFocusable(false);
        windowLengthSelect.setMaximumSize(new Dimension(78, 34));
        windowLengthSelect.setMinimumSize(new Dimension(78, 34));
        windowLengthSelect.setPreferredSize(new Dimension(78, 34));
        windowLengthSelect.setRequestFocusEnabled(false);
        windowLengthSelect.setToolTipText("Select the length of time to keep the waveform");
        panel5.add(windowLengthSelect, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return panel1;
    }

}