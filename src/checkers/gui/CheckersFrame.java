package checkers.gui;

import javax.swing.*;

/**
 * Created by tom on 13/12/13.
 */
public class CheckersFrame extends JFrame {
    private static final long serialVersionUID = 1L;
    public static CheckersPanel checkersPanel;

    public CheckersFrame() {
        setSize(405, 429);
        setResizable(false);
        setVisible(true);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        checkersPanel = new CheckersPanel(50, this);
        setContentPane(checkersPanel);
    }

    public static void main(String[] args) {
        (new CheckersFrame()).setVisible(true);
    }
}
