package ibm.gui;

import ibm.gui.design.LandingForm;

import javax.swing.*;
import java.awt.*;

public class InterfaceGUI {
    public InterfaceGUI() {
        JPanel cards = new JPanel(new CardLayout());
        CardLayout cards_switcher = (CardLayout)cards.getLayout();
        LandingForm landingForm = new LandingForm();

        cards.add(landingForm.getPanel(), "");
        landingForm.getStart_button().addActionListener(e-> {
            cards_switcher.show(cards, "LandingForm");
        });
    }
    public boolean showGUI() {

    }

}
