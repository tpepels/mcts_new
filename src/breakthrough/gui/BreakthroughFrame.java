package breakthrough.gui;

import javax.swing.*;

/**
 * Created by tom on 13/12/13.
 */
public class BreakthroughFrame extends JFrame {
    private static final long serialVersionUID = 1L;
    public static BreakthroughPanel breakthroughPanel;

    public BreakthroughFrame() {
        setSize(410, 440);
        setResizable(false);
        setVisible(true);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        breakthroughPanel = new BreakthroughPanel(50, this);
        setContentPane(breakthroughPanel);
        breakthroughPanel.aiMove();
    }

    public static void main(String[] args) {
        (new BreakthroughFrame()).setVisible(true);
    }
}
