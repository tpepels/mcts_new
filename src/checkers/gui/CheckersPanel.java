package checkers.gui;

import checkers.game.Board;
import framework.AIPlayer;
import framework.MoveCallback;
import framework.MoveList;
import framework.Options;
import framework.gui.GuiOptions;
import mcts.uct.UCTPlayer;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class CheckersPanel extends JPanel implements MouseListener, MouseMotionListener, MoveCallback, KeyListener {
    private static final long serialVersionUID = 1L;
    private final JFrame frame;
    private MoveList moves;
    private int squareSize = 40, boardX = -1, boardY = -1, clickNum = 0, winner = 0;
    private int[] clickPos = {-1, -1, -1};
    //
    private Board board;
    private AIPlayer aiPlayer1, aiPlayer2;
    private boolean aiThinking = false;

    public CheckersPanel(int squareSize, JFrame frame) {
        this.squareSize = squareSize;
        this.frame = frame;
        board = new Board();
        board.initialize();
        //
        if (board.getPlayerToMove() == Board.P2) {
            frame.setTitle("Checkers - Black's move.");
        } else {
            frame.setTitle("Checkers - White's move.");
        }
        //
        this.addMouseListener(this);
        this.addMouseMotionListener(this);
        // Moves need to be generated
        moves = board.getExpandMoves();
        Options.debug = true;
        // Definition for player 1
        aiPlayer1 = new UCTPlayer();
        Options options1 = new Options();
        options1.fixedSimulations = true;
        options1.nSimulations = 10000;
        aiPlayer1.setOptions(options1);
        // Definition for player 2
        aiPlayer2 = new UCTPlayer();
        Options options2 = new Options();
        options2.fixedSimulations = true;
        options2.nSimulations = 10000;
        aiPlayer2.setOptions(options2);
        //
        aiMove();
    }

    public void paint(Graphics g) {
        super.paint(g);
        int row, col, x, y, boardPos;
        Graphics2D g2d = (Graphics2D) g;

        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int NUM_SQUARES = 8;
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
                } else if (col == boardX && row == boardY) {
                    g.setColor(GuiOptions.S_Square_Color);
                } else if ((row % 2) == (col % 2)) {
                    g.setColor(GuiOptions.D_Square_Color);
                } else {
                    g.setColor(GuiOptions.L_Square_Color);
                }

                if (clickNum > 0) {
                    if (isAvailMove(clickPos[0] / 8, clickPos[0] % 8, row, col)) {
                        g.setColor(g.getColor().brighter());
                    }
                }

                g.fillRect(x, y, squareSize, squareSize);
                g.setColor(GuiOptions.P1_Color);
                g.drawString(Integer.toString(boardPos), x + 1, y + 11);
                int boardPiece = board.board[row][col];
                if (boardPiece != Board.EMPTY) {
                    if (boardPiece == Board.W_PIECE) {
                        g.setColor(GuiOptions.P1_Color);
                        g.fillOval(x + 5, y + 5, squareSize - 10, squareSize - 10);
                    } else if (boardPiece == Board.B_PIECE) {
                        g.setColor(GuiOptions.P2_Color);
                        g.fillOval(x + 5, y + 5, squareSize - 10, squareSize - 10);
                    } else if (boardPiece == Board.W_KING) {
                        g.setColor(GuiOptions.P1_Color);
                        g.fillOval(x + 5, y + 5, squareSize - 10, squareSize - 10);
                        g.setColor(GuiOptions.P2_Color);
                        g.drawString("K", x + 25, y + 25);
                    } else if (boardPiece == Board.B_KING) {
                        g.setColor(GuiOptions.P2_Color);
                        g.fillOval(x + 5, y + 5, squareSize - 10, squareSize - 10);
                        g.setColor(GuiOptions.P1_Color);
                        g.drawString("K", x + 25, y + 25);
                    }
                }
            }

        }
    }

    public boolean isAvailMove(int frow, int fcol, int row, int col) {
        for (int i = 0; i < moves.size(); i++) {
            if (moves.get(i)[0] == fcol &&
                    moves.get(i)[1] == frow &&
                    moves.get(i)[2] == col &&
                    moves.get(i)[3] == row) {
                // System.out.println("from : " + fcol + " " + frow + " to: " + col + " " + row);
                return true;
            }
        }
        return false;
    }

    public int[] getMove(int frow, int fcol, int row, int col) {
        for (int i = 0; i < moves.size(); i++) {
            if (moves.get(i)[0] == fcol &&
                    moves.get(i)[1] == frow &&
                    moves.get(i)[2] == col &&
                    moves.get(i)[3] == row) {
                // System.out.println("from : " + fcol + " " + frow + " to: " + col + " " + row);
                return moves.get(i);
            }
        }
        return null;
    }

    @Override
    public void mouseDragged(MouseEvent arg0) {

    }

    @Override
    public void mouseMoved(MouseEvent arg0) {
        boardX = arg0.getX() / squareSize;
        boardY = arg0.getY() / squareSize;
        repaint();
    }

    @Override
    public void mouseClicked(MouseEvent arg0) {
        int winner = board.checkWin();
        //
        if (winner == Board.P2_WIN) {
            frame.setTitle("Checkers - Black wins");
            System.out.println("P2 wins");
            board = new Board();
            board.initialize();
        } else if (winner == Board.P1_WIN) {
            frame.setTitle("Checkers - White wins.");
            System.out.println("P1 wins");
            board = new Board();
            board.initialize();
        }
        //
        int boardPos = boardY * 8 + boardX;
        if (clickNum == 0) {
            if (board.board[boardY][boardX] != board.getPlayerToMove() && board.board[boardY][boardX] / 10 != board.getPlayerToMove()) {
                return;
            }
        } else if (clickNum == 1) {
            if (board.board[boardY][boardX] != Board.EMPTY || !isAvailMove(clickPos[0] / 8, clickPos[0] % 8, boardY, boardX)) {
                clickNum--;
                return;
            }
        }
        //
        clickPos[clickNum] = boardPos;
        clickNum++;
        if (clickNum == 2) {
            int[] move = {clickPos[0] / 8, clickPos[0] % 8, clickPos[1] / 8, clickPos[1] % 8};
            board.doMove(move);
            //
            if (board.getPlayerToMove() == Board.P2) {
                frame.setTitle("Checkers - Black's move.");
            } else {
                frame.setTitle("Checkers - White's move.");
            }
            moves = board.getExpandMoves();
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
        boardX = -1;
        boardY = -1;
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
            frame.setTitle("Checkers - Black wins");
            return;
        } else if (winner == Board.P1_WIN) {
            frame.setTitle("Checkers - White wins.");
            return;
        } else if (winner == Board.DRAW) {
            frame.setTitle("Checkers - Draw!");
            return;
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
