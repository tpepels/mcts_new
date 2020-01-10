package hex.ui;

import hex.game.Board;

import javax.swing.*;

public class HexFrame extends JFrame implements UserInputListener {
    private static final long serialVersionUID = 1L;
    public static HexComponent hexPanel;

    private Board board;

    public HexFrame() {
        setSize(600, 400);
        //setResizable(false);
        setVisible(true);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        hexPanel = new HexComponent();
        hexPanel.startListening(this, true);
        setContentPane(hexPanel);
        board = new Board(5);
        board.initialize();
        hexPanel.drawField(board.board);
    }

    public static void main(String[] args) {
        (new HexFrame()).setVisible(true);
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
}
