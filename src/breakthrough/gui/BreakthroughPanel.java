package breakthrough.gui;

import breakthrough.game.Board;
import framework.AIPlayer;
import framework.MoveCallback;
import framework.MoveList;
import framework.Options;
import framework.gui.GuiOptions;
import mcts.uct.UCTPlayer;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

public class BreakthroughPanel extends JPanel implements MouseListener, MouseMotionListener, MoveCallback {
    private static final long serialVersionUID = 1L;
    private final JFrame frame;
    private MoveList moves;
    private int squareSize = 40, boardCol = -1, boardRow = -1, clickNum = 0;
    private int[] clickPos = {-1, -1, -1};
    //
    private Board board;
    private AIPlayer aiPlayer1, aiPlayer2;
    private int[] lastMove;

    public BreakthroughPanel(int squareSize, JFrame frame) {
        this.squareSize = squareSize;
        this.frame = frame;
        board = new Board();
        board.initialize();
        //
        if (board.getPlayerToMove() == Board.P2)
            frame.setTitle("Breaktrhough - Black's move.");
        else
            frame.setTitle("Breakthrough - White's move.");

        addMouseListener(this);
        addMouseMotionListener(this);

        moves = board.getExpandMoves();
        Options.debug = true;
        aiPlayer1 = new UCTPlayer();
        Options options1 = new Options();
        options1.fixedSimulations = true;
        options1.nSimulations = 100000;
        options1.heuristics = true;
        options1.RAVE = true;
        options1.imm = true;
        aiPlayer1.setOptions(options1);
        aiPlayer1.setMoveCallback(this);

        //
        if (aiPlayer1 != null)
            aiPlayer1.getMove(board.clone());
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
                } else if (col == boardCol && row == boardRow) {
                    g.setColor(GuiOptions.S_Square_Color);
                } else if ((row % 2) == (col % 2)) {
                    g.setColor(GuiOptions.L_Square_Color);
                } else {
                    g.setColor(GuiOptions.D_Square_Color);
                }

                if (clickNum > 0) {
                    if (isAvailMove(clickPos[0], row * 8 + col)) {
                        g.setColor(g.getColor().brighter());
                    }
                }

                g.fillRect(x, y, squareSize, squareSize);

                int boardPiece = board.board[row * 8 + col] / 100;
                if (boardPiece != 0) {
                    if (boardPiece == 1) {
                        g.setColor(GuiOptions.P1_Color);
                        g.fillOval(x + 5, y + 5, squareSize - 10, squareSize - 10);
                    } else if (boardPiece == 2) {
                        g.setColor(GuiOptions.P2_Color);
                        g.fillOval(x + 5, y + 5, squareSize - 10, squareSize - 10);
                    }
                }
            }

        }
    }

    public boolean isAvailMove(int from, int to) {
        for (int i = 0; i < moves.size(); i++) {
            if (moves.get(i)[0] == from && moves.get(i)[1] == to)
                return true;
        }
        return false;
    }

    public int[] getMove(int from, int to) {
        for (int i = 0; i < moves.size(); i++) {
            if (moves.get(i)[0] == from && moves.get(i)[1] == to)
                return moves.get(i);
        }
        return null;
    }

    @Override
    public void mouseDragged(MouseEvent arg0) {

    }

    @Override
    public void mouseMoved(MouseEvent arg0) {
        boardCol = arg0.getX() / squareSize;
        boardRow = arg0.getY() / squareSize;
        repaint();
    }

    @Override
    public void mouseClicked(MouseEvent arg0) {
        int winner = board.checkWin();
        //
        if (winner == Board.P2_WIN) {
            frame.setTitle("Breakthrough - Black wins");
            System.out.println("P2 wins");
            board = new Board();
            board.initialize();
        } else if (winner == Board.P1_WIN) {
            frame.setTitle("Breakthrough - White wins.");
            System.out.println("P1 wins");
            board = new Board();
            board.initialize();
        }

        int boardPos = boardRow * 8 + boardCol;
        System.out.println("clicked " + boardRow + " " + boardCol);
        int playerToMoveChar = board.getPlayerToMove() == 1 ? 1 : 2;
        int pos = board.board[boardRow * 8 + boardCol] / 100;

        if (clickNum == 0) {
            if (pos != playerToMoveChar)
                return;
        } else if (clickNum == 1) {
            if (pos == playerToMoveChar) {
                if (!isAvailMove(clickPos[0], boardRow * 8 + boardCol)) {
                    clickNum--;
                    return;
                }
            }
        }
        //
        clickPos[clickNum] = boardPos;
        clickNum++;
        if (clickNum == 2) {
            lastMove = getMove(clickPos[0], clickPos[1]);
            clickNum = 0;
            clickPos = new int[]{-1, -1, -1};
            makeMove(lastMove);
        }
    }

    @Override
    public void mouseEntered(MouseEvent arg0) {

    }

    @Override
    public void mouseExited(MouseEvent arg0) {
        boardCol = -1;
        boardRow = -1;
    }

    @Override
    public void mousePressed(MouseEvent arg0) {

    }

    @Override
    public void mouseReleased(MouseEvent arg0) {

    }

    public void makeMove(int[] move) {
        lastMove = move;
        board.doMove(move);
        clickNum = 0;
        clickPos = new int[]{-1, -1, -1};
        repaint();
        //
        int winner = board.checkWin();
        if (winner == Board.P2_WIN) {
            frame.setTitle("Breakthrough - Black wins");
            return;
        } else if (winner == Board.P1_WIN) {
            frame.setTitle("Breakthrough - White wins.");
            return;
        } else if (winner == Board.DRAW) {
            frame.setTitle("Breakthrough - Draw!");
            return;
        }
        moves = board.getExpandMoves();
        // Run the GC in between moves, to limit the runs during search
        System.gc();
        //
        if (board.getPlayerToMove() == Board.P2) {
            frame.setTitle("Breakthrough - Black's move");
            if (aiPlayer2 != null)
                aiPlayer2.getMove(board.clone());
            //aiPlayer2.getMove(board, this, Board.P2, true, lastMove);
        } else {
            frame.setTitle("Breakthrough - White's move");
            if (aiPlayer1 != null)
                aiPlayer1.getMove(board.clone());
            //aiPlayer1.getMove(board, this, Board.P1, true, lastMove);
        }
    }
}
