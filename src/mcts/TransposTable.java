package mcts;

public class TransposTable {
    private final int TT_SIZE = (int) Math.pow(2, 18);
    private final long MASK = TT_SIZE - 1;
    public int collisions = 0, positions = 0, recoveries = 0;
    //
    private State[] states;
    private int moveCounter = 0;

    public TransposTable() {
        this.states = new State[TT_SIZE];
    }

    public State getState(long hash, boolean existingOnly) {
        int hashPos = getHashPos(hash);
        State s = states[hashPos];
        if (s != null) {
            while (true) {
                if (s.hash == hash) {
                    recoveries++;
                    return s;
                }
                if (s.next == null)
                    break;
                s = s.next;
            }
            //
            if (existingOnly)
                return null;
            collisions++;
            positions++;
            // Transposition was not found, i.e. collision
            State newState = new State(hash);
            s.next = newState;
            return newState;
        } else if (!existingOnly) {
            positions++;
            // Transposition was not encountered before
            s = new State(hash);
            states[hashPos] = s;
            return s;
        } else {
            return null;
        }
    }

    public int pack(int offset) {
        recoveries = 0;
        collisions = 0;
        int prePositions = positions;
        State s, ps;
        for (int i = 0; i < TT_SIZE; i++) {
            s = states[i];
            if (s == null)
                continue;
            ps = null;
            // Check if the states were visited this round
            while (true) {
                if (s.visited && offset > 0) {
                    s.visited = false;
                    s.lastVisit = moveCounter;
                    ps = s;
                } else if (moveCounter - s.lastVisit >= offset) {
                    if (ps != null) {
                        ps.next = s.next;
                        positions--;
                        ps = s;
                    } else if (ps == null) {
                        positions--;
                        states[i] = s.next;
                        ps = null;
                    }
                }
                if (s.next == null)
                    break;
                s = s.next;
            }
        }
        moveCounter++;
        return (prePositions - positions);
    }

    private int count() {
        State s;
        int count = 0;
        for (int i = 0; i < TT_SIZE; i++) {
            s = states[i];
            if (s == null)
                continue;
            // Check if the states were visited this round
            while (true) {
                count++;
                if (s.next == null)
                    break;
                s = s.next;
            }
        }
        return count;
    }

    private int getHashPos(long hash) {
        return (int) (hash & MASK);
    }
}
