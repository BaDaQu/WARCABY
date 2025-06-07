package warcaby.main;

import warcaby.gui.frame.CheckersFrame;
import javax.swing.SwingUtilities;

public class Main {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            CheckersFrame checkersFrame = new CheckersFrame();
            checkersFrame.setVisible(true);
        });
    }
}