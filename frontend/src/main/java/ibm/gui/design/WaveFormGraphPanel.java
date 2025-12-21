package ibm.gui.design;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class WaveFormGraphPanel extends JPanel {

    private final List<Double> allSamples = new ArrayList<>();
    private final int sampleRate = 100;      // Hz
    private final int windowSeconds = 10;    // 10-second window
    private final int windowSize = sampleRate * windowSeconds;

    public WaveFormGraphPanel() {
        setOpaque(true);
        setBackground(Color.BLACK);
    }

    public void addSample(double value) {
        allSamples.add(value);

        if (allSamples.size() > 50000) {
            allSamples.subList(0, 10000).clear();
        }

        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (allSamples.isEmpty()) return;

        Graphics2D g2 = (Graphics2D) g;
        g2.setColor(new Color(0, 255, 0));
        g2.setStroke(new BasicStroke(2f));

        int startIndex = Math.max(0, allSamples.size() - windowSize);
        int count = allSamples.size() - startIndex;

        int w = getWidth();
        int h = getHeight();

        double xScale = (double) w / (double) count;

        int lastX = 0;
        int lastY = transform(allSamples.get(startIndex), h);

        for (int i = 1; i < count; i++) {
            int x = (int) (i * xScale);
            int y = transform(allSamples.get(startIndex + i), h);

            g2.drawLine(lastX, lastY, x, y);

            lastX = x;
            lastY = y;
        }
    }

    private int transform(double value, int height) {
        double min = 0;
        double max = 200;
        double normalized = (value - min) / (max - min);
        return height - (int) (normalized * height);
    }
}
