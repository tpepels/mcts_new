package amazons.gui;


import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.Timer;

import amazons.game.Board;
import framework.AIPlayer;
import framework.MoveCallback;
import framework.PlayerFactory;
import framework.gui.GuiOptions;

public class AmazonsPanel extends JPanel implements MouseListener, MouseMotionListener, MoveCallback, KeyListener {
    private static final long serialVersionUID = 1L;
    private final JFrame frame;
    private final int[] moves = new int[40];
    private int squareSize, boardx = -1, boardy = -1, clickNum = 0, movec = 0, winner;
    private int[] clickPos = {-1, -1, -1};
    //
    private Board board;
    private AIPlayer aiPlayer1, aiPlayer2;
    private Image whiteQueen, blackQueen;
    private boolean aiThinking = false;

    public AmazonsPanel(int squareSize, JFrame frame) {
        this.squareSize = squareSize;
        this.frame = frame;
        board = new Board();
        board.initialize();
        //
        if (board.getPlayerToMove() == Board.BLACK_Q) {
            frame.setTitle("Amazons - Black's move.");
        } else {
            frame.setTitle("Amazons - White's move.");
        }
        //
        this.addMouseListener(this);
        this.addMouseMotionListener(this);

        try {
            whiteQueen = ImageIO.read(new File(getClass().getResource("/img/white_queen.png").getPath()));
            blackQueen = ImageIO.read(new File(getClass().getResource("/img/black_queen.png").getPath()));
        } catch (IOException e) {
            e.printStackTrace();
        }

        aiPlayer1 = PlayerFactory.getPlayer(1, "amazons");
        aiPlayer2 = PlayerFactory.getPlayer(2, "amazons");

        aiPlayer1.setMoveCallback(this);
        aiPlayer2.setMoveCallback(this);
        repaint();
        aiMove();
        int delay = 1000; //milliseconds
        ActionListener taskPerformer = new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                if (!aiThinking)
                    aiMove();
            }
        };
        new Timer(delay, taskPerformer).start();
    }

    public void paint(Graphics g) {
        super.paint(g);
        int row, col, x, y, boardPos;
        Graphics2D g2d = (Graphics2D) g;

        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int NUM_SQUARES = Board.SIZE;
        for (row = 0; row < NUM_SQUARES; row++) {
            for (col = 0; col < NUM_SQUARES; col++) {
                boardPos = row * NUM_SQUARES + col;
                //
                x = col * squareSize;
                y = row * squareSize;
                //
                if (boardPos == clickPos[0]) {
                    g.setColor(GuiOptions.S_Square_Color);
                } else if (boardPos == clickPos[1]) {
                    g.setColor(GuiOptions.S_Square_Color);
                } else if (boardPos == clickPos[2]) {
                    g.setColor(GuiOptions.S_Square_Color);
                } else if (col == boardx && row == boardy) {
                    g.setColor(GuiOptions.S_Square_Color);
                } else if ((row % 2) == (col % 2)) {
                    g.setColor(GuiOptions.D_Square_Color);
                } else {
                    g.setColor(GuiOptions.L_Square_Color);
                }

                if (movec > 0) {
                    if (isAvailMove(boardPos)) {
                        g.setColor(g.getColor().brighter());
                    }
                }

                g.fillRect(x, y, squareSize, squareSize);
                g.setColor(GuiOptions.P1_Color);
                g.drawString(Integer.toString(boardPos), x + 1, y + 11);
                int boardPiece = board.board[boardPos];
                if (boardPiece != Board.EMPTY) {
                    if ((boardPiece / 10) == Board.WHITE_Q) {
                        g.drawImage(whiteQueen, x + 3, y + 3, squareSize - 6, squareSize - 6, null);
                    } else if ((boardPiece / 10) == Board.BLACK_Q) {
                        g.drawImage(blackQueen, x + 3, y + 3, squareSize - 6, squareSize - 6, null);
                    } else if (boardPiece == Board.ARROW) {
                        g.setColor(GuiOptions.Extra_Color_1);
                        g.fillOval(x + 5, y + 5, squareSize - 10, squareSize - 10);
                    }
                }
            }

        }
    }

    public boolean isAvailMove(int pos) {
        for (int i = 0; i < movec; i++) {
            if (moves[i] == pos) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void mouseClicked(MouseEvent arg0) {
        winner = board.checkWin();
        //
        if (winner == Board.P2_WIN) {
            frame.setTitle("Amazons - Black wins");
        } else if (winner == Board.P1_WIN) {
            frame.setTitle("Amazons - White wins.");
        }
        //
        int boardPos = boardy * Board.SIZE + boardx;
        if (clickNum == 0) {
            if ((board.board[boardPos] / Board.SIZE) != board.getPlayerToMove()) {
                return;
            }
            movec = board.getPossibleMovesFrom(boardPos, moves);
        } else if (clickNum == 1) {
            if (board.board[boardPos] != Board.EMPTY) {
                return;
            }
            movec = board.getPossibleMovesFrom(boardPos, moves);
        } else if (clickNum == 2) {
            if (board.board[boardPos] != Board.EMPTY && boardPos != clickPos[0]) {
                return;
            }
            movec = 0;
        }
        //
        clickPos[clickNum] = boardPos;
        clickNum++;
        if (clickNum == 3) {
            board.doMove(new int[]{clickPos[0], clickPos[1], clickPos[2]});
            //
            if (board.getPlayerToMove() == Board.BLACK_Q) {
                frame.setTitle("Amazons - Black's move.");
            } else {
                frame.setTitle("Amazons - White's move.");
            }
            repaint();
            clickNum = 0;
            clickPos = new int[]{-1, -1, -1};
        }
    }

    @Override
    public void mouseEntered(MouseEvent arg0) {

    }

    @Override
    public void mouseExited(MouseEvent arg0) {
        boardx = -1;
        boardy = -1;
    }

    @Override
    public void mouseDragged(MouseEvent arg0) {

    }

    @Override
    public void mouseMoved(MouseEvent arg0) {
        boardx = arg0.getX() / squareSize;
        boardy = arg0.getY() / squareSize;
        repaint();
    }

    @Override
    public void mousePressed(MouseEvent arg0) {

    }

    @Override
    public void mouseReleased(MouseEvent arg0) {

    }

    @Override
    public void makeMove(int[] move) {
        board.doMove(move);
        //
        winner = board.checkWin();
        if (winner == Board.P2_WIN) {
            frame.setTitle("Amazons - Black wins");
            return;
        } else if (winner == Board.P1_WIN) {
            frame.setTitle("Amazons - White wins.");
            return;
        } else {
            double eval1 = board.evaluate(1);
            double eval2 = board.evaluate(2);
            String player = board.currentPlayer == 1 ? "white" : "black";
            frame.setTitle(player + " to move -- eval1: " + eval1 + " eval2: " + eval2);
        }
        repaint();
        clickNum = 0;
        clickPos = new int[]{-1, -1, -1};
        // Run the GC in between moves, to limit the runs during search
        System.gc();
        aiThinking = false;
    }

    private void aiMove() {
        if (winner == Board.NONE_WIN) {
            if (board.getPlayerToMove() == 1) {
                aiThinking = true;
                frame.setTitle("AI 1 Thinking .....");
                Thread t = new Thread(aiPlayer1);
                aiPlayer1.setBoard(board.clone());
                t.run();
            } else if (board.getPlayerToMove() == 2) {
                aiThinking = true;
                frame.setTitle("AI 2 Thinking .....");
                Thread t = new Thread(aiPlayer2);
                aiPlayer2.setBoard(board.clone());
                t.run();
            }
        }
    }

    @Override
    public void keyTyped(KeyEvent keyEvent) {

    }

    @Override
    public void keyPressed(KeyEvent keyEvent) {
        if (keyEvent.getKeyCode() == KeyEvent.VK_SPACE) {
            if (!aiThinking)
                aiMove();
        }
    }

    @Override
    public void keyReleased(KeyEvent keyEvent) {

    }
}
