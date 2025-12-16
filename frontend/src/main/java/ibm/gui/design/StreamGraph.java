package ibm.gui.design;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;

//TODO Custom graph component if the plotting library not utilized. I want the graph to be scrollable
public class StreamGraph extends JPanel implements Scrollable{
    public StreamGraph() {

    }
    @Override
    public Dimension getPreferredScrollableViewportSize() {
        return null;
    }

    @Override
    public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
        return 0;
    }

    @Override
    public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
        return 0;
    }

    @Override
    public boolean getScrollableTracksViewportWidth() {
        return false;
    }

    @Override
    public boolean getScrollableTracksViewportHeight() {
        return false;
    }
}