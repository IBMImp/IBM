package ibm.gui.design;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class WaveFormGraphPanel extends JPanel {

    private final List<Double> samples = new ArrayList<>();
    private final int sampleRate = 100;     // Hz
    private final int windowSeconds = 10;   // visible window
    private final int windowSize = sampleRate * windowSeconds;

    public WaveFormGraphPanel() {
        setOpaque(true);
        setBackground(Color.BLACK);
    }

    public void addSample(double value) {
        samples.add(value);

        // Hard limit to avoid infinite growth
        if (samples.size() > 50000) {
            samples.subList(0, 30000).clear();
        }

        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (samples.isEmpty()) return;

        Graphics2D g2 = (Graphics2D) g;
        g2.setColor(Color.GREEN);
        g2.setStroke(new BasicStroke(2f));

        int start = Math.max(0, samples.size() - windowSize);
        int count = samples.size() - start;

        int w = getWidth();
        int h = getHeight();

        if (count < 2) return;

        double xScale = (double) w / count;

        int lastX = 0;
        int lastY = transform(samples.get(start), h);

        for (int i = 1; i < count; i++) {
            int x = (int) (i * xScale);
            int y = transform(samples.get(start + i), h);

            g2.drawLine(lastX, lastY, x, y);
            lastX = x;
            lastY = y;
        }
    }

    private int transform(double value, int height) {
        double min = 0, max = 200;
        double normalized = (value - min) / (max - min);
        return height - (int) (normalized * height);
    }
}
