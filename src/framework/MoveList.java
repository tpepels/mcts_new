package framework;

public class MoveList {
    private int[][] moves; // TODO Change this so more complex movetypes are possible
    private int size;

    public MoveList(int maxSize) {
        moves = new int[maxSize][2];
        size = 0;
    }

    public MoveList(int maxSize, int maxParts) {
        moves = new int[maxSize][maxParts];
        size = 0;
    }

    public void add(int... move) {
        assert moves.length > size() : "Increasing movelist size";
        if(moves.length <= size()) {
            int[][] movesNew = new int[size * 2][];
            for(int i = 0; i< moves.length; i++) {
                movesNew[i] = new int[moves[i].length];
                for(int j = 0; j < move.length; j++) {
                    movesNew[i][j] = moves[i][j];
                }
            }
            moves = movesNew;
        }
        if(moves[size] == null)
            moves[size] = new int[move.length];

        for(int i = 0; i< move.length; i++) {
            moves[size][i] = move[i];
        }
        size++;
    }

    public int[] get(int index) {
        return moves[index];
    }

    public int size() {
        return size;
    }

    public boolean isEmpty() {
        return size == 0;
    }

    public void clear() {
        size = 0;
    }

    public int[][] getArray() {
        return moves;
    }
}
