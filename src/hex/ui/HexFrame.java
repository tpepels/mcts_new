package hex.ui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import javax.swing.JFrame;
import javax.swing.Timer;

import framework.AIPlayer;
import framework.MoveCallback;
import framework.PlayerFactory;
import hex.game.Board;

public class HexFrame extends JFrame implements UserInputListener, MoveCallback, KeyListener {
    private static final long serialVersionUID = 1L;
    public static HexComponent hexPanel;
    int winner = Board.NONE_WIN;
    private Board board;
    private AIPlayer aiPlayer1, aiPlayer2;
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
        board = new Board(9);
        board.initialize();
        hexPanel.drawField(board.board);

        aiPlayer1 = PlayerFactory.getPlayer(1, "hex");
        aiPlayer2 = PlayerFactory.getPlayer(2, "hex");

        aiPlayer1.setMoveCallback(this);
        aiPlayer2.setMoveCallback(this);

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
            long startT = System.currentTimeMillis();
            String eval = "e1: "+ board.evaluate(1) + " e2 " + board.evaluate(2);
            System.out.println((System.currentTimeMillis() - startT) / 1000 + "ms.");
            setTitle(player + " to move " + eval);
        }
        System.gc();
        aiThinking = false;
        System.out.println(" e1: " + board.evaluate(1) + " e2: " + board.evaluate(2));
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
