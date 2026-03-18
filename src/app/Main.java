package app;

import view.AppFrame;
import javax.swing.SwingUtilities;

public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(AppFrame::new);
    }
}
