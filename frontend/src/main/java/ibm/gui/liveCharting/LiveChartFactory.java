package ibm.gui.liveCharting;

import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import java.awt.*;

public final class LiveChartFactory {

    private LiveChartFactory() {}

    public static ChartPanel createLiveChart(
            String title,
            String xLabel,
            String yLabel,
            XYSeries series
    ) {
        XYSeriesCollection dataset = new XYSeriesCollection(series);

        NumberAxis xAxis = new NumberAxis(xLabel);
        NumberAxis yAxis = new NumberAxis(yLabel);

        // renderer (line only)
        XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer(true, false);
        renderer.setSeriesStroke(0, new BasicStroke(2.5f));
        renderer.setSeriesPaint(0, new Color(0, 220, 180)); // monitor cyan

        XYPlot plot = new XYPlot(dataset, xAxis, yAxis, renderer);

        // ===== monitor-style colors =====
        Color bg = new Color(10, 16, 28);
        Color grid = new Color(255, 255, 255, 22); // subtle gridlines
        Color axis = new Color(255, 255, 255, 60);
        Color text = new Color(230, 230, 230);

        plot.setBackgroundPaint(bg);
        plot.setOutlineVisible(false);

        // Professional gridlines (subtle)
        plot.setDomainGridlinesVisible(true);
        plot.setRangeGridlinesVisible(true);
        plot.setDomainGridlinePaint(grid);
        plot.setRangeGridlinePaint(grid);

        // Axis styling (subtle)
        styleAxis(xAxis, text, axis);
        styleAxis(yAxis, text, axis);

        // Chart wrapper
        JFreeChart chart = new JFreeChart(title, JFreeChart.DEFAULT_TITLE_FONT, plot, false);
        chart.setBackgroundPaint(bg);
        chart.getTitle().setPaint(Color.WHITE);

        ChartPanel panel = new ChartPanel(chart);


        panel.setPreferredSize(new Dimension(1, 1));
        panel.setMinimumSize(new Dimension(0, 0));

        // Let it draw nicely at any size (stops clipping/odd scaling)
        panel.setMinimumDrawWidth(0);
        panel.setMinimumDrawHeight(0);
        panel.setMaximumDrawWidth(Integer.MAX_VALUE);
        panel.setMaximumDrawHeight(Integer.MAX_VALUE);

        // Interaction
        panel.setMouseWheelEnabled(true);   // zoom with wheel
        panel.setDomainZoomable(true);
        panel.setRangeZoomable(true);
        panel.setPopupMenu(null);           // cleaner for “medical monitor” feel
        panel.setBackground(bg);

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
}