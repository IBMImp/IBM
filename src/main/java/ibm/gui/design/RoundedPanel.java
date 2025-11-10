package ibm.gui.design;

import javax.swing.*;
import java.awt.*;

//Code written with assistance from ChatGpt (OpenAI) 2025
class RoundedPanel extends JPanel {
    private final int R, T;
    private final Color stroke = new Color(0,0,0);
    RoundedPanel(int radius, int thickness) {
        this.R = radius; this.T = thickness;
        setOpaque(false);
    }
    @Override protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int ox = T/2, oy = T/2, ww = getWidth()-T, hh = getHeight()-T;
        g2.setColor(getBackground());
        g2.fillRoundRect(ox, oy, ww, hh, 2*R, 2*R);
        g2.setStroke(new BasicStroke(T));
        g2.setColor(stroke);
        g2.drawRoundRect(ox, oy, ww, hh, 2*R, 2*R);
        g2.dispose();
    }
}
