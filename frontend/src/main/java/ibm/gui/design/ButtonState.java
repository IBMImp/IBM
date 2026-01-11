package ibm.gui.design;

import javax.swing.*;

public class ButtonState<T> {

    private final JButton button;
    private T state;

    public ButtonState(JButton button) {
        this.button = button;
    }

    public void setState(T newState) {
        if (state != newState ) {
            state = newState;
            onStateChange();
        }
    }

    public T getState() {
        return state;
    }

    public void onStateChange() {
        //default
    }
}
