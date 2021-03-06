package gomoku;

import framework.*;
import gomoku.game.Board;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class GoMokuFrame extends JFrame {
    private static final long serialVersionUID = 1L;
    public static GoMokuPanel goMokuPanel;
    public static int size = 9;

    public GoMokuFrame() {
        setResizable(false);
        setVisible(true);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        goMokuPanel = new GoMokuPanel(this);
        setContentPane(goMokuPanel);
        int sz = 2 * goMokuPanel.offset + (size * goMokuPanel.squareSize);
        setSize(sz, sz);
        this.addKeyListener(goMokuPanel);
        goMokuPanel.aiMove();
    }

    public static void main(String[] args) {
        (new GoMokuFrame()).setVisible(true);
    }

    private class GoMokuPanel extends JPanel implements MouseListener, MouseMotionListener, MoveCallback, KeyListener {
        private final Board board;
        private final JFrame frame;
        int offset = 40, squareSize = 50, winner = Board.NONE_WIN;
        AIPlayer aiPlayer1, aiPlayer2;
        boolean aiThinking = true;
        int boardCol = -1, boardRow = -1;
        private int humanPlayer = 2;
        private boolean allHuman = false, allAi = true;
        private MoveList legalMoves;

        public GoMokuPanel(JFrame frame) {
            board = new Board(size);
            this.frame = frame;
            board.initialize();
            legalMoves = board.getExpandMoves();
            Options.debug = true;
            aiPlayer1 = PlayerFactory.getPlayer(1, "gomoku");
            aiPlayer2 = PlayerFactory.getPlayer(2, "gomoku");
            aiPlayer1.setMoveCallback(this);
            aiPlayer2.setMoveCallback(this);
            addMouseListener(this);
            addMouseMotionListener(this);

            if (allHuman)
                humanPlayer = 1;

            int delay = 1000; //milliseconds
            ActionListener taskPerformer = new ActionListener() {
                public void actionPerformed(ActionEvent evt) {
                    if (!aiThinking)
                        aiMove();
                }
            };
            new Timer(delay, taskPerformer).start();
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
                String eval = " e1: "+ board.evaluate(1) + " e2: " + board.evaluate(2);
                setTitle(player + eval);
            }
            System.gc();
            aiThinking = false;
            System.out.println(" e1: " + board.longest1 + " e2: " + board.longest2);
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