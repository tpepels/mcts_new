package pentalath.gui;

import framework.Options;
import mcts.uct.UCTPlayer;
import pentalath.com.rush.HexGridCell;
import framework.AIPlayer;
import framework.MoveCallback;
import pentalath.game.Board;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.text.DecimalFormat;
import java.util.concurrent.TimeUnit;

public class PentalathPanel extends JPanel implements MouseListener, MoveCallback, ActionListener {
    public static final short[] numbers = {0, 0, 1, 2, 3, 4, 5, 0, 0, 0, 1, 2, 3, 4, 5, 6, 0, 0,
            0, 1, 2, 3, 4, 5, 6, 7, 0, 1, 2, 3, 4, 5, 6, 7, 8, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 2, 3,
            4, 5, 6, 7, 8, 9, 0, 0, 3, 4, 5, 6, 7, 8, 9, 0, 0, 4, 5, 6, 7, 8, 9, 0, 0, 0, 0, 5, 6,
            7, 8, 9, 0, 0,};
    private static final int CELL_R = 40;
    public boolean moveNotation = false;
    public AIPlayer aiPlayer1, aiPlayer2;
    public String aiMessage = "";
    DecimalFormat df3 = new DecimalFormat("#,###,###,###,#00");
    //
    private int[] cornersY = new int[6], cornersX = new int[6];
    private HexGridCell hexagons = new HexGridCell(CELL_R);
    private Board board;
    //
    private boolean p1Human = true, p2Human = true, aiThinking = false;
    private int movenum = 0;
    private int[] lastMove;
    private Timer t = new Timer(1000, this);

    public PentalathPanel(Board board, boolean p1Human, boolean p2Human) {
        this.board = board;
        board.initialize();
        this.p1Human = p1Human;
        this.p2Human = p2Human;
        //
        resetPlayers();
        addMouseListener(this);
        t = new Timer(1000, this);
        t.start();
        makeAIMove();
    }

    private void resetPlayers() {
        if (!p1Human) {
            aiPlayer1 = new UCTPlayer();
            Options options1 = new Options();
            options1.nSimulations = 50000;
            options1.fixedSimulations = true;
            aiPlayer1.setOptions(options1);
        }
        if (!p2Human) {
            aiPlayer2 = new UCTPlayer();
            Options options2 = new Options();
            options2.nSimulations = 50000;
            options2.fixedSimulations = true;
            aiPlayer2.setOptions(options2);
        }
    }

    public void makeAIMove() {
        if (board.currentPlayer == 1 && !p1Human) {
            aiThinking = true;
            aiPlayer1.getMove(board.clone());
            aiMessage = "Player 1, thinking ...";
        } else if (board.currentPlayer == 2 && !p2Human) {
            aiThinking = true;
            aiPlayer2.getMove(board.clone());
            aiMessage = "Player 2, thinking ...";
        }
    }

    public void setBoard(Board board) {
        this.board = board;
        board.initialize();
        movenum = 0;
        resetPlayers();
        repaint();
    }

