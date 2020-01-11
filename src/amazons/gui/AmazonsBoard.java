package amazons.gui;


import amazons.game.Board;
import framework.AIPlayer;
import framework.MoveCallback;
import framework.Options;
import mcts.uct.UCTPlayer;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.IOException;

public class AmazonsBoard extends JPanel implements MouseListener, MouseMotionListener, MoveCallback, KeyListener {
    private static final long serialVersionUID = 1L;
    private final JFrame frame;
    private final int[] moves = new int[40];
    private int squareSize = 40, boardx = -1, boardy = -1, clickNum = 0, movec = 0, winner;
    private int[] clickPos = {-1, -1, -1};
    //
    private Board board;
    private AIPlayer aiPlayer1, aiPlayer2;
    private Image whiteQueen, blackQueen;
    private Color arrowColor = Color.decode("#D50400");
    private boolean aiThinking = false;

    public AmazonsBoard(int squareSize, JFrame frame) {
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
        // Definition for player 1
        aiPlayer1 = new UCTPlayer();
        Options options1 = new Options();
        options1.fixedSimulations = true;
        options1.nSimulations = 100000;
        aiPlayer1.setOptions(options1);

        // Definition for player 2
        aiPlayer2 = new UCTPlayer();
        Options options2 = new Options();
        options2.fixedSimulations = true;
        options2.nSimulations = 100000;
        aiPlayer2.setOptions(options2);
        //
        aiPlayer1.getMove(board.clone());
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
                    g.setColor(Color.decode("#FFF482"));
                } else if (boardPos == clickPos[1]) {
                    g.setColor(Color.decode("#BDFF60"));
                } else if (boardPos == clickPos[2]) {
                    g.setColor(Color.decode("#FF4762"));
                } else if (col == boardx && row == boardy) {
                    g.setColor(Color.GRAY);
                } else if ((row % 2) == (col % 2)) {
                    g.setColor(Color.darkGray);
                } else {
                    g.setColor(Color.lightGray);
                }

                if (movec > 0) {
                    if (isAvailMove(boardPos)) {
                        g.setColor(g.getColor().brighter());
                    }
                }

                g.fillRect(x, y, squareSize, squareSize);
                g.setColor(Color.white);
                ((Graphics2D) g).drawString(Integer.toString(boardPos), x + 1, y + 11);
                int boardPiece = board.board[boardPos];
                if (boardPiece != Board.EMPTY) {
                    if ((boardPiece / 10) == Board.WHITE_Q) {
                        g.drawImage(whiteQueen, x + 3, y + 3, squareSize - 6, squareSize - 6, null);
                    } else if ((boardPiece / 10) == Board.BLACK_Q) {
                        g.drawImage(blackQueen, x + 3, y + 3, squareSize - 6, squareSize - 6, null);
                    } else if (boardPiece == Board.ARROW) {
                        g.setColor(arrowColor);
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
    public void mouseDragged(MouseEvent arg0) {

    }

    @Override
    public void mouseMoved(MouseEvent arg0) {
        boardx = arg0.getX() / squareSize;
        boardy = arg0.getY() / squareSize;
        repaint();
    }

    @Override
    public void mouseClicked(MouseEvent arg0) {
        int winner = board.checkWin();
        //
        if (winner == Board.P2_WIN) {
            frame.setTitle("Amazons - Black wins");
            System.out.println("P2 wins");
        } else if (winner == Board.P1_WIN) {
            frame.setTitle("Amazons - White wins.");
            System.out.println("P1 wins");
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
        if(keyEvent.getKeyCode() == KeyEvent.VK_SPACE) {
            if(!aiThinking)
                aiMove();
        }
    }

    @Override
    public void keyReleased(KeyEvent keyEvent) {

    }
}
