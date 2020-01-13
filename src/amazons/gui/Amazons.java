package amazons.gui;

import javax.swing.*;

public class Amazons extends JFrame {

    private static final long serialVersionUID = 1L;
    public static AmazonsBoard amazonsPanel;

    public Amazons() {
        setSize(415, 439);
        setResizable(false);
        setVisible(true);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        amazonsPanel = new AmazonsBoard(50, this);
        setContentPane(amazonsPanel);
        this.addKeyListener(amazonsPanel);
    }

    public static void main(String[] args) {
        (new Amazons()).setVisible(true);
    }
}
