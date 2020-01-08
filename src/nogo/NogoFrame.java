package nogo;

import framework.AIPlayer;
import framework.MoveCallback;
import framework.MoveList;
import framework.Options;
import mcts.uct.UCTPlayer;
import nogo.game.Board;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

public class NogoFrame extends JFrame {
    private static final long serialVersionUID = 1L;
    public static NogoPanel nogoPanel;

    public NogoFrame() {
        setResizable(false);
        setVisible(true);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        nogoPanel = new NogoPanel();
        setContentPane(nogoPanel);
        int size = 2 * nogoPanel.offset + ((8) * nogoPanel.squareSize);
        setSize(size, size + nogoPanel.squareSize);
        nogoPanel.startGame();
    }

    public static void main(String[] args) {
        (new NogoFrame()).setVisible(true);
    }

    private class NogoPanel extends JPanel implements MouseListener, MouseMotionListener, MoveCallback {
        private int humanPlayer = 2;
        private boolean allHuman = false, allAi = true;
        private final Board board;

        int offset = 40, squareSize = 50;
        private MoveList legalMoves;
        AIPlayer aiPlayer1, aiPlayer2;

        public void startGame() {
            if(!allHuman)
                aiPlayer1.getMove(board.clone());
        }

        public NogoPanel() {
            board = new Board(9);
            board.initialize();
            legalMoves = board.getExpandMoves();
            Options.debug = true;

            Options options1 = new Options();
            aiPlayer1 = new UCTPlayer();
            aiPlayer1.setOptions(options1);
            options1.fixedSimulations = true;
            options1.nSimulations = 100000;
            options1.imm = true;

            Options options2 = new Options();
            aiPlayer2 = new UCTPlayer();
            aiPlayer2.setOptions(options1);
            options2.fixedSimulations = true;
            options2.nSimulations = 100000;
            options2.imm = true;

            aiPlayer1.setMoveCallback(this);
            aiPlayer2.setMoveCallback(this);
            addMouseListener(this);
            addMouseMotionListener(this);

            if(allHuman)
                humanPlayer = 1;
        }

        @Override
        public void makeMove(int[] move) {
            int winner = board.checkWin();
            if (winner == Board.NONE_WIN)
                board.doMove(move);

            legalMoves = board.getExpandMoves();

            winner = board.checkWin();
            if(winner != Board.NONE_WIN) {
                System.out.println("Winner is " + board.checkWin());
                return;
            }
            repaint();

            if(!allHuman)
                if((allAi && board.cPlayer == 1) || board.cPlayer != humanPlayer)
                    aiPlayer1.getMove(board.clone());
                else if((allAi && board.cPlayer == 2) || board.cPlayer != humanPlayer)
                    aiPlayer2.getMove(board.clone());
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
                    }
                }
            }

            g.setColor(Color.darkGray);
            for (int i = 0; i < legalMoves.size(); i++) {
                y = offset + (legalMoves.get(i)[1] * squareSize);
                x = offset + (legalMoves.get(i)[0] * squareSize);
                g.fillRect(x - 4, y - 4, 8, 8);
            }

            if (boardCol < board.size && boardRow < board.size) {
                x = offset + (boardCol * squareSize);
                y = offset + (boardRow * squareSize);
                g.setColor(Color.lightGray);
                g.fillOval(x - 10, y - 10, 20, 20);
            }
        }

        @Override
        public void mouseDragged(MouseEvent e) {

        }

        int boardCol = -1, boardRow = -1;

        @Override
        public void mouseMoved(MouseEvent arg0) {
            boardCol = (arg0.getX()) / squareSize;
            boardRow = (arg0.getY()) / squareSize;
            repaint();
        }

        @Override
        public void mouseClicked(MouseEvent arg0) {
            if(!allAi && board.cPlayer == humanPlayer) {
                for (int i = 0; i < legalMoves.size(); i++) {
                    if (legalMoves.get(i)[0] == boardCol && legalMoves.get(i)[1] == boardRow) {
                        makeMove(legalMoves.get(i));
                        if(allHuman)
                            humanPlayer = 3 - humanPlayer;
                    }
                }
            }
            arg0.consume();
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
    }
}