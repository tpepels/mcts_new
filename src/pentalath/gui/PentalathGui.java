package pentalath.gui;

import pentalath.game.Board;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class PentalathGui extends JFrame {

    private static final long serialVersionUID = -1921481286866231418L;
    private JPanel contentPane;
    public static PentalathPanel pentalathPanel;
    private final ButtonGroup player2Group = new ButtonGroup();
    private final ButtonGroup player1Group = new ButtonGroup();
    private Board currentBoard;
    private final JTextField p1RValue;
    private final JTextField p2RValue;
    private static JTextArea output;
    private static JScrollPane scrollPane;

    /**
     * Launch the application.
     */
    public static void main(String[] args) {
        EventQueue.invokeLater(new Runnable() {
            public void run() {
                try {
                    PentalathGui frame = new PentalathGui();
                    frame.setVisible(true);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public static void logMessage(String s) {

        output.append(s + "\n");
        JScrollBar vertical = scrollPane.getVerticalScrollBar();
        vertical.setValue(vertical.getMaximum());
    }

    /**
     * Create the frame.
     */
    public PentalathGui() {
        setResizable(false);
        setBackground(Color.BLACK);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setBounds(100, 100, 640, 623);

        JMenuBar menuBar = new JMenuBar();
        setJMenuBar(menuBar);

        JMenu mnFile = new JMenu("File");
        menuBar.add(mnFile);
        currentBoard = new Board();
        JMenuItem mntmNewGame = new JMenuItem("New game");
        mntmNewGame.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent arg0) {
                currentBoard = new Board();
                pentalathPanel.setBoard(currentBoard);
            }
        });
        mnFile.add(mntmNewGame);

        final JRadioButtonMenuItem moveNotationMenu = new JRadioButtonMenuItem("Move notation");
        moveNotationMenu.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent arg0) {
                pentalathPanel.moveNotation = !moveNotationMenu.isSelected();
                pentalathPanel.repaint();
            }
        });

        mnFile.add(moveNotationMenu);
        mnFile.addSeparator();

        JMenu mnPlayer = new JMenu("Player 1");
        menuBar.add(mnPlayer);

        final JRadioButtonMenuItem player1Human = new JRadioButtonMenuItem("Human");
        player1Human.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent arg0) {
                pentalathPanel.setPlayer(1, player1Human.isSelected());
            }
        });
        player1Group.add(player1Human);
        mnPlayer.add(player1Human);

        final JRadioButtonMenuItem player1AI = new JRadioButtonMenuItem("A.I.");
        player1AI.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                pentalathPanel.setPlayer(1, !player1AI.isSelected());
            }
        });
        player1Group.add(player1AI);
        player1AI.setSelected(true);
        mnPlayer.add(player1AI);

        JMenu mnPlayer_1 = new JMenu("Player 2");
        menuBar.add(mnPlayer_1);

        final JRadioButtonMenuItem player2Human = new JRadioButtonMenuItem("Human");
        player2Human.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                pentalathPanel.setPlayer(2, player2Human.isSelected());
            }
        });
        player2Group.add(player2Human);
        mnPlayer_1.add(player2Human);

        final JRadioButtonMenuItem player2AI = new JRadioButtonMenuItem("A.I.");
        player2AI.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                pentalathPanel.setPlayer(2, !player2AI.isSelected());
            }
        });
        player2AI.setSelected(true);
        player2Group.add(player2AI);
        mnPlayer_1.add(player2AI);

        JMenuItem mntmNewMenuItem_1 = new JMenuItem("AI Move");
        mntmNewMenuItem_1.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent arg0) {
                pentalathPanel.makeAIMove();
            }
        });
        menuBar.add(mntmNewMenuItem_1);

        JMenuItem mntmPrintStats = new JMenuItem("Print stats");
        menuBar.add(mntmPrintStats);
        mntmPrintStats.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent arg0) {
                // pentalathPanel.printStats();
            }
        });
        contentPane = new JPanel();
        contentPane.setBackground(Color.BLACK);
        contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
        setContentPane(contentPane);
        contentPane.setLayout(new BorderLayout(0, 0));
        pentalathPanel = new PentalathPanel(currentBoard, false, false);
        pentalathPanel.setBackground(Color.BLACK);
        contentPane.add(pentalathPanel);
        pentalathPanel.setLayout(null);

        JPanel panel = new JPanel();
        //contentPane.add(panel, BorderLayout.EAST);
        panel.setLayout(new BorderLayout(0, 0));

        JPanel p1Panel = new JPanel();
        p1Panel.setBorder(new TitledBorder(null, "Player 1", TitledBorder.LEADING,
                TitledBorder.TOP, null, null));
        panel.add(p1Panel, BorderLayout.NORTH);
        p1Panel.setLayout(new GridLayout(0, 2, 0, 0));

        final JCheckBox p1NullMoves = new JCheckBox("Null moves");
        p1Panel.add(p1NullMoves);

        p1RValue = new JTextField();
        p1RValue.setText("Delta");
        p1RValue.setToolTipText("Delta");
        p1Panel.add(p1RValue);
        p1RValue.setColumns(10);

        final JCheckBox p1Transpositions = new JCheckBox("Transpositions");
        p1Panel.add(p1Transpositions);

        final JCheckBox p1HistoryHeuristic = new JCheckBox("History heuristic");
        p1Panel.add(p1HistoryHeuristic);

        JButton p1OkButton = new JButton("Set");
        p1OkButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent arg0) {
                if (pentalathPanel.aiPlayer1 != null) {
                    // pentalathPanel.aiPlayer1.nullmoves = p1NullMoves.isSelected();
                    // pentalathPanel.aiPlayer1.transpositions = p1Transpositions.isSelected();
                    // pentalathPanel.aiPlayer1.historyHeuristic = p1HistoryHeuristic.isSelected();
                    // pentalathPanel.aiPlayer1.DELTA = Integer.parseInt(p1RValue.getText());
                }
            }
        });
        p1Panel.add(p1OkButton);

        JButton p1GetButton = new JButton("Get");
        p1GetButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (pentalathPanel.aiPlayer1 != null) {
//					p1NullMoves.setSelected(pentalathPanel.aiPlayer1.nullmoves);
//					p1Transpositions.setSelected(pentalathPanel.aiPlayer1.transpositions);
//					p1HistoryHeuristic.setSelected(pentalathPanel.aiPlayer1.historyHeuristic);
//					p1RValue.setText(Integer.toString(pentalathPanel.aiPlayer1.DELTA));
                }
            }
        });
        p1Panel.add(p1GetButton);
        JPanel p2Panel = new JPanel();
        p2Panel.setBorder(new TitledBorder(UIManager.getBorder("TitledBorder.border"), "Player 2",
                TitledBorder.LEADING, TitledBorder.TOP, null, null));
        panel.add(p2Panel, BorderLayout.SOUTH);
        p2Panel.setLayout(new GridLayout(0, 2, 0, 0));

        final JCheckBox p2NullMoves = new JCheckBox("Null moves");
        p2Panel.add(p2NullMoves);

        p2RValue = new JTextField();
        p2RValue.setToolTipText("Delta");
        p2RValue.setText("Delta");
        p2RValue.setColumns(10);
        p2Panel.add(p2RValue);

        final JCheckBox p2Transpositions = new JCheckBox("Transpositions");
        p2Panel.add(p2Transpositions);

        final JCheckBox p2HistoryHeuristic = new JCheckBox("History heuristic");
        p2Panel.add(p2HistoryHeuristic);

        JButton p2OkButton = new JButton("Set");
        p2OkButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (pentalathPanel.aiPlayer2 != null) {
                    // pentalathPanel.aiPlayer2.nullmoves = p2NullMoves.isSelected();
                    // pentalathPanel.aiPlayer2.transpositions = p2Transpositions.isSelected();
                    // pentalathPanel.aiPlayer2.historyHeuristic = p2HistoryHeuristic.isSelected();
//					pentalathPanel.aiPlayer2.DELTA = Integer.parseInt(p2RValue.getText());
                }
            }
        });
        p2Panel.add(p2OkButton);

        JButton p2GetButton = new JButton("Get");
        p2GetButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (pentalathPanel.aiPlayer2 != null) {
//					p2NullMoves.setSelected(pentalathPanel.aiPlayer2.nullmoves);
//					p2Transpositions.setSelected(pentalathPanel.aiPlayer2.transpositions);
//					p2HistoryHeuristic.setSelected(pentalathPanel.aiPlayer2.historyHeuristic);
//					p2RValue.setText(Integer.toString(pentalathPanel.aiPlayer2.DELTA));
                }
            }
        });
        p2Panel.add(p2GetButton);

        output = new JTextArea();
        output.setFont(new Font("Consolas", Font.BOLD, 11));
        output.setBackground(Color.BLACK);
        output.setForeground(Color.WHITE);
        panel.add(output, BorderLayout.CENTER);
        output.setAutoscrolls(true);
        output.setEditable(false);
        scrollPane = new JScrollPane(output);
        scrollPane.setBorder(new TitledBorder(UIManager.getBorder("TitledBorder.border"), "Output",
                TitledBorder.LEADING, TitledBorder.TOP, null, null));
        scrollPane.setAutoscrolls(true);
        panel.add(scrollPane, BorderLayout.CENTER);
    }

    protected PentalathPanel getPentalathPanel() {
        return pentalathPanel;
    }

    public JTextArea getOutput() {
        return output;
    }

    public static void showBoard(Board board) {
        pentalathPanel.setBoard(board);

    }
}
