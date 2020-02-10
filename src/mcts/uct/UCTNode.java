package mcts.uct;

import framework.*;
import mcts.State;
import mcts.TransposTable;

import java.util.LinkedList;
import java.util.List;

public class UCTNode {
    public final int player;
    public final int[] move;
    private final Options options;
    private final TransposTable tt;
    private final long hash;
    public List<UCTNode> children;
    // For debug only
    public String boardString;
    public Plot.Data timeSeries;
    public State state;
    private boolean expanded = false, simulated = false;
    private double[] RAVEvalue = {0, 0};
    private int RAVEVisits = 0;

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
        if (Options.debug)
            timeSeries = Plot.data();
    }

    public double[] MCTS(IBoard board, int depth) {
        assert board.getPlayerToMove() == player : "Incorrect player to move";
        // TODO Build in some more assertions here
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

        double[] result = {0, 0};
        // (Solver) Check for proven win / loss / draw
        if (!child.isSolved()) {
            // Execute the move represented by the child
            if (!isTerminal()) {
                if (options.RAVE)
                    options.addRAVEMove(player, board.getMoveId(child.move), depth);
                if (options.MAST)
                    options.addMASTMove(player, board.getMoveId(child.move));
                board.doMove(child.move);
                assert board.hash() == child.hash : "Board hash is incorrect";
            }
            // When a leaf is reached return the result of the playout
            if (!child.simulated || isTerminal()) {
                double[] poRes;
                options.nSamples = Math.round(options.nSamples);
                for (int i = 0; i < (int) options.nSamples; i++) {
                    if (options.nSamples > 1)
                        poRes = child.playOut(board.clone(), depth + 1);
                    else
                        poRes = child.playOut(board, depth + 1); // WARN a single copy of the board is used

                    result[0] += poRes[0];
                    result[1] += poRes[1];
                }
                child.updateStats(result, (int) options.nSamples);
                child.simulated = true;
            } else {
                result = child.MCTS(board, depth + 1);
                // Update the RAVE value if this move was played
                if (options.RAVE && !child.isSolved()) {
                    for (UCTNode c : children) {
                        // No need to update the selected child, only siblings
                        if (c.equals(child) || c.isSolved())
                            continue;

                        if (options.isRAVEMove(player, board.getMoveId(c.move), depth)) {
                            c.updateRAVE(result, (int) options.nSamples);
                        }
                    }
                }
            }
            if (!child.isSolved())
                updateStats(result, (int) options.nSamples);
            // For displaying the time-series charts
            if (Options.debug && depth == 0) {
                for (int i = 0; i < (int) options.nSamples; i++) {
                    child.timeSeries.xy(child.getVisits() - i, child.getValue(1));
                }
            }
        }
        //
        if (child.getValue(player) == Integer.MAX_VALUE) {
            // One of my children is a proven win
            setSolved(player);
            // Backprop a win
            result = new double[2];
            result[player - 1] = 1;
            result[(3 - player) - 1] = -1;
            return result;
        } else if (child.getValue(player) == Integer.MIN_VALUE) {
            result = new double[2];
            result[player - 1] = -1;
            result[(3 - player) - 1] = 1;
            // Check if all children are a proven loss
            for (UCTNode c : children) {
                if (c.getValue(player) != Integer.MIN_VALUE) {
                    // Return a single loss, if not all children are a loss
                    updateStats(result, 1);
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
        double best_imVal = Integer.MIN_VALUE;
        // Add all moves as children to the current node
        for (int i = 0; i < moves.size(); i++) {
            move = moves.get(i);
            IBoard tempBoard = board.clone();
            tempBoard.doMove(move);
            UCTNode child = new UCTNode(3 - player, move, options, tempBoard.hash(), tt);
            if (Options.debug)
                child.boardString = tempBoard.toString();
            // We've expanded an already proven won node
            if (child.isSolved() && child.getValue(player) == Integer.MAX_VALUE)
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

            if (options.imm) {
                double imVal = 0;
                if (child.getImValue(player) == Integer.MIN_VALUE) {
                    imVal = tempBoard.evaluate(player);
                    child.setImValue(imVal, player);
                } else // IM Value was already determined elsewhere in the tree
                    imVal = child.getImValue(player);
                best_imVal = Math.max(best_imVal, imVal);
            }
            children.add(child);
        }
        expanded = true;
        // Back-propagate the best IM value
        if (options.imm)
            setImValue(best_imVal, player);
        // If one of the nodes is a win, return it.
        return winNode;
    }

    private UCTNode select() {
        double maxIm = Integer.MIN_VALUE, minIm = Integer.MIN_VALUE;
        if (options.imm) {
            double val;
            // Check the highest and lowest evaluation for normalization
            for (UCTNode c : children) {
                val = c.getImValue(player);
                if (val > maxIm)
                    maxIm = val;
                if (val < minIm)
                    minIm = val;
            }
        }

        UCTNode selected = null;
        double max = Double.NEGATIVE_INFINITY;
        double uctValue, np = getVisits();
        // Select a child according to the UCT Selection policy
        for (UCTNode c : children) {
            double nc = c.getVisits(), val = c.getValue(player);
            // Always select a proven win
            if (val == Integer.MAX_VALUE)
                uctValue = Integer.MAX_VALUE - Options.r.nextDouble();
            else if (val == Integer.MIN_VALUE)
                uctValue = Integer.MIN_VALUE + Options.r.nextDouble();
            else if (nc == 0) {
                // First, visit all children at least once
                uctValue = 100. + Options.r.nextDouble();
            } else {
                // Linear regression
                if (options.regression) {
                    val = c.getValue(player, options.regForecastSteps, options.regAlpha); // TODO, this could also be nSamples
                }
                // Implicit minimax
                if (options.imm && minIm != maxIm) {
                    double imVal = (c.getImValue(player) - minIm) / (maxIm - minIm);
                    val = (1. - options.imAlpha) * val + (options.imAlpha * imVal);
                }

                if (options.RAVE && c.RAVEVisits > 1) {
                    double beta = Math.sqrt(options.k / ((3 * getVisits()) + options.k));
                    val = (beta * c.getRAVE(player)) + ((1 - beta) * val);
                }
                // Compute the uct value with the (new) average value
                uctValue = val + options.c * Math.sqrt(FastLog.log(np) / nc) + (Options.r.nextDouble() * 0.00001);
            }
            // Remember the highest UCT value
            if (uctValue > max) {
                selected = c;
                max = uctValue;
            }
        }
        return selected;
    }

    private double[] playOut(IBoard board, int depth) {
        int winner = board.checkWin(), nMoves = 0, moveIndex, pl;
        assert winner == IBoard.NONE_WIN || winner == IBoard.DRAW : "Board in won position in playout.";
        // int lastMove, winMove;
        int[] move;
        boolean interrupted = false;
        MoveList moves;
        double mastMax, mastVal, mastVis;
        board.startPlayout();

        do {
            moves = board.getPlayoutMoves(options.heuristics);
            pl = board.getPlayerToMove();
            // No more moves to be made
            if (moves.size() == 0)
                break;
            moveIndex = Options.r.nextInt(moves.size());
            if (options.MAST && Options.r.nextDouble() < (1. - options.epsilon)) {
                mastMax = Double.NEGATIVE_INFINITY;
                // Select the move with the highest MAST value
                for (int i = 0; i < moves.size(); i++) {
                    mastVis = options.getMASTVisits(pl, board.getMoveId(moves.get(i)));
                    mastVal = options.getMASTValue(pl, board.getMoveId(moves.get(i)));
                    // Make sure to have visited all moves first
                    if (mastVis < 5)
                        mastVal = 1. + Options.r.nextDouble();
                    // If bigger, we have a winner, if equal, flip a coin
                    if (mastVal > mastMax) {
                        mastMax = mastVal;
                        moveIndex = i;
                    }
                }
            }

            move = moves.get(moveIndex);

            ++nMoves;

            if (options.RAVE) // Insert the rave and mast move before the play is made to get the correct player to move
                options.addRAVEMove(pl, board.getMoveId(move), depth + nMoves);
            if (options.MAST)
                options.addMASTMove(pl, board.getMoveId(move));

            board.doMove(move);
            winner = board.checkWin();
            // Interrupt playout for early evaluation
            if (winner == IBoard.NONE_WIN && options.earlyTerm && nMoves == options.termDepth)
                interrupted = true;

        } while (winner == IBoard.NONE_WIN && !interrupted);

        double[] score = {0, 0};

        if (options.earlyTerm) {
            if (!interrupted) {
                score[winner - 1] += options.etWv;
                score[(3 - winner) - 1] -= options.etWv;
            } else {
                double eval = board.evaluate(1); // TODO - This value should be between 0 and 1
                score[0] = eval;
                score[1] = -eval;
            }
        } else if (winner != IBoard.DRAW) {
            score[winner - 1] = 1;
            score[(3 - winner) - 1] = -1;
        }

        if (options.MAST)
            options.updateMASTMoves(score);

        return score;
    }

    public UCTNode getBestChild() {
        if (children == null)
            return null;
        double max = Double.NEGATIVE_INFINITY, value;
        UCTNode bestChild = null;
        for (UCTNode c : children) {
            // If there are children with INF value, choose one of them
            if (c.getValue(player) == Integer.MAX_VALUE)
                value = Integer.MAX_VALUE - Options.r.nextDouble();
            else if (c.getValue(player) == Integer.MIN_VALUE)
                value = Integer.MIN_VALUE + c.getVisits() + Options.r.nextDouble();
            else {
                if (!options.maxChild)
                    value = c.getVisits();
                else {
                    value = c.getValue(player);
                }
            }
            if (value > max) {
                max = value;
                bestChild = c;
            }
        }
        return bestChild;
    }

    private void updateStats(double[] value, int n) {
        assert value[0] > Integer.MIN_VALUE && value[1] > Integer.MIN_VALUE : "Wrong values in updateStats";

        if (state == null)
            state = tt.getState(hash, false);
        //
        state.updateStats(value, n, options.regression);

        // implicit minimax backups
        if (options.imm && children != null) {
            double bestVal = Integer.MIN_VALUE;
            for (UCTNode c : children) {
                if (c.getImValue(player) > bestVal)
                    bestVal = c.getImValue(player);
            }
            setImValue(bestVal, player);
        }
    }

    public boolean isSolved() {
        if (state == null)
            state = tt.getState(hash, true);

        if (state == null)
            return false;

        return state.isSolved();
    }

    private void setSolved(int player) {
        if (state == null)
            state = tt.getState(hash, false);

        state.setSolved(player);
    }

    private double getImValue(int player) {
        if (state == null)
            state = tt.getState(hash, true);

        if (state == null)
            return Integer.MIN_VALUE;

        return state.getImValue(player);
    }

    private void setImValue(double imValue, int player) {
        if (state == null)
            state = tt.getState(hash, false);

        state.setImValue(imValue, player);
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

    /**
     * @return The value of this node with respect to the parent
     */
    public double getValue(int player, int regSteps, double regAlpha) {
        if (state == null)
            state = tt.getState(hash, true);

        if (state == null)
            return 0;

        return state.getMean(player, regSteps, regAlpha);
    }

    public void updateRAVE(double[] values, int n) {
        RAVEvalue[0] += values[0];
        RAVEvalue[1] += values[1];
        RAVEVisits += n;
    }

    public double getRAVE(int player) {
        if (RAVEVisits > 0)
            return RAVEvalue[player - 1] / RAVEVisits;
        else
            return 0;
    }

    public double getVisits() {
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

        if (RAVEVisits > 0) {
            sb.append("\t :: RV_p1: ").append(State.df2.format(getRAVE(1)));
        }
        return sb.toString();
    }

    public String toString(IBoard board) {

        StringBuilder sb = new StringBuilder();
        sb.append(board.getMoveString(move));
        if (state != null)
            sb.append("\t :: ").append(state.toString());
        else
            sb.append(" :: ").append(" no state");
        if (RAVEVisits > 0) {
            sb.append("\t :: RV_p1: ").append(State.df2.format(getRAVE(1)));
        }

        return sb.toString();
    }
}
