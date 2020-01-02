package framework;

public class MoveList {
    private int[] movesFrom, movesTo;
    private int size;

    public MoveList(int maxSize) {
        movesFrom = new int[maxSize];
        movesTo = new int[maxSize];
        size = 0;
    }

    public void add(int from, int to) {
        if(movesTo.length <= size()) {
            int[] movesToNew = new int[size * 2];
            int[] movesFromNew = new int[size * 2];
            System.arraycopy(movesFrom, 0, movesFromNew, 0, movesFrom.length);
            System.arraycopy(movesTo, 0, movesToNew, 0, movesTo.length);
            movesFrom = movesFromNew;
            movesTo = movesToNew;
        }
        movesFrom[size] = from;
        movesTo[size++] = to;
    }

    public int[] get(int index) {
        return new int[] {movesFrom[index], movesTo[index]};
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
}
