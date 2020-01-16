package atarigo;

import atarigo.game.Board;
import framework.AIPlayer;
import framework.MoveCallback;
import framework.MoveList;
import framework.Options;
import mcts.uct.UCTPlayer;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class AtariGoFrame extends JFrame {
    private static final long serialVersionUID = 1L;
    public static NogoPanel nogoPanel;

    public AtariGoFrame() {
        setResizable(false);
        setVisible(true);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        nogoPanel = new NogoPanel(this);
        setContentPane(nogoPanel);
        int size = 2 * nogoPanel.offset + (7 * nogoPanel.squareSize);
        setSize(size, size + nogoPanel.squareSize);
        this.addKeyListener(nogoPanel);
        nogoPanel.aiMove();
    }

    public static void main(String[] args) {
        (new AtariGoFrame()).setVisible(true);
    }

    private class NogoPanel extends JPanel implements MouseListener, MouseMotionListener, MoveCallback, KeyListener {
        private final Board board;
        private final JFrame frame;
        int offset = 40, squareSize = 50, winner = Board.NONE_WIN;
        AIPlayer aiPlayer1, aiPlayer2;
        Options options1, options2;
        boolean aiThinking = true;
        int boardCol = -1, boardRow = -1;
        private int humanPlayer = 2;
        private boolean allHuman = false, allAi = true;
        private MoveList legalMoves;

        public NogoPanel(JFrame frame) {
            board = new Board(7);
            this.frame = frame;
            board.initialize();
            legalMoves = board.getExpandMoves();
            Options.debug = true;

            options1 = new Options();
            aiPlayer1 = new UCTPlayer();
            aiPlayer1.setOptions(options1);
            options1.fixedSimulations = true;
            options1.nSimulations = 50000;
            // options1.RAVE = true;
            // options1.MAST = true;
            // options1.imm = true;
            options1.regression = true;

            options2 = new Options();
            aiPlayer2 = new UCTPlayer();
            aiPlayer2.setOptions(options2);
            options2.fixedSimulations = true;
            options2.nSimulations = 50000;

            aiPlayer1.setMoveCallback(this);
            aiPlayer2.setMoveCallback(this);
            addMouseListener(this);
            addMouseMotionListener(this);

            if (allHuman)
                humanPlayer = 1;
        }

        private void aiMove() {
            if (!allHuman && winner == Board.NONE_WIN) {
                if ((allAi && board.cPlayer == 1) || board.cPlayer != humanPlayer) {
                    aiThinking = true;
                    frame.setTitle("AI 1 Thinking .....");
                    Thread t = new Thread(aiPlayer1);
                    aiPlayer1.setBoard(board.clone());
                    t.run();
                } else if ((allAi && board.cPlayer == 2) || board.cPlayer != humanPlayer) {
                    aiThinking = true;
                    frame.setTitle("AI 2 Thinking .....");
                    Thread t = new Thread(aiPlayer2);
                    aiPlayer2.setBoard(board.clone());
                    t.run();
                }
            }
        }

        @Override
        public void makeMove(int[] move) {
            winner = board.checkWin();
            if (winner == Board.NONE_WIN)
                board.doMove(move);

            legalMoves = board.getExpandMoves();
            repaint();
            winner = board.checkWin();
            if (winner != Board.NONE_WIN) {
                frame.setTitle("Winner is " + board.checkWin());
            } else {
                String player = board.getPlayerToMove() == 1 ? "bl" : "wh";
                String eval = " e1: " + board.evaluate(1) + " e2 " + board.evaluate(2);
                String maxLib = " mx1: " + board.maxLiberty[0] + " mx2: " + board.maxLiberty[1];
                String minLib = " mn1: " + board.minLiberty[0] + " mn2: " + board.minLiberty[1];
                setTitle(player + eval + maxLib + minLib);
            }
            aiThinking = false;
        }

        public void paint(Graphics g) {
            super.paint(g);
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            // Background color
            g.setColor(Color.decode("#FFE4C4"));
            g.fillRect(0, 0, getWidth(), getHeight());
            // Square pattern
            g.setColor(Color.black);
            int x, y, occ;
            for (int i = 0; i < board.size - 1; i++) {
                for (int j = 0; j < board.size - 1; j++) {
                    x = offset + (i * squareSize);
                    y = offset + (j * squareSize);
                    g.drawRect(x, y, squareSize, squareSize);
                    g.setColor(Color.BLACK);
                }
            }
            for (int i = 0; i < board.size; i++) {
                for (int j = 0; j < board.size; j++) {
                    x = offset + (i * squareSize);
                    y = offset + (j * squareSize);
                    occ = board.board[j][i];
                    if (occ != 0) {

                        if (occ == 1)
                            g.setColor(Color.BLACK);
                        else g.setColor(Color.WHITE);

                        g.fillOval(x - 20, y - 20, 40, 40);

                        if (occ == 1)
                            g.setColor(Color.WHITE);
                        else g.setColor(Color.BLACK);

                        g.drawString(Integer.toString(board.liberty[j][i]), x, y);
                    } else {
                        g.setColor(Color.BLACK);
                        g.drawString(board.emptyLiberty[0][j][i] + "/" + board.emptyLiberty[1][j][i], x, y - 10);
                    }
                }
            }
            if (options1.MAST) {
                g.setColor(Color.black);
                for (int i = 0; i < board.size; i++) {
                    for (int j = 0; j < board.size; j++) {
                        occ = board.board[j][i];
                        if (occ != 0)
                            continue;

                        x = offset + (i * squareSize);
                        y = offset + (j * squareSize);
                        String MAST = String.format("%1$,.2f", options1.getMASTValue(1, board.getMoveId(new int[]{i, j})));
                        g.drawString(MAST, x, y);
                    }
                }
            }

            g.setColor(Color.lightGray);
            if (boardCol < board.size && boardRow < board.size) {
                x = offset + (boardCol * squareSize);
                y = offset + (boardRow * squareSize);

                g.fillOval(x - 10, y - 10, 20, 20);

            }
        }

        @Override
        public void mouseClicked(MouseEvent arg0) {
            if (!allAi && board.cPlayer == humanPlayer) {
                for (int i = 0; i < legalMoves.size(); i++) {
                    if (legalMoves.get(i)[0] == boardCol && legalMoves.get(i)[1] == boardRow) {
                        makeMove(legalMoves.get(i));
                        if (allHuman)
                            humanPlayer = 3 - humanPlayer;
                    }
                }
            }
            arg0.consume();
        }

        @Override
        public void mouseDragged(MouseEvent e) {

        }

        @Override
        public void mouseMoved(MouseEvent arg0) {
            boardCol = (arg0.getX()) / squareSize;
            boardRow = (arg0.getY()) / squareSize;
            repaint();
        }

        @Override
        public void mousePressed(MouseEvent e) {

        }

        @Override
        public void mouseReleased(MouseEvent e) {

        }

        @Override
        public void mouseEntered(MouseEvent e) {
            boardCol = 0;
            boardRow = 0;
        }

        @Override
        public void mouseExited(MouseEvent e) {
            boardCol = -1;
            boardRow = -1;
        }

        @Override
        public void keyTyped(KeyEvent e) {

        }

        @Override
        public void keyPressed(KeyEvent e) {
            if (e.getKeyCode() == KeyEvent.VK_SPACE) {
                if (!aiThinking)
                    aiMove();
            }
        }

        @Override
        public void keyReleased(KeyEvent e) {

        }
    }
}