    @Override
    public void paint(Graphics g) {
        super.paint(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setFont(new Font("Arial", Font.PLAIN, 12));
        if (board.currentPlayer == Board.P2)
            g2d.setColor(Color.black);
        else
            g2d.setColor(Color.decode("#FFFFDD"));
        g2d.fillRect(0, 0, getWidth(), getHeight());
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        //
        for (int i = 0; i < Board.WIDTH; i++) {
            for (int j = 0; j < Board.WIDTH; j++) {
                hexagons.setCellIndex(i, j);
                //
                if (Board.occupancy[i * Board.WIDTH + j] == 1) {
                    hexagons.computeCorners(cornersY, cornersX);
                    g2d.setColor(Color.decode("#FFE47A"));
                    g2d.fillPolygon(cornersX, cornersY, 6);
                    g2d.setColor(Color.DARK_GRAY);
                    g2d.drawPolygon(cornersX, cornersY, 6);
                    //
                    g2d.setColor(Color.DARK_GRAY);
                    if (board.board[i * Board.WIDTH + j].occupant == Board.P2) {
                        g2d.setColor(Color.black);
                        g2d.fillOval(cornersX[0] + 10, cornersY[0] - 5, 50, 50);
                        g2d.setColor(Color.decode("#FFFFDD"));
                    }
                    //
                    if (board.board[i * Board.WIDTH + j].occupant == Board.P1) {
                        g2d.setColor(Color.decode("#FFFFDD"));
                        g2d.fillOval(cornersX[0] + 10, cornersY[0] - 5, 50, 50);
                        g2d.setColor(Color.black);
                    }
                    // g2d.drawString(Integer.toString(board.board[i * Board.WIDTH + j].position),
                    // cornersX[0] + 17, cornersY[0] + 17);
                    g2d.drawString(positionString(board.board[i * Board.WIDTH + j].position),
                            cornersX[0] + 17, cornersY[0] + 17);
                }
            }
        }
        //
        if (board.currentPlayer == Board.P1)
            g2d.setColor(Color.black);
        else
            g2d.setColor(Color.decode("#FFFFDD"));

        g2d.setFont(new Font("Arial", Font.BOLD, 14));
        g2d.drawString(aiMessage, 10, this.getHeight() - 15);
    }

    //
    private String positionString(int position) {
        if (moveNotation) {
            char letter = 'a';
            int row = position / 9;
            letter += row;
            return String.format(letter + "" + numbers[position]);
        } else {
            return Integer.toString(position);
        }
    }

    public void setPlayer(int player, boolean human) {
        System.out.println("Player " + player + " human: " + human);
        if (player == 1) {
            this.p1Human = human;
        } else {
            this.p2Human = human;
        }
        resetPlayers();
    }

    @Override
    public void update(Graphics g) {
        paint(g);
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        // TODO Auto-generated method stub

    }

    @Override
    public void mouseEntered(MouseEvent e) {
        // TODO Auto-generated method stub

    }

    @Override
    public void mouseExited(MouseEvent e) {
        // TODO Auto-generated method stub

    }

    @Override
    public void mousePressed(MouseEvent e) {
        // TODO Auto-generated method stub

    }

    private boolean isInsideBoard(int i, int j) {
        return i >= 0 && i < Board.WIDTH && j >= 0 && j < Board.WIDTH
                && Board.occupancy[i * Board.WIDTH + j] == 1;
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        // Check if human player.
        if (board.currentPlayer == 1 && !p1Human) {
            return;
        } else if (board.currentPlayer == 2 && !p2Human) {
            return;
        }
        //
        hexagons.setCellByPoint(e.getY(), e.getX());
        int clickI = hexagons.getIndexI();
        int clickJ = hexagons.getIndexJ();
        //
        if (isInsideBoard(clickI, clickJ)) {
            makeMove(new int[]{clickI * Board.WIDTH + clickJ});
        }

    }

    @Override
    public void makeMove(int[] move) {
        aiMessage = "";
        aiThinking = false;
        board.doMove(move);
        if (board.capturePieces(move[0])) { // Returns false if suicide without capture
            lastMove = move;
            movenum++;
            PentalathGui.logMessage(df3.format(movenum) + ": player: "
                    + (3 - board.currentPlayer) + " move: "
                    + positionString(move[0]));
            int winner = board.checkWin();
            if (winner != Board.NONE_WIN) {
                String message = "";
                //
                if (winner == Board.P2_WIN) {
                    message = "Black wins!";
                } else {
                    message = "White wins!";
                }
                if (winner == Board.DRAW) {
                    message = "It's a draw!";
                }
                //
                PentalathGui.logMessage(message);
                // printStats();
            } else {
                // Run the GC in between moves, to limit the runs during search
                System.gc();
                // Check if the AI should make a move
                if (board.currentPlayer == 1 && !p1Human) {
                    aiThinking = true;
                    aiPlayer1.getMove(board.clone());
                    aiMessage = "Player 1, thinking ...";
                } else if (board.currentPlayer == 2 && !p2Human) {
                    aiThinking = true;
                    aiPlayer2.getMove(board.clone());
                    aiMessage = "Player 2, thinking ...";
                }
            }
        }
        repaint();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (aiThinking) {
            repaint();
        }
    }
}
