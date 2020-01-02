package mcts.uct;

import framework.IBoard;
import framework.MoveList;
import framework.Options;
import framework.util.FastLog;
import mcts.State;
import mcts.TransposTable;

import java.text.DecimalFormat;
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
    public ArrayList<Double> timeSeries;

    public State state;

    /**
     * Constructor for the root
     */
    public UCTNode(int player, Options options, IBoard board, TransposTable tt) {
        this.player = player;
        this.options = options;
        this.tt = tt;
        this.hash = board.hash();
        this.state = tt.getState(hash, true);
        this.move = null;
    }

    /**
     * Constructor for internal node
     */
    public UCTNode(int player, int[] move, Options options, IBoard board, TransposTable tt) {
        this.player = player;
        this.move = move;
        this.options = options;
        this.tt = tt;
        this.hash = board.hash();
        this.state = tt.getState(hash, true);
        if(options.debug)
            timeSeries = new ArrayList<>();
    }

    /**
     * Run the MCTS algorithm on the given node.
     *
     * @param board The current board
     * @return the currently evaluated playout value of the node
     */
    public double[] MCTS(IBoard board, int depth) {
        if (board.hash() != hash)
            throw new RuntimeException("Incorrect hash");

        UCTNode child = null;
        // First add some leafs if required
        if (!expanded) {
            // Expand returns any node that leads to a win
            child = expand(board);
        }
        // Select the best child, if we didn't find a winning position in the expansion
        if (child == null)
            if (isTerminal())
                child = this;
            else
                child = select();

        double[] result;
        // (Solver) Check for proven win / loss / draw
        if (Math.abs(child.getValue(player)) != State.INF) {
            // Execute the move represented by the child
            board.doMove(child.move);
            // When a leaf is reached return the result of the playout
            if (!child.simulated) {
                child.updateStats(child.playOut(board));
                child.simulated = true;
            } else {
                result = child.MCTS(board, depth + 1);
            }
        }

        // (Solver) If one of the children is a win, then I'm a win
        if (child.getValue(player) == State.INF) {
            // If I have a win, my parent has a loss.
            setSolved(3 - player);
            result = new double[2];
            result[player] = 1; // Backprop a win for the current player
            // TODO ====== Hier was je gebleven
        } else if (result == -State.INF && expanded) {
            // (Solver) Check if all children are a loss
            for (UCTNode tn : children) {
                // Are all children a loss?
                if (tn.getValue() != result) {
                    // Return a single loss, if not all children are a loss
                    updateStats(1);
                    return -1;
                }
            }
            setSolved(true);
            return result; // always return in view of me
        }
        if (Math.abs(getValue()) != State.INF)
            // Update the results for the current node
            updateStats(result);
        else
            // Sometimes the node becomes solved deeper in the tree
            return getValue();

        if(options.debug && depth == 0)
            child.timeSeries.add(child.getValue());
        // Back-propagate the result always return in view of me
        return result;
    }

    private UCTNode expand(IBoard board) {
        int nextPlayer = (3 - board.getPlayerToMove());
        // If one of the nodes is a win, we don't have to select
        UCTNode winNode = null;
        MoveList moves = board.getExpandMoves(null);
        if (children == null)
            children = new LinkedList<>();
        int winner = board.checkWin();
        // Board is terminal, don't expand
        if (winner != IBoard.NONE_WIN)
            return null;
        int best_imVal = getImValue();
        int[] move;
        // Add all moves as children to the current node
        for (int i = 0; i < moves.size(); i++) {
            move = moves.get(i);
           IBoard tempBoard = board.clone();
            tempBoard.doMove(move);
            UCTNode child = new UCTNode(nextPlayer, move, options, tempBoard, tt);

            if (Math.abs(child.getValue()) != State.INF) {
                // Check for a winner, (Solver)
                winner = tempBoard.checkWin();
                if (winner == player) {
                    winNode = child;
                    child.setSolved(true);
                } else if (winner == nextPlayer) {
                    child.setSolved(false);
                }
            }
            // implicit minimax
            if (options.imm) {
                int imVal = tempBoard.evaluate(player);
                child.setImValue(imVal); // view of parent
                if (imVal > best_imVal)
                    best_imVal = imVal;
            }
            children.add(child);
        }
        expanded = true;
        if (options.imm)
            this.setImValue(-best_imVal);

        // If one of the nodes is a win, return it.
        return winNode;
    }

    private UCTNode select() {
        UCTNode selected = null;
        double max = Double.NEGATIVE_INFINITY;
        int maxIm = Integer.MIN_VALUE, minIm = Integer.MAX_VALUE;
        // Use UCT down the tree
        double uctValue, np = getVisits();

        if (options.imm) {
            int val;
            for (UCTNode c : children) {
                val = c.getImValue();
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
            if (c.getValue() == State.INF)
                uctValue = State.INF + Options.r.nextDouble();
            else if (c.getVisits() == 0 && c.getValue() != -State.INF) {
                // First, visit all children at least once
                uctValue = 100. + Options.r.nextDouble();
            } else if (c.getValue() == -State.INF) {
                uctValue = -State.INF + Options.r.nextDouble();
            } else {
                double avgValue = c.getValue();
                // Linear regression TODO Check if player value is correct!
                if(options.regression && c.getVisits() > 5) {
                    double regVal = c.getState().getRegressionValue(options.regForecastSteps, player);
                    if(!Double.isNaN(regVal))
                        avgValue = (1. - options.regAlpha) * avgValue +  options.regAlpha * regVal;
                }
                // Implicit minimax
                if (options.imm && minIm != maxIm) {
                    double imVal = (c.getImValue() - minIm) / (double) (maxIm - minIm);
                    avgValue = (1. - options.imAlpha) * avgValue + (options.imAlpha * imVal);
                }
                // Compute the uct value with the (new) average value
                uctValue = avgValue + options.c * Math.sqrt(FastLog.log(np) / nc)
                        + (Options.r.nextDouble() * 0.00001);
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
        int[] move;
        boolean interrupted = false;
        MoveList moves;
        while (winner == IBoard.NONE_WIN && !interrupted) {
            moves = board.getPlayoutMoves(options.heuristics);
            move = moves.get(Options.r.nextInt(moves.size()));
            board.doMove(move);
            winner = board.checkWin();
            nMoves++;
            if (winner == IBoard.NONE_WIN && options.earlyTerm && nMoves == options.termDepth)
                interrupted = true;
        }

        double[] score = {0,0};

        if (!interrupted) {
            score[winner] += options.etWv;
        } else {
            double eval = board.evaluate(0); // TODO - Is it better to include the value of the evaluation here?
            if (eval > options.etT)
                score[0]++;
            else if (eval < -options.etT)
                score[1]++;
        }
        return score;
    }

    public UCTNode getBestChild(boolean print) {
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
            if (print)
                System.out.println(t);
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
            int bestVal = Integer.MIN_VALUE;
            for (UCTNode c : children) {
                if (c.getImValue() > bestVal) bestVal = c.getImValue();
            }
            setImValue(-bestVal); // view of parent
        }
    }

    private void setSolved(int player) {
        if (state == null)
            state = tt.getState(hash, false);

        state.setSolved(player);
    }

    private boolean isSolved() {
        if (state == null)
            state = tt.getState(hash, false);

        return state.isSolved();
    }

    private void setImValue(int imValue) {
        if (state == null)
            state = tt.getState(hash, false);

        if (state.imValue == Integer.MIN_VALUE)
            state.setImValue(imValue);
    }

    private int getImValue() {
        if (state == null)
            state = tt.getState(hash, false);
        return state.imValue;
    }

    /**
     * @return The value of this node with respect to the parent
     */
    public double getValue(int player) {
        if (state == null)
            state = tt.getState(hash, true);

        return state.getMean(player);
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

    private State getState() {
        if (state == null)
            state = tt.getState(hash, false);
        return state;
    }

    public boolean isTerminal() {
        return children != null && children.size() == 0;
    }

    public String toString(IBoard board) {
        if(state != null)
            return board.getMoveString(move) + " - " + state.toString();
        else
            return board.getMoveString(move) + " no state";
    }
}
