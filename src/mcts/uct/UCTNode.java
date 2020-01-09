package mcts.uct;

import framework.IBoard;
import framework.MoveList;
import framework.Options;
import framework.util.FastLog;
import mcts.State;
import mcts.TransposTable;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class UCTNode {
    private final Options options;
    private final TransposTable tt;
    private final long hash;
    public final int player;
    public final int[] move;
    private boolean expanded = false, simulated = false;
    private List<UCTNode> children;

    // For debug only
    public String boardString;
    public ArrayList<Double> timeSeries;

    public State state;

    /**
     * Constructor for the root
     */
    public UCTNode(int player, Options options, long hash, TransposTable tt) {
        this.player = player;
        this.options = options;
        this.tt = tt;
        this.hash = hash;
        this.state = tt.getState(hash, true);
        this.move = null;
    }

    /**
     * Constructor for internal node
     */
    public UCTNode(int player, int[] move, Options options, long hash, TransposTable tt) {
        this.player = player;
        this.move = move;
        this.options = options;
        this.tt = tt;
        this.hash = hash;
        this.state = tt.getState(hash, true);
        if (options.debug)
            timeSeries = new ArrayList<>();
    }

    /**
     * Run the MCTS algorithm on the given node.
     *
     * @param board The current board
     * @return the currently evaluated playout value of the node
     */
    public double[] MCTS(IBoard board, int depth) {
        assert board.hash() == hash : "Board hash is incorrect";
        assert board.getPlayerToMove() == player : "Incorrect player to move";
        // TODO Build in some more assertions here
        // TODO MAST for Atarigo
        UCTNode child = null;
        // First add some leafs if required
        if (!expanded)
            child = expand(board); // Expand returns any node that leads to a win

        // Select the best child, if we didn't find a winning position in the expansion
        if (child == null)
            if (isTerminal())
                child = this;
            else
                child = select();

        double[] result = {Integer.MIN_VALUE, Integer.MIN_VALUE};
        // (Solver) Check for proven win / loss / draw
        if (!child.isSolved()) {
            // Execute the move represented by the child
            if (!isTerminal()) {
                board.doMove(child.move);
                //
                if (options.RAVE) {
                    options.insertRAVE(board.getMoveId(child.move), player);
                }
            }
            // When a leaf is reached return the result of the playout
            if (!child.simulated || isTerminal()) {
                result = child.playOut(board); // WARN a single copy of the board is used
                child.updateStats(result);
                child.simulated = true;
            } else {
                result = child.MCTS(board, depth + 1);
                // Update the RAVE value if this move was played
                if (!child.isSolved() && options.RAVE) {
                    for (UCTNode c : children) {
                        if (c.equals(child)) // No need to update the selected child, only siblings
                            continue;

                        if (options.getRAVE(board.getMoveId(c.move), player))
                            c.updateRAVE(result);
                    }
                }
            }
            //
            //if (!child.isSolved())
                updateStats(result);
            // For displaying the time-series charts
            if (options.debug && depth == 0)
                child.timeSeries.add(child.getValue(player));
        }

        // This segment solves the infrequent problem of transpositions getting solved in other parts of the tree
        UCTNode solvedChild = (child.isSolved()) ? child : null;
        if (solvedChild == null) {
            for (UCTNode c : children) {
                if (c.isSolved()) {
                    if (solvedChild == null)
                        solvedChild = c;
                    else if (c.getValue(player) == State.INF) // Take a winning node if possible
                        solvedChild = c;
                }
            }
        }

        if (solvedChild != null && solvedChild.getValue(player) == State.INF) {
            // One of my children is a proven win
            setSolved(player);
            // Backprop a win
            result = new double[2];
            result[player - 1] = 1;
            result[(3 - player) - 1] = 0;
            return result;
        } else if (solvedChild != null && solvedChild.getValue(player) == -State.INF) {
            result = new double[2];
            result[player - 1] = 0;
            result[(3 - player) - 1] = 1;
            // Check if all children are a proven loss
            for (UCTNode c : children) {
                if (c.getValue(player) != -State.INF) {
                    // Return a single loss, if not all children are a loss
                    updateStats(result);
                    return result;
                }
            }
            setSolved(3 - player); // I'm a proven win for the parent
            return result;
        }
        assert (result[0] > Integer.MIN_VALUE) && (result[1] > Integer.MIN_VALUE) : "Result not initialized";

        return result;
    }

    private UCTNode expand(IBoard board) {
        // If one of the nodes is a win, we don't have to select
        UCTNode winNode = null;
        MoveList moves = board.getExpandMoves();
        if (children == null)
            children = new LinkedList<>();

        int winner = board.checkWin();
        assert winner == IBoard.NONE_WIN || winner == IBoard.DRAW : "Trying to expand a proven node";
        // Board is terminal, don't expand
        if (winner != IBoard.NONE_WIN)
            return null;

        int[] move;
        double[] best_imVal = {-State.INF, -State.INF};
        // Add all moves as children to the current node
        for (int i = 0; i < moves.size(); i++) {
            move = moves.get(i);

            IBoard tempBoard = board.clone();
            tempBoard.doMove(move);
            UCTNode child = new UCTNode(3 - player, move, options, tempBoard.hash(), tt);

            if (Options.debug)
                child.boardString = tempBoard.toString();

            // We've expanded an already proven won node
            if (child.isSolved() && child.getValue(player) == State.INF)
                winNode = child;
            else {
                // Check for a winner, (Solver)
                winner = tempBoard.checkWin();
                if (winner == IBoard.P1 || winner == IBoard.P2) {
                    if (winner == player)
                        winNode = child;

                    child.setSolved(winner);
                }
            }

            // implicit minimax
            if (options.imm) {
                double[] imVal = new double[2];
                if (child.state == null) {
                    imVal[player - 1] = tempBoard.evaluate(player);
                    imVal[(3 - player) - 1] = -imVal[player - 1];
                    child.setImValue(imVal);
                } else // IM Value was already determined elsewhere in the tree
                    imVal = child.getImValue();

                if (imVal[player-1] > best_imVal[player - 1])
                    best_imVal = imVal;
            }
            children.add(child);
        }
        expanded = true;
        if (options.imm) {
            setImValue(best_imVal);
        }
        // If one of the nodes is a win, return it.
        return winNode;
    }

    private UCTNode select() {
        UCTNode selected = null;
        double max = Double.NEGATIVE_INFINITY;
        double maxIm = -State.INF, minIm = State.INF;
        // Use UCT down the tree
        double uctValue, np = getVisits();

        if (options.imm) {
            double val;
            for (UCTNode c : children) {
                val = c.getImValue()[player-1];
                if (val > maxIm)
                    maxIm = val;
                if (val < minIm)
                    minIm = val;
            }
        }
        // Select a child according to the UCT Selection policy
        for (UCTNode c : children) {
            double nc = c.getVisits();
            // Always select a proven win
            if (c.getValue(player) == State.INF)
                uctValue = State.INF + Options.r.nextDouble();
            else if (c.getValue(player) == -State.INF)
                uctValue = -State.INF - Options.r.nextDouble();
            else if (c.getVisits() == 0)
                // First, visit all children at least once
                uctValue = 100. + Options.r.nextDouble();
            else {
                double avgValue = c.getValue(player);

                // Linear regression
                if (options.regression && c.state != null) {
                    double regVal = c.state.getRegressionValue(options.regForecastSteps, player);
                    if (regVal > Integer.MIN_VALUE) // This value is returned if there weren't enough visits to predict
                        avgValue = (1. - options.regAlpha) * avgValue + (options.regAlpha * regVal);
                }

                // Implicit minimax
                if (options.imm && minIm != maxIm) {
                    double imVal = (c.getImValue()[player-1] - minIm) / (maxIm - minIm);
                    avgValue = (1. - options.imAlpha) * avgValue + (options.imAlpha * imVal);
                }

                if (options.RAVE && c.getVisits() > 0) {
                    double beta = Math.sqrt(options.k / ((3 * c.getVisits()) + options.k));
                    avgValue = beta * c.getRAVE(player) + ((1 - beta) * avgValue);
                }

                // Compute the uct value with the (new) average value
                uctValue = avgValue + options.c * Math.sqrt(FastLog.log(np) / nc); // tie breaker
            }

            // Remember the highest UCT value
            if (uctValue > max) {
                selected = c;
                max = uctValue;
            }
        }
        return selected;
    }

    private double[] playOut(IBoard board) {
        int winner = board.checkWin(), nMoves = 0;
        assert winner == IBoard.NONE_WIN || winner == IBoard.DRAW : "Board in terminal position in playout.";
        int[] move;
        boolean interrupted = false;
        MoveList moves;
        do {
            moves = board.getPlayoutMoves(options.heuristics);
            // No more moves to be made
            if (moves.size() == 0)
                break;
            move = moves.get(Options.r.nextInt(moves.size()));

            if (options.RAVE) // Insert the rave move before the play is made to get the correct player to move
                options.insertRAVE(board.getMoveId(move), board.getPlayerToMove());

            board.doMove(move);
            winner = board.checkWin();
            nMoves++;
            // Interrupt playout for early evaluation
            if (winner == IBoard.NONE_WIN && options.earlyTerm && nMoves == options.termDepth)
                interrupted = true;

        } while (winner == IBoard.NONE_WIN && !interrupted);

        double[] score = {0, 0};

        if (options.earlyTerm) {
            if (!interrupted) {
                score[winner - 1] += options.etWv;
            } else {
                double eval = board.evaluate(1); // TODO - Is it better to include the value of the evaluation here?
                if (eval > options.etT)
                    score[0]++;
                else if (eval < -options.etT)
                    score[1]++;
            }
        } else if (winner != IBoard.DRAW)
            score[winner - 1] = 1;

        return score;
    }

    public UCTNode getBestChild() {
        if (children == null)
            return null;
        double max = Double.NEGATIVE_INFINITY, value;
        UCTNode bestChild = null;
        for (UCTNode t : children) {
            // If there are children with INF value, choose one of them
            if (t.getValue(player) == State.INF)
                value = State.INF + Options.r.nextDouble();
            else if (t.getValue(player) == -State.INF)
                value = -State.INF + t.getVisits() + Options.r.nextDouble();
            else {
                value = t.getVisits();
            }
            if (value > max) {
                max = value;
                bestChild = t;
            }
        }
        return bestChild;
    }

    private void updateStats(double[] value) {
        if (state == null)
            state = tt.getState(hash, false);
        //
        state.updateStats(value, options.regression);
        //
        // implicit minimax backups
        if (options.imm && children != null) {
            double[] bestVal = {Integer.MIN_VALUE, Integer.MIN_VALUE};

            // TODO Check if this should minimize or maximize
            for (UCTNode c : children) {
                if (c.getImValue()[player-1] > bestVal[player-1]) {
                    bestVal = c.getImValue();
                }
            }
            setImValue(bestVal);
        }
    }

    private void updateRAVE(double[] values) {
        if (state == null)
            state = tt.getState(hash, false);

        state.updateRAVE(values);
    }

    private void setSolved(int player) {
        if (state == null)
            state = tt.getState(hash, false);

        state.setSolved(player);
    }

    public boolean isSolved() {
        if (state == null)
            state = tt.getState(hash, true);
        if (state == null)
            return false;

        return state.isSolved();
    }

    private void setImValue(double[] imValue) {
        if (state == null)
            state = tt.getState(hash, false);

        state.setImValue(imValue);
    }

    private double[] getImValue() {
        if (state == null)
            state = tt.getState(hash, true);
        if (state == null)
            return new double[]{-State.INF, -State.INF};

        return state.getImValue();
    }

    /**
     * @return The value of this node with respect to the parent
     */
    public double getValue(int player) {
        if (state == null)
            state = tt.getState(hash, true);
        if (state == null)
            return 0;

        return state.getMean(player);
    }

    public double getRAVE(int player) {
        if (state == null)
            state = tt.getState(hash, true);
        if (state == null)
            return 0;
        return state.getRAVE(player);
    }

    /**
     * @return The number of visits of the transposition
     */
    private double getVisits() {
        if (state == null)
            state = tt.getState(hash, true);

        if (state == null)
            return 0.;

        return state.getVisits();
    }

    public boolean isTerminal() {
        return children != null && children.size() == 0;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(move[0]).append(".").append(move[1]).append(" ");
        if (state != null)
            sb.append(state.toString());

        return sb.toString();
    }

    public String toString(IBoard board) {
        if (state != null)
            return board.getMoveString(move) + " ::  " + state.toString();
        else
            return board.getMoveString(move) + " no state";
    }
}
