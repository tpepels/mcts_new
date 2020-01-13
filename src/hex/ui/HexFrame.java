package hex.ui;

import framework.AIPlayer;
import framework.MoveCallback;
import framework.Options;
import hex.game.Board;
import mcts.uct.UCTPlayer;

import javax.swing.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

public class HexFrame extends JFrame implements UserInputListener, MoveCallback, KeyListener {
    private static final long serialVersionUID = 1L;
    public static HexComponent hexPanel;
    int winner = Board.NONE_WIN;
    private Board board;
    private AIPlayer aiPlayer1, aiPlayer2;
    private Options options1, options2;
    private boolean aiThinking = false;

    public HexFrame() {
        setSize(600, 400);
        //setResizable(false);
        setVisible(true);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        hexPanel = new HexComponent();
        hexPanel.startListening(this, true);
        addKeyListener(this);
        setContentPane(hexPanel);
        board = new Board(11);
        board.initialize();
        Options.debug = true;
        hexPanel.drawField(board.board);

        options1 = new Options();
        aiPlayer1 = new UCTPlayer();
        aiPlayer1.setOptions(options1);
        options1.fixedSimulations = true;
        options1.nSimulations = 50000;
        options1.RAVE = true;
        options1.MAST = true;

        options2 = new Options();
        aiPlayer2 = new UCTPlayer();
        aiPlayer2.setOptions(options2);
        options2.fixedSimulations = true;
        options2.nSimulations = 50000;

        aiPlayer1.setMoveCallback(this);
        aiPlayer2.setMoveCallback(this);

        aiMove();
    }

    public static void main(String[] args) {
        (new HexFrame()).setVisible(true);
    }

    private void aiMove() {
        if (winner == Board.NONE_WIN) {
            if (aiPlayer1 != null && board.getPlayerToMove() == 1) {
                aiThinking = true;
                setTitle("AI 1 Thinking .....");
                Thread t = new Thread(aiPlayer1);
                aiPlayer1.setBoard(board.clone());
                t.run();
            } else if (aiPlayer2 != null && board.getPlayerToMove() == 2) {
                aiThinking = true;
                setTitle("AI 2 Thinking .....");
                Thread t = new Thread(aiPlayer2);
                aiPlayer2.setBoard(board.clone());
                t.run();
            }
        }
    }

    @Override
    public void hexSelected(int x, int y) {
        if (board.board[x][y] == 0) {
            board.doMove(new int[]{x, y});
            int winner = board.checkWin();
            if (winner != Board.NONE_WIN) {
                String winStr = "player " + winner;
                this.setTitle(winStr + " won");
            }
        }
    }

    @Override
    public void makeMove(int[] move) {
        board.doMove(move);
        winner = board.checkWin();
        if (winner != Board.NONE_WIN) {
            String winStr = "player " + winner;
            this.setTitle(winStr + " won");
        }
        repaint();
        if (winner != Board.NONE_WIN) {
            setTitle("Winner is " + board.checkWin());
        } else {
            String player = board.getPlayerToMove() == 1 ? "red" : "blue";
            setTitle(player + " to move");
        }
        aiThinking = false;
    }

    @Override
    public void keyPressed(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_SPACE) {
            if (!aiThinking)
                aiMove();
        }
    }

    @Override
    public void keyTyped(KeyEvent keyEvent) {

    }

    @Override
    public void keyReleased(KeyEvent keyEvent) {

    }
}
